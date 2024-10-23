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

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.*;
import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.indices.IgnoredFileIndex;
import org.jetbrains.jps.indices.ModuleExcludeIndex;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.module.JpsModule;

import java.io.File;
import java.util.*;

public class JactlBuildTarget extends BuildTarget<JactlBuildRootDescriptor> {

  private JpsModule module;

  protected JactlBuildTarget(JpsModule module, @NotNull BuildTargetType<? extends BuildTarget<JactlBuildRootDescriptor>> targetType) {
    super(targetType);
    this.module = module;
  }

  @Override
  public @NotNull String getId() {
    return module.getName();
  }

  @Override
  public @NotNull Collection<BuildTarget<?>> computeDependencies(@NotNull BuildTargetRegistry buildTargetRegistry, @NotNull TargetOutputIndex targetOutputIndex) {
    return isTests() ? List.of(new JactlBuildTarget(module, PRODUCTION), new JactlBuildTarget(module, TESTS))
                     : List.of(new JactlBuildTarget(module, PRODUCTION));
  }

  private boolean isTests() {
    return ((Type)getTargetType()).isTests;
  }

  @Override
  public @NotNull List<JactlBuildRootDescriptor> computeRootDescriptors(@NotNull JpsModel jpsModel, @NotNull ModuleExcludeIndex moduleExcludeIndex, @NotNull IgnoredFileIndex ignoredFileIndex, @NotNull BuildDataPaths buildDataPaths) {
    List<JactlBuildRootDescriptor> result = new ArrayList<>();
    for (var root: module.getSourceRoots(isTests() ? JavaSourceRootType.TEST_SOURCE : JavaSourceRootType.SOURCE)) {
      result.add(new JactlBuildRootDescriptor(root.getFile(), this));
    }
    return result;
  }

  @Override
  public @Nullable JactlBuildRootDescriptor findRootDescriptor(@NotNull String rootId, @NotNull BuildRootIndex buildRootIndex) {
    List<JactlBuildRootDescriptor> descriptors = buildRootIndex.getRootDescriptors(new File(rootId), Collections.singletonList((Type)getTargetType()), null);
    return ContainerUtil.getFirstItem(descriptors);
  }

  @Override
  public @NotNull String getPresentableName() {
    return "Check Jactl Resources for '" + module.getName() + "' " + (isTests() ? "tests" : "production");
  }

  @Override
  public @NotNull Collection<File> getOutputRoots(@NotNull CompileContext compileContext) {
    JpsJavaExtensionService instance = JpsJavaExtensionService.getInstance();
    if (instance != null) {
      File outputDirectory = instance.getOutputDirectory(module, isTests());
      if (outputDirectory != null) {
        return List.of(outputDirectory);
      }
    }
    return List.of();
  }

  public static final Type PRODUCTION = new Type(false);
  public static final Type TESTS      = new Type(true);

  public static class Type extends BuildTargetType<JactlBuildTarget> {

    boolean isTests;

    protected Type(@NotNull boolean isTests) {
      super("jactl" + (isTests ? "_tests" : ""), true);
      this.isTests = isTests;
    }

    @Override
    public @NotNull List<JactlBuildTarget> computeAllTargets(@NotNull JpsModel jpsModel) {
      return ContainerUtil.map(jpsModel.getProject().getModules(), module -> new JactlBuildTarget(module, this));
    }

    @Override
    public @NotNull BuildTargetLoader<JactlBuildTarget> createLoader(@NotNull JpsModel jpsModel) {
      final Map<String, JpsModule> modules = new HashMap<>();
      for (JpsModule module : jpsModel.getProject().getModules()) {
        modules.put(module.getName(), module);
      }
      return new BuildTargetLoader<JactlBuildTarget>() {
        @Nullable @Override
        public JactlBuildTarget createTarget(@NotNull String targetId) {
          JpsModule module = modules.get(targetId);
          return module != null ? new JactlBuildTarget(module, Type.this) : null;
        }
      };
    }
  }
}
