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

package io.jactl.intellijplugin;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IFileElementType;

public class JactlFileElementType extends IFileElementType {

  public JactlFileElementType() {
    super("jactl.FILE", JactlLanguage.INSTANCE);
  }

  @Override
  public ASTNode createNode(CharSequence text) {
    return new JactlFileElement(text);
  }
}
