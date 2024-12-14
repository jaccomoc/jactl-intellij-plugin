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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import io.jactl.JactlType;
import io.jactl.Utils;
import io.jactl.intellijplugin.extensions.settings.JactlConfiguration;
import io.jactl.intellijplugin.jpsplugin.builder.JpsJactlSettings;
import io.jactl.runtime.Functions;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompletionTests extends BasePlatformTestCase {

  private static final String NONE = "$$NONE$$";

  private JactlConfiguration jactlConfiguration = new JactlConfiguration();
  private List<String> globalVars;

  @Override
  protected String getTestDataPath() {
    return "src/test";
  }

  protected void setUp() throws Exception {
    super.setUp();
    //System.out.println(getTestName(true));
    String testData = "";
    myFixture.copyDirectoryToProject("completionTests", testData);

    File temp = File.createTempFile("globals", "jactl");
    temp.deleteOnExit();
    try (OutputStream outputStream = new FileOutputStream(temp)) {
      outputStream.write("[ aaa:'value of aaa', bbb:'value of bbb' ]".getBytes());
      outputStream.flush();
    }
    globalVars = Utils.listOf("aaa", "bbb");
    JpsJactlSettings settings = new JpsJactlSettings();
    settings.globalVariablesScript = temp.getAbsolutePath();
    jactlConfiguration.loadState(settings);
    ServiceContainerUtil.registerOrReplaceServiceInstance(getProject(),
                                                          JactlConfiguration.class,
                                                          jactlConfiguration,
                                                          getTestRootDisposable());
  }

  enum MatchType {
    INCLUDES, EXCLUDES, ALL
  }

  private void test(String text, Stream<String>... expected) {
    test(text, Arrays.stream(expected).flatMap(s -> s).toArray(String[]::new));
  }

  private void doTest(String text, String... expected) {
    doTtestWithFileName(getFileName(), text, MatchType.ALL, expected);
  }

  private void doTest(String text, Stream<String>... expected) {
    doTtestWithFileName(getFileName(), text, MatchType.ALL, Arrays.stream(expected).flatMap(s -> s).toArray(String[]::new));
  }

  private void test(String text, String... expected) {
    testWithFileName(getFileName(), text, expected);
  }

  private void testIncludes(String text, String... expected) {
    testWithFileName(getFileName(), text, MatchType.INCLUDES, expected);
  }

  private static @NotNull String getFileName() {
    return "script.jactl";
  }

  private static @NotNull String getFileName(String pkg) {
    return pkg.replace('.',File.separatorChar) + File.separatorChar + getFileName();
  }

  private void testExcludes(String text, String... expected) {
    testWithFileName(getFileName(), text, MatchType.EXCLUDES, expected);
  }

  private void testWithFileName(String fileName, String text, Stream<String>... expected) {
    testWithFileName(fileName, text, Arrays.stream(expected).flatMap(s -> s).toArray(String[]::new));
  }

  private void testInOrgTest(String text, Stream<String>... expected) {
    testInOrgTest(text, Arrays.stream(expected).flatMap(s -> s).toArray(String[]::new));
  }

  private void testInOrgTest(String text, String... expected) {
    if (!text.startsWith("package org.test")) {
      text = "package org.test; " + text;
    }
    testWithFileName(getFileName("org.test"), text, expected);
  }

  private void testWithFileName(String fileName, String text, String... expected) {
    testWithFileName(fileName, text, MatchType.ALL, expected);
  }

  private void testWithFileName(String fileName, String text, MatchType matchType, String... expected) {
    doTtestWithFileName(fileName, text, matchType, expected);
    doTtestWithFileName(fileName, text.replaceAll("<caret>", "/*comment*/ <caret>"), matchType, expected);
    doTtestWithFileName(fileName, text.replaceAll("<caret>", "/*comment*/ <caret> /*comment*/"), matchType, expected);
    doTtestWithFileName(fileName, text.replaceAll("\\.", " . "), matchType, expected);
    doTtestWithFileName(fileName, text.replaceAll("\\.", "/*comment*/./*comment*/"), matchType, expected);
    doTtestWithFileName(fileName, text.replaceAll("\\.", " /*comment*/ . /*comment*/ "), matchType, expected);
  }

  private void doTtestWithFileName(String fileName, String text, MatchType matchType, String... expected) {
    myFixture.addFileToProject(fileName, text);
    PsiFile psiFile = myFixture.configureByFile(fileName);
    try {
      LookupElement[] result = myFixture.completeBasic();
      List<String>    items  = result == null ? Utils.listOf() : Arrays.stream(result).map(LookupElement::getLookupString).sorted().collect(Collectors.toList());
      switch (matchType) {
        case INCLUDES:
          Arrays.stream(expected).forEach(e -> assertTrue("Missing '" + e + "' in " + items, items.contains(e)));
          break;
        case EXCLUDES:
          Arrays.stream(expected).forEach(e -> assertFalse("Should not include '" + e + "' in " + items, items.contains(e)));
          break;
        case ALL:
          if (expected.length == 1 && expected[0] == NONE) {
            assertTrue("Expected no entries but got : " + items, items.isEmpty());
          }
          else {
            assertEquals(Arrays.stream(expected).sorted().collect(Collectors.toList()), items);
          }
          break;
      }
    } finally {
      WriteAction.run(() -> {
        try {
          psiFile.getVirtualFile().delete(null);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  private Stream<String> classesWithFromJson(String... classes) {
    return Arrays.stream(classes).flatMap(clss -> Stream.of(clss, clss + ".fromJson"));
  }

  ////////////////////////////////

  @Test public void testExtendsClassName() {
    test("class XYZ{}; class X <caret>", "extends");
    test("class XYZ{}\nclass X extends <caret> {}", "XYZ");
    test("class XYZ{}\nclass X extends <caret>", "XYZ");
    test("class XYZ{}; class ABC extends XYZ{}\nclass X extends <caret> {}", "XYZ", "ABC");
    test("class XYZ{}; class ABC extends XYZ{}\nclass X extends <caret>", "XYZ", "ABC");
    testInOrgTest("class ABC extends XYZ{}\nclass X extends <caret> {}", "ABC", "XYZ", "AClass");
    testInOrgTest("class ABC extends XYZ{}\nclass X extends <caret>", "ABC", "XYZ", "AClass");
  }

  @Test public void testExtendsInnerClassName() {
    test("class XYZ{\nclass Inner {}\nclass X extends <caret> {}", "XYZ", "Inner");
    test("class XYZ{\nclass Inner {}\nclass X extends <caret> {", "XYZ", "Inner");
    test("class XYZ{\nclass Inner {}\nclass X extends <caret>", "XYZ", "Inner");
    test("class XYZ{\nclass Inner {}\n}\nclass X extends <caret> {}", "XYZ");
    test("class XYZ{\nclass Inner {}\n}\nclass X extends <caret> {", "XYZ");
    test("class XYZ{\nclass Inner {}\n}\nclass X extends <caret>", "XYZ");
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nclass X extends <caret> {} }", "XYZ", "ABC", "Inner");
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nclass X extends <caret> {}", "XYZ", "ABC", "Inner");
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nclass X extends <caret>", "XYZ", "ABC", "Inner");
    testInOrgTest("class X extends AClass.B { class Y extends <caret> }", "XYZ", "AClass", "XXX", "X", "B");
    testInOrgTest("class X extends AClass.B { class Y extends <caret>", "XYZ", "AClass", "XXX", "X", "B");
    testInOrgTest("class X extends AClass.B { class Y extends <caret> {", "XYZ", "AClass", "XXX", "X", "B");
    testInOrgTest("class X extends AClass.B { class Y extends <caret> { int i = }", "XYZ", "AClass", "XXX", "X", "B");
    testInOrgTest("class X extends AClass.B { class Y extends <caret> {} }", "XYZ", "AClass", "XXX", "X", "B");
  }

  @Test public void testExtendsPackageName() {
    test("class XYZ{}\nclass X extends org.<caret> {}", "test", "test2");
    test("class XYZ{}\nclass X extends org.<caret>", "test", "test2");
    test("class XYZ{}\nclass X extends org.test.<caret> {}", "XYZ", "AClass");
    test("class XYZ{}\nclass X extends org.test.<caret>", "XYZ", "AClass");
    test("class X extends org.test2.<caret> {}", "ABC", "sub");
    test("class X extends org.test2.<caret> {", "ABC", "sub");
    test("class X extends org.test2.<caret>", "ABC", "sub");
    test("class X extends orgxxx.<caret> {}", NONE);
    test("class X extends orgxxx.<caret> {", NONE);
    test("class X extends orgxxx.<caret>", NONE);
    testWithFileName(getFileName("org.test"), "class X extends org.<caret> {}", "test", "test2");
    testWithFileName(getFileName("org.test"), "class X extends org.<caret>", "test", "test2");
    testWithFileName(getFileName("org.test"), "class X extends org.test.<caret> {}", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "class X extends org.test.<caret>", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "class X extends org.test2.<caret> {}", "ABC", "sub");
    testWithFileName(getFileName("org.test"), "class X extends org.test2.<caret>", "ABC", "sub");
  }

  @Test public void testParameterTypes() {
    test("class X{}; def f(<caret> x", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(<caret>", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(<caret> x)", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(<caret> x, ", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(int x, <caret>", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(int x, <caret>)", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(x, <caret>", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(x, <caret>)", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(x, <caret> y)", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(<caret> x", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(<caret>", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(<caret> x)", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(<caret> x, ", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(int x, <caret>", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(int x, <caret>)", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(x, <caret>", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(x, <caret>)", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(x, <caret> y)", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));

    test("class X{}; def f = { <caret> x ->", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { <caret> ->", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { <caret> x ->", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { <caret> x, ->", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { int x, <caret> ->", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { int x, <caret> ->", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { x, <caret> ->", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { x, <caret> ->", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { x, <caret> y ->", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));

    test("class XYZ{\nclass Inner {}\ndef f(<caret>", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(<caret> x", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(<caret> x)", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(<caret> x, )", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret>", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret>)", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret> y)", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret> y) }", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret> y) {", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret> y) {}", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret> y) {} }", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(<caret>", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(<caret> x", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(int x, <caret>", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(int x, <caret> y", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(int x, <caret> y)", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(int x, <caret> y) {", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(int x, <caret> y) {}", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(int x, <caret> y) {} }", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { def f(<caret>", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { def f(<caret> x", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { def f(int x, <caret>", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { def f(int x, <caret> y", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { def f(int x, <caret> y)", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { def f(int x, <caret> y) }", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));

    test("class XYZ{}\ndef f(org.<caret>", "test", "test2");
    test("class XYZ{}\ndef f(org.<caret> x", "test", "test2");
    test("class XYZ{}\ndef f(int x, org.<caret>", "test", "test2");
    test("class XYZ{}\ndef f(int x, org.<caret> y", "test", "test2");
    test("class XYZ{}\ndef f(int x, org.<caret> y) {", "test", "test2");
    test("class XYZ{}\ndef f(int x, org.<caret> y) {}", "test", "test2");
    test("class XYZ{}\nint f(org.test.<caret>", "XYZ", "AClass");
    test("class XYZ{}\nint f(org.test.<caret> x", "XYZ", "AClass");
    test("class XYZ{}\nint f(int x, org.test.<caret>", "XYZ", "AClass");
    test("class XYZ{}\nint f(int x, org.test.<caret> y", "XYZ", "AClass");
    test("def f(org.test2.<caret>", "ABC", "sub");
    test("def f(org.test2.<caret> x", "ABC", "sub");
    test("def f(int x, org.test2.<caret>", "ABC", "sub");
    test("def f(int x, org.test2.<caret> y", "ABC", "sub");
    test("def f(orgxxx.<caret>", NONE);
    test("def f(orgxxx.<caret> x", NONE);
    test("def f(int x, orgxxx.<caret>", NONE);
    test("def f(int x, orgxxx.<caret> y", NONE);
    test("def f(int x, orgxxx.<caret> y)", NONE);
    testWithFileName(getFileName("org.test"), "def f(org.<caret>", "test", "test2");
    testWithFileName(getFileName("org.test"), "def f(org.<caret> x", "test", "test2");
    testWithFileName(getFileName("org.test"), "def f(org.<caret> x)", "test", "test2");
    testWithFileName(getFileName("org.test"), "def f(int x, org.<caret>", "test", "test2");
    testWithFileName(getFileName("org.test"), "def f(int x, org.<caret> y", "test", "test2");
    testWithFileName(getFileName("org.test"), "def f(int x, org.<caret> y)", "test", "test2");
    testWithFileName(getFileName("org.test"), "def f(org.test.<caret>", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "def f(org.test.<caret> x)", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "def f(int x, org.test.<caret>", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "def f(int x, org.test.<caret> y", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "def f(org.test2.<caret>", "ABC", "sub");
    testWithFileName(getFileName("org.test"), "def f(org.test2.<caret> x", "ABC", "sub");
  }

  @Test public void testFunctionReturnTypes() {
    test("class X{}; <caret> f(", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(x)", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x)", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x, ", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x, int", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x, y", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x, y)", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x, y) {", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x, y) {}", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; <caret> f(", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; <caret> f()", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; <caret> f(int", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; <caret> f(int x", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; <caret> f(int x)", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; <caret> f(int x, ", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; <caret> f(int x, int", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; <caret> f(int x, int y", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; <caret> f(int x, int y)", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; <caret> f(int x, int y) {", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; <caret> f(int x, int y) {}", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));

    test("class XYZ{}\norg.<caret> f(", "test", "test2");
    test("class XYZ{}\norg.<caret> f()", "test", "test2");
    test("class XYZ{}\norg.<caret> f() {}", "test", "test2");
    test("class XYZ{}\norg.<caret> f(int ", "test", "test2");
    test("class XYZ{}\norg.<caret> f(int x", "test", "test2");
    test("class XYZ{}\norg.<caret> f(int x,", "test", "test2");
    test("class XYZ{}\norg.test.<caret> f(", "XYZ", "AClass");
    test("class XYZ{}\norg.test.<caret> f()", "XYZ", "AClass");
    test("class XYZ{}\norg.test.<caret> f() {", "XYZ", "AClass");
    test("org.test2.<caret> f(", "ABC", "sub");
    test("org.test2.<caret> f(int", "ABC", "sub");
    test("org.test2.<caret> f(int x", "ABC", "sub");
    test("org.test2.<caret> f(int x)", "ABC", "sub");
    test("org.test2.<caret> f(int x) {", "ABC", "sub");
    test("org.test2.<caret> f(int x) {}", "ABC", "sub");
    test("orgxxx.<caret> f(", NONE);
    test("orgxxx.<caret> f()", NONE);
    test("orgxxx.<caret> f() {", NONE);
    test("orgxxx.<caret> f() {}", NONE);
    testWithFileName(getFileName("org.test"), "org.<caret> f(", "test", "test2");
    testWithFileName(getFileName("org.test"), "org.<caret> f()", "test", "test2");
    testWithFileName(getFileName("org.test"), "org.<caret> f() {", "test", "test2");
    testWithFileName(getFileName("org.test"), "org.<caret> f() {}", "test", "test2");
    testWithFileName(getFileName("org.test"), "org.test.<caret> f(", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "org.test.<caret> f()", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "org.test.<caret> f() {", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "org.test2.<caret> f(", "ABC", "sub");
    testWithFileName(getFileName("org.test"), "org.test2.<caret> f(int", "ABC", "sub");
    testWithFileName(getFileName("org.test"), "org.test2.<caret> f(x)", "ABC", "sub");
    testWithFileName(getFileName("org.test"), "org.test2.<caret> f(x) {", "ABC", "sub");

    // Methods
    test("class XYZ{}\nclass X { org.<caret> f(", "test", "test2");
    test("class XYZ{}\nclass X { org.<caret> f()", "test", "test2");
    test("class XYZ{}\nclass X { org.<caret> f() {}", "test", "test2");
    test("class XYZ{}\nclass X { org.<caret> f(int ", "test", "test2");
    test("class XYZ{}\nclass X { org.<caret> f(int x", "test", "test2");
    test("class XYZ{}\nclass X { org.<caret> f(int x,", "test", "test2");
    test("class XYZ{}\nclass X { org.test.<caret> f(", "XYZ", "AClass");
    test("class XYZ{}\nclass X { org.test.<caret> f()", "XYZ", "AClass");
    test("class XYZ{}\nclass X { org.test.<caret> f() {", "XYZ", "AClass");
    test("class X { org.test2.<caret> f(", "ABC", "sub");
    test("class X { org.test2.<caret> f(int", "ABC", "sub");
    test("class X { org.test2.<caret> f(int x", "ABC", "sub");
    test("class X { org.test2.<caret> f(int x)", "ABC", "sub");
    test("class X { org.test2.<caret> f(int x) {", "ABC", "sub");
    test("class X { org.test2.<caret> f(int x) {}", "ABC", "sub");
    test("class X { orgxxx.<caret> f(", NONE);
    test("class X { orgxxx.<caret> f()", NONE);
    test("class X { orgxxx.<caret> f() {", NONE);
    test("class X { orgxxx.<caret> f() {}", NONE);
    testWithFileName(getFileName("org.test"), "class X { org.<caret> f(", "test", "test2");
    testWithFileName(getFileName("org.test"), "class X { org.<caret> f()", "test", "test2");
    testWithFileName(getFileName("org.test"), "class X { org.<caret> f() {", "test", "test2");
    testWithFileName(getFileName("org.test"), "class X { org.<caret> f() {}", "test", "test2");
    testWithFileName(getFileName("org.test"), "class X { org.test.<caret> f(", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "class X { org.test.<caret> f()", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "class X { org.test.<caret> f() {", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "class X { org.test2.<caret> f(", "ABC", "sub");
    testWithFileName(getFileName("org.test"), "class X { org.test2.<caret> f(int", "ABC", "sub");
    testWithFileName(getFileName("org.test"), "class X { org.test2.<caret> f(x)", "ABC", "sub");
    testWithFileName(getFileName("org.test"), "class X { org.test2.<caret> f(x) {", "ABC", "sub");

    // Inner classes
    test("class XYZ{\nclass Inner {}\n<caret> f(", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\n<caret> f(int", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\n<caret> f(int x", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\n<caret> f(x", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\n<caret> f(x)", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\n<caret> f(x) {}", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\n<caret> f(int x) {}", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\n<caret> f(", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\n<caret> f(int x", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\n<caret> f(int x) {", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\n<caret> f(int x) {}", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\n<caret> f(int x) {} }", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { <caret> f(", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { <caret> f()", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { <caret> f() {} }", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));

    // Static methods
    test("class XYZ{}\nclass X { static org.<caret> f(", "test", "test2");
    test("class XYZ{}\nclass X { static org.<caret> f()", "test", "test2");
    test("class XYZ{}\nclass X { static org.<caret> f() {}", "test", "test2");
    test("class XYZ{}\nclass X { static org.<caret> f(int ", "test", "test2");
    test("class XYZ{}\nclass X { static org.<caret> f(int x", "test", "test2");
    test("class XYZ{}\nclass X { static org.<caret> f(int x,", "test", "test2");
    test("class XYZ{}\nclass X { static org.test.<caret> f(", "XYZ", "AClass");
    test("class XYZ{}\nclass X { static org.test.<caret> f()", "XYZ", "AClass");
    test("class XYZ{}\nclass X { static org.test.<caret> f() {", "XYZ", "AClass");
    test("class X { static org.test2.<caret> f(", "ABC", "sub");
    test("class X { static org.test2.<caret> f(int", "ABC", "sub");
    test("class X { static org.test2.<caret> f(int x", "ABC", "sub");
    test("class X { static org.test2.<caret> f(int x)", "ABC", "sub");
    test("class X { static org.test2.<caret> f(int x) {", "ABC", "sub");
    test("class X { static org.test2.<caret> f(int x) {}", "ABC", "sub");
    test("class X { static orgxxx.<caret> f(", NONE);
    test("class X { static orgxxx.<caret> f()", NONE);
    test("class X { static orgxxx.<caret> f() {", NONE);
    test("class X { static orgxxx.<caret> f() {}", NONE);
    testWithFileName(getFileName("org.test"), "class X { static org.<caret> f(", "test", "test2");
    testWithFileName(getFileName("org.test"), "class X { static org.<caret> f()", "test", "test2");
    testWithFileName(getFileName("org.test"), "class X { static org.<caret> f() {", "test", "test2");
    testWithFileName(getFileName("org.test"), "class X { static org.<caret> f() {}", "test", "test2");
    testWithFileName(getFileName("org.test"), "class X { static org.test.<caret> f(", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "class X { static org.test.<caret> f()", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "class X { static org.test.<caret> f() {", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "class X { static org.test2.<caret> f(", "ABC", "sub");
    testWithFileName(getFileName("org.test"), "class X { static org.test2.<caret> f(int", "ABC", "sub");
    testWithFileName(getFileName("org.test"), "class X { static org.test2.<caret> f(x)", "ABC", "sub");
    testWithFileName(getFileName("org.test"), "class X { static org.test2.<caret> f(x) {", "ABC", "sub");

    test("class XYZ{\nclass Inner {}\nstatic <caret> f(", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\nstatic <caret> f(int", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\nstatic <caret> f(int x", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\nstatic <caret> f(x", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\nstatic <caret> f(x)", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\nstatic <caret> f(x) {}", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\nstatic <caret> f(int x) {}", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nstatic <caret> f(", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nstatic <caret> f(int x", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nstatic <caret> f(int x) {", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nstatic <caret> f(int x) {}", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nstatic <caret> f(int x) {} }", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { static <caret> f(", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { static <caret> f()", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { static <caret> f() {} }", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
  }

  @Test public void testFieldAndVariableTypes() {
    // Variables
    test("class X{}; <caret> f", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{}\norg.<caret>", "test", "test2");
    test("class XYZ{}\norg.<caret> f", "test", "test2");
    test("class XYZ{}\norg.test.<caret>", "XYZ", "AClass");
    test("class XYZ{}\norg.test.<caret> f", "XYZ", "AClass");
    test("org.test2.<caret>", "ABC", "sub");
    test("org.test2.<caret> f", "ABC", "sub");
    test("orgxxx.<caret>", NONE);
    test("orgxxx.<caret> f", NONE);
    testInOrgTest("class X{}; <caret> f", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("org.<caret>", "test", "test2");
    testInOrgTest("org.<caret> f", "test", "test2");
    testInOrgTest("org.test.<caret>", "XYZ", "AClass");
    testInOrgTest("org.test.<caret> f", "XYZ", "AClass");
    testInOrgTest("org.test2.<caret>", "ABC", "sub");
    testInOrgTest("org.test2.<caret> f", "ABC", "sub");

    // Fields
    test("class X{ <caret> f", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{ <caret> f }", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{ <caret> f", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{ <caret> f }", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{}\nclass X { org.<caret> f", "test", "test2");
    test("class XYZ{}\nclass X { org.test.<caret> f", "XYZ", "AClass");
    test("class X { org.test2.<caret> f", "ABC", "sub");
    test("class X { orgxxx.<caret> f", NONE);
    testWithFileName(getFileName("org.test"), "class X { org.<caret> f", "test", "test2");
    testWithFileName(getFileName("org.test"), "class X { org.test.<caret> f", "XYZ", "AClass");
    testWithFileName(getFileName("org.test"), "class X { org.test2.<caret> f", "ABC", "sub");

    // Inner classes
    test("class XYZ{\nclass Inner {}\n<caret> f", Stream.of("XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\n<caret> f", Stream.of("XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { <caret> f", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));

    // Const fields
    test("class X{ const <caret> f", Stream.of(JactlUtils.SIMPLE_TYPES));
    test("class X{ const <caret> f }", Stream.of(JactlUtils.SIMPLE_TYPES));
    testInOrgTest("class X{ const <caret> f", Stream.of(JactlUtils.SIMPLE_TYPES));
    testInOrgTest("class X{ const <caret> f }", Stream.of(JactlUtils.SIMPLE_TYPES));
    test("class XYZ{}\nclass X { const org.<caret> f", NONE);
    test("class XYZ{}\nclass X { const org.test.<caret> f", NONE);
    test("class X { const org.test2.<caret> f", NONE);
    test("class X { const orgxxx.<caret> f", NONE);
    testWithFileName(getFileName("org.test"), "class X { const org.<caret> f", NONE);
    testWithFileName(getFileName("org.test"), "class X { const org.test.<caret> f", NONE);
    testWithFileName(getFileName("org.test"), "class X { const org.test2.<caret> f", NONE);
    test("class XYZ{\nclass Inner {}\nconst <caret> f", Stream.of(JactlUtils.SIMPLE_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nconst <caret> f", Stream.of(JactlUtils.SIMPLE_TYPES));
    testWithFileName(getFileName("org.test"), "package const org.test; class X extends AClass.B { const <caret> f", Stream.of(JactlUtils.SIMPLE_TYPES));
  }

  @Test public void testIdentifierInExpr() {
    test("class X{ static def func() {1} }; X x; <caret>", Stream.of("x", "X", "X.func", "X.fromJson", "class"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{ static def func() {1} }; 3 + <caret>", Stream.of("X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{ static def func() {1} }; <caret> + ", Stream.of("X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{ static def func() {1} }; <caret> + 3", Stream.of("X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{ static def func() {1} }; (<caret>", Stream.of("X", "X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream(), Stream.of(JactlUtils.BUILTIN_TYPES), globalVars.stream());
    test("class X{ static def func() {1} }; (<caret> + ", Stream.of("X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{ static def func() {1} }; (<caret> + 3", Stream.of("X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{ static def func() {1} }; (<caret> + 3)", Stream.of("X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{ static def func() {1} }; X x; { int i; }; def f(){}; { def j; <caret>", Stream.of("x", "f", "j", "X", "X.func", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{ static def func() {1} }; X x; { int i; }; def f(){}; { def j; j + <caret>", Stream.of("x", "f", "j", "X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{ static def func() {1} }; X x; { int i; }; def f(){}; { def j; <caret> }", Stream.of("x", "f", "j", "X", "X.func", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{ static def func() {1} }; X x; { int i; }; def f(){}; { def j; j + <caret> }", Stream.of("x", "f", "j", "X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    testInOrgTest("class X{}; <caret>", Stream.of("class", "X", "X.fromJson", "XYZ", "XYZ.func", "XYZ.fromJson", "AClass", "AClass.func", "AClass.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    testInOrgTest("class X{}; X x; x + <caret>", Stream.of("x", "X.fromJson", "XYZ.func", "XYZ.fromJson", "AClass.func", "AClass.fromJson"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());

    test("class X{ static def func() {1}; X x; <caret>", Stream.of("static", "class", "const"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{ static def func() {1}; X x; <caret> }", Stream.of("static", "class", "const"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{ static def func() {1}; X x; def f(int i) { <caret>", Stream.of("x", "i", "X", "func", "fromJson", "f", "this"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{ static def func() {1}; X x; def f(int i) { <caret> }", Stream.of("x", "i", "X", "func", "fromJson", "f", "this"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{ static def func() {1}; X x; def f(int i) { i + <caret>", Stream.of("x", "i", "func", "fromJson", "f", "this"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    testInOrgTest("class X extends AClass { X x; def f(int i) { <caret>", classesWithFromJson("B", "C", "XYZ"), Stream.of("x", "xxx", "VALUE", "i", "X", "AClass", "func", "fromJson", "f", "this", "super", "XYZ.func"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    testInOrgTest("class X extends AClass { X x; def f(int i) { <caret> }", classesWithFromJson("B", "C", "XYZ"), Stream.of("x", "xxx", "VALUE", "i", "X", "AClass", "func", "fromJson", "f", "this", "super", "XYZ.func"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    testInOrgTest("class X extends AClass { X x; def f(int i) { i + <caret>", Stream.of("B.fromJson", "C.fromJson", "x", "xxx", "VALUE", "i", "func", "fromJson", "f", "this", "super", "XYZ.func", "XYZ.fromJson"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());

    test("//test\n<caret>", Stream.of("class", "package", "import"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("<caret>", Stream.of("class", "package", "import"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("package a.b\n<caret>", Stream.of("class", "import"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("package a.b\nimport x.y.Z\n//test\n<caret>", Stream.of("class", "import"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; <caret>", Stream.of("class", "X", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; while (true) { <caret>", Stream.of("X", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; while (true) { def i = 1\n<caret>", Stream.of("i", "X", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; if (true) { <caret>", Stream.of("X", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; for (int i; i < 10; i++) { <caret>", Stream.of("i", "X", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());

    // Globals not supported in class files:
    testWithFileName("org/X.jactl", "package org; class X{ static def func() {1}; X x; def f(int i) { <caret>", Stream.of("x", "i", "X", "func", "fromJson", "f", "this"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    testWithFileName("org/X.jactl", "package org; class X{ static def func() {1}; X x; def f(int i) { <caret> } }", Stream.of("x", "i", "X", "func", "fromJson", "f", "this"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
  }

  @Test public void testImport() {
    testIncludes("//test\npackage a.b.c; <caret>", "import");
    testIncludes("//test\npackage a.b.c\n//test\n<caret>", "import");
    testIncludes("<caret>", "import");
    testExcludes("def x = 1; <caret>", "import");
    test("import <caret>", Stream.of("static", "org"));
    test("import org<caret>", Stream.of());
    test("import org.<caret>", Stream.of("test", "test2"));
    test("import org.<caret>\ndef x = 1", Stream.of("test", "test2"));
    test("import org.test2.<caret>", Stream.of("sub", "ABC"));
    test("import org.test.<caret>", Stream.of("AClass", "XYZ"));
    test("import org.test.AClass.<caret>", Stream.of("B", "C"));
    test("import org.test.AClass.<caret> as XXX", Stream.of("B", "C"));
    test("import org.test.AClass.<caret> as XXX", Stream.of("B", "C"));
    test("import static <caret>", Stream.of("org"));
    test("import static org<caret>", Stream.of());
    test("import static org.<caret>", Stream.of("test", "test2"));
    test("import static org.<caret>\ndef x = 1", Stream.of("test", "test2"));
    test("import static org.test2.<caret>", Stream.of("sub", "ABC"));
    test("import static org.test.<caret>", Stream.of("AClass", "XYZ"));
    test("import static org.test.AClass.<caret>", Stream.of("B", "C", "VALUE", "fromJson", "func"));
    test("import static org.test.AClass.<caret> as JJJ", Stream.of("B", "C", "VALUE", "fromJson", "func"));
    test("import static org.test.AClass.B.<caret> as JJJ", Stream.of("XXX", "BBB", "fromJson"));
    test("import static org.test.AClass.B.XXX.<caret> as JJJ", Stream.of("xxxfunc", "fromJson"));
    test("import org.test.AClass; <caret>", Stream.of("AClass", "AClass.fromJson", "AClass.func", "class", "import"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
  }

  @Test public void testPackage() {
    testIncludes("<caret>", "package");
    testIncludes("//test\n<caret>", "package");
    testIncludes("//test\n/*test*/ <caret>", "package");
    testExcludes("def x = 1\n<caret>", "package");
    testExcludes("package a.b.c\n<caret>", "package");
    testExcludes("import a.b.C\n<caret>", "package");
    test("package <caret>", "org");
    doTest("package o<caret>", Stream.of());
    test("package org.<caret>", Stream.of("test","test2"));
    test("package org.test2.<caret>", Stream.of("sub"));
    test("package org.test2.sub.<caret>", Stream.of());
  }

  @Test public void testMethodsAndFields() {
    test("class X { int i }; X x = new X(); x.<caret>", "i", "fromJson", "className", "toJson", "toString");
    test("class X { int i }; new X().<caret>", "i", "fromJson", "className", "toJson", "toString");
    test("class X { int i }; def x = new X().<caret>", "i", "fromJson", "className", "toJson", "toString");
    test("class X { int i }; def x = new X().<caret>; def y = 2", "i", "fromJson", "className", "toJson", "toString");
    test("class X { int i }; if (true) { new X().<caret>; def y = 2", "i", "fromJson", "className", "toJson", "toString");
    test("class X { int i; def f(){}; static def g(){} }; new X().<caret>", "i", "f", "g", "fromJson", "className", "toJson", "toString");
    test("class Y { int j; def h(){} }; class X extends Y { int i; def f(){}; static Y g(){} }; new X().<caret>", "i", "f", "g", "j", "h", "fromJson", "className", "toJson", "toString");
    test("class Y { int j; static def h(){} }; class X extends Y { int i; def f(){}; static Y g(){} }; X.<caret>", "fromJson", "g", "h");
    test("class Y { int j; static def h(){} }; class X extends Y { class Inner{}; int i; def f(){}; static Y g(){} }; X.<caret>", "fromJson", "g", "h", "Inner");
  }

  @Test public void testBuiltinMethods() {
    testIncludes("List x; x.<caret>", "size", "map", "toString");
    test("List x; x.<caret>", Functions.getAllMethods(JactlType.LIST).stream().map(f -> f.first));
    test("var x = [1,2,3]; x.<caret>", Functions.getAllMethods(JactlType.LIST).stream().map(f -> f.first));
    testIncludes("Map x; x.<caret>", "size", "map", "toString");
    test("Map x; x.<caret>", Functions.getAllMethods(JactlType.MAP).stream().map(f -> f.first));
    test("var x = [a:1]; x.<caret>", Functions.getAllMethods(JactlType.MAP).stream().map(f -> f.first));
    test("def x; x.<caret>", "className", "toJson", "toString");
  }

  @Test public void testFinalType() {
    testIncludes("def x = [1,2,3]; x.<caret>", "size", "map");
    testIncludes("def x = 1; x = [1,2,3]; x.<caret>", "size", "map");
  }

  @Test public void testCastAndAs() {
    test("class X{}; def x; x as <caret>", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def x; (<caret>", classesWithFromJson("X"), Stream.of("x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; def x; x = (<caret>", classesWithFromJson("X"), Stream.of("x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; def x; x = 3 + (<caret>", classesWithFromJson("X"), Stream.of("x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; def x; x = 3 * (2 + (<caret>", classesWithFromJson("X"), Stream.of("x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; def x; (<caret>)", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def x; (<caret>)x", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def x; if (true) { x++; x as <caret>", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def x; if (true) { x++; (<caret>", classesWithFromJson("X"), Stream.of("x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; def x; if (true) { x++; x = (<caret>", classesWithFromJson("X"), Stream.of("x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; def x; if (true) { x++; x = 3 + (<caret>", classesWithFromJson("X"), Stream.of("x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; def x; if (true) { x++; x = true ? 3 * (2 + (<caret>", classesWithFromJson("X"), Stream.of("x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; def x; if (true) { x++; x = true ? 4 : 3 * (2 + (<caret>", classesWithFromJson("X"), Stream.of("x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("class X{}; def x; if (true) { x++; (<caret>)", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def x; if (true) { x++; (<caret>)x", Stream.of("X"), Stream.of(JactlUtils.BUILTIN_TYPES));

    testInOrgTest("class X{}; def x; x as <caret>", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
  }

  @Test public void testInnerClass() {
    test("org.<caret>", Stream.of("test", "test2"));
    test("org.test.AClass.<caret>", Stream.of("B", "C", "fromJson", "func"));
    test("new org.test.AClass.<caret>", Stream.of("B", "C"));
    test("new org.test.AClass.B.<caret>", Stream.of("XXX"));
    test("class X { class Y {} }; new X.<caret>", Stream.of("Y"));
    test("class X { class Y { static def f() {} } }; X.<caret>", Stream.of("Y", "fromJson"));
    test("class X { class Y { static def f() {} } }; X.Y.<caret>", Stream.of("f", "fromJson"));
  }

  @Test public void testNestedBlocks() {
    test("int xxx = 1\n<caret>long yyy = 2; double zzz = 3", Stream.of("xxx", "class"), Functions.getGlobalFunctionNames().stream(), globalVars.stream(), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS));
    test("int xxx = 1\nlong yyy = 2; <caret>double zzz = 3", Stream.of("xxx", "yyy", "class"), Functions.getGlobalFunctionNames().stream(), globalVars.stream(), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS));
    test("int x = 3\nint y = <caret>\nint zzz\n", Stream.of("x"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("int x = 3\n{ int y = <caret> }\nint zzz\n", Stream.of("x"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("int x = 3\ndef f(int y = <caret>) {}\nint zzz\n", Stream.of("x", "f"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("int x = 3\ndef f(int i = 2, int j = <caret>) {}\nint zzz\n", Stream.of("x", "f", "i"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("int x = 3\ndef f(int i = 2, int j = 3 + <caret>) {}\nint zzz\n", Stream.of("x", "f", "i"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("int x = 3\ndef f(int y = 2) { <caret> }\nint zzz\n", Stream.of("x", "f", "y"), Functions.getGlobalFunctionNames().stream(), globalVars.stream(), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS));
    test("int x = 3\ndef f(int y) { def g(int z = <caret>) {} }\nint zzz\n", Stream.of("x", "f", "y", "g"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("int x = 3\ndef f(int y) { <caret>def z = 2 }\nint zzz\n", Stream.of("x", "f", "y"), Functions.getGlobalFunctionNames().stream(), globalVars.stream(), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS));
    test("int x = 3\ndef f(int y) { <caret>def g(int z = 2) {} }\nint zzz\n", Stream.of("x", "f", "y" /*,"g"*/), Functions.getGlobalFunctionNames().stream(), globalVars.stream(), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS));
    test("int x = 3\nclass X { def f(int y) { <caret>def g(int z = 2) {} } }\nint zzz\n", Stream.of("f", "y", "X", "X.fromJson", "fromJson", "this" /*,"g"*/), Functions.getGlobalFunctionNames().stream(), globalVars.stream(), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS));
  }

  @Test public void testSwitchBindingVars() {
    test("def x; switch (x) {\n  [a,b,c] -> <caret>", Stream.of("x","it","a","b","c"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("def x; switch (x) {\n  [a,b,c] -> <caret>}", Stream.of("x","it","a","b","c"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("def x; switch (x) {\n  [a,b,c] -> a + <caret>", Stream.of("x","it","a","b","c"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("def x; switch (x) {\n  [a,b,c] -> a + <caret>}", Stream.of("x","it","a","b","c"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("def x; switch (x) {\n  [a,b,c] -> b + <caret>", Stream.of("x","it","a","b","c"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("def x; switch (x) {\n  [a,b,c] -> b + <caret>}", Stream.of("x","it","a","b","c"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("def x; switch (x) {\n  [a,b,c] if <caret>", Stream.of("x","it","a","b","c"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("def x; switch (x) {\n  [a,b,c] if <caret> ->", Stream.of("x","it","a","b","c"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("def x; switch (x) {\n  [a,b,c] if <caret> -> }", Stream.of("x","it","a","b","c"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
    test("def x; switch (x) {\n  [a,b,c] if <caret> -> 1\n }", Stream.of("x","it","a","b","c"), Functions.getGlobalFunctionNames().stream(), globalVars.stream());
  }
}
