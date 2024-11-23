package io.jactl.intellijplugin;

import com.intellij.codeInsight.completion.CompletionUtilCore;
import com.intellij.lang.*;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.FileContentUtil;
import io.jactl.*;
import io.jactl.intellijplugin.common.JactlPlugin;
import io.jactl.intellijplugin.extensions.debugger.JactlCodeFragment;
import io.jactl.intellijplugin.jpsplugin.builder.GlobalsException;
import io.jactl.intellijplugin.psi.*;
import io.jactl.resolver.Resolver;
import io.jactl.runtime.ClassDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.jactl.intellijplugin.psi.JactlListElementType.LIST;

public class JactlParserAdapter implements PsiParser {

  public static final Logger LOG = Logger.getInstance(JactlParserAdapter.class);

  private static final Key<Long>         LAST_REFRESH = Key.create("JACTL_LAST_REFRESH");
  private static final Key<ParsedScript> PARSED_SCRIPT = Key.create("JACTL_PARSED_SCRIPT");

  private static final int CACHE_SIZE = 100;

  private static final Map<JactlFile,ParsedScript> parsedScripts = new LinkedHashMap<JactlFile, ParsedScript>(CACHE_SIZE * 2, 0.75f, true) {
    @Override protected boolean removeEldestEntry(Map.Entry<JactlFile,ParsedScript> eldest) { return size() > CACHE_SIZE; }
  };

  private Project project;

  public JactlParserAdapter(Project project) {
    this.project = project;
  }

  @Override
  public @NotNull ASTNode parse(IElementType root, @NotNull PsiBuilder builder) {
    ProgressIndicatorProvider.checkCanceled();
    //builder.setDebugMode(true);

    JactlTokeniser tokeniser = (JactlTokeniser)((PsiBuilderImpl)builder).getLexer();
    PsiFile        userData  = builder.getUserData(FileContextUtil.CONTAINING_FILE_KEY);
    ParsedScript   parsedScript;
    if (userData instanceof DummyHolder) {
      // Used when parsing evaluation expressions in debugger
      parsedScript = parse(tokeniser, null, builder);
      parsedScript.resolve(project, tokeniser.getJactl(), userData.getContext());
    }
    else {
      JactlFile file = userData instanceof JactlFile ? (JactlFile) userData : null;
      parsedScript = parseAndResolve(project, tokeniser, file, builder);
    }

    ASTNode node = builder.getTreeBuilt();          // calls the ASTFactory.createComposite() etc...
    node.putUserData(PARSED_SCRIPT, parsedScript);  // cache for completions etc where we don't need to store for longer
    return node;
  }

  private static ParsedScript parseAndResolve(Project project, JactlTokeniser tokeniser, JactlFile jactlFile, PsiBuilder builder) {
    if (jactlFile != null) {
      jactlFile = (JactlFile) jactlFile.getOriginalFile();
    }

    ParsedScript parsed = parse(tokeniser, jactlFile, builder);

    String sourceCode = tokeniser.getBufferSequence().toString();
    synchronized (parsedScripts) {
      if (jactlFile != null && !sourceCode.contains(CompletionUtilCore.DUMMY_IDENTIFIER)) {
        parsedScripts.put(jactlFile, parsed);
      }
    }

    if (jactlFile instanceof JactlCodeFragment) {
      parsed.resolve(project, tokeniser.getJactl(), jactlFile.getContext());
    }
    else {
      parsed.resolve(project, jactlFile);
    }
    return parsed;
  }

