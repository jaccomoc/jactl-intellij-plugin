package io.jactl.intellijplugin;

import com.github.weisj.jsvg.a;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.intellij.testFramework.EditorTestUtil.BACKSPACE_FAKE_CHAR;

public class IndentFormatTests extends BaseTypingTestCase {

  public void testIndent() {
    String text = "def f() <caret>";
    myFixture.configureByText("script.jactl", text);
    performTyping("{");
    verify("def f() {}");
    assertEquals(9, myFixture.getEditor().getCaretModel().getOffset());
    performTyping("\n");
    verify("def f() {\n    \n}");
    performTyping("3\n");
    reformat();
    verify("def f() {\n    3\n    \n}");
  }

  public void testBlock() {
    myFixture.configureByText("script.jactl", "");
    performTyping("{");
    verify("{}");
    performTyping("\n");
    verify("{\n    \n}");
    performTyping("3\n4\n5");
    reformat();
    verify("{\n    3\n    4\n    5\n}");
  }

  public void testSimpleFunDeclBraceReformat() {
    String text = "def f() {<caret>";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n");
    verify("def f() {\n    \n}");
    performTyping("x++\n");
    verify("def f() {\n    x++\n    \n}");
    performTyping("    x");
    verify("def f() {\n    x++\n        x\n}");
    move(2);
    backspace(1);
    verify("def f() {\n    x++\n        x\n");
    performTyping("}");
    verify("def f() {\n    x++\n    x\n}");
    reformat();
    verify("def f() {\n    x++\n    x\n}");
  }

  public void testSimpleFunDeclBraceReformat2() {
    String text = "def f() {<caret>}";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n");
    verify("def f() {\n    \n}");
    performTyping("x++\n");
    verify("def f() {\n    x++\n    \n}");
    performTyping("    x");
    verify("def f() {\n    x++\n        x\n}");
    move(2);
    backspace(1);
    verify("def f() {\n    x++\n        x\n");
    performTyping("}");
    verify("def f() {\n    x++\n    x\n}");
    reformat();
    verify("def f() {\n    x++\n    x\n}");
  }

  public void testFunDeclWithParams() {
    String text = "def f(int x,<caret>)";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n");
    verify("def f(int x,\n      )");
    performTyping("long y,\n");
    performTyping("def z) {\n");
    verify("def f(int x,\n      long y,\n      def z) {\n    \n}");
    reformat();
    verify("def f(int x,\n      long y,\n      def z) {\n    \n}");
  }

  public void testFunDeclWithParamsReformat() {
    String text = "def f(int x,<caret>)";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n");
    verify("def f(int x,\n      )");
    performTyping("   long y,\n");
    performTyping("def z) {\n");
    verify("def f(int x,\n         long y,\n      def z) {\n    \n}");
    performTyping("    x++\n    y++");
    verify("def f(int x,\n         long y,\n      def z) {\n        x++\n        y++\n}");
    reformat();
    verify("def f(int x,\n      long y,\n      def z) {\n    x++\n    y++\n}");
  }

  public void testFunctionCallArgs() {
    String prefix = "def f(x,y,z) {};\n";
    String text   = "f(123,<caret>";
    myFixture.configureByText("script.jactl", prefix + text);
    performTyping("\n");
    verify(prefix + "f(123,\n  ");
    performTyping("456,\n");
    verify(prefix + "f(123,\n  456,\n  ");
    reformat();
    verify(prefix + "f(123,\n  456,\n  ");
  }

  public void testFunctionCallArgsWithParen() {
    String prefix = "def f(x,y,z) {};\n";
    String text   = "f(123,<caret>)";
    myFixture.configureByText("script.jactl", prefix + text);
    performTyping("\n");
    verify(prefix + "f(123,\n  )");
    performTyping("456,\n");
    verify(prefix + "f(123,\n  456,\n  )");
    performTyping("789");
    verify(prefix + "f(123,\n  456,\n  789)");
    reformat();
    verify(prefix + "f(123,\n  456,\n  789)");
  }

  public void testFunctionCallNamedArgs() {
    String prefix = "def fffff(x,y,z) {};\n\n";
    String text   = "fffff(x:123,<caret>";
    myFixture.configureByText("script.jactl", prefix + text);
    performTyping("\n");
    verify(prefix + "fffff(x:123,\n      ");
    performTyping("y:456,\n");
    verify(prefix + "fffff(x:123,\n      y:456,\n      ");
    performTyping("z:789)\n");
    verify(prefix + "fffff(x:123,\n      y:456,\n      z:789)\n");
    reformat();
    verify(prefix + "fffff(x:123,\n      y:456,\n      z:789)\n");
  }

