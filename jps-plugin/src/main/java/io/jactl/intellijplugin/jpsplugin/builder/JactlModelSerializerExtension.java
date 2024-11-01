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

import com.intellij.util.xmlb.XmlSerializer;
import io.jactl.Utils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;

import java.util.List;

public class JactlModelSerializerExtension extends JpsModelSerializerExtension {
  @Override
  public @NotNull List<? extends JpsProjectExtensionSerializer> getProjectExtensionSerializers() {
    return Utils.listOf(new JpsProjectExtensionSerializer("jactl.xml", "JactlConfiguration") {
      @Override
      public void loadExtension(@NotNull JpsProject project, @NotNull Element componentTag) {
        JpsJactlSettings configuration = XmlSerializer.deserialize(componentTag, JpsJactlSettings.class);
        project.getContainer().setChild(JpsJactlSettings.ROLE, configuration);
      }
    });
  }
}
