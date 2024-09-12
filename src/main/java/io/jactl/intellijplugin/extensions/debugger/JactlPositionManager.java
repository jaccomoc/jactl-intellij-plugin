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

import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.impl.DebuggerUtilsAsync;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import io.jactl.intellijplugin.JactlFile;
import io.jactl.intellijplugin.JactlLanguage;
import io.jactl.intellijplugin.JactlParserAdapter;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.common.JactlPlugin;
import io.jactl.intellijplugin.psi.JactlPsiElement;
import io.jactl.runtime.ClassDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public class JactlPositionManager implements PositionManager {

  private DebugProcess process;

  public JactlPositionManager(DebugProcess process) {
    this.process = process;
  }

  @Override
  public @Nullable SourcePosition getSourcePosition(@Nullable Location location) throws NoDataException {
    try {
      if (location == null) { throw NoDataException.INSTANCE; }
      String fileName = location.sourceName();
      String filePath = location.sourcePath();
      if (fileName.startsWith(JactlPlugin.SCRIPT_PREFIX)) {
        String pkgPath = JactlPlugin.dirName(filePath);
        // Strip prefix to get actual file name
        fileName = fileName.substring(JactlPlugin.SCRIPT_PREFIX.length());
        filePath = pkgPath.isEmpty() ? fileName : pkgPath + File.separator + fileName;
      }
      if (filePath.startsWith(JactlPlugin.BASE_JACTL_PKG_PATH)) {
        filePath = filePath.substring(JactlPlugin.BASE_JACTL_PKG_PATH.length() + 1);
      }
      var fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(fileName);
      if (fileType instanceof LanguageFileType lfileType && lfileType.getLanguage() == JactlLanguage.INSTANCE) {
        int lineNum = DebuggerUtilsEx.getLineNumber(location, true);
        PsiFile file = lineNum >= 0 ? JactlUtils.findFile(process.getProject(), filePath) : null;
        if (file != null) {
          return SourcePosition.createFromLine(file, lineNum);
        }
      }
    }
    catch (AbsentInformationException ignore) {}
    throw NoDataException.INSTANCE;
  }

  @Override
  public @NotNull List<ReferenceType> getAllClasses(@NotNull SourcePosition sourcePosition) throws NoDataException {
    List<ReferenceType> result = ReadAction.compute(() -> {
      ClassDescriptor descriptor = getClassDescriptor(sourcePosition);
      if (descriptor != null) {
        return process.getVirtualMachineProxy().classesByName(descriptor.getJavaPackagedName());
      }
      return null;
    });
    if (result == null) {
      throw NoDataException.INSTANCE;
    }
    return result;
  }

  @Override
  public @NotNull List<Location> locationsOfLine(@NotNull ReferenceType referenceType, @NotNull SourcePosition sourcePosition) throws NoDataException {
    getJactlFile(sourcePosition);         // Verify we have a Jactl file
    int lineNum = sourcePosition.getLine() + 1;
    try {
      var locations = DebuggerUtilsAsync.locationsOfLineSync(referenceType,  DebugProcess.JAVA_STRATUM, null, lineNum);
      if (locations != null && !locations.isEmpty()) {
        return locations;
      }
    }
    catch (AbsentInformationException ignore) {}
    throw NoDataException.INSTANCE;
  }

  @Override
  public @Nullable ClassPrepareRequest createPrepareRequest(@NotNull ClassPrepareRequestor classPrepareRequestor, @NotNull SourcePosition sourcePosition) throws NoDataException {
    ClassPrepareRequest request = ReadAction.compute(() -> {
      ClassDescriptor descriptor = getClassDescriptor(sourcePosition);
      if (descriptor != null) {
        return process.getRequestsManager().createClassPrepareRequest(classPrepareRequestor, descriptor.getJavaPackagedName());
      }
      return null;
    });
    if (request != null) {
      return request;
    }
    throw NoDataException.INSTANCE;
  }

  private JactlFile getJactlFile(SourcePosition position) throws NoDataException {
    PsiFile file = position.getFile();
    if (file instanceof JactlFile jactlFile) {
      return jactlFile;
    }
    throw NoDataException.INSTANCE;
  }

  // Should be called from within ReadAction.compute() (?)
  private ClassDescriptor getClassDescriptor(SourcePosition sourcePosition) {
    var file = sourcePosition.getFile();
    if (file instanceof JactlFile) {
      PsiElement element = file.findElementAt(sourcePosition.getOffset());
      element = JactlUtils.skipWhitespaceAndComments(element);
      while (element != null && !(element instanceof JactlPsiElement)) {
        element = element.getParent();
      }
      if (element instanceof JactlPsiElement jactlPsiElement) {
        return JactlParserAdapter.getClass(jactlPsiElement);
      }
    }
    return null;
  }
}
