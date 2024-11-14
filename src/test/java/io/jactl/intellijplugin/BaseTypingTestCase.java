package io.jactl.intellijplugin;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import io.jactl.Utils;
import io.jactl.runtime.RuntimeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.intellij.testFramework.EditorTestUtil.BACKSPACE_FAKE_CHAR;

public abstract class BaseTypingTestCase extends BasePlatformTestCase {
  @Override
  protected String getTestDataPath() {
    return "src/test";
  }

  private @NotNull String getText() {
    return myFixture.getEditor().getDocument().getText();
  }

  protected void setUp() throws Exception {
    super.setUp();
    System.out.println(getTestName(true));
  }

  protected void performTypingNoSpaces(String text) {
    String stripped = RuntimeUtils.lines(text).stream().map(String::trim).collect(Collectors.joining("\n"));
    if (text.endsWith("\n")) {
      stripped += "\n";
    }
    _performTyping(stripped, true);
  }

  protected void performTyping(String text) {
    _performTyping(text, false);
  }

  private void _performTyping(String text, boolean smartNewLines) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      // Assume that brace pairs are automatically generated when typing '{'
      // so make sure that we don't introduce extraneous '}' by skipping them
      if (smartNewLines && c == '\n' && i + 1 < text.length() && text.charAt(i + 1) == '}') {
        i++;   // ensure '}' is skipped
        // Turn newline into move cursor one line down
        myFixture.getEditor().getCaretModel().moveCaretRelatively(0, 1, false, false, false);
      }
      else {
        EditorTestUtil.performTypingAction(myFixture.getEditor(), (char) c);
      }
    }
  }

  protected void backspace(int count) {
    IntStream.range(0, count).forEach(i -> EditorTestUtil.performTypingAction(myFixture.getEditor(), BACKSPACE_FAKE_CHAR));
  }

  protected void move(int amount) {
    int currentPos = myFixture.getEditor().getCaretModel().getOffset();
    myFixture.getEditor().getCaretModel().moveToOffset(currentPos + amount);
  }

  protected void reformat() {
    WriteCommandAction.writeCommandAction(getProject()).run(
      () -> CodeStyleManager.getInstance(getProject()).reformatText(myFixture.getFile(), Utils.listOf(myFixture.getFile().getTextRange()))
    );
  }

  protected void verify(String expected) {
    assertEquals(expected, getText());
  }
}
