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

import com.intellij.execution.CantRunException;
import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.execution.util.ProgramParametersUtil;
import com.intellij.execution.util.ScriptFileUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ui.configuration.ClasspathEditor;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.listeners.RefactoringElementAdapter;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.util.net.HttpConfigurable;
import io.jactl.Utils;
import io.jactl.intellijplugin.JactlFile;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.common.JactlBundle;
import io.jactl.intellijplugin.common.JactlPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JactlRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule, Object>
                                   implements CommonJavaRunConfigurationParameters, RefactoringListenerProvider {

  private String scriptPath;
  private String workingDir;
  private String vmParams;
  private boolean isAlternativeJrePathEnabled;
  private String alternativeJrePath;
  private String programParams;
  private boolean isPassParentEnvs;

  private Map<String,String> envs = new HashMap<>();

  public JactlRunConfiguration(String name, @NotNull Project project, @NotNull ConfigurationFactory factory) {
    super(name, new RunConfigurationModule(project), factory);
  }

  @Override
  public void setVMParameters(@Nullable String s) {
    vmParams = s;
  }

  @Override
  public String getVMParameters() {
    return vmParams;
  }

  @Override
  public boolean isAlternativeJrePathEnabled() {
    return isAlternativeJrePathEnabled;
  }

  @Override
  public void setAlternativeJrePathEnabled(boolean b) {
    isAlternativeJrePathEnabled = b;
  }

  @Override
  public @Nullable String getAlternativeJrePath() {
    return alternativeJrePath;
  }

  @Override
  public void setAlternativeJrePath(@Nullable String s) {
    alternativeJrePath = s;
  }

  @Override
  public @Nullable String getRunClass() {
    return null;
  }

  @Override
  public @Nullable String getPackage() {
    return null;
  }

  @Override
  public void setProgramParameters(@Nullable String value) {
    programParams = value;
  }

  @Override
  public @Nullable String getProgramParameters() {
    return programParams;
  }

  @Override
  public void setWorkingDirectory(@Nullable String value) {
    workingDir = value;
  }

  @Override
  public @Nullable String getWorkingDirectory() {
    return workingDir;
  }

  @Override
  public void setEnvs(@NotNull Map<String, String> envs) {
    this.envs.clear();
    this.envs.putAll(envs);
  }

  @Override
  public @NotNull Map<String, String> getEnvs() {
    return envs;
  }

  @Override
  public void setPassParentEnvs(boolean passParentEnvs) {
    this.isPassParentEnvs = passParentEnvs;
  }

  @Override
  public boolean isPassParentEnvs() {
    return isPassParentEnvs;
  }

  public void setScriptPath(String path) {
    scriptPath = path;
  }

  public String getScriptPath() {
    return scriptPath;
  }

  @Override
  public Collection<Module> getValidModules() {
    return Arrays.asList(ModuleManager.getInstance(getProject()).getModules());
  }

  private Module getModule() {
    var module = getConfigurationModule().getModule();
    if (module != null) {
      return module;
    }
    Module[] modules = ModuleManager.getInstance(getProject()).getModules();
    return modules.length == 0 ? null : modules[0];
  }

  @Override
  public @Nullable RefactoringElementListener getRefactoringElementListener(PsiElement element) {
    if (scriptPath == null || !scriptPath.equals(JactlUtils.getPathForElement(element))) {
      return null;
    }
    if (element instanceof JactlFile) {
      return new RefactoringElementAdapter() {
        @Override protected void elementRenamedOrMoved(@NotNull PsiElement newElement) {
          if (newElement instanceof JactlFile) { setScriptPath(JactlUtils.getPathForElement(newElement)); }
        }
        @Override public void undoElementMovedOrRenamed(@NotNull PsiElement newElement, @NotNull String oldQualifiedName) {
          elementRenamedOrMoved(newElement);
        }
      };
    }
    return null;
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new JactlRunConfigurationEditor(getProject());
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    super.checkConfiguration();

    VirtualFile scriptVirtualFile = ScriptFileUtil.findScriptFileByPath(scriptPath);
    if (scriptVirtualFile == null) {
      throw new RuntimeConfigurationException(JactlBundle.message("script.runner.error.no.script", scriptPath));
    }

    PsiFile file = PsiManager.getInstance(getProject()).findFile(scriptVirtualFile);
    if (!(file instanceof JactlFile)) {
      throw new RuntimeConfigurationException(JactlBundle.message("script.runner.error.bad.type", scriptPath));
    }

    JavaParametersUtil.checkAlternativeJRE(this);

    Module module = getModule();
    if (getJactlJarFile(module) == null) {
      RuntimeConfigurationException e = new RuntimeConfigurationException(
        JactlBundle.message("script.runner.error.no.jactl.for.module", module.getName())
      );
      e.setQuickFix(() -> ModulesConfigurator.showDialog(module.getProject(), module.getName(), ClasspathEditor.getName()));
      throw e;
    }
  }

  @Override
  public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
    final VirtualFile scriptFile = ScriptFileUtil.findScriptFileByPath(getScriptPath());
    if (scriptFile == null) return null;

    return new JavaCommandLineState(environment) {
      @NotNull @Override
      protected OSProcessHandler startProcess() throws ExecutionException {
        final OSProcessHandler handler = super.startProcess();
        handler.setShouldDestroyProcessRecursively(true);
        return handler;
      }

      @Override
      protected JavaParameters createJavaParameters() throws ExecutionException {
        final Module module = getModule();
        final boolean tests = ProjectRootManager.getInstance(getProject()).getFileIndex().isInTestSourceContent(scriptFile);
        String jrePath = isAlternativeJrePathEnabled() ? getAlternativeJrePath() : null;
        JavaParameters params = new JavaParameters();
        params.setUseClasspathJar(true);
        params.setDefaultCharset(getProject());
        params.setJdk(
          module == null ? JavaParametersUtil.createProjectJdk(getProject(), jrePath)
                         : JavaParametersUtil.createModuleJdk(module, !tests, jrePath)
        );
        ProgramParametersUtil.configureConfiguration(params, new CommonRunConfigParamsDelegator(JactlRunConfiguration.this) {
          @Override public @Nullable String getProgramParameters() { return null; }
        });
        configureCommandLine(params, module, tests);

        return params;
      }
    };
  }

  private void configureCommandLine(JavaParameters params, Module module, boolean tests) throws CantRunException {
    params.getVMParametersList().addParametersString(getVMParameters());

    // Add jactl jar to class path
    var jactlJar = getJactlJarFile(module);
    if (jactlJar == null) {
      throw new CantRunException("Cannot locate jactl jar file");
    }
    params.getClassPath().add(jactlJar);

    // Add module class path
    JavaParameters tmpParams = new JavaParameters();
    tmpParams.configureByModule(module, tests ? JavaParameters.CLASSES_AND_TESTS : JavaParameters.CLASSES_ONLY);
    tmpParams.getClassPath().getVirtualFiles().forEach(file -> params.getClassPath().add(file));

    // Set tools jar
    Sdk jdk = params.getJdk();
    if (jdk != null && jdk.getSdkType() instanceof JavaSdkType) {
      String toolsPath = ((JavaSdkType)jdk.getSdkType()).getToolsPath(jdk);
      if (toolsPath != null) {
        params.getVMParametersList().add("-Dtools.jar=" + toolsPath);
      }
    }

    // Set properties
    HttpConfigurable.getInstance().getJvmProperties(false, null).forEach(p -> params.getVMParametersList().addProperty(p.first, p.second));

    // Main class
    params.setMainClass("io.jactl.Jactl");

    // Add class package roots
    params.getProgramParametersList().add("-P");
    params.getProgramParametersList().add(JactlUtils.getSourceRoots(module.getProject()).stream().collect(Collectors.joining(",")));

    // Script location and script args
    params.getProgramParametersList().add("-C");
    String classPath = JactlUtils.pathToClass(getProject(), getScriptPath());
    classPath = JactlPlugin.BASE_JACTL_PKG + '.' + classPath;
    int idx = classPath.lastIndexOf('.');
    classPath = classPath.substring(0, idx + 1) + JactlPlugin.SCRIPT_PREFIX + classPath.substring(idx + 1);

    //params.getProgramParametersList().add(FileUtil.toSystemDependentName(getScriptPath()));
    params.getProgramParametersList().add(classPath);
    params.getProgramParametersList().addParametersString(getProgramParameters());
  }

  private static VirtualFile getJactlJarFile(Module module) {
    VirtualFile[] allRoots = OrderEnumerator.orderEntries(module).getAllLibrariesAndSdkClassesRoots();
    final String  prefix   = "jactl-";
    return Arrays.stream(allRoots)
                 .filter(file -> file.getName().startsWith(prefix) &&
                                 Character.isDigit(file.getName().charAt(prefix.length())) &&
                                 "jar".equals(file.getExtension()))
                 .findFirst()
                 .orElse(null);
  }
}
