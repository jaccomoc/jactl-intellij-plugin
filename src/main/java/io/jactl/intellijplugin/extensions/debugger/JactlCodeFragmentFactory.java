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

import com.intellij.debugger.engine.evaluation.CodeFragmentFactory;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.engine.evaluation.expression.EvaluatorBuilder;
import com.intellij.debugger.engine.evaluation.expression.EvaluatorBuilderImpl;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import io.jactl.CompileError;
import io.jactl.JactlContext;
import io.jactl.compiler.Compiler;
import io.jactl.intellijplugin.JactlFileType;
import io.jactl.intellijplugin.JactlLanguage;
import io.jactl.intellijplugin.JactlUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class JactlCodeFragmentFactory extends CodeFragmentFactory {

  @Override
  public JavaCodeFragment createCodeFragment(TextWithImports item, PsiElement context, Project project) {
    String text = item.getText();
    if (text.trim().equals("")) {
      return JavaCodeFragmentFactory.getInstance(project).createCodeBlockCodeFragment("", null, true);
    }
    // Record which "globals" are asked for as those will be the variables needed
    Map<String,Object> vars = new HashMap() {
      @Override public boolean containsKey(Object key) { return true; }
      @Override public Object get(Object key) { return put((String)key, null); }
    };
    try {
      Compiler.parseAndResolve(text, JactlContext.create().classAccessToGlobals(true).build(), null, "", vars);
    }
    catch (CompileError e) {
      // Ignore since there is no way to return error from here.
      // We will, instead, get the error when the eval() is run.
    }
    StringBuilder sb = new StringBuilder();
    sb.append("java.util.Map<String,Object> _$j_vars = new java.util.HashMap();\n");
    vars.keySet().forEach(var -> sb.append("_$j_vars.put(\"").append(var).append("\",").append(var).append(");\n"));
    // Convert any HeapLocals back to native values
    sb.append("for (java.util.Map.Entry<String,Object> entry:  _$j_vars.entrySet()) {\n" +
              "  Object value = entry.getValue();\n" +
              "  if (value instanceof io.jactl.runtime.HeapLocal) {\n" +
              "    entry.setValue(((io.jactl.runtime.HeapLocal) value).getValue());\n" +
              "  }\n" +
              "}\n");
    sb.append("io.jactl.Jactl.eval(\"")
      .append(text.replaceAll("\"","\\\""))
      .append("\", _$j_vars);\n");
    return JavaCodeFragmentFactory.getInstance(project).createCodeBlockCodeFragment(sb.toString(), null, true);
  }

  @Override
  public JavaCodeFragment createPresentationCodeFragment(TextWithImports item, PsiElement context, Project project) {
    String text = item.getText();
    //text = text.replaceAll(".*io.jactl.Jactl.eval\\(\"", "").replaceAll(", _$j_vars.*", "");
    return new JactlCodeFragment(project, JactlUtils.CODE_FRAGMENT_FILE_NAME, text, context);
  }

  @Override
  public boolean isContextAccepted(PsiElement contextElement) {
    return contextElement != null && contextElement.getLanguage().equals(JactlLanguage.INSTANCE);
  }

  @Override
  public @NotNull LanguageFileType getFileType() {
    return JactlFileType.INSTANCE;
  }

  @Override
  public EvaluatorBuilder getEvaluatorBuilder() {
    return EvaluatorBuilderImpl.getInstance();
  }
}
