package io.jactl.intellijplugin;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import io.jactl.*;
import io.jactl.intellijplugin.common.JactlPlugin;
import io.jactl.intellijplugin.psi.JactlTokenType;
import io.jactl.intellijplugin.psi.JactlTokenTypes;
import io.jactl.runtime.BuiltinFunctions;
import io.jactl.runtime.ClassDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

import static io.jactl.TokenType.EOL;
import static io.jactl.TokenType.WHITESPACE;

public class JactlTokeniser extends Lexer {

  public static final Logger LOG = Logger.getInstance(JactlTokeniser.class);

  IElementType EOF = JactlTokenTypes.EOF;  // Force initialisation of token types

  CharSequence        bufferSequence;
  Tokeniser           tokeniser;
  IElementType        current;
  int                 startOffset;
  int                 endOffset;
  JactlTokenBuilder   jactlBuilder;
  Stmt.ClassDecl      script;
  JactlContext        jactlContext;
  ListIterator<Token> tokenIter = null;
  Token               nextToken = null;
  int                 tokenStart;
  int                 tokenEnd;
  Project             project;

  public JactlTokeniser(Project project) {
    this.project = project;
  }

  @Override
  public void start(@NotNull CharSequence charSequence, int startOffset, int endOffset, int initialState) {
    tokenise(charSequence, startOffset, endOffset);
    nextToken = null;
    for (tokenIter = jactlBuilder.getEvents().stream().filter(JactlTokenBuilder.Event::isToken).map(evt -> ((JactlTokenBuilder.TokenEvent) evt).token).toList().listIterator();
         tokenIter.hasNext(); ) {
      advance();
      if (nextToken.getOffset() >= startOffset) {
        break;
      }
    }
  }

  public void tokenise(CharSequence charSequence, int startOffset, int endOffset) {
    //System.out.println("DEBUG: JactlTokeniser.tokenise(this=" + System.identityHashCode(this) + ", startOffset=" + startOffset + ", endOffset=" + endOffset + "): text=<" + System.identityHashCode(charSequence) + ">");
    bufferSequence   = charSequence;
    this.startOffset = startOffset;
    this.endOffset   = endOffset;
    tokeniser        = new Tokeniser(charSequence.toString(), true);
    jactlBuilder     = new JactlTokenBuilder(tokeniser);
    jactlContext     = createJactlContext();

    // We don't know directory or file name so for the moment use dummy package/class names
    Parser parser    = new Parser(jactlBuilder, jactlContext, "");
    script           = parser.parseScriptOrClass("test");
  }

  public CharSequence             getText()         { return bufferSequence; }
  public Stmt.ClassDecl           getJactl()        { return script; }
  public JactlContext             getJactlContext() { return jactlContext; }
  public List<JactlTokenBuilder.Event> getEvents()       { return jactlBuilder.events; }
  @Override public int            getState()        { return 0; }
  @Override public IElementType   getTokenType()    { return current; }
  @Override public int            getTokenStart()   { return tokenStart; }
  @Override public int            getTokenEnd()     { return tokenEnd; }

  @Override public void advance() {
    try {
      if (nextToken == null || nextToken.getOffset() < endOffset) {
        if (!tokenIter.hasNext()) {
          LOG.error("tokenIter.hasNext() is false: nextToken=" + nextToken + " (at offset " + nextToken.getOffset() + "), endOffset=" + endOffset);
        }
        nextToken  = tokenIter.next();
        current    = JactlTokenType.getIElementType(nextToken.is(EOL,WHITESPACE) ? WHITESPACE : nextToken.getType());
        tokenStart = nextToken.getOffset();
        // Merge EOLs and whitespace into one whitespace token since this is what Intellij expects for code formatting purposes
        if (nextToken.is(EOL,WHITESPACE) && tokenIter.hasNext()) {
          while (tokenIter.hasNext()) {
            nextToken = tokenIter.next();
            if (!nextToken.is(EOL,WHITESPACE)) {
              tokenIter.previous();
              tokenIter.previous();
              nextToken = tokenIter.next();
              break;
            }
          }
        }
        tokenEnd = nextToken.getOffset() + nextToken.getChars().length();
      }
      if (nextToken.getOffset() >= endOffset) {
        current = EOF;
      }
      if (current == EOF) {
        current = null;
      }
      else if (nextToken.is(io.jactl.TokenType.ERROR)) {
        current = TokenType.BAD_CHARACTER;
      }
    }
    catch (JactlError error) {
      current = TokenType.BAD_CHARACTER;
    }
  }

  @Override public @NotNull LexerPosition getCurrentPosition() {
    return new JactlLexerPosition();
  }

  @Override public void restore(@NotNull LexerPosition lexerPosition) {
    throw new UnsupportedOperationException("restore not supported");
//    JactlLexerPosition pos = (JactlLexerPosition)lexerPosition;
//    tokeniser.rewind(pos.saved);
  }

  @Override public @NotNull CharSequence getBufferSequence() {
    return bufferSequence;
  }

  @Override public int getBufferEnd() {
    return endOffset;
  }

  private class JactlLexerPosition implements LexerPosition {
    int offset;
    JactlLexerPosition() {
      offset = tokeniser.peek().getOffset();
    }
    @Override public int getOffset() {
      return offset + startOffset;
    }
    @Override public int getState() {
      return 0;
    }
  }

  private JactlContext createJactlContext() {
    if (project == null) {
      return JactlContext.create().evaluateConstExprs(false).build();
    }
    String baseJavaPkg = JactlPlugin.BASE_JACTL_PKG;
    String baseJavaPkgFile = baseJavaPkg.replace('.', File.separatorChar);
    return JactlContext.create()
                       .javaPackage(baseJavaPkg)
                       .evaluateConstExprs(false)
                       .idePlugin(true)
                       .packageChecker(pkgName -> {
                         var pkgs = JactlUtils.pkgNames(project);
                         return pkgs.contains(pkgName);
                       })
                       .classLookup(name -> lookup(name, baseJavaPkgFile, project))
                       .build();
  }

  private ClassDescriptor lookup(String name, String baseJavaPkgFile, Project project) {
    if (name.startsWith(baseJavaPkgFile)) {
      name = name.substring(baseJavaPkgFile.length() + 1);
    }
    else {
      LOG.warn("Class lookup name does not start with expected java package name (name=" + name + ")");
    }

    var file = JactlUtils.findFileForClassPath(project, name);
    if (file == null) {
      return null;
    }

    // Strip package name
    int    idx       = name.lastIndexOf('/');
    String className = idx == -1 ? name : name.substring(idx + 1);
    if (className.startsWith(JactlPlugin.SCRIPT_PREFIX)) {
      className = className.substring(JactlPlugin.SCRIPT_PREFIX.length());
      className.replace('$', '.');               // Turn A$B$C back into A.B.C
      className = JactlPlugin.SCRIPT_PREFIX + className;
    }
    else {
      className.replace('$', '.');               // Turn A$B$C back into A.B.C
    }
    Stmt.ClassDecl classDecl = JactlParserAdapter.getClassDecl(file, file.getText(), className);
    if (classDecl == null) {
      return null;
    }
    return classDecl.classDescriptor;
  }
}
