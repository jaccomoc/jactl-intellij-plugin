package io.jactl.intellijplugin.psi;

import com.intellij.lang.ASTNode;
import io.jactl.JactlType;
import org.jetbrains.annotations.NotNull;

public class AbstractJactlPsiType extends AbstractJactlPsiElement {

  protected AbstractJactlPsiType(@NotNull ASTNode node) {
    super(node);
  }
}
