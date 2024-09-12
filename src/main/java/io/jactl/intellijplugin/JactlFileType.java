package io.jactl.intellijplugin;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JactlFileType extends LanguageFileType {

  public static final JactlFileType INSTANCE = new JactlFileType();

  private JactlFileType() {
    super(JactlLanguage.INSTANCE);
  }

  @Override
  public @NonNls @NotNull String getName() {
    return "Jactl File";
  }

  @Override
  public @NlsContexts.Label @NotNull String getDescription() {
    return "Jactl script file";
  }

  @Override
  public @NlsSafe @NotNull String getDefaultExtension() {
    return "jactl";
  }

  @Override
  public Icon getIcon() {
    return JactlIcons.FILE;
  }
}
