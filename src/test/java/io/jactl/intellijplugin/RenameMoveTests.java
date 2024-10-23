package io.jactl.intellijplugin;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import io.jactl.intellijplugin.extensions.JactlMoveFileHandler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

public class RenameMoveTests extends BasePlatformTestCase {
  @Override
  protected String getTestDataPath() {
    return "src/test";
  }

  protected void setUp() throws Exception {
    super.setUp();
    System.out.println(getTestName(true));
    String testData = "";
    myFixture.copyDirectoryToProject("completionTests", testData);
  }

  private String getFileContent(String filePath) {
    String      path        = filePath.replace(File.separatorChar, '/');
    VirtualFile virtualFile = myFixture.getTempDirFixture().getFile(path);
    assertNotNull("Could not file file " + path, virtualFile);
    PsiFile psiFile = myFixture.getPsiManager().findFile(virtualFile);
    assertNotNull(virtualFile.getPath(), psiFile);
    return psiFile.getText();
  }

  private void test(String text) {
    singleFileTest(text, "fff", "ggg");
  }

  private void singleFileTest(String text, String from, String to) {
    myFixture.configureByText("script.jactl", text);
    myFixture.renameElementAtCaret(to);
    String expected    = text.replaceAll("<caret>","").replaceAll(from,to);
    String currentText = myFixture.getEditor().getDocument().getText();
    //System.out.println("New text: " + currentText);
    assertEquals(expected, currentText);
  }

  private void verifyFile(String file, String text, String from, String to) {
    verifyFile(file, text.replaceAll("<caret>","").replaceAll(from,to));
  }

  private void verifyFile(String file, String expectedText) {
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    assertEquals("File: " + file, expectedText, getFileContent(file));
  }

  private static @NotNull String renamedText(String originalText) {
    return renamedText(originalText, "fff", "ggg");
  }

  private static String renamedText(String text, String from, String to) {
    return text.replaceAll("<caret>", "").replaceAll(from,to);
  }

  private void moveDirectory(@NotNull final String dirPath, @NotNull final String toPath) {
    WriteCommandAction.writeCommandAction(getProject()).run(() -> {
      VirtualFile  virtualDir    = myFixture.findFileInTempDir(dirPath);
      PsiDirectory directory     = getPsiManager().findDirectory(virtualDir);
      VirtualFile virtualDestDir = myFixture.findFileInTempDir(toPath);
      PsiDirectory destDir       = getPsiManager().findDirectory(virtualDestDir);
      new MoveFilesOrDirectoriesProcessor(getProject(), new PsiElement[]{directory}, destDir, false, true, null, null).run();
    });
  }

  ///////////////////////////////////////////////////

  public void testFunction() {
    String text = "def f<caret>ff() {}; fff(); fff(); \"${fff()}\"";
    singleFileTest(text, "fff", "ggg");
  }

  public void testFunction2() {
    singleFileTest("def fff() {}; f<caret>ff(); fff();", "fff", "ggg");
  }

  public void testFunction3() {
    String scriptText = "def f<caret>ff(){}; fff();";
    myFixture.addFileToProject("script.jactl", scriptText);
    String script2Text = "fff()";
    myFixture.addFileToProject("script2.jactl", script2Text);  // Invalid since fff is local
    myFixture.configureByFiles("script.jactl");
    myFixture.renameElementAtCaret("ggg");
    verifyFile("script.jactl", renamedText(scriptText));
    verifyFile("script2.jactl", script2Text);   // script2 should not be renamed since fff() is not global
  }

  public void testFunction4() {
    String scriptText = "def f<caret>ff(){}; fff(); int x = { -> def fff() {3}; fff() }()";
    myFixture.addFileToProject("script.jactl", scriptText);
    myFixture.configureByFiles("script.jactl");
    myFixture.renameElementAtCaret("ggg");
    String expected = "def ggg(){}; ggg(); int x = { -> def fff() {3}; fff() }()";
    verifyFile("script.jactl", expected);
  }

