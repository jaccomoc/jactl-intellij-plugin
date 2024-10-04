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

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import io.jactl.intellijplugin.JactlLanguage;
import io.jactl.intellijplugin.JactlParserDefinition;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.psi.*;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.jactl.intellijplugin.psi.JactlTokenTypes.*;

public class JactlFormatingModelBuilder implements FormattingModelBuilder {
  @Override
  public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
    final CodeStyleSettings codeStyleSettings = formattingContext.getCodeStyleSettings();
    return FormattingModelProvider.createFormattingModelForPsiFile(formattingContext.getContainingFile(),
                                                                   new JactlCodeBlock(formattingContext.getNode(),
                                                                                      Wrap.createWrap(WrapType.NONE, false),
                                                                                      null,
                                                                                      createSpaceBuilder(codeStyleSettings),
                                                                                      true),
                                                                   codeStyleSettings);
  }

  private SpacingBuilder createSpaceBuilder(CodeStyleSettings settings) {
    return new SpacingBuilder(settings, JactlLanguage.INSTANCE);
  }

  private boolean isList(ASTNode node) {
    return node.getElementType() == JactlListElementType.LIST;
  }

  private List<Block> buildChildren(JactlAbstractBlock parentBlock, ASTNode node, SpacingBuilder spacingBuilder) {
    List<Block> blocks = new ArrayList<>();
    Alignment alignment   = null;
    if (isList(node)) {
      // Create alignment for args or for init/cond/update part of "for" stmt
      alignment = Alignment.createAlignment();
    }
    else {
      alignment = parentBlock.getAlignment();
    }
    for (ASTNode child  = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (child.getElementType() == TokenType.WHITE_SPACE) {
        continue;
      }
      if (JactlUtils.isElementType(child, JactlExprElementType.CLOSURE)) {
        // Don't align closures even when passed as args to calls
        alignment = null;
      }
      Block block;
      if (child.getFirstChildNode() == null) {
        boolean isSpecialChar = JactlUtils.isElementType(child, LEFT_BRACE, RIGHT_BRACE, LEFT_SQUARE, RIGHT_SQUARE, LEFT_PAREN, RIGHT_PAREN);
        Indent indent = isSpecialChar                            ? Indent.getNoneIndent() :
                        JactlUtils.isElementType(child, COMMENT) ? Indent.getNormalIndent() :
                        alignment != null                        ? Indent.getNoneIndent()
                                                                 : Indent.getContinuationWithoutFirstIndent();
        block = new JactlLeafBlock(child, spacingBuilder, indent, alignment);
      }
      else if (child.getElementType() == JactlStmtElementType.BLOCK) {
        block = new JactlCodeBlock(child, Wrap.createWrap(WrapType.NONE, false), alignment, spacingBuilder, false);
      }
      else if (child.getElementType() instanceof JactlStmtElementType || child.getElementType() instanceof JactlListElementType || child.getElementType() == JactlNameElementType.PACKAGE) {
        block = new JactlStmtBlock(child, node.getElementType() == JactlParserDefinition.FILE, spacingBuilder, alignment);
      }
      else if (child.getElementType() instanceof JactlExprElementType) {
        block = new JactlBlock(child, spacingBuilder, alignment);
      }
      else {
        block = new JactlBlock(child, spacingBuilder, alignment);
      }
      blocks.add(block);
    }
    return blocks;
  }

  abstract class JactlAbstractBlock extends AbstractBlock {
    boolean        isTopLevel;
    SpacingBuilder spacingBuilder;
    List<Block>    childBlocks;
    JactlAbstractBlock(ASTNode node, Wrap wrap, Alignment alignment, SpacingBuilder spacingBuilder, boolean isTopLevel) {
      super(node, /*wrap*/ null, alignment);
      this.spacingBuilder = spacingBuilder;
      this.isTopLevel     = isTopLevel;
    }
    @Override protected List<Block> buildChildren() { return childBlocks = JactlFormatingModelBuilder.this.buildChildren(this, getNode(), spacingBuilder); }

    @Override
    public boolean isIncomplete() {
      var result = super.isIncomplete();
      return result;
    }

    //
    // This controls how the indenting/alignment works for the first line after "enter" pressed
    //
    @Override public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
      Alignment alignment = null;
      if (isList(getNode()) && childBlocks != null && childBlocks.size() >= 2) {
        // Align args to call based on alignment of first arg
        alignment = childBlocks.get(0).getAlignment();
      }
      else if (getNode().getElementType() == JactlStmtElementType.FOR_STMT && childBlocks != null) {
        alignment = childBlocks.stream().map(Block::getAlignment).filter(Objects::nonNull).findFirst().orElse(null);
      }
      else if (newChildIndex == 0) {
        alignment = getAlignment();    // If parent is aligned then first child should be aligned
      }
      return new ChildAttributes(isTopLevel ? Indent.getNoneIndent() : Indent.getNormalIndent(), alignment);
    }
  }

  class JactlLeafBlock extends JactlAbstractBlock {
    Indent indent;
    JactlLeafBlock(ASTNode node, SpacingBuilder spacingBuilder, Indent indent, Alignment alignment) {
      super(node, Wrap.createWrap(WrapType.NORMAL, false), alignment, spacingBuilder, false);
      this.indent = indent;
    }
    @Override protected List<Block> buildChildren() { return List.of(); }
    @Override public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) { return null; }
    @Override public @Nullable Indent getIndent() { return indent; }
    @Override public boolean isLeaf() { return true; }
  }

  class JactlStmtBlock extends JactlAbstractBlock {
    JactlStmtBlock(ASTNode node, boolean isTopLevel, SpacingBuilder spacingBuilder, Alignment alignment) {
      super(node, null, alignment, spacingBuilder, isTopLevel);
    }
    @Override public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) { return null; }
    @Override public boolean isLeaf() { return false; }
    @Override public @Nullable Indent getIndent() {
      if (myNode instanceof JactlPsiList) {
        return Indent.getNormalIndent();
      }
      return isTopLevel || myAlignment != null ? Indent.getNoneIndent() : Indent.getNormalIndent();
    }
  }

  class JactlBlock extends JactlAbstractBlock {
    JactlBlock(ASTNode node, SpacingBuilder spacingBuilder, Alignment alignment) {
      super(node, null, alignment, spacingBuilder, false);
    }
    @Override public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) { return null; }
    @Override public @Nullable Indent getIndent() { return getAlignment() == null ? Indent.getContinuationWithoutFirstIndent() : Indent.getNoneIndent(); }
    @Override public boolean isLeaf() { return false; }
  }

  class JactlCodeBlock extends JactlAbstractBlock {
    JactlCodeBlock(ASTNode node, Wrap wrap, Alignment alignment, SpacingBuilder spacingBuilder, boolean isTopLevel) {
      super(node, wrap, alignment, spacingBuilder, isTopLevel);
    }
    @Override public Indent getIndent() { return Indent.getNormalIndent(); }
    @Override public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
      return spacingBuilder.getSpacing(this, child1, child2);
    }
    @Override public boolean isLeaf() { return false; }
  }
}
