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

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.PsiElement;
import io.jactl.intellijplugin.JactlUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

public class JactlDocumentationProvider extends AbstractDocumentationProvider {
  @Override
  public @Nullable @Nls String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    boolean isNotNavigable = element.equals(originalElement) || (originalElement != null && element.equals(originalElement.getParent()));
    String defaultText = isNotNavigable ? null : element.getText();
    return JactlUtils.getDocumentation(element, defaultText);
  }
}
