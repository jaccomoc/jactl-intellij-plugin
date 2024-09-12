package io.jactl.intellijplugin.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import io.jactl.Stmt;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.psi.AbstractJactlPsiStmt;
import io.jactl.intellijplugin.psi.JactlNameElementType;
import io.jactl.intellijplugin.psi.JactlPsiElement;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiName;
import io.jactl.runtime.ClassDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JactlPsiNameImpl extends AbstractJactlPsiStmt implements JactlPsiName {

  JactlNameElementType type;

  public JactlPsiNameImpl(ASTNode node, JactlNameElementType type) {
    super(node);
    this.type = type;
  }

  public JactlNameElementType getType() {
    return type;
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    return this;
  }

  @Override
  public String getName() {
    String name = getText();
    return name;
  }

  @Override
  public PsiElement setName(@NlsSafe @NotNull String name) throws IncorrectOperationException {
    PsiElement oldElement = JactlUtils.getFirstChild(this, JactlPsiIdentifierImpl.class);
    PsiElement newElement = JactlUtils.newElement(getProject(), type, name, JactlPsiIdentifierImpl.class);
    if (isTopLevelClass()) {
      if (!getFile().getFileNameNoSuffix().equals(getName())) {
        throw new IncorrectOperationException("Cannot rename (class name does not match file name)");
      }
    }
    oldElement.replace(newElement);
    if (isTopLevelClass()) {
      getFile().setFileName(name + ".jactl");
    }
    return this;
  }

  public boolean isTopLevelClass() {
    if (type == JactlNameElementType.CLASS) {
      // Check if top level class in a class file and verify that class name matches file name
      PsiElement parent = getParent();
      if (parent instanceof JactlPsiElement jactlPsiElement) {
        var jactlAstNode = jactlPsiElement.getJactlAstNode();
        return jactlAstNode instanceof Stmt.ClassDecl classDecl && classDecl.isPrimaryClass;
      }
    }
    return false;
  }

  @Override
  public @NotNull SearchScope getUseScope() {
    if (type == JactlNameElementType.CLASS) {
      var jactlAstNode = ((JactlPsiElement) getParent()).getJactlAstNode();
      var block = jactlAstNode.getBlock();
      // If are a class nested within a top level class then we are potentially visible to global scope
      for (ClassDescriptor descriptor = block.owningClass.classDescriptor; descriptor != null; descriptor = descriptor.getEnclosingClass()) {
        if (descriptor.isTopLevelClass()) {
          return GlobalSearchScope.projectScope(getProject());
        }
      }
    }
    return new LocalSearchScope(getFile());
  }

  @Override
  public void delete() throws IncorrectOperationException {
    // If we are being deleted we need to delete the actual class/function/method/field instead
    switch (JactlNameElementType.getNameType(type)) {
      case FILE,PACKAGE -> throw new IncorrectOperationException("Invalid operation for " + type);
      case CLASS,FUNCTION,METHOD,VARIABLE,PARAMETER -> getParent().delete();
    }
  }
}
