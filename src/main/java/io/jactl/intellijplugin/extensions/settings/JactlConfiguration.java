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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import io.jactl.intellijplugin.common.JactlPlugin;
import io.jactl.intellijplugin.jpsplugin.builder.JpsJactlSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Service(Service.Level.PROJECT)
@State(name="JactlConfiguration",storages = @Storage("jactl.xml"))
public final class JactlConfiguration implements PersistentStateComponent<JpsJactlSettings> {

  private String             globalVariablesScript;
  private boolean            verboseEnabled;

  public static JactlConfiguration getInstance(Project project) {
    return project.getService(JactlConfiguration.class);
  }

  @Override
  public @Nullable JpsJactlSettings getState() {
    var bean = new JpsJactlSettings();
    bean.globalVariablesScript = globalVariablesScript;
    bean.verboseEnable         = verboseEnabled;
    return bean;
  }

  @Override
  public void loadState(@NotNull JpsJactlSettings state) {
    globalVariablesScript = state.globalVariablesScript;
  }

  public @Nullable String getGlobalVariablesScript() {
    return globalVariablesScript;
  }

  public boolean isVerboseEnabled() {
    return verboseEnabled;
  }

  public Map<String,Object> getGlobals() {
    return JactlPlugin.getGlobals(globalVariablesScript);
  }

  public void setGlobalVariablesScript(String scriptPath) {
    globalVariablesScript = scriptPath;
  }

  public void setVerboseEnabled(boolean verboseEnabled) {
    this.verboseEnabled = verboseEnabled;
  }
}