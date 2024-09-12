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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.testIntegration.TestFramework;
import io.jactl.JactlType;
import io.jactl.runtime.Functions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class CompletionTests {

  private static final String NONE = "$$NONE$$";

  protected void setUp() throws Exception {
//    setProject(null);
//    super.setUp();
//    Project project = getProject();
//    System.out.println(getTestName(true));
//    String testData = "testData";
//    JactlUtils.setSourceRoots(testData);
//    String sourceRoot = "src/test/completionTests";
//    VirtualFile from = LocalFileSystem.getInstance().refreshAndFindFileByPath(sourceRoot);
//    assertNotNull(from);
//    WriteAction.computeAndWait(() -> {
//      try {
//        VirtualFile baseDir = project.getBaseDir();
//        VirtualFile destDir = baseDir.findFileByRelativePath(testData);
//        if (destDir == null) {
//          destDir = baseDir.createChildDirectory(this, testData);
//          VfsUtil.copyDirectory(this, from, destDir, VirtualFileFilter.ALL);
//        }
//        return true;
//      }
//      catch (Throwable e) {
//        throw new RuntimeException(e);
//      }
//    });
  }

  enum MatchType {
    INCLUDES, EXCLUDES, ALL
  }

  private void test(String text, Stream<String>... expected) {
    test(text, Arrays.stream(expected).flatMap(s -> s).toArray(String[]::new));
  }

  private void test(String text, String... expected) {
    testWithFileName("script.jactl", text, expected);
  }

  private void testIncludes(String text, String... expected) {
    testWithFileName("script.jactl", text, MatchType.INCLUDES, expected);
  }

  private void testExcludes(String text, String... expected) {
    testWithFileName("script.jactl", text, MatchType.EXCLUDES, expected);
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
    testWithFileName("org/test/script.jactl", text, expected);
  }

  private void testWithFileName(String fileName, String text, String... expected) {
    testWithFileName(fileName, text, MatchType.ALL, expected);
  }

  private void testWithFileName(String fileName, String text, MatchType matchType, String... expected) {
//    configureFromFileText(fileName, text);
//    try {
//      complete();
//    }
//    catch (Throwable throwable) {
//      throw throwable;
//    }
//    List<String> items = myItems == null ? List.of() : Arrays.stream(myItems).map(LookupElement::getLookupString).sorted().toList();
//    switch (matchType) {
//      case INCLUDES -> {
//        Arrays.stream(expected).forEach(e -> assertTrue("Missing '" + e + "' in " + items, items.contains(e)));
//      }
//      case EXCLUDES -> {
//        Arrays.stream(expected).forEach(e -> assertFalse("Should not include '" + e + "' in " + items, items.contains(e)));
//      }
//      case ALL -> {
//        if (expected.length == 1 && expected[0] == NONE) {
//          assertTrue("Expected no entries but got : " + items, items.isEmpty());
//        }
//        else {
//          assertEquals(Arrays.stream(expected).sorted().toList(), items);
//        }
//      }
//    }
  }

  private Stream<String> classesWithFromJson(String... classes) {
    return Arrays.stream(classes).flatMap(clss -> Stream.of(clss, clss + ".fromJson"));
  }

  ////////////////////////////////

  public void testExtendsClassName() {
    test("class XYZ{}; class X <caret>", "extends");
    test("class XYZ{}\nclass X extends <caret> {}", "XYZ", "TopLevel");
    test("class XYZ{}\nclass X extends <caret>", "XYZ", "TopLevel");
    test("class XYZ{}; class ABC extends XYZ{}\nclass X extends <caret> {}", "XYZ", "TopLevel", "ABC");
    test("class XYZ{}; class ABC extends XYZ{}\nclass X extends <caret>", "XYZ", "TopLevel", "ABC");
    testInOrgTest("class ABC extends XYZ{}\nclass X extends <caret> {}", "ABC", "XYZ", "AClass");
    testInOrgTest("class ABC extends XYZ{}\nclass X extends <caret>", "ABC", "XYZ", "AClass");
  }

  public void testExtendsInnerClassName() {
    test("class XYZ{\nclass Inner {}\nclass X extends <caret> {}", "XYZ", "Inner", "TopLevel");
    test("class XYZ{\nclass Inner {}\nclass X extends <caret> {", "XYZ", "Inner", "TopLevel");
    test("class XYZ{\nclass Inner {}\nclass X extends <caret>", "XYZ", "Inner", "TopLevel");
    test("class XYZ{\nclass Inner {}\n}\nclass X extends <caret> {}", "XYZ", "TopLevel");
    test("class XYZ{\nclass Inner {}\n}\nclass X extends <caret> {", "XYZ", "TopLevel");
    test("class XYZ{\nclass Inner {}\n}\nclass X extends <caret>", "XYZ", "TopLevel");
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nclass X extends <caret> {} }", "XYZ", "ABC", "Inner", "TopLevel");
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nclass X extends <caret> {}", "XYZ", "ABC", "Inner", "TopLevel");
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nclass X extends <caret>", "XYZ", "ABC", "Inner", "TopLevel");
    testInOrgTest("class X extends AClass.B { class Y extends <caret> }", "XYZ", "AClass", "XXX", "X", "B");
    testInOrgTest("class X extends AClass.B { class Y extends <caret>", "XYZ", "AClass", "XXX", "X", "B");
    testInOrgTest("class X extends AClass.B { class Y extends <caret> {", "XYZ", "AClass", "XXX", "X", "B");
    testInOrgTest("class X extends AClass.B { class Y extends <caret> { int i = }", "XYZ", "AClass", "XXX", "X", "B");
    testInOrgTest("class X extends AClass.B { class Y extends <caret> {} }", "XYZ", "AClass", "XXX", "X", "B");
  }

  public void testExtendsPackageName() {
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
    testWithFileName("org/test/script.jactl", "class X extends org.<caret> {}", "test", "test2");
    testWithFileName("org/test/script.jactl", "class X extends org.<caret>", "test", "test2");
    testWithFileName("org/test/script.jactl", "class X extends org.test.<caret> {}", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "class X extends org.test.<caret>", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "class X extends org.test2.<caret> {}", "ABC", "sub");
    testWithFileName("org/test/script.jactl", "class X extends org.test2.<caret>", "ABC", "sub");
  }

  public void testParameterTypes() {
    test("class X{}; def f(<caret> x", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(<caret>", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(<caret> x)", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(<caret> x, ", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(int x, <caret>", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(int x, <caret>)", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(x, <caret>", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(x, <caret>)", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f(x, <caret> y)", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(<caret> x", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(<caret>", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(<caret> x)", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(<caret> x, ", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(int x, <caret>", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(int x, <caret>)", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(x, <caret>", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(x, <caret>)", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{}; def f(x, <caret> y)", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));

    test("class X{}; def f = { <caret> x ->", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { <caret> ->", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { <caret> x ->", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { <caret> x, ->", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { int x, <caret> ->", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { int x, <caret> ->", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { x, <caret> ->", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { x, <caret> ->", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def f = { x, <caret> y ->", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));

    test("class XYZ{\nclass Inner {}\ndef f(<caret>", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(<caret> x", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(<caret> x)", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(<caret> x, )", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret>", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret>)", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret> y)", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret> y) }", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret> y) {", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret> y) {}", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\ndef f(int x, <caret> y) {} }", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(<caret>", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(<caret> x", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(int x, <caret>", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(int x, <caret> y", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(int x, <caret> y)", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(int x, <caret> y) {", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(int x, <caret> y) {}", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\ndef f(int x, <caret> y) {} }", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
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
    testWithFileName("org/test/script.jactl", "def f(org.<caret>", "test", "test2");
    testWithFileName("org/test/script.jactl", "def f(org.<caret> x", "test", "test2");
    testWithFileName("org/test/script.jactl", "def f(org.<caret> x)", "test", "test2");
    testWithFileName("org/test/script.jactl", "def f(int x, org.<caret>", "test", "test2");
    testWithFileName("org/test/script.jactl", "def f(int x, org.<caret> y", "test", "test2");
    testWithFileName("org/test/script.jactl", "def f(int x, org.<caret> y)", "test", "test2");
    testWithFileName("org/test/script.jactl", "def f(org.test.<caret>", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "def f(org.test.<caret> x)", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "def f(int x, org.test.<caret>", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "def f(int x, org.test.<caret> y", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "def f(org.test2.<caret>", "ABC", "sub");
    testWithFileName("org/test/script.jactl", "def f(org.test2.<caret> x", "ABC", "sub");
  }

  public void testFunctionReturnTypes() {
    test("class X{}; <caret> f(", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(x)", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x)", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x, ", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x, int", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x, y", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x, y)", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x, y) {", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; <caret> f(int x, y) {}", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
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
    testWithFileName("org/test/script.jactl", "org.<caret> f(", "test", "test2");
    testWithFileName("org/test/script.jactl", "org.<caret> f()", "test", "test2");
    testWithFileName("org/test/script.jactl", "org.<caret> f() {", "test", "test2");
    testWithFileName("org/test/script.jactl", "org.<caret> f() {}", "test", "test2");
    testWithFileName("org/test/script.jactl", "org.test.<caret> f(", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "org.test.<caret> f()", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "org.test.<caret> f() {", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "org.test2.<caret> f(", "ABC", "sub");
    testWithFileName("org/test/script.jactl", "org.test2.<caret> f(int", "ABC", "sub");
    testWithFileName("org/test/script.jactl", "org.test2.<caret> f(x)", "ABC", "sub");
    testWithFileName("org/test/script.jactl", "org.test2.<caret> f(x) {", "ABC", "sub");

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
    testWithFileName("org/test/script.jactl", "class X { org.<caret> f(", "test", "test2");
    testWithFileName("org/test/script.jactl", "class X { org.<caret> f()", "test", "test2");
    testWithFileName("org/test/script.jactl", "class X { org.<caret> f() {", "test", "test2");
    testWithFileName("org/test/script.jactl", "class X { org.<caret> f() {}", "test", "test2");
    testWithFileName("org/test/script.jactl", "class X { org.test.<caret> f(", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "class X { org.test.<caret> f()", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "class X { org.test.<caret> f() {", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "class X { org.test2.<caret> f(", "ABC", "sub");
    testWithFileName("org/test/script.jactl", "class X { org.test2.<caret> f(int", "ABC", "sub");
    testWithFileName("org/test/script.jactl", "class X { org.test2.<caret> f(x)", "ABC", "sub");
    testWithFileName("org/test/script.jactl", "class X { org.test2.<caret> f(x) {", "ABC", "sub");

    // Inner classes
    test("class XYZ{\nclass Inner {}\n<caret> f(", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\n<caret> f(int", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\n<caret> f(int x", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\n<caret> f(x", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\n<caret> f(x)", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\n<caret> f(x) {}", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\n<caret> f(int x) {}", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\n<caret> f(", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\n<caret> f(int x", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\n<caret> f(int x) {", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\n<caret> f(int x) {}", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\n<caret> f(int x) {} }", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
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
    testWithFileName("org/test/script.jactl", "class X { static org.<caret> f(", "test", "test2");
    testWithFileName("org/test/script.jactl", "class X { static org.<caret> f()", "test", "test2");
    testWithFileName("org/test/script.jactl", "class X { static org.<caret> f() {", "test", "test2");
    testWithFileName("org/test/script.jactl", "class X { static org.<caret> f() {}", "test", "test2");
    testWithFileName("org/test/script.jactl", "class X { static org.test.<caret> f(", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "class X { static org.test.<caret> f()", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "class X { static org.test.<caret> f() {", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "class X { static org.test2.<caret> f(", "ABC", "sub");
    testWithFileName("org/test/script.jactl", "class X { static org.test2.<caret> f(int", "ABC", "sub");
    testWithFileName("org/test/script.jactl", "class X { static org.test2.<caret> f(x)", "ABC", "sub");
    testWithFileName("org/test/script.jactl", "class X { static org.test2.<caret> f(x) {", "ABC", "sub");

    test("class XYZ{\nclass Inner {}\nstatic <caret> f(", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\nstatic <caret> f(int", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\nstatic <caret> f(int x", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\nstatic <caret> f(x", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\nstatic <caret> f(x)", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\nstatic <caret> f(x) {}", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{\nclass Inner {}\nstatic <caret> f(int x) {}", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nstatic <caret> f(", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nstatic <caret> f(int x", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nstatic <caret> f(int x) {", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nstatic <caret> f(int x) {}", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nstatic <caret> f(int x) {} }", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { static <caret> f(", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { static <caret> f()", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X extends AClass.B { static <caret> f() {} }", Stream.of("XYZ", "AClass", "XXX", "X", "B"), Stream.of(JactlUtils.BUILTIN_TYPES));
  }

  public void testFieldAndVariableTypes() {
    // Variables
    test("class X{}; <caret> f", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
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
    test("class X{ <caret> f", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{ <caret> f }", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{ <caret> f", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    testInOrgTest("class X{ <caret> f }", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{}\nclass X { org.<caret> f", "test", "test2");
    test("class XYZ{}\nclass X { org.test.<caret> f", "XYZ", "AClass");
    test("class X { org.test2.<caret> f", "ABC", "sub");
    test("class X { orgxxx.<caret> f", NONE);
    testWithFileName("org/test/script.jactl", "class X { org.<caret> f", "test", "test2");
    testWithFileName("org/test/script.jactl", "class X { org.test.<caret> f", "XYZ", "AClass");
    testWithFileName("org/test/script.jactl", "class X { org.test2.<caret> f", "ABC", "sub");

    // Inner classes
    test("class XYZ{\nclass Inner {}\n<caret> f", Stream.of("TopLevel", "XYZ", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\n<caret> f", Stream.of("TopLevel", "XYZ", "ABC", "Inner"), Stream.of(JactlUtils.BUILTIN_TYPES));
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
    testWithFileName("org/test/script.jactl", "class X { const org.<caret> f", NONE);
    testWithFileName("org/test/script.jactl", "class X { const org.test.<caret> f", NONE);
    testWithFileName("org/test/script.jactl", "class X { const org.test2.<caret> f", NONE);
    test("class XYZ{\nclass Inner {}\nconst <caret> f", Stream.of(JactlUtils.SIMPLE_TYPES));
    test("class XYZ{ class Inner{} }; class ABC extends XYZ{\nconst <caret> f", Stream.of(JactlUtils.SIMPLE_TYPES));
    testWithFileName("org/test/script.jactl", "package const org.test; class X extends AClass.B { const <caret> f", Stream.of(JactlUtils.SIMPLE_TYPES));
  }

  public void testIdentifierInExpr() {
    test("class X{ static def func() {1} }; X x; <caret>", Stream.of("x", "TopLevel", "TopLevel.staticFunc", "TopLevel.fromJson", "X", "X.func", "X.fromJson", "class"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("class X{ static def func() {1} }; 3 + <caret>", Stream.of("TopLevel.staticFunc", "TopLevel.fromJson", "X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream());
    test("class X{ static def func() {1} }; <caret> + ", Stream.of("TopLevel.staticFunc", "TopLevel.fromJson", "X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream());
    test("class X{ static def func() {1} }; <caret> + 3", Stream.of("TopLevel.staticFunc", "TopLevel.fromJson", "X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream());
    test("class X{ static def func() {1} }; (<caret>", Stream.of("TopLevel", "X", "TopLevel.staticFunc", "TopLevel.fromJson", "X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream(), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{ static def func() {1} }; (<caret> + ", Stream.of("TopLevel.staticFunc", "TopLevel.fromJson", "X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream());
    test("class X{ static def func() {1} }; (<caret> + 3", Stream.of("TopLevel.staticFunc", "TopLevel.fromJson", "X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream());
    test("class X{ static def func() {1} }; (<caret> + 3)", Stream.of("TopLevel.staticFunc", "TopLevel.fromJson", "X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream());
    test("class X{ static def func() {1} }; X x; { int i; }; def f(){}; { def j; <caret>", Stream.of("x", "f", "j", "TopLevel", "TopLevel.staticFunc", "TopLevel.fromJson", "X", "X.func", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("class X{ static def func() {1} }; X x; { int i; }; def f(){}; { def j; j + <caret>", Stream.of("x", "f", "j", "TopLevel.staticFunc", "TopLevel.fromJson", "X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream());
    test("class X{ static def func() {1} }; X x; { int i; }; def f(){}; { def j; <caret> }", Stream.of("x", "f", "j", "TopLevel", "TopLevel.staticFunc", "TopLevel.fromJson", "X", "X.func", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("class X{ static def func() {1} }; X x; { int i; }; def f(){}; { def j; j + <caret> }", Stream.of("x", "f", "j", "TopLevel.staticFunc", "TopLevel.fromJson", "X.func", "X.fromJson"), Functions.getGlobalFunctionNames().stream());
    testInOrgTest("class X{}; <caret>", Stream.of("class", "X", "X.fromJson", "XYZ", "XYZ.func", "XYZ.fromJson", "AClass", "AClass.func", "AClass.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    testInOrgTest("class X{}; X x; x + <caret>", Stream.of("x", "X.fromJson", "XYZ.func", "XYZ.fromJson", "AClass.func", "AClass.fromJson"), Functions.getGlobalFunctionNames().stream());

    test("class X{ static def func() {1}; X x; <caret>", Stream.of("static", "class", "const"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{ static def func() {1}; X x; <caret> }", Stream.of("static", "class", "const"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{ static def func() {1}; X x; def f(int i) { <caret>", Stream.of("x", "i", "TopLevel", "TopLevel.staticFunc", "TopLevel.fromJson", "X", "func", "fromJson", "f", "this"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("class X{ static def func() {1}; X x; def f(int i) { <caret> }", Stream.of("x", "i", "TopLevel", "TopLevel.staticFunc", "TopLevel.fromJson", "X", "func", "fromJson", "f", "this"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("class X{ static def func() {1}; X x; def f(int i) { i + <caret>", Stream.of("x", "i", "TopLevel.staticFunc", "TopLevel.fromJson", "func", "fromJson", "f", "this"), Functions.getGlobalFunctionNames().stream());
    testInOrgTest("class X extends AClass { X x; def f(int i) { <caret>", classesWithFromJson("B", "C", "XYZ"), Stream.of("x", "xxx", "VALUE", "i", "X", "AClass", "func", "fromJson", "f", "this", "super", "XYZ.func"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    testInOrgTest("class X extends AClass { X x; def f(int i) { <caret> }", classesWithFromJson("B", "C", "XYZ"), Stream.of("x", "xxx", "VALUE", "i", "X", "AClass", "func", "fromJson", "f", "this", "super", "XYZ.func"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    testInOrgTest("class X extends AClass { X x; def f(int i) { i + <caret>", Stream.of("B.fromJson", "C.fromJson", "x", "xxx", "VALUE", "i", "func", "fromJson", "f", "this", "super", "XYZ.func", "XYZ.fromJson"), Functions.getGlobalFunctionNames().stream());

    test("//test\n<caret>", Stream.of("TopLevel", "TopLevel.staticFunc", "TopLevel.fromJson", "class", "package", "import"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("<caret>", Stream.of("TopLevel", "TopLevel.staticFunc", "TopLevel.fromJson", "class", "package", "import"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("package a.b\n<caret>", Stream.of("class", "import"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("package a.b\nimport x.y.Z\n//test\n<caret>", Stream.of("class", "import"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("class X{}; <caret>", Stream.of("class", "TopLevel", "TopLevel.staticFunc", "TopLevel.fromJson", "X", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("class X{}; while (true) { <caret>", Stream.of("TopLevel", "TopLevel.staticFunc", "TopLevel.fromJson", "X", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("class X{}; while (true) { def i = 1\n<caret>", Stream.of("i", "TopLevel", "TopLevel.staticFunc", "TopLevel.fromJson", "X", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("class X{}; if (true) { <caret>", Stream.of("TopLevel", "TopLevel.staticFunc", "TopLevel.fromJson", "X", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
    test("class X{}; for (int i; i < 10; i++) { <caret>", Stream.of("i", "TopLevel", "TopLevel.staticFunc", "TopLevel.fromJson", "X", "X.fromJson"), Stream.of(JactlUtils.BUILTIN_TYPES), Stream.of(JactlUtils.BEGINNING_KEYWORDS), Functions.getGlobalFunctionNames().stream());
  }

  public void testImport() {
    testIncludes("//test\npackage a.b.c; <caret>", "import");
    testIncludes("//test\npackage a.b.c\n//test\n<caret>", "import");
    testIncludes("<caret>", "import");
    testExcludes("def x = 1; <caret>", "import");
    test("import <caret>", Stream.of("static", "TopLevel", "org"));
    test("import org<caret>", Stream.of());
    test("import org.<caret>", Stream.of("test", "test2"));
    test("import org.<caret>\ndef x = 1", Stream.of("test", "test2"));
    test("import org.test2.<caret>", Stream.of("sub", "ABC"));
    test("import org.test.<caret>", Stream.of("AClass", "XYZ"));
    test("import org.test.AClass.<caret>", Stream.of("B", "C"));
    test("import org.test.AClass.<caret> as XXX", Stream.of("B", "C"));
    test("import org.test.AClass.<caret> as XXX", Stream.of("B", "C"));
    test("import static <caret>", Stream.of("TopLevel", "org"));
    test("import static org<caret>", Stream.of());
    test("import static org.<caret>", Stream.of("test", "test2"));
    test("import static org.<caret>\ndef x = 1", Stream.of("test", "test2"));
    test("import static org.test2.<caret>", Stream.of("sub", "ABC"));
    test("import static org.test.<caret>", Stream.of("AClass", "XYZ"));
    test("import static org.test.AClass.<caret>", Stream.of("B", "C", "VALUE", "fromJson", "func"));
    test("import static org.test.AClass.<caret> as JJJ", Stream.of("B", "C", "VALUE", "fromJson", "func"));
    test("import static org.test.AClass.B.<caret> as JJJ", Stream.of("XXX", "BBB", "fromJson"));
    test("import static org.test.AClass.B.XXX.<caret> as JJJ", Stream.of("xxxfunc", "fromJson"));
  }

  public void testPackage() {
    testIncludes("<caret>", "package");
    testIncludes("//test\n<caret>", "package");
    testIncludes("//test\n/*test*/ <caret>", "package");
    testExcludes("def x = 1\n<caret>", "package");
    testExcludes("package a.b.c\n<caret>", "package");
    testExcludes("import a.b.C\n<caret>", "package");
    test("package <caret>", "org");
    test("package o<caret>", Stream.of());
    test("package org.<caret>", Stream.of("test","test2"));
    test("package org.test2.<caret>", Stream.of("sub"));
    test("package org.test2.sub.<caret>", Stream.of());
  }

  public void testMethodsAndFields() {
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

  public void testBuiltinMethods() {
    testIncludes("List x; x.<caret>", "size", "map", "toString");
    test("List x; x.<caret>", Functions.getAllMethods(JactlType.LIST).stream().map(f -> f.name));
    test("var x = [1,2,3]; x.<caret>", Functions.getAllMethods(JactlType.LIST).stream().map(f -> f.name));
    testIncludes("Map x; x.<caret>", "size", "map", "toString");
    test("Map x; x.<caret>", Functions.getAllMethods(JactlType.MAP).stream().map(f -> f.name));
    test("var x = [a:1]; x.<caret>", Functions.getAllMethods(JactlType.MAP).stream().map(f -> f.name));
    test("def x; x.<caret>", "className", "toJson", "toString");
  }

  public void testFinalType() {
    testIncludes("def x = [1,2,3]; x.<caret>", "size", "map");
    testIncludes("def x = 1; x = [1,2,3]; x.<caret>", "size", "map");
  }

  public void testCastAndAs() {
    test("class X{}; def x; x as <caret>", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def x; (<caret>", classesWithFromJson("TopLevel", "X"), Stream.of("TopLevel.staticFunc", "x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream());
    test("class X{}; def x; x = (<caret>", classesWithFromJson("TopLevel", "X"), Stream.of("TopLevel.staticFunc", "x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream());
    test("class X{}; def x; x = 3 + (<caret>", classesWithFromJson("TopLevel", "X"), Stream.of("TopLevel.staticFunc", "x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream());
    test("class X{}; def x; x = 3 * (2 + (<caret>", classesWithFromJson("TopLevel", "X"), Stream.of("TopLevel.staticFunc", "x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream());
    test("class X{}; def x; (<caret>)", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def x; (<caret>)x", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def x; if (true) { x++; x as <caret>", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def x; if (true) { x++; (<caret>", classesWithFromJson("TopLevel", "X"), Stream.of("TopLevel.staticFunc", "x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream());
    test("class X{}; def x; if (true) { x++; x = (<caret>", classesWithFromJson("TopLevel", "X"), Stream.of("TopLevel.staticFunc", "x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream());
    test("class X{}; def x; if (true) { x++; x = 3 + (<caret>", classesWithFromJson("TopLevel", "X"), Stream.of("TopLevel.staticFunc", "x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream());
    test("class X{}; def x; if (true) { x++; x = true ? 3 * (2 + (<caret>", classesWithFromJson("TopLevel", "X"), Stream.of("TopLevel.staticFunc", "x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream());
    test("class X{}; def x; if (true) { x++; x = true ? 4 : 3 * (2 + (<caret>", classesWithFromJson("TopLevel", "X"), Stream.of("TopLevel.staticFunc", "x"), Stream.of(JactlUtils.BUILTIN_TYPES), Functions.getGlobalFunctionNames().stream());
    test("class X{}; def x; if (true) { x++; (<caret>)", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));
    test("class X{}; def x; if (true) { x++; (<caret>)x", Stream.of("TopLevel", "X"), Stream.of(JactlUtils.BUILTIN_TYPES));

    testInOrgTest("class X{}; def x; x as <caret>", Stream.of("X", "XYZ", "AClass"), Stream.of(JactlUtils.BUILTIN_TYPES));
  }
}
