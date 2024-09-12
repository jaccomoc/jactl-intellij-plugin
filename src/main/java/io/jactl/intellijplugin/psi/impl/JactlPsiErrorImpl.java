package io.jactl.intellijplugin.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import io.jactl.CompileError;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiError;
import org.jetbrains.annotations.NotNull;

public class JactlPsiErrorImpl extends ASTWrapperPsiElement implements JactlPsiError {

  public JactlPsiErrorImpl(@NotNull ASTNode node) {
    super(node);
  }
}