  private static ParsedScript parse(JactlTokeniser tokeniser, JactlFile file, PsiBuilder builder) {
    ParsedScript parsed = new ParsedScript(tokeniser.getJactl(), tokeniser.getJactlContext(), tokeniser.getBufferSequence().toString());

    //List<JactlTokenBuilder.Event> events = tokeniser.getEvents().stream().filter(e -> !e.isDropped()).collect(Collectors.toList());
    List<JactlTokenBuilder.Event> events = tokeniser.getEvents();
    for (int i = 0; i < events.size(); i++) {
      JactlTokenBuilder.Event event = events.get(i);
      if (event.isDropped()) {
        continue;
      }
      if (event.isToken()) {
        if (builder != null && !event.getToken().isCommentOrWhiteSpace()) {
          builder.advanceLexer();
        }
        continue;
      }
      JactlTokenBuilder.MarkerEvent markerEvent = (JactlTokenBuilder.MarkerEvent) event;
      JactlTokenBuilder.JactlMarker marker      = markerEvent.getMarker();

      if (marker.type == JactlExprElementType.CLOSURE && ((Expr.Closure) marker.astNode).closureIsBlock) {
        // Closure was turned into a block so ignore it
        continue;
      }
      if (markerEvent.state == JactlTokenBuilder.MarkerEvent.State.START) {
        marker.psiMarker = builder == null ? null : builder.mark();
        if (!markerEvent.isToken()) {
          // Find next non-whitespace/non-comment token in order to get offset
          marker.offset = -1;
          for (int j = i; j < events.size(); j++) {
            JactlTokenBuilder.Event token = events.get(j);
            if (token.isToken() && !token.getToken().isCommentOrWhiteSpace()) {
              marker.offset = token.getToken().getOffset();
              break;
            }
          }
          if (marker.offset == -1) {
            throw new IllegalStateException("Couldn't find a token after " + markerEvent);
          }

          if (marker.type == JactlStmtElementType.FUN_DECL ||
              marker.type == JactlStmtElementType.CLASS_DECL && marker.astNode instanceof Stmt.ClassDecl && !((Stmt.ClassDecl) marker.astNode).isScriptClass()) {
            // Find first name as this will be location we want to jump to when jumping to declaration
            marker.nameKey = IntStream.range(i, events.size())
                                      .mapToObj(events::get)
                                      .filter(e -> e.isStart() && !e.isToken() && e.getMarker().type instanceof JactlNameElementType)
                                      .map(e -> new JactlAstKey(file, e.getMarker().type, e.getMarker().offset))
                                      .findFirst()
                                      .orElse(null);
          }
        }
      }
      else {
        if (marker.psiMarker != null && marker.error != null) {
          marker.psiMarker.error(marker.error.getErrorMessage());
          marker.doneFlagged = true;
        }
        else {
          parsed.addASTNode(file, marker.type, marker.offset, marker.astNode);

          // Special case for Stmt.VarDecl and Stmt.FunDecl since resolver resolves to the Expr.VarDecl inside it we need
          // to make the Expr.VarDecl point back to the Stmt.VarDecl
          if (marker.astNode instanceof Stmt.VarDecl) {
            Stmt.VarDecl stmt = (Stmt.VarDecl) marker.astNode;
            stmt.declExpr.setUserData(new JactlAstKey(file, marker.type, marker.offset));
          }
          else if (marker.astNode instanceof Stmt.FunDecl) {
            Stmt.FunDecl stmt = (Stmt.FunDecl) marker.astNode;
            if (marker.nameKey == null) {
              LOG.warn("No name found for function declaration: funDecl=" + marker.astNode);
            }
            stmt.declExpr.varDecl.setUserData(marker.nameKey);
          }
          else if (marker.astNode instanceof Stmt.ClassDecl && !((Stmt.ClassDecl) marker.astNode).isScriptClass()) {
            Stmt.ClassDecl classDecl = (Stmt.ClassDecl) marker.astNode;
            if (marker.nameKey == null) {
              LOG.warn("No name found for class declaration: classDecl=" + marker.astNode);
            }
            classDecl.setUserData(marker.nameKey);
          }
          if (marker.psiMarker != null) {
            marker.psiMarker.done(marker.type);
            marker.doneFlagged = true;
          }
        }
      }
    }

    return parsed;
  }

  public static JactlUserDataHolder getJactlAstNode(JactlPsiElement element) {
    return getJactlAstNode(element.getFile(), element.getSourceCode(), element.getAstKey());
  }

  @NotNull
  private static ParsedScript getParsedScript(JactlPsiElement element) {
    ParsedScript parsedScript = element.getFile().getUserData(PARSED_SCRIPT);
    if (parsedScript != null) {
      return parsedScript;
    }
    return getParsedScript(element.getFile(), element.getSourceCode());
  }

  private static ParsedScript getParsedScript(JactlFile file, String sourceCode) {
    ParsedScript parsed;
    synchronized (parsedScripts) {
      parsed = parsedScripts.get(file);
      if (parsed == null || !parsed.getSourceCode().equals(sourceCode)) {
        JactlTokeniser tokeniser = new JactlTokeniser(file.getProject());
        tokeniser.tokenise(sourceCode, 0, sourceCode.length());
        parsed = parseAndResolve(file.getProject(), tokeniser, file, null);
      }
    }
    return parsed;
  }

  public static final class FieldDescriptor {
    private final String    name;
    private final JactlType type;
    private final boolean   isStatic;
    public FieldDescriptor(String name, JactlType type, boolean isStatic) {
      this.name = name;
      this.type = type;
      this.isStatic = isStatic;
    }
    public String name()      { return name; }
    public JactlType type()   { return type; }
    public boolean isStatic() { return isStatic; }
  }

