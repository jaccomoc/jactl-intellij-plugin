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

package io.jactl.intellijplugin.common;

import com.intellij.openapi.util.io.FileUtil;
import io.jactl.CompileError;
import io.jactl.Jactl;
import io.jactl.intellijplugin.jpsplugin.builder.GlobalsException;
import io.jactl.runtime.RuntimeUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

public class JactlPlugin {
  public static final String SUFFIX              = "jactl";
  public static final String DOT_SUFFIX          = "." + SUFFIX;
  public static final String SCRIPT_PREFIX       = "Jactl$$";  // Must be something that can't occur naturally
  public static final String BASE_JACTL_PKG      = "jactl.pkg";
  public static final String BASE_JACTL_PKG_PATH = BASE_JACTL_PKG.replace('.', File.separatorChar);
  public static final String DUMMY_FILE_PATH     = "/Dummy" + DOT_SUFFIX;

  public static String removeSuffix(String name) {
    int idx = name.lastIndexOf('.');
    return idx == -1 ? name : name.substring(0, idx);
  }

  public static String removePackage(String classPath) {
    int idx = classPath.lastIndexOf('.');
    return idx == -1 ? classPath : classPath.substring(idx + 1);
  }

  public static String stackTrace(Throwable throwable) {
    StringWriter sw = new StringWriter();
    throwable.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  /**
   * Return directory name part of path or empty string if just a file name without any path.
   * @param path the path
   * @return "" if no directory part or directory portion ("a/b/c" -> "a/b")
   */
  public static String dirName(String path) {
    return stripFromLast(path, File.separatorChar);
  }

  public static String stripSeparatedPrefix(String str, String prefix, String separator) {
    if (str.startsWith(prefix)) {
      if (str.length() == prefix.length()) {
        return "";
      }
      str = str.substring(prefix.length());
      if (separator != null && !separator.isEmpty() && str.startsWith(separator)) {
        if (str.length() == separator.length()) {
          return "";
        }
        return str.substring(separator.length());
      }
    }
    return str;
  }

  public static String stripFromFirst(String str, Character c) {
    int idx = str.indexOf(c);
    return idx == -1 ? str : str.substring(idx);
  }

  public static String stripFromLast(String str, Character c) {
    int idx = str.lastIndexOf(c);
    return idx == -1 ? "" : str.substring(0, idx);
  }

  public static Map<String,Object> getGlobals(String scriptPath) {
    Map<String,Object> globals = null;
    if (scriptPath != null) {
      String fileName = FileUtil.toSystemIndependentName(scriptPath.trim());
      if (!fileName.isEmpty()) {
        if (!FileUtil.exists(fileName)) {
          throw new GlobalsException(fileName, JactlBundle.message("script.runner.error.no.global.variables.script", fileName));
        }
        if (!Files.isReadable(Path.of(fileName))) {
          throw new GlobalsException(fileName, JactlBundle.message("script.runner.error.global.variables.script.not.readable", fileName));
        }
        if (Files.isDirectory(Path.of(fileName))) {
          throw new GlobalsException(fileName, JactlBundle.message("script.runner.error.global.variables.script.is.directory", fileName));
        }

        try {
          String scriptContents = new String(Files.readAllBytes(Paths.get(fileName)));
          Object globalsObj    = Jactl.eval(scriptContents, Collections.EMPTY_MAP);
          if (globalsObj != null && !(globalsObj instanceof Map)) {
            throw new GlobalsException(fileName, JactlBundle.message("script.runner.error.global.variables.script.bad.type", RuntimeUtils.className(globalsObj)));
          }
          globals = (Map<String,Object>)globalsObj;
        }
        catch (CompileError e) {
          // Only show first error when error compiling globals script
          throw new GlobalsException(fileName, e.getErrors().get(0).getSingleLineMessage());
        }
        catch (Throwable e) {
          throw new GlobalsException(fileName, e.toString());
        }
      }
    }
    return globals == null ? Collections.EMPTY_MAP : globals;
  }
}
