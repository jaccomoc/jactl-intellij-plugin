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

import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.ui.classFilter.DebuggerClassFilterProvider;
import io.jactl.Utils;

import java.util.ArrayList;
import java.util.List;

public class JactlDebuggerClassFilterProvider implements DebuggerClassFilterProvider {
  private final static List<ClassFilter> classFilters = Utils.listOf(new ClassFilter("java.*"),
                                                                     new ClassFilter("io.jactl.*"));

  @Override
  public List<ClassFilter> getFilters() {
    return classFilters;
  }
}
