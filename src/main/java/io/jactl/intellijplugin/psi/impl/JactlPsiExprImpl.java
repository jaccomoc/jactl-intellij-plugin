package io.jactl.intellijplugin.psi.impl;

import com.intellij.lang.ASTNode;
import io.jactl.Expr;
import io.jactl.intellijplugin.psi.AbstractJactlPsiExpr;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiExpr;
import org.jetbrains.annotations.NotNull;

public class JactlPsiExprImpl extends AbstractJactlPsiExpr implements JactlPsiExpr {

  public JactlPsiExprImpl(@NotNull ASTNode node) {
    super(node);
  }
}