  public void testVariable() {
    String text = "def x<caret>x = 1; fff(2); def fff(y) { xx + y + \"${xx}\".size() }; fff(1) + xx; fff(xx);";
    myFixture.addFileToProject("script.jactl", text);
    myFixture.configureByFiles("script.jactl");
    myFixture.renameElementAtCaret("yyy");
    verifyFile("script.jactl", text, "xx", "yyy");
  }

  public void testVariable2() {
    singleFileTest("def abc = 2, x<caret>x = 1; fff(2); def fff(y) { xx + y + \"${xx}\".size() }; fff(1) + xx; fff(xx);", "xx", "yyyyy");
  }

  public void testParameter() {
    singleFileTest("def fff(int x<caret>x, int yy) { fff(2); xx + yy + \"${xx}\".size(); xx; def g() { fff(xx); } }", "xx", "yyy");
  }

  public void testClass() {
    String scriptText = "package a.b; class CC<caret>C{ static fff() {} }";
    myFixture.addFileToProject("a/b/CCC.jactl", scriptText);
    String[] fileContents = new String[] {
      "a/b/script.jactl",  "package a.b; new CCC(); def f() { CCC c; c.fff() }",
      "script2.jactl",     "new a.b.CCC(); \"${new a.b.CCC()}\"",
      "script3.jactl",     "import a.b.CCC; new CCC(); \"${new CCC()}\"",
      "a/script4.jactl",   "package a; new a.b.CCC().fff()",
      "a/b/script5.jactl", "package a.b; new a.b.CCC().fff()"
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.configureByFiles("a/b/CCC.jactl");
    myFixture.renameElementAtCaret("ABC");
    verifyFile("a/b/ABC.jactl", renamedText(scriptText, "CCC", "ABC"));
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      verifyFile(iter.next(), renamedText(iter.next(), "CCC", "ABC"));
    }
  }

