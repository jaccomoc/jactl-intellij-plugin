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

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import io.jactl.intellijplugin.JactlIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

final class JactlColorSettingsPage implements ColorSettingsPage {

  private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
    new AttributesDescriptor("Keyword", JactlSyntaxHighLighter.KEY),
    new AttributesDescriptor("Separator", JactlSyntaxHighLighter.SEMICOLON),
    new AttributesDescriptor("Number", JactlSyntaxHighLighter.NUMBER),
    new AttributesDescriptor("String", JactlSyntaxHighLighter.STRING),
    new AttributesDescriptor("Identifiers//Identifier", JactlSyntaxHighLighter.IDENTIFIER),
    new AttributesDescriptor("Identifiers//Function", JactlSyntaxHighLighter.FUNCTION),
    new AttributesDescriptor("Identifiers//Variable", JactlSyntaxHighLighter.LOCAL_VARIABLE),
    new AttributesDescriptor("Identifiers//Field", JactlSyntaxHighLighter.FIELD),
    new AttributesDescriptor("Identifiers//Parameter", JactlSyntaxHighLighter.PARAMETER),
    new AttributesDescriptor("Identifiers//Method", JactlSyntaxHighLighter.METHOD),
    new AttributesDescriptor("Identifiers//Class", JactlSyntaxHighLighter.CLASS),
    new AttributesDescriptor("Identifiers//Package", JactlSyntaxHighLighter.PACKAGE),
    new AttributesDescriptor("Symbols//Braces", JactlSyntaxHighLighter.BRACES),
    new AttributesDescriptor("Symbols//Brackets", JactlSyntaxHighLighter.BRACKETS),
    new AttributesDescriptor("Symbols//Parentheses", JactlSyntaxHighLighter.PARENTHESES),
    new AttributesDescriptor("Symbols//SemiColon", JactlSyntaxHighLighter.SEMICOLON),
    new AttributesDescriptor("Symbols//Dot", JactlSyntaxHighLighter.DOT),
    new AttributesDescriptor("Symbols//Comma", JactlSyntaxHighLighter.COMMA),
    new AttributesDescriptor("Symbols//Operators", JactlSyntaxHighLighter.OPERATOR),
    new AttributesDescriptor("Type", JactlSyntaxHighLighter.TYPE),
    new AttributesDescriptor("Comment", JactlSyntaxHighLighter.COMMENT),
    new AttributesDescriptor("Bad value", JactlSyntaxHighLighter.BAD_CHARACTER)
  };

  @Override
  public @NotNull Icon getIcon() {
    return JactlIcons.FILE;
  }

  @NotNull
  @Override
  public SyntaxHighlighter getHighlighter() {
    return new JactlSyntaxHighLighter(null);
  }

  @NotNull
  @Override
  public String getDemoText() {
    return
      """
      package <package>a.b.c</package>
      import <class>a.b.BaseClass</class>
      import static <class>a.b.BaseClass</class>.<field>FFF</field> as <identifier>GGG</identifier>
      
      // This is a line comment
      /* And this is a multi-
         line comment */
      def <function>func</function>(String <parameter>str</parameter>) {
        def (<variable>xvar</variable>,<variable>yvar</variable>) = [1234, 1.234]
        String <variable>result</variable> = "Interpolated ${'string'}: $<parameter>str</parameter>: x=$<variable>xvar</variable>, y=$<variable>yvar</variable>"
        return <variable>result</variable>
      }
      
      def <variable>variable</variable> = <function>func</function>('data')
      
      class <class>SomeClass</class> extends <class>BaseClass</class> {
        class <class>Inner</class> {
          const <field>MAX</field> = 10000
          static def <function>calc</function>(int <parameter>count</parameter>, def <parameter>callable</parameter>) {
            def <variable>result</variable>
            for (int <variable>i</variable> = 0; <variable>i</variable> < <parameter>count</parameter> && <variable>i</variable> < <field>MAX</field>; <variable>i</variable>++) {
              <variable>result</variable> += <parameter>callable</parameter>(<variable>i</variable>)
            }
            return <variable>result</variable>
          }
        }
      
        int <field>intField</field>
        double <field>doubleField</field>
        def <method>instanceFunc</method>(<parameter>param</parameter>) {
          switch (<parameter>param</parameter>) {
            [String,_,int] -> true
            default        -> false
          }
        }
      }
      """;
  }

  private static final Map<String,TextAttributesKey> ADDITIONAL = new HashMap<>() {{
    put("variable",  JactlSyntaxHighLighter.LOCAL_VARIABLE);
    put("parameter", JactlSyntaxHighLighter.PARAMETER);
    put("field",     JactlSyntaxHighLighter.FIELD);
    put("class",     JactlSyntaxHighLighter.CLASS);
    put("package",   JactlSyntaxHighLighter.PACKAGE);
    put("method",    JactlSyntaxHighLighter.METHOD);
    put("function",  JactlSyntaxHighLighter.FUNCTION);
  }};

  @Nullable
  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return ADDITIONAL;
  }

  @Override
  public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
    return DESCRIPTORS;
  }

  @Override
  public ColorDescriptor @NotNull [] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Jactl";
  }

}