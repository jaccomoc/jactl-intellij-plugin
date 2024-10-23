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
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.FileContentUtil;
import io.jactl.*;
import io.jactl.intellijplugin.common.JactlPlugin;
import io.jactl.intellijplugin.jpsplugin.builder.GlobalsException;
import io.jactl.intellijplugin.extensions.settings.JactlConfiguration;
import io.jactl.intellijplugin.psi.*;
import io.jactl.resolver.Resolver;
import io.jactl.runtime.ClassDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.jactl.intellijplugin.psi.JactlListElementType.LIST;

public class JactlParserAdapter implements PsiParser {

  public static final Logger LOG = Logger.getInstance(JactlParserAdapter.class);

  private static final Key<Long> LAST_REFRESH = Key.create("LAST_REFRESH");

  private static final int CACHE_SIZE = 100;

  private static final Map<JactlFile,ParsedScript> parsedScripts = new LinkedHashMap<>(CACHE_SIZE * 2, 0.75f, true) {
    @Override protected boolean removeEldestEntry(Map.Entry<JactlFile,ParsedScript> eldest) { return size() > CACHE_SIZE; }
  };

  private Project project;

  public JactlParserAdapter(Project project) {
    this.project = project;
  }

  @Override
  public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
    ProgressIndicatorProvider.checkCanceled();

    JactlFile file = (JactlFile)builder.getUserData(FileContextUtil.CONTAINING_FILE_KEY);

    builder.setDebugMode(true);

