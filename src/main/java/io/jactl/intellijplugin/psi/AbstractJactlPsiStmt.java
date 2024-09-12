package io.jactl.intellijplugin.psi;

import com.intellij.lang.ASTNode;
import io.jactl.Stmt;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractJactlPsiStmt extends AbstractJactlPsiElement {
  protected AbstractJactlPsiStmt(@NotNull ASTNode node) {
    super(node);
  }
}
