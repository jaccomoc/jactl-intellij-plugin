package io.jactl.intellijplugin;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import io.jactl.Stmt;
import io.jactl.intellijplugin.common.JactlPlugin;
import io.jactl.intellijplugin.psi.JactlNameElementType;
import io.jactl.intellijplugin.psi.JactlPsiElement;
import io.jactl.intellijplugin.psi.JactlStmtElementType;
import io.jactl.intellijplugin.psi.impl.JactlPsiNameImpl;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class JactlFile extends PsiFileBase implements JactlPsiElement {

  public JactlFile(FileViewProvider viewProvider) {
    super(viewProvider, JactlLanguage.INSTANCE);
  }

  /**
   * Get the base file name without its suffix. Turns .../src/main/jactl/a/b/c/X.jactl into "X".
   * @return the base file name
   */
  public String getFileNameNoSuffix() {
    String name = getVirtualFile().getName();
    if (name.endsWith(JactlPlugin.SUFFIX)) {
      name = name.substring(0, name.length() - JactlPlugin.SUFFIX.length());
    }
    return JactlPlugin.removeSuffix(name);
  }

  /**
   * Return which package the file belongs to.
   * E.g. return "a.b.c" for ".../src/main/jactl/a/b/c/X.jactl"
   * @return the package name
   */
  public String getPackageName() {
    return JactlUtils.packageNameFor(this);
  }

  @Override
  public @NotNull FileType getFileType() {
    return JactlFileType.INSTANCE;
  }

  @Override
  public String toString() {
    return "Jactl File[0x" + Long.toHexString(System.identityHashCode(this)) + "]";
  }

  @Override
  public JactlFile getFile() {
    return this;
  }

  @Override
  public JactlAstKey getAstKey() {
    return new JactlAstKey(this, JactlNameElementType.JACTL_FILE, 0);
  }

  @Override
  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    if (getTopLevelClass() != null) {
      var className = (JactlPsiNameImpl)JactlUtils.getFirstDescendant(topClassPsiElement(), JactlPsiNameImpl.class);
      className.setName(name);
    }
    return super.setName(name);
  }

  /**
   * Only set file name. Do not change name of any top level class in this file.
   * @param name the new name
   * @throws IncorrectOperationException
   */
  public void setFileName(@NotNull String name) throws IncorrectOperationException {
    super.setName(name);
  }

  /**
   * Return the top level class for a class file or null if we are a script file
   * @return the top level class or null
   */
  public Stmt.ClassDecl getTopLevelClass() {
    var topClass = topClassPsiElement();
    var jactlNode = topClass == null ? null : topClass.getJactlAstNode();
    if (jactlNode instanceof Stmt.ClassDecl classDecl && classDecl.isPrimaryClass) {
      return classDecl;
    }
    return null;
  }

  public JactlPsiElement topClassPsiElement() {
    return (JactlPsiElement)JactlUtils.getFirstDescendant(this, JactlStmtElementType.CLASS_DECL);
  }

  public boolean isScriptFile() {
    var classDecl = getTopLevelClass();
    // Script if no top level class or top level class does not match file name
    // (such as when editing a script and first stmt is a class declaration and
    // editing not yet complete).
    return classDecl == null || !classDecl.name.getStringValue().equals(JactlPlugin.removeSuffix(this.getName()));
  }
}
