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

package io.jactl.intellijplugin.jpsplugin.builder;

import io.jactl.intellijplugin.common.JactlPlugin;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

import java.util.Map;

public class JpsJactlSettings extends JpsElementBase<JpsJactlSettings> {
  static final JpsElementChildRole<JpsJactlSettings> ROLE = JpsElementChildRoleBase.create("Jactl Configuration");

  public String  globalVariablesScript = "";
  public boolean verboseEnable = false;

  public static JpsJactlSettings getSettings(JpsProject project) {
    var settings = project.getContainer().getChild(ROLE);
    return settings == null ? new JpsJactlSettings() : settings;
  }

  public Map<String,Object> getGlobals() {
    return JactlPlugin.getGlobals(globalVariablesScript);
  }
}
