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

package io.jactl.intellijplugin.extensions;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import io.jactl.Tokeniser;
import io.jactl.intellijplugin.JactlTokeniser;
import io.jactl.intellijplugin.psi.JactlTokenType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class JactlSyntaxHighLighter extends SyntaxHighlighterBase {

  public static final TextAttributesKey SEPARATOR            = createTextAttributesKey("JACTL_SEPARATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
  public static final TextAttributesKey KEY                  = createTextAttributesKey("JACTL_KEY", DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey NUMBER               = createTextAttributesKey("JACTL_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
  public static final TextAttributesKey STRING               = createTextAttributesKey("JACTL_STRING", DefaultLanguageHighlighterColors.STRING);
  public static final TextAttributesKey IDENTIFIER           = createTextAttributesKey("JACTL_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
  public static final TextAttributesKey FUNCTION_DECLARATION = createTextAttributesKey("JACTL_FUNCTION_DECLARATION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);
  public static final TextAttributesKey VARIABLE_DECLARATION = createTextAttributesKey("JACTL_VARIABLE", DefaultLanguageHighlighterColors.GLOBAL_VARIABLE);
  public static final TextAttributesKey TYPE                 = createTextAttributesKey("JACTL_TYPE", DefaultLanguageHighlighterColors.CLASS_REFERENCE);
  public static final TextAttributesKey COMMENT              = createTextAttributesKey("JACTL_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
  public static final TextAttributesKey BAD_CHARACTER        = createTextAttributesKey("JACTL_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);


  private static final TextAttributesKey[] SEPARATOR_KEYS  = new TextAttributesKey[]{SEPARATOR};
  private static final TextAttributesKey[] BAD_CHAR_KEYS   = new TextAttributesKey[]{BAD_CHARACTER};
  private static final TextAttributesKey[] KEY_KEYS        = new TextAttributesKey[]{KEY};
  private static final TextAttributesKey[] IDENTIFIER_KEYS = new TextAttributesKey[]{IDENTIFIER};
  private static final TextAttributesKey[] NUMBER_KEYS     = new TextAttributesKey[]{NUMBER};
  private static final TextAttributesKey[] STRING_KEYS     = new TextAttributesKey[]{STRING};
  private static final TextAttributesKey[] COMMENT_KEYS    = new TextAttributesKey[]{COMMENT};
  private static final TextAttributesKey[] TYPE_KEYS       = new TextAttributesKey[]{TYPE};
  private static final TextAttributesKey[] EMPTY_KEYS      = new TextAttributesKey[0];

  private Project project;

  public JactlSyntaxHighLighter(Project project) {
    this.project = project;
  }

  @NotNull
  @Override
  public Lexer getHighlightingLexer() {
    return new JactlTokeniser(project);
  }

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(IElementType elementType) {

    if (elementType instanceof JactlTokenType) {
      JactlTokenType type = (JactlTokenType)elementType;
      return switch (type.tokenType) {
        case COMMENT           -> COMMENT_KEYS;
        case IDENTIFIER        -> IDENTIFIER_KEYS;
        case DOLLAR_IDENTIFIER -> IDENTIFIER_KEYS;
        case STRING_CONST      -> STRING_KEYS;
        case EXPR_STRING_START -> STRING_KEYS;
        case EXPR_STRING_END   -> STRING_KEYS;
        case SEMICOLON         -> SEPARATOR_KEYS;
        case DEF               -> TYPE_KEYS;
        case BYTE              -> TYPE_KEYS;
        case STRING            -> TYPE_KEYS;
        case INT               -> TYPE_KEYS;
        case LONG              -> TYPE_KEYS;
        case DECIMAL           -> TYPE_KEYS;
        case DOUBLE            -> TYPE_KEYS;
        case BOOLEAN           -> TYPE_KEYS;
        default -> {
          if (type.tokenType.asString != null && Tokeniser.isIdentifier(type.tokenType.asString)) {
            yield KEY_KEYS;
          }
          yield type.tokenType.isNumber() ? NUMBER_KEYS : EMPTY_KEYS;
        }
      };
    }
    if (elementType.equals(TokenType.BAD_CHARACTER)) {
      return BAD_CHAR_KEYS;
    }
    return EMPTY_KEYS;
  }

}