  /**
   * Return list of variables/fields (as FieldDescriptor) and functions (FunctionDescriptors)
   * visible at the given element
   * @param element    the element
   * @return the list of variables/fields
   */
  public static List<Object> getVariablesAndFunctions(JactlPsiElement element) {
    return getVariablesAndFunctions(element, element);
  }

  public static List<Object> getVariablesAndFunctions(JactlPsiElement context, JactlPsiElement element) {
    ParsedScript parsedScript = getParsedScript(context);
    return parsedScript.getVariablesAndFunctions(context, element);
  }

  /**
   * Get the Stmt.ClassDecl for given class in given file
   * @param file        the JactlFile
   * @param sourceCode  the source code
   * @param className   the class name (A$B$C) without package name
   * @return
   */
  public static Stmt.ClassDecl getClassDecl(JactlFile file, String sourceCode, String className) {
    return getParsedScript(file, sourceCode).getClassDecl(className);
  }

  public static JactlUserDataHolder getJactlAstNode(JactlFile file, String sourceCode, JactlAstKey astKey) {
    ParsedScript        parsed = getParsedScript(astKey.getFile(), sourceCode);
    JactlUserDataHolder result = parsed.getJactlAstNode(astKey);
    return result;
  }

  public static List<ClassDescriptor> getClasses(JactlPsiElement element) {
    return getParsedScript(element).getClasses(element);
  }

  public static ClassDescriptor getClass(JactlPsiElement element) {
    ParsedScript parsedScript = getParsedScript(element);
    if (parsedScript == null) {
      return null;
    }
    return parsedScript.getClass(element.getAstKey());
  }

  public static List<String> getErrors(JactlFile file, String sourceCode, ASTNode node) {
    return getParsedScript(file, sourceCode).getErrors(node);
  }

  public static Stmt.ClassDecl getClassDecl(Project project, String fqClassName) {
    JactlFile file = JactlUtils.findFileForClass(project, fqClassName);
    if (file == null) {
      return null;
    }
    return getClassDecl(file, file.getText(), JactlPlugin.removePackage(fqClassName));
  }

  public static boolean isImported(JactlPsiElement className) {
    ParsedScript parser = getParsedScript(className.getFile(), className.getSourceCode());
    return parser.resolver.getImports().containsKey(className.getText());
  }

  //////////////////////////////////////////////////

  private static class ParsedScript {
    String                                sourceCode;
    Map<JactlAstKey, JactlUserDataHolder> jactlAstNodes = new HashMap<>();
    Stmt.ClassDecl                        jactlAst;
    JactlContext                          jactlContext;
    Resolver                              resolver;
    GlobalsException                      globalsError;
    int                                   firstAstNodeOffset = Integer.MAX_VALUE;
    Map<Integer, List<String>>            errors        = new HashMap<>();

    ParsedScript(Stmt.ClassDecl jactlAst, JactlContext jactlContext, String sourceCode) {
      this.jactlAst = jactlAst;
      this.jactlContext = jactlContext;
      this.sourceCode = sourceCode;
    }

    public String getSourceCode() {
      return sourceCode;
    }

    public void addASTNode(JactlFile file, IElementType type, int offset, JactlUserDataHolder node) {
      JactlAstKey key = new JactlAstKey(file, type, offset);
      if (offset < firstAstNodeOffset) {
        firstAstNodeOffset = offset;
      }
      jactlAstNodes.put(key, node);
      if (node != null) {
        node.setUserData(key);
        return;
      }
      if (type != null && type != LIST) {
        LOG.warn("Node is null (type=" + type + ", offset=" + offset + ")");
      }
    }

    public JactlUserDataHolder getJactlAstNode(JactlAstKey astKey) {
      JactlUserDataHolder result = jactlAstNodes.get(astKey);
      if (result == null) {
        LOG.info("Returning null for AST Node: key=" + astKey);
      }
      return result;
    }

    /**
     * Return the Stmt.ClassDecl for given class name within the file
     * @param name  the class name (A$B$C)
     * @return the Stmt.ClassDecl or null
     */
    public Stmt.ClassDecl getClassDecl(String name) {
      return resolver.getClassDecl(name);
    }

