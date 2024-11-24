package io.jactl.intellijplugin;

import com.intellij.lang.*;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.psi.tree.TokenSet;
import io.jactl.intellijplugin.psi.JactlNameElementType;
import io.jactl.intellijplugin.psi.JactlTokenTypes;
import io.jactl.intellijplugin.psi.JactlTypes;
import io.jactl.intellijplugin.psi.impl.JactlPsiIdentifierImpl;
import org.jetbrains.annotations.NotNull;

public class JactlParserDefinition extends DefaultASTFactoryImpl implements ParserDefinition {
  public static final IFileElementType JACTL_FILE_ELEMENT_TYPE = JactlNameElementType.JACTL_FILE;

  public static final Logger LOG = Logger.getInstance(JactlTypes.class);

  @NotNull
  @Override
  public Lexer createLexer(Project project) {
    return new JactlTokeniser(project);
  }

  @NotNull
  @Override
  public TokenSet getCommentTokens() {
    return JactlTokenSets.COMMENT;
  }

  @Override
  public @NotNull TokenSet getWhitespaceTokens() {
    return JactlTokenSets.WHITESPACE;
  }

  @NotNull
  @Override
  public TokenSet getStringLiteralElements() {
    return JactlTokenSets.STRING_LITERALS;
  }

  @NotNull
  @Override
  public PsiParser createParser(final Project project) {
    JactlParserAdapter parser = new JactlParserAdapter(project);
    //System.out.println("DEBUG: new parser=" + System.identityHashCode(parser));
    return parser;
  }

  @NotNull
  @Override
  public IFileElementType getFileNodeType() {
    return JACTL_FILE_ELEMENT_TYPE;
  }

  @NotNull
  @Override
  public PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    JactlFile file = new JactlFile(viewProvider);
    //System.out.println("DEBUG: new file=" + System.identityHashCode(file));
    return file;
  }

//  @Override
//  public @NotNull LazyParseableElement createLazy(@NotNull ILazyParseableElementType type, CharSequence text) {
//    if (type instanceof IFileElementType) {
//      return new JactlFileElement(text);
//    }
//
//    return super.createLazy(type, text);
//  }

  @NotNull
  @Override
  public PsiElement createElement(ASTNode node) {
    PsiElement elem = JactlTypes.Factory.createElement(node);
    return elem;
  }

  @Override
  public @NotNull LeafElement createLeaf(@NotNull IElementType type, @NotNull CharSequence text) {
    if (type == JactlTokenTypes.IDENTIFIER) {
      return new JactlPsiIdentifierImpl(type, text);
    }
    return super.createLeaf(type, text);
  }

  @Override
  public @NotNull CompositeElement createComposite(@NotNull IElementType type) {
    if (type instanceof IFileElementType || type == JactlNameElementType.JACTL_FILE) {
      return new FileElement(type, null);
    }

    return new JactlCompositeElement(type);
  }
}