  public void testFunctionCallNamedArgsWithParen() {
    String prefix = "def fffff(x,y,z) {};\n";
    String text   = "fffff(x:123,<caret>)";
    myFixture.configureByText("script.jactl", prefix + text);
    performTyping("\n");
    verify(prefix + "fffff(x:123,\n      )");
    performTyping("y:456,\n");
    verify(prefix + "fffff(x:123,\n      y:456,\n      )");
    performTyping("z:789");
    verify(prefix + "fffff(x:123,\n      y:456,\n      z:789)");
    reformat();
    verify(prefix + "fffff(x:123,\n      y:456,\n      z:789)");
  }

  public void testSimpleClosureBraceReformat() {
    String text = "def f = {<caret>";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n");
    verify("def f = {\n    \n}");
    performTyping("x++\n");
    verify("def f = {\n    x++\n    \n}");
    performTyping("    x");
    verify("def f = {\n    x++\n        x\n}");
    move(2);
    backspace(1);
    verify("def f = {\n    x++\n        x\n");
    performTyping("}");
    verify("def f = {\n    x++\n    x\n}");
    reformat();
    verify("def f = {\n    x++\n    x\n}");
  }

  public void testClosureWithParams() {
    String text = "def f = {<caret>}";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n");
    verify("def f = {\n    \n}");
    performTyping("long x,");
    performTyping("\n");
    verify("def f = {\n    long x,\n         \n}");
    performTyping("long y,\ndef z ->\n");
    verify("def f = {\n    long x,\n         long y,\n    def z ->\n    \n}");
    reformat();
    verify("def f = {\n    long x,\n    long y,\n    def z ->\n    \n}");
  }

  public void testClosureWithParamsAsArg() {
    String fixed = "def f(def x) { x() }; ";
    String text = fixed + "f({<caret>})";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n");
    verify(fixed + "f({\n    \n})");
    performTyping("long x,");
    performTyping("\n");
    verify(fixed + "f({\n    long x,\n         \n})");
    performTyping("long y,\ndef z ->\n");
    verify(fixed + "f({\n    long x,\n         long y,\n    def z ->\n    \n})");
    reformat();
    verify(fixed + "f({\n    long x,\n    long y,\n    def z ->\n    \n})");
  }

  public void testClosureWithParamsAsArgNoParens() {
    String fixed = "def f(def x) { x() }; ";
    String text = fixed + "f{<caret>}";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n");
    verify(fixed + "f{\n    \n}");
    performTyping("long x,");
    performTyping("\n");
    verify(fixed + "f{\n    long x,\n         \n}");
    performTyping("long y,\ndef z ->\n");
    verify(fixed + "f{\n    long x,\n         long y,\n    def z ->\n    \n}");
    reformat();
    verify(fixed + "f{\n    long x,\n    long y,\n    def z ->\n    \n}");
  }

  public void testForLoop() {
    String text = "for (int i = 0;<caret>)";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n");
    verify("for (int i = 0;\n     )");
    performTyping("i < 10;\n");
    verify("for (int i = 0;\n     i < 10;\n     )");
    performTyping("i++) {\n");
    verify("for (int i = 0;\n     i < 10;\n     i++) {\n    \n}");
    performTyping("i += 2");
    verify("for (int i = 0;\n     i < 10;\n     i++) {\n    i += 2\n}");
    reformat();
    verify("for (int i = 0;\n     i < 10;\n     i++) {\n    i += 2\n}");
  }

  public void testForLoop2() {
    String text = "for (i = 0;<caret>)";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n");
    verify("for (i = 0;\n     )");
    performTyping("i < 10;\n");
    verify("for (i = 0;\n     i < 10;\n     )");
    performTyping("i++) {\n");
    verify("for (i = 0;\n     i < 10;\n     i++) {\n    \n}");
    performTyping("i += 2");
    verify("for (i = 0;\n     i < 10;\n     i++) {\n    i += 2\n}");
    reformat();
    verify("for (i = 0;\n     i < 10;\n     i++) {\n    i += 2\n}");
  }

  public void testListLiteral() {
    String text = "[<caret>]";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n    1,\n");
    verify("[\n    1,\n    ]");
    performTyping("2\n");
    verify("[\n    1,\n    2\n]");
  }

  public void testListLiteral2() {
    String text = "   [<caret>\n]";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n1,\n");
    verify("   [\n       1,\n       \n]");
    performTyping("2");
    verify("   [\n       1,\n       2\n]");
  }

  public void testListLiteral3() {
    String text = "[<caret>";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n1,\n");
    verify("[\n    1,\n    ");
    performTyping("2\n]");
    verify("[\n    1,\n    2\n    ]");
    reformat();
    verify("[\n    1,\n    2\n]");
  }

