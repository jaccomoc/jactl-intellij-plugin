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
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import io.jactl.intellijplugin.JactlFile;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.psi.JactlNameElementType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;

public class JactlMoveFileHandler extends MoveFileHandler {
  @Override
  public boolean canProcessElement(PsiFile element) {
    return element instanceof JactlFile;
  }

  @Override
  public void prepareMovedFile(PsiFile file, PsiDirectory moveDestination, Map<PsiElement, PsiElement> oldToNewMap) {
    var packageDecl = JactlUtils.getFirstDescendant(file, JactlNameElementType.PACKAGE);
    if (packageDecl != null) {
      String newPackage = JactlUtils.getProjectPath(moveDestination).replace(File.separator,".");
      packageDecl.replace(JactlUtils.newElement(file.getProject(), "package " + newPackage, JactlNameElementType.PACKAGE));
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
