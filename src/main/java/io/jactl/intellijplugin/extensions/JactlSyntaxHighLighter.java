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

import com.intellij.ide.highlighter.JavaHighlightingColors;
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
import static io.jactl.TokenType.*;

public class JactlSyntaxHighLighter extends SyntaxHighlighterBase {

  public static final TextAttributesKey SEMICOLON       = createTextAttributesKey("JACTL_SEMICOLON", JavaHighlightingColors.JAVA_SEMICOLON);
  public static final TextAttributesKey COMMA           = createTextAttributesKey("JACTL_COMMA", JavaHighlightingColors.COMMA);
  public static final TextAttributesKey DOT             = createTextAttributesKey("JACTL_DOT", JavaHighlightingColors.DOT);
  public static final TextAttributesKey BRACES          = createTextAttributesKey("JACTL_BRACES", JavaHighlightingColors.BRACES);
  public static final TextAttributesKey BRACKETS        = createTextAttributesKey("JACTL_BRACKETS", JavaHighlightingColors.BRACKETS);
  public static final TextAttributesKey PARENTHESES     = createTextAttributesKey("JACTL_PARENTHESE", JavaHighlightingColors.PARENTHESES);
  public static final TextAttributesKey OPERATOR        = createTextAttributesKey("JACTL_OPERATOR", JavaHighlightingColors.OPERATION_SIGN);
  public static final TextAttributesKey KEY             = createTextAttributesKey("JACTL_KEY", JavaHighlightingColors.KEYWORD);
  public static final TextAttributesKey NUMBER          = createTextAttributesKey("JACTL_NUMBER", JavaHighlightingColors.NUMBER);
  public static final TextAttributesKey STRING          = createTextAttributesKey("JACTL_STRING", JavaHighlightingColors.STRING);
  public static final TextAttributesKey IDENTIFIER      = createTextAttributesKey("JACTL_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
  public static final TextAttributesKey TYPE            = createTextAttributesKey("JACTL_TYPE", JavaHighlightingColors.CLASS_NAME_ATTRIBUTES);
  public static final TextAttributesKey COMMENT         = createTextAttributesKey("JACTL_COMMENT", JavaHighlightingColors.LINE_COMMENT);
  public static final TextAttributesKey BAD_CHARACTER   = createTextAttributesKey("JACTL_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

  public static final TextAttributesKey LOCAL_VARIABLE  = createTextAttributesKey("JACTL_LOCAL_VARIABLE", JavaHighlightingColors.LOCAL_VARIABLE_ATTRIBUTES);
  public static final TextAttributesKey PARAMETER       = createTextAttributesKey("JACTL_PARAMETER", JavaHighlightingColors.PARAMETER_ATTRIBUTES);
  public static final TextAttributesKey FIELD           = createTextAttributesKey("JACTL_PARAMETER", JavaHighlightingColors.INSTANCE_FIELD_ATTRIBUTES);
  public static final TextAttributesKey METHOD          = createTextAttributesKey("JACTL_METHOD", JavaHighlightingColors.METHOD_DECLARATION_ATTRIBUTES);
  public static final TextAttributesKey FUNCTION        = createTextAttributesKey("JACTL_FUNCTION", JavaHighlightingColors.METHOD_DECLARATION_ATTRIBUTES);
  public static final TextAttributesKey CLASS           = createTextAttributesKey("JACTL_CLASS", JavaHighlightingColors.CLASS_NAME_ATTRIBUTES);
  public static final TextAttributesKey PACKAGE         = createTextAttributesKey("JACTL_PACKAGE", JavaHighlightingColors.CLASS_NAME_ATTRIBUTES);

  private static final TextAttributesKey[] SEMICOLON_KEYS   = new TextAttributesKey[]{SEMICOLON};
  private static final TextAttributesKey[] COMMA_KEYS       = new TextAttributesKey[]{COMMA};
  private static final TextAttributesKey[] DOT_KEYS         = new TextAttributesKey[]{DOT};
  private static final TextAttributesKey[] BRACES_KEYS      = new TextAttributesKey[]{BRACES};
  private static final TextAttributesKey[] BRACKETS_KEYS    = new TextAttributesKey[]{BRACKETS};
  private static final TextAttributesKey[] PARENTHESES_KEYS = new TextAttributesKey[]{PARENTHESES};
  private static final TextAttributesKey[] OPERATOR_KEYS    = new TextAttributesKey[]{OPERATOR};
  private static final TextAttributesKey[] BAD_CHAR_KEYS    = new TextAttributesKey[]{BAD_CHARACTER};
  private static final TextAttributesKey[] KEY_KEYS         = new TextAttributesKey[]{KEY};
  private static final TextAttributesKey[] IDENTIFIER_KEYS  = new TextAttributesKey[]{IDENTIFIER};
  private static final TextAttributesKey[] NUMBER_KEYS      = new TextAttributesKey[]{NUMBER};
  private static final TextAttributesKey[] STRING_KEYS      = new TextAttributesKey[]{STRING};
  private static final TextAttributesKey[] COMMENT_KEYS     = new TextAttributesKey[]{COMMENT};
  private static final TextAttributesKey[] TYPE_KEYS        = new TextAttributesKey[]{TYPE};
  private static final TextAttributesKey[] EMPTY_KEYS       = new TextAttributesKey[0];

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
    if (elementType instanceof JactlTokenType type) {
      return switch (type.tokenType) {
        case COMMENT           -> COMMENT_KEYS;
        case IDENTIFIER        -> IDENTIFIER_KEYS;
        case DOLLAR_IDENTIFIER -> IDENTIFIER_KEYS;
        case STRING_CONST      -> STRING_KEYS;
        case EXPR_STRING_START -> STRING_KEYS;
        case EXPR_STRING_END   -> STRING_KEYS;
        case SEMICOLON         -> SEMICOLON_KEYS;
        case COMMA             -> COMMA_KEYS;
        case DOT,QUESTION_DOT  -> DOT_KEYS;
        case LEFT_BRACE        -> BRACES_KEYS;
        case RIGHT_BRACE       -> BRACES_KEYS;
        case LEFT_SQUARE       -> BRACKETS_KEYS;
        case QUESTION_SQUARE   -> BRACKETS_KEYS;
        case RIGHT_SQUARE      -> BRACKETS_KEYS;
        case LEFT_PAREN        -> PARENTHESES_KEYS;
        case RIGHT_PAREN       -> PARENTHESES_KEYS;
        case DEF               -> TYPE_KEYS;
        case BYTE              -> TYPE_KEYS;
        case STRING            -> TYPE_KEYS;
        case INT               -> TYPE_KEYS;
        case LONG              -> TYPE_KEYS;
        case DECIMAL           -> TYPE_KEYS;
        case DOUBLE            -> TYPE_KEYS;
        case BOOLEAN           -> TYPE_KEYS;
        case OBJECT            -> TYPE_KEYS;
        case MAP               -> TYPE_KEYS;
        case LIST              -> TYPE_KEYS;
        default -> {
          if (type.tokenType.asString != null && Tokeniser.isIdentifier(type.tokenType.asString)) {
            yield KEY_KEYS;
          }
          if (isOperator(type)) {
            yield OPERATOR_KEYS;
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

  private boolean isOperator(JactlTokenType jactlTokenType) {
    var tokenType = jactlTokenType.tokenType;
    return tokenType.isBitOperator() ||
           tokenType.isBooleanOperator() ||
           tokenType.isNumericOperator() ||
           tokenType.isAssignmentLike() ||
           tokenType.is(QUESTION,QUESTION_QUESTION,QUESTION_COLON);
  }
}