  public void testMoveClass() {
    String scriptText = "/*abc*/ package /*abc*/a/*abc*/  ./*abc*/ b /*abc*/ ; class CCC{ static fff() {} }";
    myFixture.addFileToProject("a/b/CCC.jactl", scriptText);
    String[] fileContents = new String[] {
      "a/xyz/script6.jactl", "def f(){}"
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.moveFile("a/b/CCC.jactl", "a/xyz");
    String expected = "/*abc*/ package /*abc*/a/*abc*/  ./*abc*/ xyz /*abc*/ ; class CCC{ static fff() {} }";
    verifyFile("a/xyz/CCC.jactl", expected);
  }

  public void testMoveClass2() {
    String scriptText = "/*abc*/ package /*abc*/a/*abc*/  ./*abc*/ b /*abc*/ ; class CCC{ static fff() {} }";
    myFixture.addFileToProject("a/b/CCC.jactl", scriptText);
    String[] fileContents = new String[] {
      "a/script6.jactl", "def f(){}"
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.moveFile("a/b/CCC.jactl", "a");
    String expected = "/*abc*/ package /*abc*/a/*abc*/  /*abc*/  /*abc*/ ; class CCC{ static fff() {} }";
    verifyFile("a/CCC.jactl", expected);
  }

  public void testMoveClass3() {
    String scriptText = "/*abc*/ package /*abc*/a/*abc*/  ./*abc*/ b /*abc*/ ; class CCC{ static fff() {} }";
    myFixture.addFileToProject("a/b/CCC.jactl", scriptText);
    String[] fileContents = new String[] {
      "a/b/c/d/script6.jactl", "def f(){}"
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.moveFile("a/b/CCC.jactl", "a/b/c/d");
    String expected = "/*abc*/ package /*abc*/a/*abc*/  ./*abc*/ b.c.d /*abc*/ ; class CCC{ static fff() {} }";
    verifyFile("a/b/c/d/CCC.jactl", expected);
  }

  public void testMoveClass4() {
    String scriptText = "/*abc*/ package /*abc*/a/*abc*/  ./*abc*/ b /*abc*/ ; class CCC{ static fff() {} }";
    myFixture.addFileToProject("a/b/CCC.jactl", scriptText);
    String[] fileContents = new String[] {
      "b/c/d/script6.jactl", "def f(){}"
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.moveFile("a/b/CCC.jactl", "b/c/d");
    String expected = "/*abc*/ package /*abc*/b/*abc*/  ./*abc*/ c.d /*abc*/ ; class CCC{ static fff() {} }";
    verifyFile("b/c/d/CCC.jactl", expected);
  }

  public void testMoveClass5() {
    String scriptText = "/*xxx*/ package /*xxx*/ a /*xxx*/ . /*xxx*/ b /*xxx*/; class CCC{ class DDD{ static fff() {} }}";
    myFixture.addFileToProject("a/b/CCC.jactl", scriptText);
    String[] fileContents = new String[] {
      "a/b/script.jactl",    "package a.b; new CCC.DDD(); def f() { CCC c = new CCC() }",
                             "package a.b; new a.xyz.CCC.DDD(); def f() { a.xyz.CCC c = new a.xyz.CCC() }",
      "script2.jactl",       "new /*xxx*/ a /*xxx*/ . /*xxx*/ b /*xxx*/ . /*xxx*/ CCC /*xxx*/ . /*xxx*/ DDD(); \"${new a/*xxx*/. /*xxx*/ b /*xxx*/ . /*xxx*/ CCC()}\"",
                             "new /*xxx*/ a /*xxx*/ . /*xxx*/ xyz /*xxx*/ . /*xxx*/ CCC /*xxx*/ . /*xxx*/ DDD(); \"${new a/*xxx*/. /*xxx*/ xyz /*xxx*/ . /*xxx*/ CCC()}\"",
      "script3.jactl",       "import a.b.CCC; new CCC(); \"${new CCC()}\"", null,
      "a/script4.jactl",     "package a; new a.b.CCC().fff()", null,
      "a/b/script5.jactl",   "package a.b; new a.b.CCC().fff()", null,
      "a/xyz/script6.jactl", "def f(){}", null
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); iter.next()) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.moveFile("a/b/CCC.jactl", "a/xyz");
    verifyFile("a/xyz/CCC.jactl", "/*xxx*/ package /*xxx*/ a /*xxx*/ . /*xxx*/ xyz /*xxx*/; class CCC{ class DDD{ static fff() {} }}");
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      String file = iter.next();
      String original = iter.next();
      String expected = iter.next();
      verifyFile(file, expected == null ? renamedText(original, "a.b.CCC", "a.xyz.CCC") : expected);
    }
  }

  public void testMoveClass6() {
    String scriptText = "/*xxx*/ package /*xxx*/ a /*xxx*/ . /*xxx*/ b /*xxx*/; class CCC{ class DDD{ static fff() {} }}";
    myFixture.addFileToProject("a/b/CCC.jactl", scriptText);
    String[] fileContents = new String[] {
      "a/b/script.jactl",    "package a.b; new CCC.DDD(); def f() { CCC c = new CCC() }",
                             "package a.b; new a.CCC.DDD(); def f() { a.CCC c = new a.CCC() }",
      "script2.jactl",       "new /*xxx*/ a /*xxx*/ . /*xxx*/ b /*xxx*/ . /*xxx*/ CCC /*xxx*/ . /*xxx*/ DDD(); \"${new a/*xxx*/. /*xxx*/ b /*xxx*/ . /*xxx*/ CCC()}\"",
                             "new /*xxx*/ a /*xxx*/  /*xxx*/  /*xxx*/ . /*xxx*/ CCC /*xxx*/ . /*xxx*/ DDD(); \"${new a/*xxx*/ /*xxx*/  /*xxx*/ . /*xxx*/ CCC()}\"",
      "script3.jactl",       "import a.b.CCC; new CCC(); \"${new CCC()}\"", null,
      "a/script4.jactl",     "package a; new a.b.CCC().fff()", null,
      "a/b/script5.jactl",   "package a.b; new a.b.CCC().fff()", null,
      "a/script6.jactl", "def f(){}", null
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); iter.next()) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.moveFile("a/b/CCC.jactl", "a");
    verifyFile("a/CCC.jactl", "/*xxx*/ package /*xxx*/ a /*xxx*/  /*xxx*/  /*xxx*/; class CCC{ class DDD{ static fff() {} }}");
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      String file = iter.next();
      String original = iter.next();
      String expected = iter.next();
      verifyFile(file, expected == null ? renamedText(original, "a.b.CCC", "a.CCC") : expected);
    }
  }

