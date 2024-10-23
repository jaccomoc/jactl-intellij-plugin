package io.jactl.intellijplugin;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.CommonProcessors;

public class FindUsageTests extends BasePlatformTestCase {
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

  private int usageCount() {
    final PsiElement resolved = TargetElementUtil.findTargetElement(myFixture.getEditor(),
                                                                    TargetElementUtil.getInstance().getReferenceSearchFlags());
    assertNotNull("Could not resolve reference", resolved);
    FindUsagesManager findUsagesManager = ((FindManagerImpl) FindManager.getInstance(getProject())).getFindUsagesManager();
    FindUsagesHandler handler           = findUsagesManager.getFindUsagesHandler(resolved, false);
    assertNotNull(handler);
    FindUsagesOptions                            options   = handler.getFindUsagesOptions();
    CommonProcessors.CollectProcessor<UsageInfo> processor = new CommonProcessors.CollectProcessor<UsageInfo>();
    for (PsiElement element : handler.getPrimaryElements()) {
      handler.processElementUsages(element, processor, options);
    }
    for (PsiElement element : handler.getSecondaryElements()) {
      handler.processElementUsages(element, processor, options);
    }
    return processor.getResults().size();
  }

  private void test(String text, int usageCount) {
    myFixture.configureByText("script.jactl", text);
    assertEquals(usageCount, usageCount());
  }


  public void testFunction() {
    test("def f<caret>ff() {}; fff(); fff(); \"${fff()}\"", 3);
  }

  public void testFunction2() {
    test("def fff() {}; f<caret>ff(); fff();", 2);
  }

  public void testFunction3() {
    myFixture.addFileToProject("script.jactl", "def f<caret>ff(){}; fff();");
    myFixture.addFileToProject("script2.jactl", "fff()");  // Invalid since fff is local
    myFixture.configureByFiles("script.jactl");
    assertEquals(1, usageCount());
  }

  public void testVariable() {
    test("def x<caret>x = 1; fff(2); def fff(y) { xx + y + \"${xx}\".size() }; fff(1) + xx; fff(xx);", 4);
  }

  public void testVariable2() {
    test("def abc = 2, x<caret>x = 1; fff(2); def fff(y) { xx + y + \"${xx}\".size() }; fff(1) + xx; fff(xx);", 4);
  }

  public void testParameter() {
    test("def fff(int x<caret>x, int yy) { fff(2); xx + yy + \"${xx}\".size(); xx; def g() { fff(xx); } }", 4);
  }

