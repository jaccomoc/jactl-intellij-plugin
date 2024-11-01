package io.jactl.intellijplugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.EditorTestUtil;
import io.jactl.Utils;

import java.util.List;

public class TypingTest extends BaseTypingTestCase {

  private void performTypingWithMistakes(String text, char mistakeChar) {
    assertTrue(myFixture.getEditor().getCaretModel().isUpToDate() || !ApplicationManager.getApplication().isDispatchThread());
    text.chars().forEach(c -> {
      enter(c);
      enter(mistakeChar);
      backspace(1);
      if (c == ' ' && mistakeChar == '\n') {
        // Backspace after newline will consume space that was there so re-enter the space
        enter(c);
      }
    });
  }

  private void introduceErrors(List<Character> specialChars) {
    int size = myFixture.getEditor().getDocument().getText().length();
    for (int i = 0; i < size; i++) {
      moveTo(i);
      specialChars.forEach(c -> {
        enter(c);
        backspace(1);
      });
    }
  }

  private void enter(int c) {
    EditorTestUtil.performTypingAction(myFixture.getEditor(), (char)c);
  }

  private void moveTo(int offset) {
    myFixture.getEditor().getCaretModel().moveToOffset(offset);
  }

  // = TESTS

  private static final String CLASS_DECL =
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
    "    def ppp(it, x) {\n" +
    "        x++\n" +
    "        for (def i = 0; i < xx; i++) {\n" +
    "            /abc.*([^ ]*)/ and x--\n" +
    "            if (x < 10) {\n" +
    "                x += 10  /* comment */\n" +
    "            }\n" +
    "        }\n" +
    "        switch (it) {\n" +
    "            [_,_,int] -> true\n" +
    "            default   -> false\n" +
    "        }\n" +
    "        while (true) {\n" +
    "            x += 10 if x > 100\n" +
    "            x -= 20 if /abc/in\n" +
    "            it =~ /xyz/ or x++\n" +
    "        }\n" +
    "        println x\n" +
    "        return x\n" +
    "    }\n" +
    "    def f() {\n" +
    "        ppp(2)\n" +
    "    }\n" +
    "}";

  public void testClassDecl() {
    String text = CLASS_DECL;
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
    verify(text);
    reformat();
    verify(text);
  }

  private void classDeclWithMistakes(char mistakeChar) {
    String text = CLASS_DECL;
    myFixture.configureByText("script.jactl", "");
    // Just want to make sure there are no unexpected exceptions if parse input looks
    // a bit strange
    performTypingWithMistakes(text, mistakeChar);
  }

  //public void testClassDeclWithMistakeRBrace() { classDeclWithMistakes('}'); }
  public void testClassDeclWithMistakeNewLine() { classDeclWithMistakes('\n'); }
  public void testClassDeclWithMistakeRSquare() { classDeclWithMistakes(']'); }
  public void testClassDeclWithMistakeRParen() { classDeclWithMistakes(')'); }
  public void testClassDeclWithMistakeSingleQuote() { classDeclWithMistakes('\''); }
  public void testClassDeclWithMistakeDoubleQuote() { classDeclWithMistakes('"'); }
  public void testClassDeclWithMistakeLetter() { classDeclWithMistakes('a'); }
  public void testClassDeclWithMistakeNumber() { classDeclWithMistakes('1'); }
  public void testClassDeclWithMistakeDot() { classDeclWithMistakes('.'); }
  public void testClassDeclWithMistakeSemicolon() { classDeclWithMistakes(';'); }

  public void testClassDeclWithIntroducedErrors() {
    String text = CLASS_DECL;
    myFixture.configureByText("script.jactl", text);
    // Mutate after text entered to make sure that there are no strange exceptions thrown
    introduceErrors(Utils.listOf('{', '}', '[', ']', '(', ')', '\\', '"', '\'', 'a', '1', '.', ';', '\n'));
  }

  public void testForLoop() {
    String text = "for (i = 0; i < 10; i++) {}";
    myFixture.configureByText("script.jactl", "");
    performTypingNoSpaces(text);
  }
}
