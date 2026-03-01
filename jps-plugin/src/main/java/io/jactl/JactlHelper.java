/*
 * Copyright © 2022,2023,2024,2025,2026  James Crawford
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

package io.jactl;

public class JactlHelper {
  public static JactlContext createContext(String baseJavaPkg, JactlContext.PackageChecker packageChecker, JactlContext.ClassLookup classLookup, JactlContext.ClassAdder classAdder) {
    return JactlContext.create()
                       .debug(Integer.getInteger("jactl.debug", 0))
                       .javaPackage(baseJavaPkg)
                       .evaluateConstExprs(false)
                       .idePlugin(true)
                       .packageChecker(packageChecker)
                       .classLookup(classLookup)
                       .classAdder(classAdder)
                       .build();
  }

  public static JactlContext createContext(String baseJavaPkg, JactlContext.PackageChecker packageChecker, JactlContext.ClassLookup classLookup) {
    return JactlContext.create()
                       .javaPackage(baseJavaPkg)
                       .evaluateConstExprs(false)
                       .idePlugin(true)
                       .packageChecker(packageChecker)
                       .classLookup(classLookup)
                       .build();

  }
}