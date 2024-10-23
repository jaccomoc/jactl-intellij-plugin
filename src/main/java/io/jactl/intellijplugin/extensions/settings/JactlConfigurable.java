/*
 * Copyright © 2022,2023,2024  James Crawford
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.jactl.intellijplugin.extensions.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.io.FileUtil;
import io.jactl.intellijplugin.common.JactlBundle;
import io.jactl.intellijplugin.common.JactlPlugin;
import io.jactl.intellijplugin.jpsplugin.builder.GlobalsException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class JactlConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  private JactlConfigurationPanel panel;
  private JactlConfiguration      settings;

  public JactlConfigurable(Project project) {
    settings = JactlConfiguration.getInstance(project);
  }

  @Override
  public @NotNull @NonNls String getId() {
    return "prefences.Jactl";
  }

  @Override
  public @NlsContexts.ConfigurableName String getDisplayName() {
    return JactlBundle.message("jactl.configuration.settings.display.name");
  }

  @Override
  public @Nullable JComponent createComponent() {
    panel = new JactlConfigurationPanel();
    return panel.mainPanel;
  }

  @Override
  public boolean isModified() {
    return panel.isModified(settings);
  }

  @Override
  public void apply() throws ConfigurationException {
    panel.apply(settings);
  }

  @Override
  public void reset() {
    panel.reset(settings);
  }

  public static class JactlConfigurationPanel {
    private TextFieldWithBrowseButton globalVariablesScript;
    private JPanel     mainPanel;

    private void apply(JactlConfiguration settings) throws ConfigurationException {
      String fileName = FileUtil.toSystemIndependentName(globalVariablesScript.getText().trim());
      if (!fileName.isEmpty()) {
        try {
          JactlPlugin.getGlobals(fileName);
        }
        catch (GlobalsException e) {
          throw new ConfigurationException(e.getMessage());
        }
      }
      settings.setGlobalVariablesScript(fileName);
    }

    private void reset(JactlConfiguration settings) {
      String scriptPath = settings.getGlobalVariablesScript();
      if (scriptPath != null) {
        globalVariablesScript.setText(FileUtil.toSystemDependentName(scriptPath));
      }
    }

    private boolean isModified(JactlConfiguration settings) {
      String settingsGlobalScriptPath = settings.getGlobalVariablesScript();
      if (settingsGlobalScriptPath != null) {
        settingsGlobalScriptPath = FileUtil.toSystemIndependentName(settingsGlobalScriptPath.trim());
      }
      String panelGlobalScriptPath = FileUtil.toSystemIndependentName(globalVariablesScript.getText().trim());
      return !panelGlobalScriptPath.equals(settingsGlobalScriptPath);
    }

    private void createUIComponents() {
      globalVariablesScript = new TextFieldWithBrowseButton();
      globalVariablesScript.addBrowseFolderListener(JactlBundle.message("jactl.configuration.globals.script.selector.title"),
                                                    JactlBundle.message("jactl.configuration.globals.script.selector.description"),
                                                    null,
                                                    FileChooserDescriptorFactory.createSingleFileDescriptor());
    }
  }
}
