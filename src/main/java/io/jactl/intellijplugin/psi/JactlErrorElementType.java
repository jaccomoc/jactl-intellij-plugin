package io.jactl.intellijplugin.psi;

import com.intellij.psi.tree.IElementType;
import io.jactl.intellijplugin.JactlLanguage;

public class JactlErrorElementType extends IElementType {

  public static final JactlErrorElementType ERROR = new JactlErrorElementType("ERROR");

  public JactlErrorElementType(String name) {
    super(name, JactlLanguage.INSTANCE);
  }
}
