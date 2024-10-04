package io.jactl.intellijplugin.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.psi.AbstractJactlPsiType;
import io.jactl.intellijplugin.psi.JactlPsiElement;
import io.jactl.intellijplugin.psi.JactlPsiReference;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiType;
import org.jetbrains.annotations.NotNull;

import static io.jactl.intellijplugin.psi.JactlExprElementType.CLASS_PATH_EXPR;

public class JactlPsiTypeImpl extends AbstractJactlPsiType implements JactlPsiType {

  private final boolean isBuiltIn;
//  private final JactlCachedValue<JactlPsiTypeReference> reference;

  public JactlPsiTypeImpl(@NotNull ASTNode node, boolean isBuiltIn) {
    super(node);
    this.isBuiltIn = isBuiltIn;
//    this.reference = new JactlCachedValue<>(() -> new JactlPsiTypeReference(this, getAstKey()));
  }

  public boolean isBuiltIn() {
    return isBuiltIn;
  }

  /**
   * If a class reference then return the PsiElement for the class
   */
  public JactlPsiElement findClassDefinition() {
    if (isBuiltIn) {
      return null;
    }
    JactlPsiElement classPathExpr = (JactlPsiElement)JactlUtils.getFirstChild(this, CLASS_PATH_EXPR);
    return (JactlPsiElement)new JactlPsiReference(classPathExpr == null ? this : classPathExpr).resolve();
  }

//  @Override public PsiReference getReference() {
//    if (isBuiltin) {
//      return null;
//    }
//    return reference.getValue();
//  }

  @Override
  public void subtreeChanged() {
//    super.subtreeChanged();
//    reference.clear();
  }
}
