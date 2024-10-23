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

package io.jactl.intellijplugin.extensions.runner;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.ui.CommonJavaParametersPanel;
import com.intellij.execution.ui.DefaultJreSelector;
import com.intellij.execution.ui.JrePathEditor;
import com.intellij.execution.util.ScriptFileUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.util.ui.UIUtil;
import io.jactl.intellijplugin.JactlFileType;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.common.JactlBundle;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

public class JactlRunConfigurationEditor extends SettingsEditor<JactlRunConfiguration> implements PanelWithAnchor {
  private JPanel                                      mainPanel;
  private LabeledComponent<TextFieldWithBrowseButton> scriptPathComponent;
  private CommonJavaParametersPanel                   javaParametersPanel;
  private LabeledComponent<ModulesComboBox>           modulesComboBoxComponent;
  private JrePathEditor                               jrePathEditor;
  private JCheckBox                                   verbose;
  private TextFieldWithBrowseButton                   globalVariablesScript;

  private JComponent                                  anchor;

  public JactlRunConfigurationEditor(Project project) {
    anchor = UIUtil.mergeComponentsWithAnchor(scriptPathComponent, javaParametersPanel, modulesComboBoxComponent, jrePathEditor);
    TextFieldWithBrowseButton scriptPath = scriptPathComponent.getComponent();
    scriptPath.addBrowseFolderListener(
      JactlBundle.message("script.runner.chooser.title"),
      JactlBundle.message("script.runner.chooser.description"),
      project,
      FileChooserDescriptorFactory.createSingleFileDescriptor(JactlFileType.INSTANCE)
    );

    final ModulesComboBox modulesComboBox = modulesComboBoxComponent.getComponent();
    modulesComboBox.addActionListener(e -> javaParametersPanel.setModuleContext(modulesComboBox.getSelectedModule()));
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    Function0<Boolean> productionOnly = () -> {
      VirtualFile script = ScriptFileUtil.findScriptFileByPath(scriptPath.getText());
      return script != null && !fileIndex.isInTestSourceContent(script);
    };
    jrePathEditor.setDefaultJreSelector(
      new DefaultJreSelector.SdkFromModuleDependencies<>(modulesComboBox, ModulesComboBox::getSelectedModule, productionOnly) {
        @Override public void addChangeListener(@NotNull Runnable listener) {
          super.addChangeListener(listener);
          scriptPath.getChildComponent().getDocument().addDocumentListener(new DocumentAdapter() {
              @Override protected void textChanged(@NotNull DocumentEvent e) { listener.run(); }
          });
        }
      });

    globalVariablesScript.addBrowseFolderListener(JactlBundle.message("jactl.configuration.globals.script.selector.title"), JactlBundle.message("jactl.configuration.globals.script.selector.description"),
                                                  project,
                                                  new FileChooserDescriptor(true, false, false, false, false, false)
                                                    .withRoots(JactlUtils.getSourceRootFiles(project)));
  }

  @Override
  protected void resetEditorFrom(@NotNull JactlRunConfiguration config) {
    scriptPathComponent.getComponent().setText(config.getScriptPath());
    javaParametersPanel.reset(config);
    modulesComboBoxComponent.getComponent().setModules(config.getValidModules());
    modulesComboBoxComponent.getComponent().setSelectedModule(config.getConfigurationModule().getModule());
    jrePathEditor.setPathOrName(config.getAlternativeJrePath(), config.isAlternativeJrePathEnabled());
    globalVariablesScript.setText(config.getGlobalVariablesScript());
    verbose.setSelected(config.isVerboseEnabled());
  }

  @Override
  protected void applyEditorTo(@NotNull JactlRunConfiguration config) {
    config.setScriptPath(scriptPathComponent.getComponent().getText().trim());
    javaParametersPanel.applyTo(config);
    config.setModule(modulesComboBoxComponent.getComponent().getSelectedModule());
    config.setAlternativeJrePathEnabled(jrePathEditor.isAlternativeJreSelected());
    config.setAlternativeJrePath(jrePathEditor.getJrePathOrName());
    config.setGlobalVariablesScript(globalVariablesScript.getText());
    config.setVerboseEnabled(verbose.isSelected());
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return mainPanel;
  }

  @Override
  public JComponent getAnchor() {
    return anchor;
  }

  @Override
  public void setAnchor(@Nullable JComponent anchor) {
    this.anchor = anchor;
    scriptPathComponent.setAnchor(anchor);
    javaParametersPanel.setAnchor(anchor);
    modulesComboBoxComponent.setAnchor(anchor);
    jrePathEditor.setAnchor(anchor);
  }
}
