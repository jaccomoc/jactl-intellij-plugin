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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.BuildRootDescriptor;
import org.jetbrains.jps.builders.BuildTarget;
import org.jetbrains.jps.builders.java.ResourceRootDescriptor;

import java.io.File;
import java.io.FileFilter;

public class JactlBuildRootDescriptor extends BuildRootDescriptor {

  File             rootFile;
  JactlBuildTarget buildTarget;

  public JactlBuildRootDescriptor(File roofile, JactlBuildTarget buildTarget) {
    this.rootFile    = roofile;
    this.buildTarget = buildTarget;
  }

  @Override
  public @NotNull String getRootId() {
    return rootFile.getAbsolutePath();
  }

  @Override
  public @NotNull File getRootFile() {
    return rootFile;
  }

  @Override
  public @NotNull BuildTarget<?> getTarget() {
    return buildTarget;
  }

  @Override
  public @NotNull FileFilter createFileFilter() {
    return file -> file.getName().endsWith(".jactl");
  }
}