    JactlTokeniser tokeniser = (JactlTokeniser)((PsiBuilderImpl)builder).getLexer();
    parse(tokeniser, file, builder);
    return builder.getTreeBuilt(); // calls the ASTFactory.createComposite() etc...
  }

  private static ParsedScript parse(JactlTokeniser tokeniser, JactlFile file) {
    return parse(tokeniser, file, null);
  }

  private static ParsedScript parse(JactlTokeniser tokeniser, JactlFile jactlFile, PsiBuilder builder) {
    synchronized (tokeniser) {
      ParsedScript parsed = new ParsedScript(tokeniser.getJactl(), tokeniser.getJactlContext(), tokeniser.getBufferSequence().toString());

      if (jactlFile == null) {
        LOG.warn("JactlFile is null");
      }
      else {
        //new Exception("Parsing: File=" + jactlFile + ", virtualFile=" + jactlFile.getVirtualFile() + ", originalFile=" + jactlFile.getOriginalFile() + ", source=" + tokeniser.getBufferSequence().toString().replaceAll("\n", "\\\\n")).printStackTrace();
        jactlFile = (JactlFile)jactlFile.getOriginalFile();
      }

      final JactlFile file = jactlFile;

      //List<JactlTokenBuilder.Event> events = tokeniser.getEvents().stream().filter(Predicate.not(JactlTokenBuilder.Event::isDropped)).toList();
      List<JactlTokenBuilder.Event> events = tokeniser.getEvents();
      for (int i = 0; i < events.size(); i++) {
        var event = events.get(i);
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
              var token = events.get(j);
              if (token.isToken() && !token.getToken().isCommentOrWhiteSpace()) {
                marker.offset = token.getToken().getOffset();
                break;
              }
            }
            if (marker.offset == -1) {
              throw new IllegalStateException("Couldn't find a token after " + markerEvent);
            }

            if (marker.type == JactlStmtElementType.FUN_DECL ||
                marker.type == JactlStmtElementType.CLASS_DECL && marker.astNode instanceof Stmt.ClassDecl classDecl && !classDecl.isScriptClass()) {
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
            if (marker.astNode instanceof Stmt.VarDecl stmt) {
              stmt.declExpr.setUserData(new JactlAstKey(file, marker.type, marker.offset));
            }
            else if (marker.astNode instanceof Stmt.FunDecl stmt) {
              if (marker.nameKey == null) {
                LOG.warn("No name found for function declaration: funDecl=" + marker.astNode);
              }
              stmt.declExpr.varDecl.setUserData(marker.nameKey);
            }
            else if (marker.astNode instanceof Stmt.ClassDecl classDecl && !classDecl.isScriptClass()) {
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

//      var notDone = events.stream().filter(e -> e.getMarker() != null && e.getMarker().psiMarker != null && !e.getMarker().doneFlagged).toList();
//      if (!notDone.isEmpty()) {
//        System.out.println("ERROR: not done markers: " + notDone);
//      }

      String sourceCode = tokeniser.getBufferSequence().toString();
      synchronized (parsedScripts) {
        if (file != null && !sourceCode.contains(CompletionUtilCore.DUMMY_IDENTIFIER)) {
          parsedScripts.put(file, parsed);
        }
      }

      parsed.resolve(jactlFile);
      return parsed;
    }
  }

  public static JactlUserDataHolder getJactlAstNode(JactlPsiElement element) {
    return getJactlAstNode(element.getFile(), element.getSourceCode(), element.getAstKey());
  }

  @NotNull
  private static ParsedScript getParsedScript(JactlFile file, String sourceCode) {
    ParsedScript parsed;
    synchronized (parsedScripts) {
      parsed = parsedScripts.get(file);
      if (parsed == null || !parsed.getSourceCode().equals(sourceCode)) {
        JactlTokeniser tokeniser = new JactlTokeniser(file.getProject());
        tokeniser.tokenise(sourceCode, 0, sourceCode.length());
        parsed = parse(tokeniser, file);
      }
    }
    return parsed;
  }

  public record FieldDescriptor(String name, JactlType type, boolean isStatic) {}

  /**
   * Return list of variables/fields (as FieldDescriptor) and functions (FunctionDescriptors)
   * @param file        the file
   * @param sourceCode  the source code
   * @param astKey      the location in the code from which to search for vars/functions
   * @return the list
   */
  public static List<Object> getVariablesAndFunctions(JactlFile file, String sourceCode, JactlAstKey astKey) {
    ParsedScript parsedScript = getParsedScript(astKey.getFile(), sourceCode);
    return parsedScript.getVariablesAndFunctions(astKey);
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
    ParsedScript parsed = getParsedScript(astKey.getFile(), sourceCode);
    var result = parsed.getJactlAstNode(astKey);
    return result;
  }

  public static List<ClassDescriptor> getClasses(JactlFile file, String sourceCode, JactlAstKey astKey) {
    return getParsedScript(astKey.getFile(), sourceCode).getClasses(file.getProject(), astKey);
  }

  public static ClassDescriptor getClass(JactlPsiElement element) {
    var key = element.getAstKey();
    if (key != null) {
      return getClass(key.getFile(), element.getSourceCode(), key);
    }
    return null;
  }

  public static ClassDescriptor getClass(JactlFile file, String sourceCode, JactlAstKey astKey) {
    return getParsedScript(astKey.getFile(), sourceCode).getClass(file.getProject(), astKey);
  }

  public static List<String> getErrors(JactlFile file, String sourceCode, ASTNode node) {
    return getParsedScript(file, sourceCode).getErrors(node);
  }

  public static Stmt.ClassDecl getClassDecl(Project project, String fqClassName) {
    var file = JactlUtils.findFileForClass(project, fqClassName);
    if (file == null) {
      return null;
    }
    return getClassDecl(file, file.getText(), JactlPlugin.removePackage(fqClassName));
  }

  public static boolean isImported(JactlPsiElement className) {
    var parser = getParsedScript(className.getFile(), className.getSourceCode());
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
      var result = jactlAstNodes.get(astKey);
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

    public List<Object> getVariablesAndFunctions(JactlAstKey astKey) {
      JactlUserDataHolder astNode = getJactlAstNode(astKey);
      if (astNode == null) {
        return Collections.EMPTY_LIST;
      }
      if (!(astNode instanceof Expr.Identifier expr)) {
        LOG.warn("Completions not possible for node of type " + astNode.getClass().getName());
        return Collections.EMPTY_LIST;
      }

      Stmt.Block block = expr.getBlock();

      Set<Object> names = new HashSet<>();
      List<Object> result = new ArrayList<>();

      BiConsumer<String,Object> addResult = (name,value) -> {
        if (!names.contains(name)) {
          result.add(value);
          names.add(name);
        }
      };

      // Get all variables in current block declared before us
      if (block != null) {
        String name = expr.identifier.getStringValue();
        block.variables.values()
                       .stream()
                       .filter(v -> Utils.isEarlier(v.location, expr.location))
                       .filter(v -> !v.name.getStringValue().equals(name))
                       .filter(v -> !v.name.getStringValue().startsWith(Utils.JACTL_PREFIX))
                       .forEach(v -> addResult.accept(v.name.getStringValue(), v));

        // Add all local functions
        block.functions.stream()
                       .map(f -> f.declExpr.varDecl)
                       .filter(f -> !f.name.getStringValue().startsWith(Utils.JACTL_PREFIX))
                       .forEach(f -> addResult.accept(f.name.getStringValue(), f));

        // Iterator up through all enclosing blocks and add vars/functions
        for (Stmt.Block parent = block.enclosingBlock; parent != null; parent = parent.enclosingBlock) {
          Stream.concat(parent.variables.values().stream(),
                        parent.functions.stream().map(f -> f.declExpr.varDecl))
                .filter(v -> !v.name.getStringValue().startsWith(Utils.JACTL_PREFIX))
                .forEach(v -> addResult.accept(v.name.getStringValue(), v));
        }

        // Get class that owns this block so that we can add fields/methods and of the
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

    public List<ClassDescriptor> getClasses(Project project, JactlAstKey astKey) {
      Stmt.Block block = getBlock(astKey);
      if (block == null) {
        return Collections.EMPTY_LIST;
      }
      return getClasses(project, block);
    }

    public ClassDescriptor getClass(Project project, JactlAstKey astKey) {
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
      if (block == null && astNode instanceof Stmt.FunDecl funDecl) {
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

    public void resolve(JactlFile file) {
      boolean globalsFile = JactlUtils.isGlobalsFile(file);
      globalsError = null;
      Map<String,Object> globals = Collections.EMPTY_MAP;
      Project            project = file.getProject();
      if (!globalsFile) {
        try {
          globals = JactlUtils.getGlobals(project);
        }
        catch (GlobalsException e) {
          globalsError = e;
        }
      }
      resolver = new Resolver(jactlContext, globals, jactlAst.location);
      String packageName = JactlUtils.packageNameFor(file);
      if (packageName == null && !globalsFile) {
        errors.putIfAbsent(0, new ArrayList<>());
        errors.get(0).add("File exists outside configured source roots");
        return;
      }
      String scriptName = JactlPlugin.removeSuffix(file.getName());
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
          List<VirtualFile> openFiles = Stream.of(FileEditorManager.getInstance(project).getOpenFiles()).filter(vf -> !vf.equals(file.getVirtualFile())).toList();
          ApplicationManager.getApplication().invokeLater(() -> FileContentUtil.reparseFiles(project, openFiles, false));
        }
      }
    }
  }

}

