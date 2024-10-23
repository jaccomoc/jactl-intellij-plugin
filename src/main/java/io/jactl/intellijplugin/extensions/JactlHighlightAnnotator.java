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

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import io.jactl.intellijplugin.JactlParserAdapter;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.psi.*;
import io.jactl.intellijplugin.psi.impl.JactlPsiIdentifierImpl;
import io.jactl.intellijplugin.psi.impl.JactlPsiTypeImpl;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiName;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class JactlHighlightAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    Consumer<String> error = msg -> holder.newAnnotation(HighlightSeverity.ERROR, msg)
                                          .range(element.getTextRange())
                                          .create();

    JactlPsiElement ancestor = (JactlPsiElement)JactlUtils.getAncestor(element, JactlPsiElement.class);
    if (ancestor != null) {
      JactlParserAdapter.getErrors(ancestor.getFile(), ancestor.getSourceCode(), element.getNode())
                        .forEach(error);
    }

    if (element instanceof JactlPsiIdentifierImpl &&
        element.getParent() instanceof JactlPsiName &&
        JactlUtils.isElementType((JactlPsiName) element.getParent(), JactlNameElementType.PACKAGE)) {
      JactlPsiName parent = (JactlPsiName) element.getParent();
      List<String> idents = new ArrayList<>();
      for (PsiElement e = parent.getFirstChild(); e != element; e = e.getNextSibling()) {
        if (e instanceof JactlPsiIdentifierImpl) {
          idents.add(e.getText());
        }
      }
      idents.add(element.getText());
      String packageName = String.join(".", idents);
      Set<String> pkgNames = JactlUtils.pkgNames(element.getProject());
      if (!pkgNames.contains(packageName)) {
        error.accept("Unknown package");
        return;
      }
    }
    else if (element instanceof JactlPsiName && JactlUtils.isElementType(element, JactlNameElementType.PACKAGE)) {
      List<String> idents = new ArrayList<>();
      for (PsiElement e = element.getFirstChild(); e != null; e = e.getNextSibling()) {
        if (e instanceof JactlPsiIdentifierImpl) {
          idents.add(e.getText());
        }
      }
      String packageName = String.join(".", idents);
      String filePackage = ((JactlPsiName)element).getFile().getPackageName();
      if (!packageName.equals(filePackage)) {
        error.accept("Package name does not match package name for file (" + filePackage + ")");
      }
    }
    else if (element instanceof JactlPsiName && ((JactlPsiName) element).isTopLevelClass()) {
      JactlPsiName psiName = (JactlPsiName) element;
      // Check if top level class in a class file and verify that class name matches file name
      String classNameFromFile = psiName.getFile().getFileNameNoSuffix();
      if (!psiName.getText().equals(classNameFromFile)) {
        error.accept("Class name must match file name (" + classNameFromFile + ")");
        return;
      }
    }
    else if (element instanceof JactlPsiTypeImpl) {
      JactlPsiTypeImpl type = (JactlPsiTypeImpl) element;
      if (!type.isBuiltIn() && type.findClassDefinition() == null) {
        error.accept("Unknown type " + type.getText());
        return;
      }
      else {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .range(element.getTextRange())
              .textAttributes(JactlSyntaxHighLighter.TYPE)
              .create();
        return;
      }
    }

    // Annotate based on type of identifier
    if (JactlUtils.isElementType(element, JactlTokenTypes.IDENTIFIER)) {
      PsiElement   parent     = element.getParent();
      IElementType parentType = parent.getNode().getElementType();
      if (!(parentType instanceof JactlNameElementType)) {
        PsiReference reference = element.getReference();
        PsiElement   resolved  = reference != null ? reference.resolve() : null;
        if (resolved == null || resolved.getNode() == null) {
          return;
        }
        parentType = resolved.getNode().getElementType();
      }
      if (parentType instanceof JactlNameElementType) {
        JactlNameElementType parentNameType = (JactlNameElementType) parentType;
        TextAttributesKey textType;
        switch (JactlNameElementType.getNameType(parentNameType)) {
          case PACKAGE:   textType = JactlSyntaxHighLighter.PACKAGE;         break;
          case CLASS:     textType = JactlSyntaxHighLighter.CLASS;           break;
          case FUNCTION:  textType = JactlSyntaxHighLighter.FUNCTION;        break;
          case METHOD:    textType = JactlSyntaxHighLighter.METHOD;          break;
          case VARIABLE:  textType = JactlSyntaxHighLighter.LOCAL_VARIABLE;  break;
          case FIELD:     textType = JactlSyntaxHighLighter.FIELD;           break;
          case PARAMETER: textType = JactlSyntaxHighLighter.PARAMETER;       break;
          default:        textType = null;                                   break;
        };
        if (textType != null) {
          holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.getTextRange())
                .textAttributes(textType)
                .create();
        }
      }
    }
  }
}