  public void testClass() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CC<caret>C{ static fff() {} }");
    myFixture.addFileToProject("a/b/script.jactl", "package a.b; new CCC()");
    myFixture.addFileToProject("script2.jactl", "new a.b.CCC(); \"${new a.b.CCC()}\"");
    myFixture.addFileToProject("script3.jactl", "import a.b.CCC; new CCC(); \"${new CCC()}\"");
    myFixture.addFileToProject("a/script4.jactl", "package a; new a.b.CCC().fff()");
    myFixture.addFileToProject("a/b/script5.jactl", "package a.b; new a.b.CCC().fff()");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(8, usageCount());
  }

  public void testMethod() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ def f<caret>ff(){}; def f(){fff()} }");
    myFixture.addFileToProject("a/b/script.jactl", "package a.b; CCC c = new CCC(); c.fff()");
    myFixture.addFileToProject("script2.jactl", "new a.b.CCC(); \"${new a.b.CCC().fff()}\"");
    myFixture.addFileToProject("a/script3.jactl", "package a; a.b.CCC c = new a.b.CCC(); \"${c.fff()}\"");
    myFixture.addFileToProject("a/b/script4.jactl", "package a.b; new a.b.CCC().fff()");
    myFixture.addFileToProject("script5.jactl", "import a.b.CCC; new CCC().fff()");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(6, usageCount());
  }

  public void testMethod2() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ def f<caret>ff(){} }");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(0, usageCount());
  }

  public void testMethod3() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ def f<caret>ff(){} }");
    myFixture.addFileToProject("script5.jactl", "import a.b.CCC; new CCC().fff()");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(1, usageCount());
  }

  public void testMethod4() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ def fff() {} }");
    myFixture.addFileToProject("a/b/script.jactl", "package a.b; CCC c = new CCC(); c.f<caret>ff()");
    myFixture.addFileToProject("script2.jactl", "new a.b.CCC(); \"${new a.b.CCC().fff()}\"");
    myFixture.addFileToProject("a/script3.jactl", "package a; a.b.CCC c = new a.b.CCC(); \"${c.fff()}\"");
    myFixture.addFileToProject("a/b/script4.jactl", "package a.b; new a.b.CCC().fff()");
    myFixture.configureByFiles("a/b/script.jactl");
    assertEquals(4, usageCount());
  }

  public void testStaticMethod() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ static def f<caret>ff() {}; static def g(){fff()}; def h(){fff()} }");
    myFixture.addFileToProject("a/b/script.jactl", "package a.b; new CCC().fff()");
    myFixture.addFileToProject("script2.jactl", "new a.b.CCC(); \"${a.b.CCC.fff()}\"");
    myFixture.addFileToProject("a/script3.jactl", "package a; a.b.CCC.fff()");
    myFixture.addFileToProject("a/b/script4.jactl", "package a.b; a.b.CCC.fff()");
    myFixture.addFileToProject("script5.jactl", "import static a.b.CCC.fff as F; F()");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(7, usageCount());
  }

  public void testStaticMethod2() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ static def f<caret>ff() {}; static def g(){fff()} }");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(1, usageCount());
  }

  public void testStaticMethod3() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ static def f<caret>ff() {} }");
    myFixture.addFileToProject("script.jactl", "import static a.b.CCC.fff as F; F()");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(1, usageCount());
  }

  public void testStaticMethod4() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ static def f<caret>ff() {} }");
    myFixture.addFileToProject("script.jactl", "import a.b.CCC; new CCC().fff()");
    myFixture.addFileToProject("script2.jactl", "import a.b.CCC; CCC c = new CCC(); c.fff()");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(2, usageCount());
  }

  public void testStaticMethodInnerClass() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ class EEE { static def f<caret>ff(){} } }");
    myFixture.addFileToProject("script.jactl", "import a.b.CCC; CCC.EEE.fff()");
    myFixture.addFileToProject("script2.jactl", "import a.b.CCC; CCC.EEE eee = new CCC.EEE(); eee.fff()");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(2, usageCount());
  }

  public void testStaticMethodInnerClass2() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ class DDD { class EEE { static def f<caret>ff(){} } } }");
    myFixture.addFileToProject("script.jactl", "import a.b.CCC; CCC.DDD.EEE.fff()");
    myFixture.addFileToProject("script2.jactl", "import a.b.CCC; CCC.DDD.EEE eee = new CCC.DDD.EEE(); eee.fff()");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(2, usageCount());
  }

  public void testField() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ def f<caret>ff; def f(){fff} }");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(1, usageCount());
  }

  public void testFieldList() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ int ggg=1, f<caret>ff; def f(){fff} }");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(1, usageCount());
  }

  public void testField2() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ def f<caret>ff; def f(){fff} }");
    myFixture.addFileToProject("a/b/script.jactl", "package a.b; CCC c = new CCC(); c.fff");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(2, usageCount());
  }

  public void testField3() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ def f<caret>ff; def f(){fff} }");
    myFixture.addFileToProject("a/b/script.jactl", "package a.b; CCC c = new CCC(); c.fff");
    myFixture.addFileToProject("script2.jactl", "new a.b.CCC(); \"${new a.b.CCC().fff}\"");
    myFixture.addFileToProject("a/script3.jactl", "package a; a.b.CCC c = new a.b.CCC(); \"${c.fff}\"");
    myFixture.addFileToProject("a/b/script4.jactl", "package a.b; new a.b.CCC().fff");
    myFixture.addFileToProject("script5.jactl", "import a.b.CCC; new CCC().fff");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(6, usageCount());
  }

  public void testConst() {
    myFixture.addFileToProject("a/b/CCC.jactl", "package a.b; class CCC{ const f<caret>ff = 1; def f(){fff} }");
    myFixture.addFileToProject("a/b/script.jactl", "package a.b; CCC c = new CCC(); c.fff");
    myFixture.addFileToProject("script2.jactl", "\"${a.b.CCC.fff}\"");
    myFixture.addFileToProject("a/b/script3.jactl", "\"${CCC.fff}\"");
    myFixture.addFileToProject("a/script4.jactl", "package a; a.b.CCC c = new a.b.CCC(); \"${c.fff}\"");
    myFixture.addFileToProject("a/b/script5.jactl", "package a.b; a.b.CCC.fff");
    myFixture.addFileToProject("a/b/script6.jactl", "package a.b; CCC.fff");
    myFixture.addFileToProject("script7.jactl", "import a.b.CCC; CCC.fff");
    myFixture.configureByFiles("a/b/CCC.jactl");
    assertEquals(8, usageCount());
  }
}
