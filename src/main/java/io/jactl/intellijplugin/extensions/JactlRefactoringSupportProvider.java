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

package io.jactl.intellijplugin.extensions;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import io.jactl.intellijplugin.psi.impl.JactlPsiNameImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JactlRefactoringSupportProvider extends RefactoringSupportProvider {

  @Override
  public boolean isInplaceRenameAvailable(@NotNull PsiElement element, PsiElement context) {
    return true;
  }

  @Override
  public boolean isMemberInplaceRenameAvailable(@NotNull PsiElement element, @Nullable PsiElement context) {
    return true;
  }

  @Override
  public boolean isSafeDeleteAvailable(@NotNull PsiElement element) {
    return element instanceof JactlPsiNameImpl;
  }


}
