package io.jactl.intellijplugin;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    reformat();
    verify("def f() {\n    \n}");
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
    verify("def f = {\n    long x,\n    \n}");
    performTyping("long y,\ndef z ->\n");
    verify("def f = {\n    long x,\n    long y,\n    def z ->\n    \n}");
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
    verify(fixed + "f({\n    long x,\n    \n})");
    performTyping("long y,\ndef z ->\n");
    verify(fixed + "f({\n    long x,\n    long y,\n    def z ->\n    \n})");
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
    verify(fixed + "f{\n    long x,\n    \n}");
    performTyping("long y,\ndef z ->\n");
    verify(fixed + "f{\n    long x,\n    long y,\n    def z ->\n    \n}");
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
      """
        package org.test.sub

        class AClass {
            class BBB {
                const CCC = 1
                def fff(int xx = 1) { xx }
            }
            class C { def f() { def x = new BBB() } }
            //  int xxx
            //  static def func(int i) { 'func'+i }
            //  const VALUE = 2
            def ppp(x) {
                x++
                println x
                return x
            }
            def f() {
                ppp(2)
            }
        }""";

    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    verify(text);
  }

  public void testForWhileIf() {
    String text =
      """
      def ppp(it, x) {
          x++
          for (def i = 0; i < xx; i++) {
              /abc.*([^ ]*)/ and x--
              if (x < 10) {
                  x += 10  /* comment */
              }
          }
          while (true) {
              x += 10 if x > 100
              x -= 20 if /abc/in
              it =~ /xyz/ or x++
          }
          println x
          return x
      }""";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    verify(text);
  }

  public void testDoWhile() {
    String text =
      """
        def f() {
            do {
                for (int j = 0;;) {
                    break if j == 0
                }
            } until (x < 5)
        }""";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    verify(text);
  }

  public void testSwitchStmt() {
    String text =
      """
        def f() {
            switch ('abc') {
                /abc/i  -> true
                [_,_,_] -> false
                default -> true
            }
        }""";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    verify(text);
  }
}