  public void testListLiteralsAlignment() {
    String text = "[<caret>]";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n   1,\n");
    verify("[\n   1,\n   ]");
    performTyping("2\n");
    verify("[\n   1,\n   2\n]");
  }

  public void testMapLiteral() {
    String text = "[<caret>]";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n    a:1,\n");
    verify("[\n    a:1,\n    ]");
    performTyping("b:b:2\n");
    verify("[\n    a:1,\n    b:b:2\n]");
  }

  public void testMapLiteral2() {
    String text = "   [<caret>\n]";
    myFixture.configureByText("script.jactl", text);
    performTyping("\na:1,\n");
    verify("   [\n       a:1,\n       \n]");
    performTyping("b:2");
    verify("   [\n       a:1,\n       b:2\n]");
  }

  public void testMapLiteral3() {
    String text = "[<caret>";
    myFixture.configureByText("script.jactl", text);
    performTyping("\na:1,\n");
    verify("[\n    a:1,\n    ");
    performTyping("b:2\n]");
    verify("[\n    a:1,\n    b:2\n    ]");
    reformat();
    verify("[\n    a:1,\n    b:2\n]");
  }

  public void testMapLiteralsAlignment() {
    String text = "[<caret>]";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n   a:1,\n");
    verify("[\n   a:1,\n   ]");
    performTyping("b:2\n");
    verify("[\n   a:1,\n   b:2\n]");
  }

  public void testClass() {
    String text = "package a.b.c\nclass X {<caret>}";
    myFixture.configureByText("script.jactl", text);
    performTyping("\n");
    verify("package a.b.c\nclass X {\n    \n}");
    performTyping("Decimal i,\nj = 2\ndef fff(long x,\nint j) {\n");
    verify("package a.b.c\nclass X {\n    Decimal i,\n            j = 2\n    def fff(long x,\n            int j) {\n        \n    }\n}");
    reformat();
    verify("package a.b.c\nclass X {\n    Decimal i,\n            j = 2\n    def fff(long x,\n            int j) {\n        \n    }\n}");
  }

  public void testInnerClass() {
    String text = "package a.b.c\nclass X {\n    <caret>\n}";
    myFixture.configureByText("script.jactl", text);
    performTyping("class Y {");
    performTyping("\n");
    verify("package a.b.c\nclass X {\n    class Y {\n        \n    }\n}");
    performTyping("Decimal i,\nj = 2\ndef fff(long x,\nint j) {");
    verify("package a.b.c\nclass X {\n    class Y {\n        Decimal i,\n                j = 2\n        def fff(long x,\n                int j) {}\n    }\n}");
  }

  public void testClass2() {
    String text =
      "package org.test.sub\n" +
      "\n" +
      "class AClass {\n" +
      "    class BBB {\n" +
      "        const CCC = 1\n" +
      "        def fff(int xx = 1) { xx }\n" +
      "    }\n" +
      "    class C { def f() { def x = new BBB() } }\n" +
      "    //  int xxx\n" +
      "    //  static def func(int i) { 'func'+i }\n" +
      "    //  const VALUE = 2\n" +
      "    def ppp(x) {\n" +
      "        x++\n" +
      "        println x\n" +
      "        return x\n" +
      "    }\n" +
      "    def f() {\n" +
      "        ppp(2)\n" +
      "    }\n" +
      "}";

    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    verify(text);
  }

  public void testForWhileIf() {
    String text =
      "def ppp(it, x) {\n" +
      "    x++\n" +
      "    for (def i = 0; i < xx; i++) {\n" +
      "        /abc.*([^ ]*)/ and x--\n" +
      "        if (x < 10) {\n" +
      "            x += 10  /* comment */\n" +
      "        }\n" +
      "    }\n" +
      "    while (true) {\n" +
      "        x += 10 if x > 100\n" +
      "        x -= 20 if /abc/in\n" +
      "        it =~ /xyz/ or x++\n" +
      "    }\n" +
      "    println x\n" +
      "    return x\n" +
      "}";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    verify(text);
  }

  public void testDoWhile() {
    String text =
      "def f() {\n" +
      "    do {\n" +
      "        for (int j = 0;;) {\n" +
      "            break if j == 0\n" +
      "        }\n" +
      "    } until (x < 5)\n" +
      "}";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    verify(text);
  }

  public void testSwitchStmt() {
    String text =
      "def f() {\n" +
      "    switch ('abc') {\n" +
      "        /abc/i  -> true\n" +
      "        [_,_,_] -> false\n" +
      "        default -> true\n" +
      "    }\n" +
      "}";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    verify(text);
  }

