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

package io.jactl.intellijplugin.jpsplugin.builder;

import com.intellij.openapi.diagnostic.Logger;
import io.jactl.*;
import io.jactl.Utils;
import io.jactl.compiler.ClassCompiler;
import io.jactl.compiler.ScriptCompiler;
import io.jactl.intellijplugin.common.JactlBundle;
import io.jactl.intellijplugin.common.JactlPlugin;
import io.jactl.resolver.Resolver;
import io.jactl.runtime.ClassDescriptor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.*;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class JactlBuilder extends ModuleLevelBuilder {

  Logger LOG = Logger.getInstance(JactlBuilder.class);

  protected JactlBuilder() {
    super(BuilderCategory.TRANSLATOR);
  }

  @Override
  public ExitCode build(CompileContext compileContext, ModuleChunk moduleChunk, DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder, OutputConsumer outputConsumer) throws IOException {
    Map<String,ClassDescriptor> parsedClasses   = new HashMap<>();
    dirtyFilesHolder.processDirtyFiles((target, file, sourceRoot) -> {
      if (file.getPath().endsWith(JactlPlugin.DOT_SUFFIX)) {
        JactlContext context    = createContext(compileContext, moduleChunk, outputConsumer, target, file, parsedClasses);
        String       sourcePath = file.getCanonicalPath();
        String       rootPath   = sourceRoot.getRootFile().getCanonicalPath();
        if (!sourcePath.startsWith(rootPath)) {
          error(compileContext, "File " + sourcePath + " should be under root " + rootPath, sourcePath);
          return true;
        }
        String relativePath = JactlPlugin.stripSeparatedPrefix(sourcePath, rootPath, File.separator);
        int    slashIdx     = relativePath.lastIndexOf(File.separatorChar);
        String className    = relativePath.substring(slashIdx + 1);
        String pkgName      = relativePath.substring(0, slashIdx <= 0 ? 0 : slashIdx).replace(File.separatorChar, '.');
        className = JactlPlugin.removeSuffix(className);
        String source = getFileContent(compileContext, sourcePath);
        if (source == null) {
          return true;
        }
        Stmt.ClassDecl classDecl = parseAndResolve(compileContext, source, sourcePath, className, pkgName, context);
        if (classDecl == null) {
          return true;
        }
        try {
          Analyser analyser = new Analyser(context);
          analyser.analyseClass(classDecl);
          if (classDecl.isScriptClass()) {
            ScriptCompiler compiler = new ScriptCompiler(source, context, classDecl);
            compiler.compile();
            LOG.warn("Compilation of script " + className + " finished");
          }
          else {
            ClassCompiler compiler = new ClassCompiler(source, context, pkgName, classDecl, className + JactlPlugin.DOT_SUFFIX);
            compiler.compileClass();
            LOG.warn("Compilation of class " + className + " finished");
          }
        }
        catch (CompileError e) {
          e.getErrors().forEach(err -> error(compileContext, err, sourcePath));
        }
        catch (Throwable e) {
          error(compileContext, JactlPlugin.stackTrace(e), sourcePath);
        }
      }
      return true;
    });
    return null;
  }

  private Class<?> addClass(Map<String,ClassDescriptor> parsedClasses, ClassDescriptor descriptor, byte[] bytes, File sourceFile, OutputConsumer outputConsumer, ModuleBuildTarget target, CompileContext compileContext) {
    parsedClasses.put(descriptor.getPackagedName().replace('.', '/'), descriptor);
    target.getOutputRoots(compileContext)
          .forEach(outputRoot -> {
            File outputFile = new File(outputRoot, descriptor.getInternalName() + ".class");
            try {
              outputFile.getParentFile().mkdirs();
              try (OutputStream stream = new FileOutputStream(outputFile)) {
                stream.write(bytes);
              }
              outputConsumer.registerCompiledClass(target, new CompiledClass(outputFile, Utils.listOf(sourceFile), descriptor.getJavaPackagedName(), new BinaryContent(bytes)));
            }
            catch (IOException e) {
              error(compileContext, e.getMessage());
            }
          });
    // We are writing bytes to .class file so don't need to create class in memory
    return null;
  }


  @Override
  public @NotNull List<String> getCompilableFileExtensions() {
    return Utils.listOf(JactlPlugin.SUFFIX);
  }

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getPresentableName() {
    return "Jactl";
  }

  private String getBuilderName() {
    return getPresentableName().toLowerCase();
  }

  @Override
  public long getExpectedBuildTime() {
    return 50;
  }

  private JactlContext createContext(CompileContext compileContext, ModuleChunk moduleChunk, OutputConsumer outputConsumer, ModuleBuildTarget target, File sourceFile, Map<String,ClassDescriptor> parsedClasses) {
    List<String> sourceRoots = moduleChunk.getModules()
                                          .stream()
                                          .flatMap(module -> module.getSourceRoots().stream().map(JpsModuleSourceRoot::getPath))
                                          .map(Path::toString)
                                          .collect(Collectors.toList());

    String baseJavaPkg     = JactlPlugin.BASE_JACTL_PKG;   // Should come from project configuration?
    String baseJavaPkgFile = JactlPlugin.BASE_JACTL_PKG_PATH;
    AtomicReference<JactlContext> jactlContextRef = new AtomicReference<>();
    jactlContextRef.set(JactlContext.create()
                                    .debug(Integer.getInteger("jactl.debug", 0))
                                    .javaPackage(baseJavaPkg)
                                    .evaluateConstExprs(false)
                                    .idePlugin(true)
                                    .packageChecker(pkgName -> sourceRoots.stream().anyMatch(root -> Files.isDirectory(Path.of(root, pkgName.replace('.', File.separatorChar)))))
                                    .classLookup(name -> lookup(parsedClasses, jactlContextRef.get(), compileContext, name, baseJavaPkgFile, sourceRoots))
                                    .classAdder((descriptor, bytes) -> addClass(parsedClasses, descriptor, bytes, sourceFile, outputConsumer, target, compileContext))
                                    .build());
    return jactlContextRef.get();
  }

  /**
   * Get ClassDescriptor for given class. If we already have a cached version then return that,
   * otherwise find the file for the class and parse it.
   * @param parsedClasses    cache of already parsed classes
   * @param jactlContext     context
   * @param compileContext   Intellij compileContext for reporting any errors
   * @param internalName     internal name of class (e.g. io/jactl/pkg/a/b/c/_$j$Script123$X$Y$Z)
   * @param baseJavaPkgFile  base Java package in file form (e.g. io/jactl/pkg)
   * @param sourceRoots      list of source roots for project/module
   * @return the ClassDescriptor
   */
  private ClassDescriptor lookup(Map<String,ClassDescriptor> parsedClasses, JactlContext jactlContext, CompileContext compileContext, String internalName, String baseJavaPkgFile, List<String> sourceRoots) {
    ClassDescriptor descriptor = parsedClasses.get(internalName);
    if (descriptor != null) {
      return descriptor;
    }
    // We need to find the file containing the class.
    // First we get the directory part of the name.
    String dir      = JactlPlugin.dirName(internalName);
    String fileBase = JactlPlugin.stripSeparatedPrefix(internalName, dir, "/");

    // If there is a '$' then it is an inner class, so we strip everything from the dollar
    // onwards to get the containing class name
    if (fileBase.startsWith(JactlPlugin.SCRIPT_PREFIX)) {
      fileBase = fileBase.substring(JactlPlugin.SCRIPT_PREFIX.length());
      fileBase = JactlPlugin.SCRIPT_PREFIX + JactlPlugin.stripFromFirst(fileBase, '$');
    }
    else {
      fileBase = JactlPlugin.stripFromFirst(fileBase, '$');
    }
    String className = fileBase;
    fileBase = fileBase + JactlPlugin.DOT_SUFFIX;    // add '.jactl'

    // Strip io/jactl/pkg from dir
    dir = JactlPlugin.stripSeparatedPrefix(dir, baseJavaPkgFile, "/");
    final String fileName = fileBase;
    final String dirName  = dir;
    // Find file in sourceRoots
    String filePath = sourceRoots.stream()
                                 .map(root -> root + File.separatorChar + dirName + File.separatorChar + fileName)
                                 .filter(p -> new File(p).exists())
                                 .findFirst()
                                 .orElse(null);
    if (filePath == null) {
      return null;
    }
    String pkgName = dir.replace(File.separatorChar, '.');
    Stmt.ClassDecl classDecl = parseAndResolve(compileContext, filePath, className, pkgName, jactlContext);
    if (classDecl == null) {
      return null;
    }
    descriptor = classDecl.classDescriptor;
    parsedClasses.put(internalName, descriptor);
    return descriptor;
  }

  private void error(CompileContext compileContext, String msg) {
    compileContext.processMessage(new CompilerMessage(getBuilderName(), BuildMessage.Kind.ERROR, msg));
  }

  private void error(CompileContext compileContext, String msg, String sourcePath) {
    compileContext.processMessage(new CompilerMessage(getBuilderName(), BuildMessage.Kind.ERROR, msg, sourcePath));
  }

  private void error(CompileContext compileContext, CompileError err, String sourcePath) {
    err.getErrors().forEach(e -> compileContext.processMessage(new CompilerMessage(getBuilderName(),
                                                                                   BuildMessage.Kind.ERROR,
                                                                                   e.getErrorMessage(),
                                                                                   sourcePath,
                                                                                   e.getLocation().getOffset(),
                                                                                   e.getLocation().getOffset() + 1,
                                                                                   e.getLocation().getOffset(),
                                                                                   e.getLocation().getLineNum(),
                                                                                   e.getLocation().getColumn())));
  }

  private Stmt.ClassDecl parseAndResolve(CompileContext compileContext, String sourcePath, String className, String packageName, JactlContext jactlContext) {
    String source = getFileContent(compileContext, sourcePath);
    if (source == null) return null;
    return parseAndResolve(compileContext, source, sourcePath, className, packageName, jactlContext);
  }

  @Nullable
  private String getFileContent(CompileContext compileContext, String sourcePath) {
    String source;
    try {
      source = Files.readString(Path.of(sourcePath));
    }
    catch (IOException e) {
      error(compileContext, JactlBundle.message("build.io.error", sourcePath, e.getClass().getName()), sourcePath);
      return null;
    }
    return source;
  }

  private Stmt.ClassDecl parseAndResolve(CompileContext compileContext, String source, String sourcePath, String scriptName, String packageName, JactlContext jactlContext) {
    try {
      scriptName = JactlPlugin.SCRIPT_PREFIX + scriptName;
      Tokeniser      tokeniser    = new Tokeniser(source);
      BuilderImpl    tokenBuilder = new BuilderImpl(tokeniser) {
        @Override public void done() {
          // Don't throw error
        }
      };
      Parser         parser       = new Parser(tokenBuilder, jactlContext, packageName);
      Stmt.ClassDecl script       = parser.parseScriptOrClass(scriptName);

      if (tokenBuilder.hasErrors()) {
        tokenBuilder.getErrors().forEach(err -> error(compileContext, err, sourcePath));
        return null;
      }

      JpsJactlSettings   settings = JpsJactlSettings.getSettings(compileContext.getProjectDescriptor().getProject());
      Map<String,Object> globals  = settings.getGlobals();

      Resolver           resolver = new Resolver(jactlContext, globals, script.location);
      List<CompileError> errs     = resolver.resolveScriptOrClass(script, true, scriptName, packageName);
      if (errs.isEmpty()) {
        return script;
      }
      errs.forEach(e -> error(compileContext, e, sourcePath));
    }
    catch (GlobalsException error) {
      error(compileContext, error.getMessage(), error.getGlobalsScriptPath());
    }
    catch (CompileError error) {
      error(compileContext, error, sourcePath);
    }
    return null;
  }
}
