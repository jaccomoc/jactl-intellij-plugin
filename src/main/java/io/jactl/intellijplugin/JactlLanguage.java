package io.jactl.intellijplugin;

import com.intellij.lang.Language;

public class JactlLanguage extends Language {

  public static final JactlLanguage INSTANCE = new JactlLanguage();

  private JactlLanguage() {
    super("Jactl");
  }


}