    public List<Object> getVariablesAndFunctions(JactlPsiElement context, JactlPsiElement element) {
      JactlAstKey astKey = context.getAstKey();
      JactlUserDataHolder astNode = getJactlAstNode(astKey);
      if (astNode == null) {
        return Collections.EMPTY_LIST;
      }
      Stmt.Block block = astNode.getBlock();

      // Get location from element
      Token location = element.getJactlAstNode().getLocation();

      Set<Object> names = new HashSet<>();
      List<Object> result = new ArrayList<>();

      BiConsumer<String,Object> addResult = (name,value) -> {
        if (!names.contains(name)) {
          result.add(value);
          names.add(name);
        }
      };

      // Get all variables in current block declared before us.
      // If we are inside a VarDecl then we need to use its location to avoid returning the
      // variable name of the variable being declared.
      if (astNode instanceof Expr.Identifier) {
        JactlPsiElement varDeclPsi = (JactlPsiElement) JactlUtils.getAncestor(element, JactlStmtElementType.VAR_DECL);
        location = varDeclPsi == null ? location : varDeclPsi.getJactlAstNode().getLocation();
      }
      final Token finalLocation = location;
      if (block != null) {
        // Iterator up through all enclosing blocks and add vars/functions that were already declared before reference
        for (Stmt.Block parent = block; parent != null; parent = parent.enclosingBlock) {
          Stream.concat(parent.variables.values().stream().filter(v -> Utils.isEarlier(v.location, finalLocation)),
                        parent.functions.stream().map(f -> f.declExpr.varDecl))
                .filter(v -> !v.name.getStringValue().startsWith(Utils.JACTL_PREFIX))
                .forEach(v -> addResult.accept(v.name.getStringValue(), v));
        }

        // Get class that owns this block so that we can add fields/methods of the
        // class and its base classes
        ClassDescriptor desc = block.owningClass.classDescriptor;
        desc.getAllFields()
            .entrySet()
            .stream()
            .map(e -> new FieldDescriptor(e.getKey(), e.getValue(), false))
            .forEach(fd -> addResult.accept(fd.name, fd));
        desc.getAllStaticFields()
            .entrySet()
            .stream()
            .map(e -> new FieldDescriptor(e.getKey(), e.getValue().first, true))
            .forEach(fd -> addResult.accept(fd.name, fd));

        desc.getAllMethods()
            .filter(entry -> !entry.getKey().startsWith(Utils.JACTL_PREFIX))
            .forEach(entry -> addResult.accept(entry.getKey(), entry.getValue()));
      }

      return result;
    }

    public List<ClassDescriptor> getClasses(JactlPsiElement element) {
      // Make sure we get element that is not a type since we can't set block on types as
      // they are shared
      element = JactlUtils.getJactlPsiParent(element);
      Stmt.Block block = getBlock(element.getAstKey());
      if (block == null) {
        return Collections.EMPTY_LIST;
      }
      return getClasses(element.getProject(), block);
    }

    public ClassDescriptor getClass(JactlAstKey astKey) {
      Stmt.Block block = getBlock(astKey);
      if (block == null) {
        return null;
      }
      return block.owningClass.classDescriptor;
    }

    private Stmt.Block getBlock(JactlAstKey astKey) {
      JactlUserDataHolder astNode = getJactlAstNode(astKey);
      if (astNode == null) {
        return null;
      }
      Stmt.Block block = astNode.getBlock();
      if (block == null && astNode instanceof Stmt.FunDecl) {
        Stmt.FunDecl funDecl = (Stmt.FunDecl) astNode;
        block = funDecl.declExpr.block;
      }
      return block;
    }

    private List<ClassDescriptor> getClasses(Project project, Stmt.Block block) {
      List<ClassDescriptor> result = new ArrayList<>();
      Set<String> names = new HashSet<>();

      Consumer<ClassDescriptor> addClass = desc -> {
        if (!names.contains(desc.getPackagedName())) {
          names.add(desc.getPackagedName());
          result.add(desc);
        }
      };

      if (block != null) {
        // Add any class names from enclosing blocks and any inner classes of them or their base classes
        for (ClassDescriptor clss = block.owningClass.classDescriptor; clss != null; clss = clss.getEnclosingClass()) {
          for (ClassDescriptor clss2 = clss; clss2 != null; clss2 = clss2.getBaseClass()) {
            if (!clss2.isScriptClass()) {
              addClass.accept(clss2);
            }
            clss2.getInnerClasses().forEach(addClass);
          }
        }
      }

      synchronized (this) {
        resolver.getImports().values().forEach(addClass);
      }

      // Find package we are in and add any classes belonging to the same package
      String packageName = block.owningClass.packageName;
      JactlUtils.packageClasses(project, packageName).forEach(addClass);
      return result;
    }

