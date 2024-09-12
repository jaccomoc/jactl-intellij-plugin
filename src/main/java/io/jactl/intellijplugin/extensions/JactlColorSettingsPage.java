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
import java.util.Map;

final class JactlColorSettingsPage implements ColorSettingsPage {

  private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
    new AttributesDescriptor("Keyword", JactlSyntaxHighLighter.KEY),
    new AttributesDescriptor("Separator", JactlSyntaxHighLighter.SEPARATOR),
    new AttributesDescriptor("Number", JactlSyntaxHighLighter.NUMBER),
    new AttributesDescriptor("String", JactlSyntaxHighLighter.STRING),
    new AttributesDescriptor("Identifier", JactlSyntaxHighLighter.IDENTIFIER),
    new AttributesDescriptor("Function declaration", JactlSyntaxHighLighter.FUNCTION_DECLARATION),
    new AttributesDescriptor("Variable declaration", JactlSyntaxHighLighter.VARIABLE_DECLARATION),
    new AttributesDescriptor("Type", JactlSyntaxHighLighter.TYPE),
    new AttributesDescriptor("Comment", JactlSyntaxHighLighter.COMMENT),
    new AttributesDescriptor("Bad value", JactlSyntaxHighLighter.BAD_CHARACTER)
  };

  @Nullable
  @Override
  public Icon getIcon() {
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
    return "// This is a line comment\n" +
           "/* And this is a multi-line comment */\n" +
           "def func(String s) {\n" +
           "  \"Interpolated ${'string'}: $s\"\n" +
           "}\n" +
           "def (x,y) = [1234, 1.234]; println \"x=$x, y=$y\"\n";
  }

  @Nullable
  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
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