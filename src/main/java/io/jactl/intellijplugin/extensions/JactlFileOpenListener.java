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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.FileContentUtil;
import io.jactl.Utils;
import io.jactl.intellijplugin.JactlUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JactlFileOpenListener implements FileEditorManagerListener {
  @Override
  public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    // Reparse if globals have changed since last time file was modified
    VirtualFile globalsFile = JactlUtils.getGlobalsFile(source.getProject());
    if (globalsFile != null && globalsFile.getModificationStamp() >= file.getModificationStamp()) {
      ApplicationManager.getApplication().invokeLater(() -> FileContentUtil.reparseFiles(source.getProject(), Utils.listOf(file), false));
    }
  }
}
