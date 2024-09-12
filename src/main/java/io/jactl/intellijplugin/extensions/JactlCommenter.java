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

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

public class JactlCommenter implements Commenter {
  @Override
  public @Nullable String getLineCommentPrefix() {
    return "//";
  }

  @Override
  public @Nullable String getBlockCommentPrefix() {
    return "/*";
  }

  @Override
  public @Nullable String getBlockCommentSuffix() {
    return "*/";
  }

  @Override
  public @Nullable String getCommentedBlockCommentPrefix() {
    return null;
  }

  @Override
  public @Nullable String getCommentedBlockCommentSuffix() {
    return null;
  }
}