  public void testBinaryExpressions() {
    String text =
      "def x = a.b\n" +
      "         .c\n" +
      "         .d\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }

  public void testBinaryExpressions2() {
    String text =
      "def x = a + b\n" +
      "          + c\n" +
      "          + d\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }

  public void testBinaryExpressions3() {
    String text =
      "(a + b)\n" +
      "   + c\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }

  public void testBinaryExpressions4() {
    String text =
      "Decimal xxxx = 1 +\n" +
      "               2 +\n" +
      "               3\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }

  public void testBinaryExpressions5() {
    String text =
      "x = 1 +\n" +
      "    2 +\n" +
      "    3\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }

  public void testBinaryExpressions6() {
    String text =
      "x = 1 + 2\n" +
      "      + 3\n" +
      "      + 4\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }

  public void testBinaryExpressions6a() {
    String text =
      "xxx = 1 + 2<caret>\n" +
      "        + 4\n";
    myFixture.configureByText("script.jactl", text);
    performTypingNoSpaces("\n+ 3");
    EditorTestUtil.executeAction(myFixture.getEditor(), IdeActions.ACTION_EDITOR_EMACS_TAB);
    String expected = "xxx = 1 + 2\n" +
                      "        + 3\n" +
                      "        + 4\n";
    verify(expected);
    reformat();
    verify(expected);
  }

  public void testBinaryExpressions7() {
    String text =
      "def x = (a+b) + b\n" +
      "          + (c+d)\n" +
      "          + d\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }

  public void testChainedCalls() {
    String text =
      "def x = abc.method1()\n" +
      "           .filter()\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }

  public void testChainedCalls2() {
    String text =
      "def x = abc.method1()\n" +
      "           .filter()\n" +
      "           .size()\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }

  public void testChainedCalls2a() {
    String text =
      "def xxxxxx = abc.method1()<caret>";
    myFixture.configureByText("script.jactl", text);
    performTypingNoSpaces("\n.filter()");
    EditorTestUtil.executeAction(myFixture.getEditor(), IdeActions.ACTION_EDITOR_EMACS_TAB);
    String expected =
      "def xxxxxx = abc.method1()\n" +
      "                .filter()";
    reformat();
    verify(expected);
  }

  public void testChainedCalls2b() {
    String text =
      "def x = abc.method1()<caret>\n" +
      "           .filter()\n";
    myFixture.configureByText("script.jactl", text);
    performTypingNoSpaces("\n.map()");
    EditorTestUtil.executeAction(myFixture.getEditor(), IdeActions.ACTION_EDITOR_EMACS_TAB);
    String expected =
      "def x = abc.method1()\n" +
      "           .map()\n" +
      "           .filter()\n";
    reformat();
    verify(expected);
  }

  public void testChainedCalls2c() {
    String text =
      "abc.method1()<caret>\n" +
      "   .filter()\n";
    myFixture.configureByText("script.jactl", text);
    performTypingNoSpaces("\n.map()");
    EditorTestUtil.executeAction(myFixture.getEditor(), IdeActions.ACTION_EDITOR_EMACS_TAB);
    String expected =
      "abc.method1()\n" +
      "   .map()\n" +
      "   .filter()\n";
    reformat();
    verify(expected);
  }

  public void testChainedCalls3() {
    String text =
      "def x = [1,2,3].map{ it + 1 }\n" +
      "               .filter{ it != 2 }\n" +
      "               .size()\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }

  public void testChainedCalls4() {
    String text =
      "def x = [1,2,3].map{ " +
      "                 it + 1" +
      "               }\n" +
      "               .filter{ it != 2 }\n" +
      "               .size()\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }

  public void testChainedCalls5() {
    String text =
      "[1,2,3].map{ it + 1 }<caret>";
    myFixture.configureByText("script.jactl", text);
    // Note: for some reason without the leading space the auto-indent won't work
    performTyping("\n .filter{ it != 2 }");
    EditorTestUtil.executeAction(myFixture.getEditor(), IdeActions.ACTION_EDITOR_EMACS_TAB);
    verify("[1,2,3].map{ it + 1 }\n" +
           "       .filter{ it != 2 }");
    performTyping("\n .map{ it }");
    EditorTestUtil.executeAction(myFixture.getEditor(), IdeActions.ACTION_EDITOR_EMACS_TAB);
    String expected = "[1,2,3].map{ it + 1 }\n" +
                      "       .filter{ it != 2 }\n" +
                      "       .map{ it }";
    verify(expected);
    reformat();
    verify(expected);
  }

  public void testTernary() {
    String text =
      "def x = true ? 1 + 2\n" +
      "             : 3\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }

  public void testTernary2() {
    String text =
      "def x = 1 +\n" +
      "        true ? 1 +\n" +
      "               2\n" +
      "             : 3 + 2\n" +
      "                 + 1\n";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    reformat();
    verify(text);
  }
}
