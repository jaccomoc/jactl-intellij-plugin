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

import java.util.function.Supplier;

public class JactlCachedValue<T> {
  private volatile T value;
  private final Supplier<T> valueProvider;

  public JactlCachedValue(Supplier<T> valueProvider) {
    this.valueProvider = valueProvider;
  }

  public T getValue() {
    var localRef = value;
    if (localRef == null) {
      synchronized (this) {
        localRef = value;
        if (localRef == null) {
          value = localRef = valueProvider.get();
        }
      }
    }
    return localRef;
  }

  public void clear() {
    synchronized (this) {
      value = null;
    }
  }
}
