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

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import io.jactl.intellijplugin.psi.JactlTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JactlPairedBraceMatcher implements PairedBraceMatcher {

  private static final BracePair[] PAIRS = new BracePair[]{
    new BracePair(JactlTokenTypes.LEFT_BRACE, JactlTokenTypes.RIGHT_BRACE, true),
    new BracePair(JactlTokenTypes.LEFT_PAREN, JactlTokenTypes.RIGHT_PAREN, false),
    new BracePair(JactlTokenTypes.LEFT_SQUARE, JactlTokenTypes.RIGHT_SQUARE, false),
    };

  @Override
  public BracePair @NotNull [] getPairs() {
    return PAIRS;
  }

  @Override
  public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
    return true;
  }

  @Override
  public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
    return openingBraceOffset;
  }
}