  public void testMoveClass7() {
    String scriptText = "/*xxx*/ package /*xxx*/ a /*xxx*/ . /*xxx*/ b /*xxx*/; class CCC{ class DDD{ static fff() {} }}";
    myFixture.addFileToProject("a/b/CCC.jactl", scriptText);
    String[] fileContents = new String[] {
      "a/b/script.jactl",    "package a.b; new CCC.DDD(); def f() { CCC c = new CCC() }",
      "package a.b; new x.y.z.CCC.DDD(); def f() { x.y.z.CCC c = new x.y.z.CCC() }",
      "script2.jactl",       "new /*xxx*/ a /*xxx*/ . /*xxx*/ b /*xxx*/ . /*xxx*/ CCC /*xxx*/ . /*xxx*/ DDD(); \"${new a/*xxx*/. /*xxx*/ b /*xxx*/ . /*xxx*/ CCC()}\"",
      "new /*xxx*/ x /*xxx*/ . /*xxx*/ y.z /*xxx*/ . /*xxx*/ CCC /*xxx*/ . /*xxx*/ DDD(); \"${new x/*xxx*/. /*xxx*/ y.z /*xxx*/ . /*xxx*/ CCC()}\"",
      "script3.jactl",       "import a.b.CCC; new CCC(); \"${new CCC()}\"", null,
      "a/script4.jactl",     "package a; new a.b.CCC().fff()", null,
      "a/b/script5.jactl",   "package a.b; new a.b.CCC().fff()", null,
      "x/y/z/script6.jactl", "def f(){}", null
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); iter.next()) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.moveFile("a/b/CCC.jactl", "x/y/z");
    verifyFile("x/y/z/CCC.jactl", "/*xxx*/ package /*xxx*/ x /*xxx*/ . /*xxx*/ y.z /*xxx*/; class CCC{ class DDD{ static fff() {} }}");
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      String file = iter.next();
      String original = iter.next();
      String expected = iter.next();
      verifyFile(file, expected == null ? renamedText(original, "a.b.CCC", "x.y.z.CCC") : expected);
    }
  }

  public void testMoveClass8() {
    String scriptText = "/*xxx*/ package /*xxx*/ a /*xxx*/ . /*xxx*/ b /*xxx*/; class CCC{ class DDD{ static fff() {} }}";
    myFixture.addFileToProject("a/b/CCC.jactl", scriptText);
    String[] fileContents = new String[] {
      "a/b/script.jactl",    "package a.b; new CCC.DDD(); def f() { CCC c = new CCC() }",
      "package a.b; new CCC.DDD(); def f() { CCC c = new CCC() }",
      "script2.jactl",       "new /*xxx*/ a /*xxx*/ . /*xxx*/ b /*xxx*/ . /*xxx*/ CCC /*xxx*/ . /*xxx*/ DDD(); \"${new a/*xxx*/. /*xxx*/ b /*xxx*/ . /*xxx*/ CCC()}\"",
      "new /*xxx*/ CCC /*xxx*/ . /*xxx*/ DDD(); \"${new CCC()}\"",
      "script3.jactl",       "import a.b.CCC; new CCC(); \"${new CCC()}\"", null,
      "a/script4.jactl",     "package a; new a.b.CCC().fff()", null,
      "a/b/script5.jactl",   "package a.b; new a.b.CCC().fff()", null,
      "x/y/z/script6.jactl", "def f(){}", null
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); iter.next()) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.moveFile("a/b/CCC.jactl", "");
    verifyFile("CCC.jactl", "/*xxx*/ " + JactlMoveFileHandler.PACKAGE_REMOVAL_COMMENT + " /*xxx*/  /*xxx*/  /*xxx*/  /*xxx*/; class CCC{ class DDD{ static fff() {} }}");
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      String file = iter.next();
      String original = iter.next();
      String expected = iter.next();
      verifyFile(file, expected == null ? renamedText(original, "a.b.CCC", "CCC") : expected);
    }
  }

  public void testMoveClass9() {
    String scriptText = "package a.b; class CCC{ static fff() {} }";
    myFixture.addFileToProject("a/b/CCC.jactl", scriptText);
    String[] fileContents = new String[] {
      "a/b/script.jactl",    "package a.b; new CCC(); def f() { CCC c; c.fff() }",
      "a/xyz/script6.jactl", "def f(){}"
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.moveFile("a/b/CCC.jactl", "a/xyz");
    verifyFile("a/xyz/CCC.jactl", renamedText(scriptText, "a.b", "a.xyz"));
    verifyFile("a/b/script.jactl", renamedText(fileContents[1], "CCC", "a.xyz.CCC"));
  }

  public void testRenamePackage() {
    String  scriptText = "/*xxx*/ package /*xxx*/ a /*xxx*/ . /*xxx*/ b /*xxx*/; class CCC{ class DDD{ static fff() {} }}";
    PsiFile classFile  = myFixture.addFileToProject("a/b/CCC.jactl", scriptText);
    String[] fileContents = new String[] {
      "a/b/script.jactl",    "package a.b; new CCC.DDD(); def f() { CCC c = new CCC() }", null,
      "a/b/DDD.jactl",       "package a.b; class DDD { def f() { CCC c = new CCC() } }", null,
      "script2.jactl",       "new /*xxx*/ a /*xxx*/ . /*xxx*/ b /*xxx*/ . /*xxx*/ CCC /*xxx*/ . /*xxx*/ DDD(); \"${new a/*xxx*/. /*xxx*/ b /*xxx*/ . /*xxx*/ CCC()}\"",
      "new /*xxx*/ a /*xxx*/ . /*xxx*/ xyz /*xxx*/ . /*xxx*/ CCC /*xxx*/ . /*xxx*/ DDD(); \"${new a/*xxx*/. /*xxx*/ xyz /*xxx*/ . /*xxx*/ CCC()}\"",
      "script3.jactl",       "import a.b.CCC; new CCC(); \"${new CCC()}\"", null,
      "a/script4.jactl",     "package a; new a.b.CCC().fff()", null,
      "script5.jactl",       "a.b.CCC.DDD.fff()", null,
      "x/y/z/script6.jactl", "def f(){}", null
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); iter.next()) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    assert classFile.getParent() != null;
    myFixture.renameElement(classFile.getParent(), "xyz");
    verifyFile("a/xyz/CCC.jactl", "/*xxx*/ package /*xxx*/ a /*xxx*/ . /*xxx*/ xyz /*xxx*/; class CCC{ class DDD{ static fff() {} }}");
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      String file = iter.next().replaceAll("a/b", "a/xyz");
      String original = iter.next();
      String expected = iter.next();
      verifyFile(file, expected == null ? renamedText(original, "a.b", "a.xyz") : expected);
    }
  }

  public void testMovePackage() {
    String[] fileContents = new String[] {
      "a/b/c/CCC.jactl",     "package a.b.c; class CCC{ static def fff() {} }", null,
      "a/b/c/DDD.jactl",     "package a.b.c; class DDD { static def ggg() { a.b.c.CCC.fff() } }", null,
      "a/b/EEE.jactl",       "package a.b; class EEE { static def hhh() { a.b.c.CCC.fff() } }", null,
      "script5.jactl",       "a.b.CCC.fff()", null,
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); iter.next()) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    moveDirectory("a/b/c", "a");
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      String file = iter.next().replaceAll("a/b/c", "a/c");
      String original = iter.next();
      String expected = iter.next();
      verifyFile(file, expected == null ? renamedText(original, "a.b.c", "a.c") : expected);
    }
  }

  public void testMethod() {
    String cccText = "package a.b; class CCC{ def f<caret>ff(){}; def f(){fff()} }";
    myFixture.addFileToProject("a/b/CCC.jactl", cccText);
    String[] fileContents = new String[]{
      "a/b/script.jactl",  "package a.b; CCC c = new CCC(); c.fff()",
      "script2.jactl",     "new a.b.CCC(); \"${new a.b.CCC().fff()}\"",
      "a/script3.jactl",   "package a; a.b.CCC c = new a.b.CCC(); \"${c.fff()}\"",
      "a/b/script4.jactl", "package a.b; new a.b.CCC().fff()",
      "script5.jactl",     "import a.b.CCC; new CCC().fff()"
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.configureByFiles("a/b/CCC.jactl");
    myFixture.renameElementAtCaret("ggg");
    verifyFile("a/b/CCC.jactl", renamedText(cccText, "fff", "ggg"));
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      verifyFile(iter.next(), renamedText(iter.next(), "fff", "ggg"));
    }
  }

  public void testStaticMethod() {
    String cccText = "package a.b; class CCC{ static def f<caret>ff() {}; static def g(){fff()}; def h(){fff()} }";
    myFixture.addFileToProject("a/b/CCC.jactl", cccText);
    String[] fileContents = new String[] {
      "a/b/script.jactl", "package a.b; new CCC().fff()",
      "script2.jactl", "new a.b.CCC(); \"${a.b.CCC.fff()}\"",
      "a/script3.jactl", "package a; a.b.CCC.fff()",
      "a/b/script4.jactl", "package a.b; a.b.CCC.fff()",
      "script5.jactl", "import static a.b.CCC.fff as F; F()"
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.configureByFiles("a/b/CCC.jactl");
    myFixture.renameElementAtCaret("ggg");
    verifyFile("a/b/CCC.jactl", renamedText(cccText, "fff", "ggg"));
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      verifyFile(iter.next(), renamedText(iter.next(), "fff", "ggg"));
    }
  }

  public void testStaticMethodInnerClass() {
    String cccText = "package a.b; class CCC{ class EEE { static def f<caret>ff(){} } }";
    myFixture.addFileToProject("a/b/CCC.jactl", cccText);
    String[] fileContents = new String[]{
      "script.jactl", "import a.b.CCC; CCC.EEE.fff()",
      "script2.jactl", "import a.b.CCC; CCC.EEE eee = new CCC.EEE(); eee.fff()"
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.configureByFiles("a/b/CCC.jactl");
    myFixture.renameElementAtCaret("ggg");
    verifyFile("a/b/CCC.jactl", renamedText(cccText, "fff", "ggg"));
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      verifyFile(iter.next(), renamedText(iter.next(), "fff", "ggg"));
    }
  }

  public void testStaticMethodInnerClass2() {
    String cccText = "package a.b; class CCC{ class DDD { class EEE { static def f<caret>ff(){} } } }";
    String[] fileContents = new String[] {
      "script.jactl", "import a.b.CCC; CCC.DDD.EEE.fff()",
      "script2.jactl", "import a.b.CCC; CCC.DDD.EEE eee = new CCC.DDD.EEE(); eee.fff()"
    };
    myFixture.addFileToProject("a/b/CCC.jactl", cccText);
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.configureByFiles("a/b/CCC.jactl");
    myFixture.renameElementAtCaret("ggg");
    verifyFile("a/b/CCC.jactl", renamedText(cccText, "fff", "ggg"));
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      verifyFile(iter.next(), renamedText(iter.next(), "fff", "ggg"));
    }
  }

  public void testField() {
    String text = "package a.b; class CCC{ def f<caret>ff; def f(){fff} }";
    myFixture.addFileToProject("a/b/CCC.jactl", text);
    myFixture.configureByFiles("a/b/CCC.jactl");
    myFixture.renameElementAtCaret("ggg");
    verifyFile("a/b/CCC.jactl", renamedText(text, "fff", "ggg"));
  }

  public void testFieldList() {
    String text = "package a.b; class CCC{ int ggg=1, f<caret>ff; def f(){fff} }";
    myFixture.addFileToProject("a/b/CCC.jactl", text);
    myFixture.configureByFiles("a/b/CCC.jactl");
    myFixture.renameElementAtCaret("ggg");
    verifyFile("a/b/CCC.jactl", renamedText(text, "fff", "ggg"));
  }

  public void testField2() {
    String text = "package a.b; class CCC{ def f<caret>ff; def f(){fff} }";
    myFixture.addFileToProject("a/b/CCC.jactl", text);
    myFixture.addFileToProject("a/b/script.jactl", "package a.b; CCC c = new CCC(); c.fff");
    myFixture.configureByFiles("a/b/CCC.jactl");
    myFixture.renameElementAtCaret("ggg");
    verifyFile("a/b/CCC.jactl", renamedText(text, "fff", "ggg"));
  }

  public void testField3() {
    String cccText = "package a.b; class CCC{ def f<caret>ff; def f(){fff} }";
    myFixture.addFileToProject("a/b/CCC.jactl", cccText);
    String[] fileContents = new String[]{
      "a/b/script.jactl", "package a.b; CCC c = new CCC(); c.fff",
      "script2.jactl", "new a.b.CCC(); \"${new a.b.CCC().fff}\"",
      "a/script3.jactl", "package a; a.b.CCC c = new a.b.CCC(); \"${c.fff}\"",
      "a/b/script4.jactl", "package a.b; new a.b.CCC().fff",
      "script5.jactl", "import a.b.CCC; new CCC().fff"
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.configureByFiles("a/b/CCC.jactl");
    myFixture.renameElementAtCaret("ggg");
    verifyFile("a/b/CCC.jactl", renamedText(cccText, "fff", "ggg"));
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      verifyFile(iter.next(), renamedText(iter.next(), "fff", "ggg"));
    }
  }

  public void testConst() {
    String cccText = "package a.b; class CCC{ const f<caret>ff = 1; def f(){fff} }";
    myFixture.addFileToProject("a/b/CCC.jactl", cccText);
    String[] fileContents = new String[]{
      "a/b/script.jactl", "package a.b; CCC c = new CCC(); c.fff",
      "script2.jactl", "\"${a.b.CCC.fff}\"",
      "a/b/script3.jactl", "\"${CCC.fff}\"",
      "a/script4.jactl", "package a; a.b.CCC c = new a.b.CCC(); \"${c.fff}\"",
      "a/b/script5.jactl", "package a.b; a.b.CCC.fff",
      "a/b/script6.jactl", "package a.b; CCC.fff",
      "script7.jactl", "import a.b.CCC; CCC.fff"
    };
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      myFixture.addFileToProject(iter.next(), iter.next());
    }
    myFixture.configureByFiles("a/b/CCC.jactl");
    myFixture.renameElementAtCaret("ggg");
    verifyFile("a/b/CCC.jactl", renamedText(cccText, "fff", "ggg"));
    for (Iterator<String> iter = Arrays.stream(fileContents).iterator(); iter.hasNext(); ) {
      verifyFile(iter.next(), renamedText(iter.next(), "fff", "ggg"));
    }
  }
}
