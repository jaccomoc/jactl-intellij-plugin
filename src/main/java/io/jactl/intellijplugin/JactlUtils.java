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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import io.jactl.*;
import io.jactl.TokenType;
import io.jactl.intellijplugin.common.JactlPlugin;
import io.jactl.intellijplugin.psi.JactlNameElementType;
import io.jactl.intellijplugin.psi.JactlPsiElement;
import io.jactl.intellijplugin.psi.JactlTokenTypes;
import io.jactl.intellijplugin.psi.impl.JactlPsiIdentifierImpl;
import io.jactl.intellijplugin.psi.impl.JactlPsiTypeImpl;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiName;
import io.jactl.runtime.ClassDescriptor;
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
                 .toList();
  }

  public static List<VirtualFile> getSourceRootFiles(Project project) {
    return List.of(ProjectRootManager.getInstance(project).getContentSourceRoots());
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
        if (file.isDirectory() && !Objects.equals(file.getCanonicalPath(), base.getCanonicalPath())) {
          String pkgName = file.getCanonicalPath().substring(base.getCanonicalPath().length() + 1).replace(File.separatorChar, '.');
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
    path = JactlPlugin.removeSuffix(path);
    return path.replace(File.separatorChar, '.');
  }

  @NotNull
  public static String getParameterText(Expr.FunDecl f) {
    return "(" +
           f.parameters.stream()
                               .map(p -> p.declExpr.type.toString() + " " + p.declExpr.name.getStringValue())
                               .collect(Collectors.joining(", "))
           + ")";
  }

  public static JactlPsiElement getNameElementForPsiElementInTree(JactlAstKey key) {
    var element = getPsiElementInTree(key);
    if (element == null || element instanceof JactlPsiName) {
      return element;
    }
    return (JactlPsiElement)getFirstChild(element, JactlPsiName.class);
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
    JactlPsiElement.LOG.warn("Could not find node corresponding to " + key);
    return null;
  }

  public static String getDocumentation(PsiElement element, String defaultText) {
    if (element instanceof JactlPsiElement jactlPsiElement) {
      JactlUserDataHolder astNode = jactlPsiElement.getJactlAstNode();
      if (astNode == null) {
        return defaultText;
      }
      if (astNode instanceof Stmt.VarDecl varDeclStmt) {
        StringBuilder sb      = new StringBuilder();
        var           varDecl = varDeclStmt.declExpr;
        if (varDecl.isConstVar) {
          sb.append("const ");
        }
        String type = getType(element, varDecl.type.toString());
        sb.append(type).append(" ").append(varDecl.name.getStringValue());
        Expr initialiser = varDecl.initialiser;
        if (initialiser instanceof Expr.Noop noop) {
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
        JactlPsiElement parent = (JactlPsiElement) element.getParent();
        astNode = parent.getJactlAstNode();
        if (astNode == null) {
          return defaultText;
        }
      }

      if (astNode instanceof Stmt.ClassDecl classDecl) {
        StringBuilder sb = new StringBuilder();
        sb.append("class ").append(classDecl.name.getStringValue());
        if (classDecl.baseClass != null) {
          sb.append(" extends ").append(classDecl.baseClass.getClassName().stream().map(e -> e instanceof Expr.Identifier identifier ? identifier.identifier.getStringValue() : "?").collect(Collectors.joining(".")));
        }
        return sb.toString();
      }

      if (astNode instanceof Stmt.FunDecl funDeclStmt) {
        StringBuilder sb = new StringBuilder();
        var funDecl = funDeclStmt.declExpr;
        String type = getType(element, funDecl.returnType.toString());
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

  public static <T extends PsiElement> PsiElement getFirstDescendant(PsiElement parent, IElementType elementType) {
    return getFirstDescendant(parent, child -> child.getNode().getElementType() == elementType);
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

  public static PsiElement getFirstDescendant(PsiElement parent, Predicate<PsiElement> matcher) {
    for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (matcher.test(child)) {
        return child;
      }
      var descendant = getFirstDescendant(child, matcher);
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
      if (isElementType(prev.getNode(), JactlTokenTypes.COMMA, JactlTokenTypes.WHITESPACE, WHITE_SPACE, JactlTokenTypes.COMMENT)) {
        continue;
      }
      return prev;
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
    return switch (JactlNameElementType.getNameType(type)) {
      case FILE    -> throw new IncorrectOperationException("Invalid operation");
      case PACKAGE -> throw new IncorrectOperationException("Package rename not supported");
      case CLASS -> "class " + name + "{}";
      case FUNCTION, METHOD -> "def " + name + "(){}";
      case VARIABLE, PARAMETER -> "def " + name;
    };
  }
  public static PsiElement newReferenceElement(Project project, JactlNameElementType type, String name, Class<? extends JactlPsiElement> clss) {
    String text = referenceElementText(name, type);
    return newElement(project, text, clss);
  }

  public static PsiElement newElement(Project project, String text, Class<? extends JactlPsiElement> clss) {
    PsiFile newFile = PsiFileFactory.getInstance(project).createFileFromText("A.jactl", JactlLanguage.INSTANCE, text);
    return JactlUtils.getFirstDescendant(newFile, clss);
  }

  public static PsiElement newElement(Project project, String text, IElementType type) {
    PsiFile newFile = PsiFileFactory.getInstance(project).createFileFromText("A.jactl", JactlLanguage.INSTANCE, text);
    return JactlUtils.getFirstDescendant(newFile, type);
  }

  public static boolean isElementType(ASTNode node, IElementType... types) {
    return Arrays.stream(types).anyMatch(t -> node.getElementType() == t);
  }

  public static boolean isElementType(PsiElement element, IElementType... types) {
    return element != null && Arrays.stream(types).anyMatch(t -> element.getNode().getElementType() == t);
  }

  @NotNull
  public static String referenceElementText(@NotNull String name, JactlNameElementType type) {
    return switch (JactlNameElementType.getNameType(type)) {
      case FILE    -> throw new IncorrectOperationException("Invalid operation");
      case PACKAGE -> "package " + name;
      case CLASS -> name + " xxx";
      case FUNCTION, METHOD -> name + "()";
      case VARIABLE, PARAMETER -> name;
    };
  }

  public record PackageEntry(boolean isPackage, String name) {}

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
          var jactlFile = getJactlFile(project, child);
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
    for (var root: getSourceRootFiles(project)) {
      VirtualFile baseFile = VfsUtil.findRelativeFile(root, packagePath);
      if (baseFile == null) {
        continue;
      }
      Arrays.stream(baseFile.getChildren()).forEach(processor);
    }
  }

  public static List<ClassDescriptor> packageClasses(Project project, String packageName) {
    List<ClassDescriptor> contents = new ArrayList<>();
    processPackage(project, packageName, child -> {
      if (!child.isDirectory() && child.getFileType() == JactlFileType.INSTANCE) {
        String         className = JactlPlugin.removeSuffix(child.getName());
        var            file      = getJactlFile(project, child);
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
    String className = fqClassName.substring(pkgName.isEmpty() ? 0 : pkgName.length() + 1);

    // First strip prefix if we have script class
    if (className.startsWith(JactlPlugin.SCRIPT_PREFIX)) {
      className = className.substring(JactlPlugin.SCRIPT_PREFIX.length());
    }
    // Remove inner classes (X$Y$Z -> X)
    className = JactlPlugin.stripFromFirst(className, '$');

    String filePath = pkgName.replace('.', File.separatorChar) + File.separatorChar + className + JactlPlugin.DOT_SUFFIX;
    var file = findVirtualFile(project, filePath);
    if (file != null) {
      return getJactlFile(project, file);
    }
    return null;
  }

  public static JactlFile findFile(Project project, String filePath) {
    var file = findVirtualFile(project, filePath);
    if (file != null) {
      return getJactlFile(project, file);
    }
    return null;
  }

  public static VirtualFile findVirtualFile(Project project, String fileName) {
    for (var root: getSourceRootFiles(project)) {
      var file = VfsUtil.findRelativeFile(root, fileName.split(File.separator));
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
    String packageName = packageName(element, File.separator, true);
    var dir = findVirtualFile(element.getProject(), packageName);
    if (dir != null) {
      return PsiManager.getInstance(element.getProject()).findDirectory(dir);
    }
    return null;
  }

  public static String getProjectPath(Project project, String path) {
    for (var root: getSourceRoots(project)) {
      if (path.startsWith(root)) {
        return path.substring(root.length() + 1);
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
      if (file.getPath().startsWith(moduleSourceRoot.getPath())) {
        String projectPath = file.getPath().substring(moduleSourceRoot.getPath().length() + 1);
        return projectPath;
      }
    }
    return defaultValue;
  }

  public static String getProjectPath(PsiFileSystemItem item, String defaultValue) {
    return getProjectPath(item.getProject(), item.getVirtualFile(), defaultValue);
  }

  public static JactlPsiElement createNewPackagePath(PsiDirectory dir, JactlPsiElement psiElement) {
    String newPackage = JactlUtils.getProjectPath(dir).replace(File.separator, ".");
    String oldPackage = JactlUtils.parentPackage(psiElement, ".");
    if (!newPackage.equals(oldPackage)) {
      String[]      names = newPackage.isEmpty() ? new String[0] : newPackage.split("\\.");
      int           idx   = 0;
      StringBuilder sb    = new StringBuilder();
      for (PsiElement child = psiElement.getParent().getFirstChild(); child != psiElement; child = child.getNextSibling()) {
        if (child instanceof JactlPsiIdentifierImpl) {
          if (idx < names.length) {
            if (child.getText().equals(names[idx])) {
              sb.append(names[idx++]);
            }
          }
        }
        else if (child.getNode().getElementType() == JactlTokenTypes.DOT) {
          // Add '.' for each element of names (including last one)
          if (idx <= names.length && names.length != 0) {
            sb.append('.');
            idx = idx == names.length ? idx + 1 : idx;
          }
        }
        else {
          sb.append(child.getText());
        }
      }
      // If we still have names left (package name is now longer)
      for (; idx < names.length; idx++) {
        sb.append(names[idx]).append('.');
      }

      // Add rest of the class path
      for (PsiElement child = psiElement; child != null; child = child.getNextSibling()) {
        sb.append(child.getText());
      }
      var newParent = JactlUtils.newElement(psiElement.getProject(), sb.toString(), psiElement.getParent().getNode().getElementType());
      newParent = psiElement.getParent().replace(newParent);
      // Find where we are now in new parent
      return (JactlPsiElement)JactlUtils.getNthChild(newParent, names.length, JactlTokenTypes.IDENTIFIER);
    }
    return psiElement;
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

}