    public List<String> getErrors(ASTNode node) {
      // Find innermost node at same offset and only report errors on that one to avoid
      // multiple errors at same offset
      int offset = node.getStartOffset();
      ASTNode firstChildNode = node.getFirstChildNode();
      if (firstChildNode != null && firstChildNode.getStartOffset() == offset) {
        return Collections.EMPTY_LIST;
      }
      if (offset == firstAstNodeOffset && globalsError != null) {
        // Any errors in globals we show at first AST node
        ArrayList<String> errs = new ArrayList<>();
        errs.add("Error in globals script '" + globalsError.getGlobalsScriptPath() + "': " + globalsError.getMessage());
        errs.addAll(errors.getOrDefault(offset, Collections.EMPTY_LIST));
        return errs;
      }
      return errors.getOrDefault(offset, Collections.EMPTY_LIST);
    }

    public void resolve(Project project, JactlFile file) {
      boolean globalsFile = JactlUtils.isGlobalsFile(file);
      Map<String,Object> globals = getGlobals(project);
      resolver = new Resolver(jactlContext, globals, jactlAst.location);
      String packageName = JactlUtils.packageNameFor(file);
      if (packageName == null && !globalsFile && file != null && !file.getName().equals(JactlUtils.CODE_FRAGMENT_FILE_NAME)) {
        errors.putIfAbsent(0, new ArrayList<>());
        errors.get(0).add("File exists outside configured source roots");
        return;
      }
      String scriptName = file == null ? "Dummy" : JactlPlugin.removeSuffix(file.getName());
      if (jactlAst.isScriptClass()) {
        scriptName = JactlPlugin.SCRIPT_PREFIX + scriptName;
      }
      resolver.resolveScriptOrClass(jactlAst, false, scriptName, packageName).forEach(e -> {
        int offset = e.getLocation().getOffset();
        errors.putIfAbsent(offset, new ArrayList<>());
        errors.get(offset).add(e.getErrorMessage());
      });
      if (globalsFile) {
        Long lastRefresh = file.getUserData(LAST_REFRESH);
        if (lastRefresh == null || file.getModificationStamp() > lastRefresh) {
          file.putUserData(LAST_REFRESH, file.getModificationStamp());
          // Get all open files (excluding the globals script) and reparse in case they depend on any globals
          List<VirtualFile> openFiles = Stream.of(FileEditorManager.getInstance(project).getOpenFiles()).filter(vf -> !vf.equals(file.getVirtualFile())).collect(Collectors.toList());
          ApplicationManager.getApplication().invokeLater(() -> FileContentUtil.reparseFiles(project, openFiles, false));
        }
      }
    }

    // For resolving debugger evaluation expressions for completions
    public void resolve(Project project, Stmt.ClassDecl scriptClass, PsiElement context) {
      JactlPsiElement parent = JactlUtils.getJactlPsiParent(context);
      if (parent == null) {
        LOG.warn("Could not find JactlPsiElement parent for " + context);
        return;
      }
      Map<String,Object> globals = getGlobals(project);
      resolver = new Resolver(jactlContext, globals, jactlAst.location);

      // Turn expression into a called closure so we can resolve within context of script we are debugging
      Stmt.Block scriptBlock = scriptClass.scriptMain.declExpr.block;
      Stmt.Stmts stmts       = new Stmt.Stmts(scriptBlock.openBrace);
      Stmt.Block newBlock    = new Stmt.Block(scriptBlock.openBrace, stmts);
      Predicate<Stmt> notGlobalDecl = s -> !(s instanceof Stmt.VarDecl) || !((Stmt.VarDecl)s).name.getStringValue().equals(Utils.JACTL_GLOBALS_NAME);
      // Strip out the VarDecl for _j$$globals
      stmts.stmts.addAll(scriptBlock.stmts.stmts.stream().filter(notGlobalDecl).collect(Collectors.toList()));
      Expr                closure      = Parser.convertBlockToInvokedClosure(newBlock);
      JactlUserDataHolder jactlAstNode = parent.getJactlAstNode();
      Stmt.Block          contextBlock = jactlAstNode.getBlock();
      resolver.resolveExpr(contextBlock, closure, jactlAstNode.getLocation()).forEach(e -> {
        int offset = e.getLocation().getOffset();
        errors.putIfAbsent(offset, new ArrayList<>());
        errors.get(offset).add(e.getErrorMessage());
      });
    }

    private Map<String,Object> getGlobals(Project project) {
      globalsError = null;
      try {
        return JactlUtils.getGlobals(project);
      }
      catch (GlobalsException e) {
        globalsError = e;
      }
      return Collections.EMPTY_MAP;
    }
  }
}

