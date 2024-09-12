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

package io.jactl.intellijplugin.extensions.compiler;

import com.intellij.compiler.impl.BuildTargetScopeProvider;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import io.jactl.intellijplugin.JactlFileType;
import io.jactl.intellijplugin.jpsplugin.builder.JactlBuildTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.api.CmdlineProtoUtil;
import org.jetbrains.jps.api.CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.List;
import java.util.stream.Stream;

public class JactlTargetScopeProvider extends BuildTargetScopeProvider {
  @Override
  public @NotNull List<TargetTypeBuildScope> getBuildTargetScopes(@NotNull CompileScope baseScope, @NotNull Project project, boolean forceBuild) {
    return List.of(createTargetScope(project, JavaSourceRootType.SOURCE, JactlBuildTarget.PRODUCTION, forceBuild),
                   createTargetScope(project, JavaSourceRootType.TEST_SOURCE, JactlBuildTarget.TESTS, forceBuild));
  }

  private TargetTypeBuildScope createTargetScope(Project project, JpsModuleSourceRootType<?> rootType, JactlBuildTarget.Type targetType, boolean forceBuild) {
    List<Module> jactlModules = getJactlModules(project, rootType);
    return CmdlineProtoUtil.createTargetsScope(targetType.getTypeId(),
                                               jactlModules.stream().map(Module::getName).toList(),
                                               forceBuild);
  }

  private List<Module> getJactlModules(Project project, JpsModuleSourceRootType<?> rootType) {
    return Stream.of(ModuleManager.getInstance(project).getModules())
                 .filter(module -> containsJactlResource(module, rootType))
                 .toList();
  }

  private boolean containsJactlResource(Module module, JpsModuleSourceRootType<?> rootType) {
    ModuleRootManager moduleManager = ModuleRootManager.getInstance(module);
    List<VirtualFile> roots         = moduleManager.getSourceRoots(rootType);
    return roots.stream().anyMatch(root -> containsJactlFile(module, root));
  }

  private static boolean containsJactlFile(Module module, VirtualFile root) {
    boolean iterationAborted = ModuleRootManager.getInstance(module)
                                                .getFileIndex()
                                                .iterateContentUnderDirectory(root, file -> !isJactlFile(file)); // Stop once we have found a jactl file
    // If tree work aborted due to finding Jactl file then return true
    return !iterationAborted;
  }

  private static boolean isJactlFile(VirtualFile file) {
    return !file.isDirectory() && FileTypeRegistry.getInstance().isFileOfType(file, JactlFileType.INSTANCE);
  }
}
