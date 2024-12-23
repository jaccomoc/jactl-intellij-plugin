/*
 * Copyright © 2022,2023,2024  James Crawford
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.jactl.intellijplugin;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import io.jactl.*;
import io.jactl.TokenType;
import io.jactl.intellijplugin.common.JactlBundle;
import io.jactl.intellijplugin.common.JactlPlugin;
import io.jactl.intellijplugin.extensions.settings.JactlConfiguration;
import io.jactl.intellijplugin.jpsplugin.builder.GlobalsException;
import io.jactl.intellijplugin.psi.*;
import io.jactl.intellijplugin.psi.impl.JactlPsiTypeImpl;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiName;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiType;
import io.jactl.runtime.ClassDescriptor;
import io.jactl.runtime.RuntimeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.psi.TokenType.WHITE_SPACE;

public class JactlUtils {
  public static final String CODE_FRAGMENT_FILE_NAME = "_$j__fragment__.jactl";
  private static final Logger LOG = Logger.getInstance(JactlUtils.class);

  public static final String[] BUILTIN_TYPES = Parser.typesAndVar.stream().map(tok -> tok.asString).toArray(String[]::new);
  public static final String[] SIMPLE_TYPES  = Arrays.stream(Parser.simpleTypes).map(tok -> tok.asString).toArray(String[]::new);

  public static final String[] BEGINNING_KEYWORDS = new String[] { TokenType.IF.asString,
                                                                   TokenType.WHILE.asString,
                                                                   TokenType.FOR.asString,
                                                                   TokenType.DO.asString,
                                                                   TokenType.RETURN.asString,
                                                                   TokenType.PRINT.asString,
                                                                   TokenType.PRINTLN.asString,
                                                                   TokenType.DIE.asString,
                                                                   TokenType.EVAL.asString,
                                                                   TokenType.SWITCH.asString
                                                                   };

  public static List<String> getSourceRoots(Project project) {
    return Arrays.stream(ProjectRootManager.getInstance(project).getContentSourceRoots())
                 .map(VirtualFile::getCanonicalPath)
                 .collect(Collectors.toList());
  }

  public static List<VirtualFile> getSourceRootFiles(Project project) {
    return new ArrayList<>(Utils.listOf(ProjectRootManager.getInstance(project).getContentSourceRoots()));
  }

  public static <T> int indexOf(T[] elements, T element) {
    for (int i = 0; i < elements.length; i++) {
      if (elements[i] == element) {
        return i;
      }
    }
    return -1;
  }

  public static Set<String> pkgNames(VirtualFile base) {
    Set<String> names = new HashSet<>();
    VfsUtilCore.visitChildrenRecursively(base, new VirtualFileVisitor<Void>() {
      @Override
      public boolean visitFile(@NotNull VirtualFile file) {
        if (file.isDirectory()) {
          String pkgName = JactlPlugin.stripSeparatedPrefix(Objects.requireNonNull(file.getCanonicalPath()), base.getCanonicalPath(), File.separator).replace(File.separatorChar, '.');
          names.add(pkgName);
        }
        return true;
      }
    });
    return names;
  }

  public static Set<String> pkgNames(Project project) {
    return getSourceRootFiles(project).stream()
                                      .map(JactlUtils::pkgNames)
                                      .flatMap(Collection::stream)
                                      .collect(Collectors.toSet());
  }

  public static String pathToClass(Project project, String path) {
    String projectPath = getProjectPath(project, path);
    if (projectPath == null) {
      projectPath = path;
    }
    projectPath = JactlPlugin.removeSuffix(projectPath);
    return projectPath.replace(File.separatorChar, '.');
  }

  @NotNull
  public static String getParameterText(Expr.FunDecl f) {
    return "(" +
           f.parameters.stream()
                               .map(p -> p.declExpr.type.toString() + " " + p.declExpr.name.getStringValue())
                               .collect(Collectors.joining(", "))
           + ")";
  }

  public static JactlPsiElement getGlobal(String name, Project project) {
    String globalsScript = JactlConfiguration.getInstance(project).getGlobalVariablesScript();
    if (globalsScript != null && !globalsScript.trim().isEmpty()) {
      VirtualFile globalsFile = VfsUtil.findFile(Path.of(FileUtil.toSystemDependentName(globalsScript.trim())), true);
      if (globalsFile != null) {
        PsiFile psiGlobals = PsiManager.getInstance(project).findFile(globalsFile);
        return (JactlPsiElement)getFirstDescendant(psiGlobals, element -> true);
//        if (psiGlobals != null) {
//          return (JactlPsiElement)getFirstDescendant(psiGlobals, element -> isElementType(element, JactlTokenTypes.IDENTIFIER) && element.getText().equals(name));
//        }
      }
    }
    return null;
  }

  public static JactlPsiElement getNameElementForPsiElementInTree(JactlAstKey key) {
    JactlPsiElement element = getPsiElementInTree(key);
    if (element == null || element instanceof JactlPsiName) {
      return element;
    }
    return (JactlPsiElement)getFirstDescendant(element, JactlPsiName.class);
  }

  private static JactlPsiElement getPsiElementInTree(JactlAstKey key) {
    if (key == null) {
      return null;       // Just to be safe
    }
    JactlFile file = key.getFile();
    if (file == null) {
      return null;
    }
    // Find leaf element at location and work our way up until we get to node corresponding to type
    PsiElement leaf = file.findElementAt(key.getOffset());
    PsiElement psi;
    for (psi = leaf.getParent(); psi != null && psi.getNode() != null; psi = psi.getParent()) {
       if (psi.getNode().getElementType().equals(key.getType())) {
         return (JactlPsiElement)psi;
       }
    }
    LOG.warn("Could not find node corresponding to " + key);
    return null;
  }

  public static String getDocumentation(PsiElement element, String defaultText) {
    if (element instanceof JactlFile && JactlUtils.isGlobalsFile((JactlFile) element)) {
      JactlFile file = (JactlFile) element;
      return "Global variable";
    }
    if (element instanceof JactlPsiElement) {
      JactlPsiElement     jactlPsiElement = (JactlPsiElement) element;
      JactlUserDataHolder astNode         = jactlPsiElement.getJactlAstNode();
      if (astNode == null) {
        return defaultText;
      }
      if (astNode instanceof Stmt.VarDecl) {
        Stmt.VarDecl  varDeclStmt = (Stmt.VarDecl) astNode;
        StringBuilder sb          = new StringBuilder();
        Expr.VarDecl  varDecl     = varDeclStmt.declExpr;
        if (varDecl.isConstVar) {
          sb.append("const ");
        }
        String type = getType(element, varDecl.type.toString());
        sb.append(type).append(" ").append(varDecl.name.getStringValue());
        Expr initialiser = varDecl.initialiser;
        if (initialiser instanceof Expr.Noop) {
          Expr.Noop noop = (Expr.Noop) initialiser;
          // For parameters initialiser is replaced with a Noop but we store original one in the Noop
          initialiser = noop.originalExpr;
        }
        if (initialiser != null) {
          PsiElement declElement = getPsiElementInTree(initialiser.getUserData(JactlAstKey.class));
          sb.append(" = ").append(firstLine(declElement.getText()));
        }
        return sb.toString();
      }

      if (astNode instanceof JactlName) {
        PsiElement      psiParent = element.getParent();
        if (psiParent instanceof JactlPsiElement) {
          JactlPsiElement parent = (JactlPsiElement) psiParent;
          astNode = parent.getJactlAstNode();
          if (astNode == null) {
            return defaultText;
          }
        }
        return defaultText;
      }

      if (astNode instanceof Stmt.ClassDecl) {
        Stmt.ClassDecl classDecl = (Stmt.ClassDecl) astNode;
        StringBuilder  sb        = new StringBuilder();
        sb.append("class ").append(classDecl.name.getStringValue());
        if (classDecl.baseClass != null) {
          sb.append(" extends ").append(classDecl.baseClass.getClassName().stream().map(e -> e instanceof Expr.Identifier ? ((Expr.Identifier) e).identifier.getStringValue() : "?").collect(Collectors.joining(".")));
        }
        return sb.toString();
      }

      if (astNode instanceof Stmt.FunDecl) {
        Stmt.FunDecl  funDeclStmt = (Stmt.FunDecl) astNode;
        StringBuilder sb          = new StringBuilder();
        Expr.FunDecl  funDecl     = funDeclStmt.declExpr;
        String        type        = getType(element, funDecl.returnType.toString());
        if (funDecl.isStatic()) {
          sb.append("static ");
        }
        sb.append(type)
          .append(" ")
          .append(funDecl.nameToken.getStringValue())
          .append(getParameterText(funDecl));
        return sb.toString();
      }
    }
    return defaultText;
  }

  private static String firstLine(String text) {
    int idx = text.indexOf('\n');
    return text.substring(0, Math.min(80, idx == -1 ? text.length() : idx));
  }

  private static String getType(PsiElement element, String defaultValue) {
    for (PsiElement prev = element.getPrevSibling(); prev != null; prev = prev.getPrevSibling()) {
      if (prev instanceof JactlPsiTypeImpl) {
        return prev.getText();
      }
    }
    return defaultValue;
  }

  public static PsiElement getNthChild(PsiElement parent, int n, IElementType... types) {
    for (PsiElement child = parent.getFirstChild(); child != null && n >= 0; child = child.getNextSibling()) {
      final PsiElement finalChild = child;
      if (Arrays.stream(types).anyMatch(type -> finalChild.getNode().getElementType() == type)) {
        n--;
        if (n < 0) {
          return child;
        }
      }
    }
    return null;
  }

  // Get first non-comment, non-whitespace child
  public static PsiElement getFirstChildNotWhiteSpace(PsiElement parent) {
    return getFirstChild(parent, child -> Stream.of(JactlTokenTypes.COMMENT, JactlTokenTypes.WHITESPACE, WHITE_SPACE)
                                                .noneMatch(type -> child.getNode().getElementType().equals(type)));
  }

  public static PsiElement getFirstChild(PsiElement parent, IElementType... types) {
    return getFirstChild(parent, child -> Arrays.stream(types).anyMatch(type -> child.getNode().getElementType().equals(type)));
  }

  public static <T extends JactlPsiElement> PsiElement getFirstChild(PsiElement parent, Class<T> clss) {
    return getFirstChild(parent, child -> clss.isAssignableFrom(child.getClass()));
  }

  public static <T extends JactlPsiElement> PsiElement getLastChild(PsiElement parent, Class<T> clss) {
    return getLastChild(parent, child -> clss.isAssignableFrom(child.getClass()));
  }

  public static <T extends PsiElement> PsiElement getFirstDescendant(PsiElement parent, Class<T> clss) {
    return getFirstDescendant(parent, child -> clss.isAssignableFrom(child.getClass()));
  }

  public static <T extends PsiElement> PsiElement getFirstDescendant(PsiElement parent, IElementType... elementTypes) {
    return getFirstDescendant(parent, child -> isElementType(child.getNode(), elementTypes));
  }

  public static PsiElement getFirstChild(PsiElement parent, Predicate<PsiElement> matcher) {
    for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (matcher.test(child)) {
        return child;
      }
    }
    return null;
  }

  public static PsiElement getLastChild(PsiElement parent, Predicate<PsiElement> matcher) {
    for (PsiElement child = parent.getLastChild(); child != null; child = child.getPrevSibling()) {
      if (matcher.test(child)) {
        return child;
      }
    }
    return null;
  }

  public static int countChildren(PsiElement parent, Predicate<PsiElement> matcher) {
    int count = 0;
    for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (matcher.test(child)) {
        count++;
      }
    }
    return count;

  }

  public static PsiElement getFirstDescendant(PsiElement parent, Predicate<PsiElement> matcher) {
    if (matcher.test(parent)) {
      return parent;
    }
    for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      PsiElement descendant = getFirstDescendant(child, matcher);
      if (descendant != null) {
        return descendant;
      }
    }
    return null;
  }

  /**
   * Get previous sibling skipping any comments/whitespace
   * @param element the element
   * @return the previous sibling that is not comments/whitespace or null if none
   */
  public static PsiElement getPrevSibling(PsiElement element) {
    for (PsiElement prev = element.getPrevSibling(); prev != null; prev = prev.getPrevSibling()) {
      if (isElementType(prev.getNode(), JactlTokenTypes.WHITESPACE, WHITE_SPACE, JactlTokenTypes.COMMENT)) {
        continue;
      }
      return prev;
    }
    return null;
  }

  public static PsiElement getNextSibling(PsiElement element) {
    for (PsiElement next = element.getNextSibling(); next != null; next = next.getNextSibling()) {
      if (isElementType(next.getNode(), JactlTokenTypes.WHITESPACE, WHITE_SPACE, JactlTokenTypes.COMMENT)) {
        continue;
      }
      return next;
    }
    return null;
  }

  public static PsiElement getNextSibling(PsiElement element, IElementType type) {
    for (PsiElement next = element.getNextSibling(); next != null; next = next.getNextSibling()) {
      if (isElementType(next, type)) {
        return next;
      }
    }
    return null;
  }

  public static PsiElement skipWhitespaceAndComments(PsiElement element) {
    while (isElementType(element, JactlTokenTypes.WHITESPACE, WHITE_SPACE, JactlTokenTypes.COMMENT)) {
      element = element.getNextSibling();
    }
    return element;
  }

  public static PsiElement getAncestor(PsiElement element, Class<?>... classes) {
    return getAncestor(element, e -> Arrays.stream(classes).anyMatch(cls -> cls.isAssignableFrom(e.getClass())));
  }

  public static PsiElement getAncestor(PsiElement element, IElementType... types) {
    return getAncestor(element, e -> Arrays.stream(types).anyMatch(type -> e.getNode().getElementType() == type));
  }

  public static PsiElement getAncestor(PsiElement element, Predicate<PsiElement> matcher) {
    for (PsiElement parent = element; parent != null; parent = parent.getParent()) {
      if (matcher.test(parent)) {
        return parent;
      }
    }
    return null;
  }

  public static PsiElement newElement(Project project, JactlNameElementType type, String name, Class<? extends JactlPsiElement> clss) {
    String text = elementText(name, type);
    PsiFile newFile = PsiFileFactory.getInstance(project).createFileFromText("A.jactl", JactlLanguage.INSTANCE, text);
    return JactlUtils.getFirstDescendant(newFile, clss);
  }

  @NotNull
  public static String elementText(@NotNull String name, JactlNameElementType type) {
    switch (JactlNameElementType.getNameType(type)) {
      case PACKAGE:   throw new IncorrectOperationException("Package rename not supported");
      case CLASS:     return "class " + name + "{}";
      case FUNCTION:  return "def " + name + "(){}";
      case METHOD:    return "def " + name + "(){}";
      case VARIABLE:  return "def " + name;
      case FIELD:     return "def " + name;
      case PARAMETER: return "def " + name;
      default:        throw new IllegalArgumentException();
    }
  }
  public static PsiElement newReferenceElement(Project project, JactlNameElementType type, String name, Class<? extends JactlPsiElement> clss) {
    String text = referenceElementText(name, type);
    return newElement(project, text, clss);
  }

  public static PsiElement newElement(Project project, String text, Class<? extends JactlPsiElement> clss) {
    PsiFile newFile = PsiFileFactory.getInstance(project).createFileFromText("A.jactl", JactlLanguage.INSTANCE, text);
    return JactlUtils.getFirstDescendant(newFile, clss);
  }

  public static PsiElement newElement(Project project, String code, IElementType... types) {
    PsiFile newFile = PsiFileFactory.getInstance(project).createFileFromText("A.jactl", JactlLanguage.INSTANCE, code);
    return JactlUtils.getFirstDescendant(newFile, types);
  }

  public static boolean isElementType(ASTNode node, IElementType... types) {
    if (node == null) {
      return false;
    }
    return Arrays.stream(types).anyMatch(t -> node.getElementType() == t);
  }

  public static boolean isElementType(PsiElement element, IElementType... types) {
    return element != null && Arrays.stream(types).anyMatch(t -> isElementType(element.getNode(),t));
  }

  @NotNull
  public static String referenceElementText(@NotNull String name, JactlNameElementType type) {
    switch (JactlNameElementType.getNameType(type)) {
      case FILE:      throw new IncorrectOperationException("Invalid operation");
      case PACKAGE:   return "package " + name;
      case CLASS:     return name + " xxx";
      case FUNCTION:  return name + "()";
      case METHOD:    return name + "()";
      case VARIABLE:  return name;
      case FIELD:     return name;
      case PARAMETER: return name;
      default:        throw new IllegalArgumentException();
    }
  }

  public static boolean isGlobalsFile(JactlFile file) {
    if (file == null) {
      return false;
    }
    String globalsScriptName = JactlConfiguration.getInstance(file.getProject()).getGlobalVariablesScript();
    if (globalsScriptName != null && !globalsScriptName.trim().isEmpty()) {
      String canonicalPath = file.getVirtualFile().getCanonicalPath();
      return FileUtil.toSystemIndependentName(canonicalPath).equals(FileUtil.toSystemIndependentName(globalsScriptName));
    }
    return false;
  }

  public static final class PackageEntry {
    private final boolean isPackage;
    private final String name;
    public PackageEntry(boolean isPackage, String name) {
      this.isPackage = isPackage;
      this.name = name;
    }
    public boolean isPackage() { return isPackage; }
    public String name() { return name; }
  }

  /**
   * Return contents of given package being the immediate sub-packages and classes for this package.
   * @param project      the project
   * @param packageName  the source package to search ("a.b.c")
   * @return a set of PackageEntry objects
   */
  public static Set<PackageEntry> packageContents(Project project, String packageName) {
    Set<PackageEntry> contents = new HashSet<>();
    processPackage(project, packageName, child -> {
      // Add all subdirs and class files (not script files)
      if (child.isDirectory() || child.getFileType() == JactlFileType.INSTANCE) {
        if (!child.isDirectory()) {
          JactlFile jactlFile = getJactlFile(project, child);
          if (jactlFile == null || jactlFile.isScriptFile()) {
            return;
          }
        }
        contents.add(new PackageEntry(child.isDirectory(), JactlPlugin.removeSuffix(child.getName())));
      }
    });
    return contents;
  }

  private static void processPackage(Project project, String packageName, Consumer<VirtualFile> processor) {
    String[] packagePath = packageName == null || packageName.isEmpty() ? new String[0] : packageName.split("\\.");
    for (VirtualFile root: getSourceRootFiles(project)) {
      VirtualFile baseFile = VfsUtil.findRelativeFile(root, packagePath);
      if (baseFile == null) {
        continue;
      }
      Arrays.stream(baseFile.getChildren()).forEach(processor);
    }
  }

  public static List<ClassDescriptor> packageClasses(Project project, String packageName) {
    if (packageName == null || packageName.isEmpty()) {
      // No automatic import of classes if we are at root level (i.e. no package)
      return Collections.EMPTY_LIST;
    }
    List<ClassDescriptor> contents = new ArrayList<>();
    processPackage(project, packageName, child -> {
      if (!child.isDirectory() && child.getFileType() == JactlFileType.INSTANCE) {
        String         className = JactlPlugin.removeSuffix(child.getName());
        JactlFile      file      = getJactlFile(project, child);
        Stmt.ClassDecl classDecl = JactlParserAdapter.getClassDecl(file, file.getText(), className);
        if (classDecl != null && !classDecl.isScriptClass()) {
          contents.add(classDecl.classDescriptor);
        }
      }
    });
    return contents;
  }

  private static @Nullable JactlFile getJactlFile(Project project, VirtualFile child) {
    return (JactlFile) PsiManager.getInstance(project).findFile(child);
  }

  public static JactlFile findFileForClassPath(Project project, String classPathName) {
    return findFileForClass(project, classPathName.replace(File.separatorChar, '.'));
  }

  public static JactlFile findFileForClass(Project project, String fqClassName) {
    String pkgName   = JactlPlugin.stripFromLast(fqClassName, '.');
    String className = JactlPlugin.stripSeparatedPrefix(fqClassName, pkgName, ".");

    // First strip prefix if we have script class
    className = JactlPlugin.stripSeparatedPrefix(className, JactlPlugin.SCRIPT_PREFIX, null);

    // Remove inner classes (X$Y$Z -> X)
    className = JactlPlugin.stripFromFirst(className, '$');

    String      filePath = pkgName.replace('.', File.separatorChar) + File.separatorChar + className + JactlPlugin.DOT_SUFFIX;
    VirtualFile file     = findVirtualFile(project, filePath);
    if (file != null) {
      return getJactlFile(project, file);
    }
    return null;
  }

  public static JactlFile findFile(Project project, String filePath) {
    VirtualFile file = findVirtualFile(project, filePath);
    if (file != null) {
      return getJactlFile(project, file);
    }
    return null;
  }

  public static VirtualFile findVirtualFile(Project project, String fileName) {
    for (VirtualFile root: getSourceRootFiles(project)) {
      VirtualFile file = VfsUtil.findRelativeFile(root, fileName.split(File.separator));
      if (file != null) {
        return file;
      }
    }
    return null;
  }

  /**
   * For a dot separated list of package names return the parent package of the specified element.
   * E.g. if we have a.b.c.d and element is c then parentPackage will be a.b
   * @param element     the element in the list
   * @param join        the join string to use to the multiple names
   * @return the package name for the packages to the left of the specified element
   */
  public static String parentPackage(PsiElement element, String join) {
    return packageName(element, join, false);
  }

  private static String packageName(PsiElement element,  String join, boolean includeElement) {
    List<String> path = new ArrayList<>();
    for (PsiElement child = JactlUtils.getFirstChild(element.getParent(), JactlTokenTypes.IDENTIFIER); child != element; child = child.getNextSibling()) {
      if (child.getNode().getElementType().equals(JactlTokenTypes.IDENTIFIER)) {
        path.add(child.getText());
      }
    }
    if (includeElement) {
      path.add(element.getText());
    }
    return String.join(join, path);
  }

  public static PsiDirectory getPackage(PsiElement element) {
    String      packageName = packageName(element, File.separator, true);
    VirtualFile dir         = findVirtualFile(element.getProject(), packageName);
    if (dir != null) {
      return PsiManager.getInstance(element.getProject()).findDirectory(dir);
    }
    return null;
  }

  public static String getProjectPath(Project project, String path) {
    for (String root: getSourceRoots(project)) {
      if (path.startsWith(root)) {
        return JactlPlugin.stripSeparatedPrefix(path, root, File.separator);
      }
    }
    return null;
  }

  public static String getProjectPath(Project project, VirtualFile file) {
    return getProjectPath(project, file, file.getCanonicalPath());
  }

  public static String getProjectPath(PsiFileSystemItem item) {
    return getProjectPath(item, item.getVirtualFile().getCanonicalPath());
  }

  public static String getProjectPath(Project project, VirtualFile file, String defaultValue) {
    VirtualFile moduleSourceRoot = ProjectRootManager.getInstance(project)
                                                     .getFileIndex()
                                                     .getSourceRootForFile(file);
    if (moduleSourceRoot != null) {
      String path = moduleSourceRoot.getPath();
      if (file.getPath().startsWith(path)) {
        return JactlPlugin.stripSeparatedPrefix(file.getPath(), path, File.separator);
      }
    }
    return defaultValue;
  }

  public static String getProjectPath(PsiFileSystemItem item, String defaultValue) {
    return getProjectPath(item.getProject(), item.getVirtualFile(), defaultValue);
  }

  /**
   * Replace identifiers of a package name with package name corresponding to destDir and return the generated Jactl
   * string. Note that this preserves comments/whitespace as much as possible.
   *
   * @param psiElement    the element (CLASS_PATH_EXPR or PACKAGE)
   * @param destDir       the destination package
   * @param preserveFinal true if we should keep last identifier
   * @return the new Jactl code as a string
   */
  public static String replacePackage(PsiElement psiElement, String destDir, boolean preserveFinal) {
    String dottedPackage  = destDir.replace(File.separatorChar, '.');
    if (JactlUtils.isElementType(psiElement, JactlTypeElementType.CLASS_TYPE)) {
      dottedPackage = dottedPackage.isEmpty() ? dottedPackage : dottedPackage + ".";
      return dottedPackage + psiElement.getText();
    }
    String[]      newPackage   = dottedPackage.contains(".") ? dottedPackage.split("\\.") : dottedPackage.isEmpty() ? new String[0] : new String[] { dottedPackage };
    StringBuilder sb           = new StringBuilder();
    // Replace each identifier in the package with identifier from new package to preserve comments/whitespace etc
    int identCount = JactlUtils.countChildren(psiElement, child -> JactlUtils.isElementType(child, JactlTokenTypes.IDENTIFIER));
    if (preserveFinal) {
      identCount--;
    }
    int newIdx = 0;
    int dotCounter = 0;
    int identCounter = 0;
    for (PsiElement child = psiElement.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (newIdx < newPackage.length) {
        if (JactlUtils.isElementType(child, JactlTokenTypes.IDENTIFIER)) {
          identCounter++;
          sb.append(newPackage[newIdx++]);
          if (newIdx == identCount && newIdx < newPackage.length) {
            // If no more identifiers in the old pacakge then add remainder of new package
            sb.append('.').append(Arrays.stream(newPackage).skip(newIdx).collect(Collectors.joining(".")));
            newIdx = newPackage.length;
          }
        }
        else {
          if (JactlUtils.isElementType(child, JactlTokenTypes.DOT)) {
            dotCounter++;
          }
          sb.append(child.getText());
        }
      }
      else {
        // Add anything that is not a DOT or an IDENTIFIER but also add last DOT and IDENTIFIER if preserveLast is set
        if (JactlUtils.isElementType(child, JactlTokenTypes.IDENTIFIER)) {
          identCounter++;
          if (preserveFinal && identCounter == identCount + 1) {
            sb.append(child.getText());
          }
        }
        else
        if (JactlUtils.isElementType(child, JactlTokenTypes.DOT)) {
          dotCounter++;
          if (preserveFinal && dotCounter == identCount && newPackage.length > 0) {
            sb.append(child.getText());
          }
        }
        else {
          sb.append(child.getText());
        }
      }
    }
    return sb.toString();
  }

  /**
   * Given an identifier that is part of a CLASS_PATH_EXPR or CLASS_TYPE,
   * replace the package part with the package corresponding to the given
   * dir and then return the identifier of the new CLASS_PATH_EXPR or CLASS_TYPE.
   * @param dir         the directory of the package
   * @param psiElement  the original element
   * @return the identifier in the new CLASS_PATH_EXPR or CLASS_TYPE
   */
  public static JactlPsiElement createNewPackagePath(PsiDirectory dir, JactlPsiElement psiElement) {
    String destDir = JactlUtils.getProjectPath(dir);
    String newCode = replacePackage(psiElement.getParent(), destDir, true);
    // Prepend "new" to the code to force it to be parsed as a CLASS_PATH_EXPR or CLASS_TYPE as appropriate
    newCode = "new " + newCode;
    PsiElement newParent = JactlUtils.newElement(psiElement.getProject(), newCode, JactlExprElementType.CLASS_PATH_EXPR, JactlTypeElementType.CLASS_TYPE);
    // If we have a nested CLASS_PATH_EXPR then prefer to use it unless we previously had a CLASS_TYPE
    if (JactlUtils.isElementType(newParent, JactlTypeElementType.CLASS_TYPE) && !JactlUtils.isElementType(psiElement.getParent(), JactlTypeElementType.CLASS_TYPE)) {
      PsiElement classPathExpr = JactlUtils.getFirstDescendant(newParent, JactlExprElementType.CLASS_PATH_EXPR);
      newParent = classPathExpr == null ? newParent : classPathExpr;
    }
    newParent = psiElement.getParent().replace(newParent);
    // Find where we are now in new parent
    if (JactlUtils.isElementType(newParent, JactlExprElementType.CLASS_PATH_EXPR)) {
      int partsCount = (int) destDir.chars().filter(c -> c == File.separatorChar).count();
      return (JactlPsiElement)JactlUtils.getNthChild(newParent, partsCount, JactlTokenTypes.IDENTIFIER);
    }
    // Must have a CLASS_TYPE
    return (JactlPsiElement)JactlUtils.getFirstDescendant(newParent, JactlTokenTypes.IDENTIFIER);
  }

  public static <T> Stream<T> stream(T initial, Function<T,T> generator) {
    List<T> result = new ArrayList<>();
    for (T obj = generator.apply(initial); obj != null; obj = generator.apply(obj)) {
      result.add(obj);
    }
    return result.stream();
  }

  public static String getPathForElement(PsiElement psiElement) {
    PsiFile     containingFile = psiElement     == null ? null : psiElement.getContainingFile();
    VirtualFile virtualFile    = containingFile == null ? null : containingFile.getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    return virtualFile.getPath();
  }

  public static String packageNameFor(JactlFile file) {
    if (file == null) {
      return null;
    }
    if (file.getVirtualFile().getCanonicalPath().equals(JactlPlugin.DUMMY_FILE_PATH)) {
      return "";
    }
    String projectPath = getProjectPath(file, null);
    if (projectPath == null) {
      return null;
    }
    int idx = projectPath.lastIndexOf(File.separatorChar);
    if (idx == -1) {
      return "";
    }
    return projectPath.substring(0, idx).replace(File.separatorChar, '.');
  }

  public static JactlContext createJactlContext(Project project) {
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
                         Set<String> pkgs = JactlUtils.pkgNames(project);
                         return pkgs.contains(pkgName);
                       })
                       .classLookup(name -> lookup(name, baseJavaPkgFile, project))
                       .build();
  }

  /**
   * Get ClassDecriptor for given class name
   * @param name             name of the class (jactl/pkg/x/y/z/A$B$C)
   * @param baseJavaPkgFile  base java package in file format (jactl/pkg)
   * @param project          the project
   * @return the ClassDescriptor or null
   */
  private static ClassDescriptor lookup(String name, String baseJavaPkgFile, Project project) {
    if (name.startsWith(baseJavaPkgFile)) {
      name = JactlPlugin.stripSeparatedPrefix(name, baseJavaPkgFile, "/");
    }
    else {
      LOG.warn("Class lookup name does not start with expected java package name (name=" + name + ")");
    }

    JactlFile file = JactlUtils.findFileForClassPath(project, name);
    if (file == null) {
      return null;
    }

    // Strip package name
    String className = name.substring(name.lastIndexOf('/') + 1);
    Stmt.ClassDecl classDecl = JactlParserAdapter.getClassDecl(file, file.getText(), className);
    if (classDecl == null) {
      return null;
    }
    return classDecl.classDescriptor;
  }

  public static VirtualFile getGlobalsFile(Project project) {
    String scriptPath = JactlConfiguration.getInstance(project).getGlobalVariablesScript();
    if (scriptPath != null) {
      String fileName = FileUtil.toSystemIndependentName(scriptPath.trim());
      if (!fileName.isEmpty()) {
        return VfsUtil.findFile(Path.of(fileName), true);
      }
    }
    return null;
  }

  public static Map<String,Object> getGlobals(Project project) {
    Map<String,Object> globals     = Collections.EMPTY_MAP;
    VirtualFile        globalsFile = getGlobalsFile(project);
    if (globalsFile != null) {
      String scriptPath = JactlConfiguration.getInstance(project).getGlobalVariablesScript();
      String fileName = FileUtil.toSystemIndependentName(scriptPath.trim());
      PsiFile file = PsiManager.getInstance(project).findFile(globalsFile);
      if (file == null) {
        throw new GlobalsException(fileName, JactlBundle.message("script.runner.error.no.global.variables.script", fileName));
      }
      if (file.isDirectory()) {
        throw new GlobalsException(fileName, JactlBundle.message("script.runner.error.global.variables.script.is.directory", fileName));
      }

      try {
        String scriptContents = file.getText();
        Object globalsObj    = Jactl.eval(scriptContents, Collections.EMPTY_MAP);
        if (globalsObj != null && !(globalsObj instanceof Map)) {
          throw new GlobalsException(fileName, JactlBundle.message("script.runner.error.global.variables.script.bad.type", RuntimeUtils.className(globalsObj)));
        }
        globals = (Map<String,Object>)globalsObj;
      }
      catch (CompileError e) {
        // Only show first error when error compiling globals script
        throw new GlobalsException(fileName, e.getErrors().get(0).getSingleLineMessage());
      }
      catch (Throwable e) {
        throw new GlobalsException(fileName, e.toString());
      }
    }
    return globals;
  }

  /**
   * Look for most immediate parent that is a JactlPsiElement (and not a JactlPsiType
   * because JactlPsiType does not have a block that we can get)
   * @param element the element
   * @return the parent or null if none can be found
   */
  public static JactlPsiElement getJactlPsiParent(PsiElement element) {
    for (PsiElement parent = element; parent != null; parent = parent.getParent()) {
      if (parent instanceof JactlPsiElement && !(parent instanceof JactlPsiType)) {
        return (JactlPsiElement)parent;
      }
    }
    return null;
  }
}
