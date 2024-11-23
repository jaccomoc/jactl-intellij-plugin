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

package io.jactl.intellijplugin.extensions.debugger;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.testFramework.LightVirtualFile;
import io.jactl.Expr;
import io.jactl.intellijplugin.*;
import io.jactl.intellijplugin.psi.JactlPsiElement;
import io.jactl.runtime.ClassDescriptor;
import io.jactl.runtime.FunctionDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class JactlCodeFragment extends JactlFile implements JavaCodeFragment, IntentionFilterOwner {

  public static final Logger LOG = Logger.getInstance(JactlCodeFragment.class);

  private PsiElement                    context;
  private LinkedHashMap<String, String> imports = new LinkedHashMap<>();
  private VisibilityChecker             visibilityChecker;
  private ExceptionHandler              exceptionHandler;
  private GlobalSearchScope             resolveScope;
  private IntentionActionsFilter        intentionActionsFilter;
  private PsiType                       thisType;
  private PsiType                       superType;
  private boolean                       isPhysical;
  private SingleRootFileViewProvider    viewProvider;

  public JactlCodeFragment(Project project, String fileName, String text, PsiElement context) {
    super(PsiManagerEx.getInstanceEx(project)
                      .getFileManager()
                      .createFileViewProvider(new LightVirtualFile(fileName,
                                                                   FileTypeManager.getInstance().getFileTypeByFileName(fileName),
                                                                   text),
                                              true));
    isPhysical = true;
    this.context = context;
    FileViewProvider vp = getViewProvider();
    if (vp instanceof SingleRootFileViewProvider) {
      ((SingleRootFileViewProvider) vp).forceCachedPsi(this);
    }
  }

  @Override
  public Map<String, Object> getGlobals() {
    // Find Jactl element so we can get block and get variables from block
    JactlPsiElement parent = JactlUtils.getJactlPsiParent(context);
    if (parent != null) {
      Map<String,Object> result = new HashMap<>();
      JactlParserAdapter.getVariablesAndFunctions((JactlPsiElement) parent)
                        .stream()
                        .map(this::getVarOrFuncName)
                        .filter(Objects::nonNull)
                        .forEach(n -> result.put(n,null));
      result.putAll(super.getGlobals());
      return result;
    }
    return Collections.EMPTY_MAP;
  }

  private String getVarOrFuncName(Object obj) {
    if (obj instanceof Expr.VarDecl)                       { return ((Expr.VarDecl) obj).name.getStringValue(); }
    if (obj instanceof JactlParserAdapter.FieldDescriptor) { return ((JactlParserAdapter.FieldDescriptor) obj).name(); }
    if (obj instanceof FunctionDescriptor)                 { return ((FunctionDescriptor) obj).name; }
    if (obj instanceof ClassDescriptor)                    { return ((ClassDescriptor) obj).getClassName(); }
    LOG.warn("Unexpected type for completion: " + obj.getClass().getName());
    return null;
  }

  @Override
  protected JactlCodeFragment clone() {
    JactlCodeFragment clone = (JactlCodeFragment)cloneImpl((FileElement)calcTreeElement().clone());
    clone.isPhysical     = false;
    clone.myOriginalFile = this;
    clone.imports        = new LinkedHashMap<>(imports);
    FileManager      fileManager = ((PsiManagerEx)getManager()).getFileManager();
    LightVirtualFile vFile       = new LightVirtualFile(getName(), JactlLanguage.INSTANCE, getText());
    clone.viewProvider = (SingleRootFileViewProvider)fileManager.createFileViewProvider(vFile, false);
    clone.viewProvider.forceCachedPsi(clone);
    clone.context = context;
    return clone;
  }

  @Override
  public IElementType getContentElementType() {
    return JactlParserDefinition.JACTL_FILE_ELEMENT_TYPE;
  }

  @Override
  public @NotNull FileViewProvider getViewProvider() {
    return viewProvider != null ? viewProvider : super.getViewProvider();
  }

  @Override
  public PsiElement getContext() {
    return context != null ? context : super.getContext();
  }

  @Override
  public boolean isPhysical() {
    return isPhysical;
  }

  @Override
  public void setIntentionActionsFilter(@NotNull IntentionActionsFilter filter) {
    intentionActionsFilter = filter;
  }

  @Override
  public IntentionActionsFilter getIntentionActionsFilter() {
    return intentionActionsFilter;
  }

  @Override
  public PsiType getThisType() {
    return thisType;
  }

  @Override
  public void setThisType(PsiType psiType) {
    thisType = psiType;
  }

  @Override
  public PsiType getSuperType() {
    return superType;
  }

  @Override
  public void setSuperType(PsiType superType) {
    this.superType = superType;
  }

  @Override
  public String importsToString() {
    return String.join(",", imports.values());
  }

  @Override
  public void addImportsFromString(String importsStr) {
    for (String fullName: importsStr.split(",")) {
      imports.put(PsiNameHelper.getShortClassName(fullName), fullName);
    }
  }

  @Override
  public void setVisibilityChecker(VisibilityChecker checker) {
    visibilityChecker = checker;
  }

  @Override
  public VisibilityChecker getVisibilityChecker() {
    return visibilityChecker;
  }

  @Override
  public void setExceptionHandler(ExceptionHandler checker) {
    exceptionHandler = checker;
  }

  @Override
  public ExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }

  @Override
  public void forceResolveScope(GlobalSearchScope scope) {
    this.resolveScope = scope;
  }

  @Override
  public GlobalSearchScope getForcedResolveScope() {
    return resolveScope;
  }

  @Override
  public boolean importClass(@NotNull PsiClass aClass) {
    throw new UnsupportedOperationException();
  }
}
