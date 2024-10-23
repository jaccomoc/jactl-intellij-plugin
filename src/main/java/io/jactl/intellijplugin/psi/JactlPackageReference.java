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

package io.jactl.intellijplugin.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import io.jactl.intellijplugin.*;
import io.jactl.intellijplugin.psi.impl.JactlPsiIdentifierImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class JactlPackageReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  public static final Logger LOG = Logger.getInstance(JactlPackageReference.class);

  protected JactlPsiElement psiElement;

  private final JactlCachedValue<PsiElement> resolvedElement;

  public JactlPackageReference(JactlPsiElement psi) {
    // TextRange must be relative to start of this psi
    super(psi, TextRange.from(0, psi.getTextLength()));
    this.psiElement = psi;
    this.resolvedElement = new JactlCachedValue<>(this::_resolve);
  }

  @Override
  public boolean isReferenceTo(@NotNull PsiElement element) {
    boolean equals = element.equals(resolvedElement.getValue());
    return equals;
  }

  @Override
  public @NotNull PsiElement getElement() {
    return psiElement;
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    PsiElement newElement = JactlUtils.newReferenceElement(psiElement.getProject(), JactlNameElementType.PACKAGE, newElementName, psiElement.getClass());
    psiElement = (JactlPsiElement)psiElement.replace(newElement);
    return psiElement;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    if (element instanceof PsiDirectory) {
      PsiDirectory dir = (PsiDirectory) element;
      psiElement = JactlUtils.createNewPackagePath(dir, psiElement);
      return psiElement;
    }
    return super.bindToElement(element);
  }

  @Override
  public @Nullable PsiElement resolve() {
    return resolvedElement.getValue();
  }

  protected @Nullable PsiElement _resolve() {
    return JactlUtils.getPackage(psiElement);
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    PsiElement resolved = resolve();
    return new ResolveResult[] {
      new ResolveResult() {
        @Override public @Nullable PsiElement getElement() { return resolved; }
        @Override public boolean isValidResult()           { return resolved != null; }
      }
    };
  }

}
