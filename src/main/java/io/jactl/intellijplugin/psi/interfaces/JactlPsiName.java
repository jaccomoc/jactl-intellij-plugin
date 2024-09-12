package io.jactl.intellijplugin.psi.interfaces;

import com.intellij.psi.PsiNameIdentifierOwner;
import io.jactl.intellijplugin.psi.JactlNameElementType;
import io.jactl.intellijplugin.psi.JactlPsiElement;

public interface JactlPsiName extends PsiNameIdentifierOwner, JactlPsiElement {
  boolean isTopLevelClass();
  JactlNameElementType getType();
}
