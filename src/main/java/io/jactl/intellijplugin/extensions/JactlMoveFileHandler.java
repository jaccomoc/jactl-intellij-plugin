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

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import io.jactl.intellijplugin.JactlFile;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.psi.JactlNameElementType;
import io.jactl.intellijplugin.psi.JactlTokenType;
import io.jactl.intellijplugin.psi.JactlTokenTypes;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;

public class JactlMoveFileHandler extends MoveFileHandler {

  public static final String PACKAGE_REMOVAL_COMMENT = "// (removed package) ";

  @Override
  public boolean canProcessElement(PsiFile element) {
    return element instanceof JactlFile;
  }

  @Override
  public void prepareMovedFile(PsiFile file, PsiDirectory moveDestination, Map<PsiElement, PsiElement> oldToNewMap) {
    var packageDecl = JactlUtils.getFirstDescendant(file, JactlNameElementType.PACKAGE);
    String destDir = JactlUtils.getProjectPath(moveDestination);
    if (packageDecl != null) {
      String newCode;
      IElementType elementType;
      if (destDir.isEmpty()) {
        // If moving to top dir then remove package statement
        StringBuilder sb = new StringBuilder(PACKAGE_REMOVAL_COMMENT);
        for (PsiElement child = packageDecl.getFirstChild(); child != null; child = child.getNextSibling()) {
          if (!JactlUtils.isElementType(child, JactlTokenTypes.DOT, JactlTokenTypes.IDENTIFIER, JactlTokenTypes.PACKAGE)) {
            sb.append(child.getText());
          }
        }
        sb.append('\n');
        newCode = sb.toString();
        elementType = JactlTokenTypes.COMMENT;
      }
      else {
        newCode = JactlUtils.replacePackage(packageDecl, destDir, false);
        elementType = JactlNameElementType.PACKAGE;
      }
      packageDecl.replace(JactlUtils.newElement(file.getProject(), newCode, elementType));
    }
    else {
      // No package declaration so add one if we need one
      if (!destDir.isEmpty()) {
        String code = "package " + destDir.replace(File.separatorChar, '.') + "\n";
        PsiElement packageElement = JactlUtils.newElement(file.getProject(), code, JactlNameElementType.PACKAGE);
        // Add newline first
        file.addBefore(packageElement.getNextSibling(), file.getFirstChild());
        // Now insert the package declaration before the newline
        file.addBefore(packageElement, file.getFirstChild());
      }
    }
  }

  @Override
  public @Nullable List<UsageInfo> findUsages(PsiFile psiFile, PsiDirectory newParent, boolean searchInComments, boolean searchInNonJavaFiles) {
    return List.of();
  }

  @Override
  public void retargetUsages(List<UsageInfo> usageInfos, Map<PsiElement, PsiElement> oldToNewMap) {
    int i = 1;
  }

  @Override
  public void updateMovedFile(PsiFile file) throws IncorrectOperationException {
  }
}
