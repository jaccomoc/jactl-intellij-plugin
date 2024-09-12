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

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import io.jactl.intellijplugin.JactlIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JactlRunConfigurationType implements ConfigurationType {

  private final JactlConfigurationFactory configurationFactory;

  public JactlRunConfigurationType() {
    configurationFactory = new JactlConfigurationFactory(this);
  }

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
    return "Jactl";
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) String getConfigurationTypeDescription() {
    return "Jactl class or script";
  }

  @Override
  public Icon getIcon() {
    return JactlIcons.FILE;
  }

  @Override
  public @NotNull @NonNls String getId() {
    return "JactlRunConfiguration";
  }

  public static JactlRunConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(JactlRunConfigurationType.class);
  }

  public JactlConfigurationFactory getConfigurationFactory() {
    return configurationFactory;
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{ configurationFactory };
  }

  private static class JactlConfigurationFactory extends ConfigurationFactory {
    JactlConfigurationFactory(ConfigurationType type) { super(type); }

    @Override
    public @NotNull @NonNls String getId() {
      return "Jactl";
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new JactlRunConfiguration("Jactl Script", project, this);
    }
  }
}
