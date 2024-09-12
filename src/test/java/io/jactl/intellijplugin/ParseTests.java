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

import com.intellij.lexer.Lexer;
//import com.intellij.testFramework.LexerTestCase;
import org.jetbrains.annotations.NotNull;
//import org.junit.Test;

public class ParseTests /*extends LexerTestCase*/ {
  //@Override
  protected @NotNull Lexer createLexer() {
    return new JactlParserDefinition().createLexer(null);
  }

  //@Override
  protected @NotNull String getDirPath() {
    return null;
  }

  //@Test
  public void testSimple() {
//    doTest("int i",
//           "JactlTokenType.INT ('int')\n" +
//           "JactlTokenType.WHITESPACE (' ')\n" +
//           "JactlTokenType.IDENTIFIER ('i')");
  }

//  @Test
  public void testExpressionString() {
//    doTest("println \"x=${x\"\n",
//           "JactlTokenType.PRINTLN ('println')\n" +
//           "JactlTokenType.WHITESPACE (' ')\n" +
//           "JactlTokenType.EXPR_STRING_START ('\"x=')\n" +
//           "JactlTokenType.DOLLAR_BRACE ('${')\n" +
//           "JactlTokenType.IDENTIFIER ('x')\n" +
//           "BAD_CHARACTER ('\"')\n" +
//           "BAD_CHARACTER ('\\n')\n" +
//           "JactlTokenType.WHITESPACE ('\\n')");
  }
}
