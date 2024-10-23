package io.jactl.intellijplugin.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.util.IncorrectOperationException;
import io.jactl.JactlUserDataHolder;
import io.jactl.Stmt;
import io.jactl.intellijplugin.psi.AbstractJactlPsiStmt;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiStmt;
import org.jetbrains.annotations.NotNull;

public class JactlPsiStmtImpl extends AbstractJactlPsiStmt implements JactlPsiStmt {
  public JactlPsiStmtImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void delete() throws IncorrectOperationException {
    // Special case for top level class where we need to also delete the file
    JactlUserDataHolder node = getJactlAstNode();
    if (node instanceof Stmt.ClassDecl && ((Stmt.ClassDecl) node).isPrimaryClass) {
      Stmt.ClassDecl classDecl = (Stmt.ClassDecl) node;
      getFile().delete();
      return;
    }
    super.delete();
  }
}
