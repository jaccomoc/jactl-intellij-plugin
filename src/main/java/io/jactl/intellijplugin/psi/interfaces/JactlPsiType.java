package io.jactl.intellijplugin.psi.interfaces;

import com.intellij.psi.PsiElement;
import io.jactl.intellijplugin.psi.JactlPsiElement;

public interface JactlPsiType extends JactlPsiElement {
  boolean isBuiltIn();
  JactlPsiElement findClassDefinition();
}
