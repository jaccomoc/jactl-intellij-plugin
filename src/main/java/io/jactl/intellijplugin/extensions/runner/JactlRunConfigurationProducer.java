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

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import io.jactl.intellijplugin.JactlFile;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.extensions.settings.JactlConfigurable;
import io.jactl.intellijplugin.extensions.settings.JactlConfiguration;
import io.jactl.intellijplugin.jpsplugin.builder.JpsJactlSettings;
import io.jactl.intellijplugin.psi.JactlNameElementType;
import io.jactl.intellijplugin.psi.impl.JactlPsiNameImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JactlRunConfigurationProducer extends LazyRunConfigurationProducer<JactlRunConfiguration> {

  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return JactlRunConfigurationType.getInstance().getConfigurationFactory();
  }

  @Override
  protected boolean setupConfigurationFromContext(@NotNull JactlRunConfiguration configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
    var location = context.getLocation();
    if (location == null) { return false; }
    var element = location.getPsiElement();
    var file = element.getContainingFile();
    if (file instanceof JactlFile jactlFile) {
      var virtualFile = jactlFile.getVirtualFile();
      if (virtualFile == null) { return false; }
      var firstClass = (JactlPsiNameImpl)JactlUtils.getFirstDescendant(jactlFile, JactlNameElementType.CLASS);
      if (firstClass != null && firstClass.isTopLevelClass()) {
        // Can only run scripts not classes
        return false;
      }
      sourceElement.set(element);
      VirtualFile parent = virtualFile.getParent();
      configuration.setWorkingDirectory(parent == null ? null : parent.getPath());
      configuration.setName(virtualFile.getName());
      configuration.setScriptPath(virtualFile.getCanonicalPath());
      configuration.setModule(ModuleUtilCore.findModuleForPsiElement(jactlFile));
      configuration.setGlobalVariablesScript(JactlConfiguration.getInstance(configuration.getProject()).getGlobalVariablesScript());
      configuration.setVerboseEnabled(JactlConfiguration.getInstance(configuration.getProject()).isVerboseEnabled());
      return true;
    }
    return false; // Not a Jactl file
  }

  @Override
  public boolean isConfigurationFromContext(@NotNull JactlRunConfiguration configuration, @NotNull ConfigurationContext context) {
    Location<PsiElement> location   = context.getLocation();
    PsiElement           psiElement = location == null ? null : location.getPsiElement();
    return configuration.getScriptPath().equals(JactlUtils.getPathForElement(psiElement));
  }
}
