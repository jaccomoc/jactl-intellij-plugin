package io.jactl.intellijplugin.psi;

import com.intellij.lang.ASTNode;
import io.jactl.Expr;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractJactlPsiExpr extends AbstractJactlPsiElement {

  protected AbstractJactlPsiExpr(@NotNull ASTNode node) {
    super(node);
  }
}
