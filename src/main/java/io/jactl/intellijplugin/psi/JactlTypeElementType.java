package io.jactl.intellijplugin.psi;

import com.intellij.psi.tree.IElementType;
import io.jactl.intellijplugin.JactlLanguage;

public class JactlTypeElementType extends IElementType {
  public static IElementType CLASS_TYPE = new JactlTypeElementType("CLASS_TYPE");
  public static IElementType BUILT_IN_TYPE = new JactlTypeElementType("BUILT_IN_TYPE");

  public JactlTypeElementType(String name) {
    super(name, JactlLanguage.INSTANCE);
  }
}
