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
import org.jetbrains.jps.builders.BuildTargetType;
import org.jetbrains.jps.incremental.BuilderService;
import org.jetbrains.jps.incremental.ModuleLevelBuilder;
import org.jetbrains.jps.incremental.TargetBuilder;

import java.util.List;

public class JactlBuilderService extends BuilderService {

  @Override
  public @NotNull List<? extends BuildTargetType<?>> getTargetTypes() {
    return List.of(io.jactl.intellijplugin.jpsplugin.builder.JactlBuildTarget.PRODUCTION, io.jactl.intellijplugin.jpsplugin.builder.JactlBuildTarget.TESTS);
  }

  @Override
  public @NotNull List<? extends ModuleLevelBuilder> createModuleLevelBuilders() {
    return List.of(new JactlBuilder());
  }

  @Override
  public @NotNull List<? extends TargetBuilder<?, ?>> createBuilders() {
    return List.of();
  }
}