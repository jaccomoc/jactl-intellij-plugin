package io.jactl.intellijplugin.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiReference;
import io.jactl.Expr;
import io.jactl.intellijplugin.psi.AbstractJactlPsiExpr;
import io.jactl.intellijplugin.psi.JactlPsiReference;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiIdentifierExpr;
import org.jetbrains.annotations.NotNull;

public class JactlPsiIdentifierExprImpl extends AbstractJactlPsiExpr implements JactlPsiIdentifierExpr {

  public JactlPsiIdentifierExprImpl(@NotNull ASTNode node) {
    super(node);
  }
}
