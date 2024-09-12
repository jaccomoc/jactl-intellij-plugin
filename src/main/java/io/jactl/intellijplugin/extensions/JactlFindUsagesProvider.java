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

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import io.jactl.intellijplugin.JactlTokenSets;
import io.jactl.intellijplugin.JactlTokeniser;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiName;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JactlFindUsagesProvider implements FindUsagesProvider {

  @Override
  public @Nullable WordsScanner getWordsScanner() {
    return new DefaultWordsScanner(new JactlTokeniser(null), JactlTokenSets.IDENTIFIERS, JactlTokenSets.COMMENT, JactlTokenSets.STRING_LITERALS);
  }

  @Override
  public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
    return psiElement instanceof JactlPsiName;
  }

  @Override
  public @Nullable @NonNls String getHelpId(@NotNull PsiElement psiElement) {
    return null;
  }

  @Override
  public @Nls @NotNull String getType(@NotNull PsiElement element) {
    return element.getNode().getElementType().toString().toLowerCase();
  }

  @Override
  public @Nls @NotNull String getDescriptiveName(@NotNull PsiElement element) {
    return JactlUtils.getDocumentation(element, "Default descriptive text: " + element.getText());
  }

  @Override
  public @Nls @NotNull String getNodeText(@NotNull PsiElement element, boolean useFullName) {
    return "Node text: " + element.getText();
  }
}
