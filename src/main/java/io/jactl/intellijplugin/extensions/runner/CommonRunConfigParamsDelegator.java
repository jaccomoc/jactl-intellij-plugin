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

import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class CommonRunConfigParamsDelegator implements CommonProgramRunConfigurationParameters {
  CommonProgramRunConfigurationParameters delegate;

  public CommonRunConfigParamsDelegator(CommonProgramRunConfigurationParameters delegate) {
    this.delegate = delegate;
  }

  @Override
  public Project getProject() {
    return delegate.getProject();
  }

  @Override
  public void setProgramParameters(@Nullable String value) {
    delegate.setProgramParameters(value);
  }

  @Override
  public void setWorkingDirectory(@Nullable String value) {
    delegate.setWorkingDirectory(value);
  }

  @Override
  @Nullable
  public String getWorkingDirectory() {
    return delegate.getWorkingDirectory();
  }

  @Override
  public void setEnvs(@NotNull Map<String, String> envs) {
    delegate.setEnvs(envs);
  }

  @Override
  public @NotNull Map<String, String> getEnvs() {
    return delegate.getEnvs();
  }

  @Override
  public void setPassParentEnvs(boolean passParentEnvs) {
    delegate.setPassParentEnvs(passParentEnvs);
  }

  @Override
  public boolean isPassParentEnvs() {
    return delegate.isPassParentEnvs();
  }
}
