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
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import io.jactl.intellijplugin.JactlLanguage;
import io.jactl.intellijplugin.JactlParserDefinition;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.psi.*;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.jactl.intellijplugin.psi.JactlExprElementType.TERNARY_EXPR;
import static io.jactl.intellijplugin.psi.JactlTokenTypes.*;

public class JactlFormatingModelBuilder implements FormattingModelBuilder {
  @Override
  public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
    final CodeStyleSettings codeStyleSettings = formattingContext.getCodeStyleSettings();
    return FormattingModelProvider.createFormattingModelForPsiFile(formattingContext.getContainingFile(),
                                                                   new JactlCodeBlock(null,
                                                                                      formattingContext.getNode(),
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

  private boolean isRhsExpr(ASTNode node) {
    return node.getElementType() == JactlExprElementType.RHS_EXPR;
  }

  private List<JactlAbstractBlock> buildChildren(JactlAbstractBlock parentBlock, ASTNode parentNode, SpacingBuilder spacingBuilder) {
    Alignment                alignment = null;
    if (isList(parentNode) || isRhsExpr(parentNode)) {
      // Create alignment for args or for init/cond/update part of "for" stmt or for rhs of assignment
      alignment = Alignment.createAlignment();
    }
    return _buildChildren(parentBlock, parentNode, spacingBuilder, alignment);
  }

  private List<JactlAbstractBlock> _buildChildren(JactlAbstractBlock parentBlock, ASTNode parentNode, SpacingBuilder spacingBuilder, Alignment alignment) {
    List<JactlAbstractBlock> blocks    = new ArrayList<>();
    for (ASTNode child  = parentNode.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      // If we are a method call then subsume binary expr children into ours to make alignement
      // work better with auto-indent and aligning on '.' of method calls. This is due to how
      // auto-indent looks for candidate alignments. Only if text range of element overlaps with
      // place of indent will it then drill down into children to look for alignments.
      if (JactlUtils.isElementType(parentNode, JactlExprElementType.METHOD_CALL_EXPR) && JactlUtils.isElementType(child, JactlExprElementType.BINARY_EXPR)) {
        blocks.addAll(_buildChildren(parentBlock, child, spacingBuilder, alignment));
        continue;
      }
      JactlAbstractBlock block = createBlock(parentBlock, parentNode, spacingBuilder, child, alignment == null ? parentBlock.getAlignment(child) : alignment);
      if (block != null) {
        blocks.add(block);
      }
    }
    return blocks;
  }

  private JactlAbstractBlock createBlock(JactlAbstractBlock parentBlock, ASTNode node, SpacingBuilder spacingBuilder, ASTNode child, Alignment alignment) {
    if (child.getElementType() == TokenType.WHITE_SPACE) {
      return null;
    }
    if (child.getFirstChildNode() == null) {
      boolean isSpecialChar = JactlUtils.isElementType(child, LEFT_BRACE, RIGHT_BRACE, LEFT_SQUARE, RIGHT_SQUARE, LEFT_PAREN, RIGHT_PAREN);
      Indent indent = isSpecialChar                            ? Indent.getNoneIndent() :
                      JactlUtils.isElementType(child, COMMENT) ? Indent.getNormalIndent() :
                      alignment != null ? Indent.getNoneIndent()
                                        : Indent.getContinuationWithoutFirstIndent();
      return new JactlLeafBlock(parentBlock, child, spacingBuilder, indent, alignment);
    }
    else if (child.getElementType() == JactlStmtElementType.BLOCK && JactlUtils.isElementType(child.getTreeNext(), LEFT_BRACE)) {
      return new JactlCodeBlock(parentBlock, child, Wrap.createWrap(WrapType.NONE, false), alignment, spacingBuilder, false);
    }
    else if (child.getElementType() instanceof JactlStmtElementType || isList(child) || child.getElementType() == JactlNameElementType.PACKAGE) {
      return new JactlStmtBlock(parentBlock, child, node.getElementType() == JactlParserDefinition.JACTL_FILE_ELEMENT_TYPE, spacingBuilder, alignment);
    }
    else if (isBinaryOrMethodCallExpr(child)) {
      return new JactlBinaryExpr(parentBlock, child, spacingBuilder, alignment);
    }
    else if (JactlUtils.isElementType(child, TERNARY_EXPR)) {
      return new JactlTernaryExpr(parentBlock, child, spacingBuilder, alignment);
    }
    else if (child.getElementType() instanceof JactlExprElementType) {
      // Don't align closures even when passed as args to calls
      return new JactlBlock(parentBlock, child, spacingBuilder, JactlUtils.isElementType(child, JactlExprElementType.CLOSURE) ? null : alignment);
    }
    else {
      return new JactlBlock(parentBlock, child, spacingBuilder, alignment);
    }
  }

  abstract class JactlAbstractBlock extends AbstractBlock {
    boolean                  isTopLevel;
    SpacingBuilder           spacingBuilder;
    List<JactlAbstractBlock> childBlocks;
    Alignment                alignment;
    JactlAbstractBlock       parentBlock;
    JactlAbstractBlock(JactlAbstractBlock parentBlock, ASTNode node, Wrap wrap, Alignment alignment, SpacingBuilder spacingBuilder, boolean isTopLevel) {
      super(node, /*wrap*/ null, alignment);
      this.parentBlock    = parentBlock;
      this.spacingBuilder = spacingBuilder;
      this.isTopLevel     = isTopLevel;
      this.alignment      = alignment;
    }

    @Override protected List<Block> buildChildren() {
      childBlocks = JactlFormatingModelBuilder.this.buildChildren(this, getNode(), spacingBuilder);
      return new ArrayList<>(childBlocks);
    }

    public Alignment getOperatorAlignment() {
      return parentBlock == null ? null : parentBlock.getOperatorAlignment();
    }

    @Override
    public boolean isIncomplete() {
      boolean result = super.isIncomplete();
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
      return new ChildAttributes(isTopLevel && !(this instanceof JactlStmtBlock) ? Indent.getNoneIndent() : Indent.getNormalIndent(), alignment);
    }

    public Alignment getAlignment(ASTNode node) {
      return getAlignment();
    }

    @Override
    public Alignment getAlignment() {
      return alignment;
    }
  }

  enum Pos { LEFT, RIGHT };
  class JactlBinaryExpr extends JactlBlock {
    Alignment        operandAlignment = Alignment.createAlignment();
    Alignment        operatorAlignment;
    Map<ASTNode,Pos> childrenPos       = new HashMap<>();

    JactlBinaryExpr(JactlAbstractBlock parentBlock, ASTNode node, SpacingBuilder spacingBuilder, Alignment alignment) {
      super(parentBlock, node, spacingBuilder, alignment);
      Pos pos = Pos.LEFT;
      // If we are a method call then include our binary expr children as ours so we pretend
      // that we are the binary expr
      boolean isMethodCall = JactlUtils.isElementType(node, JactlExprElementType.METHOD_CALL_EXPR);
      Predicate<ASTNode> flattenChildren = child -> isMethodCall && JactlUtils.isElementType(child, JactlExprElementType.BINARY_EXPR);
      Alignment parentOpAlign = parentBlock.getOperatorAlignment();
      for (ASTNode child: Arrays.stream(getNode().getChildren(null))
                                .flatMap(child ->  flattenChildren.test(child) ? Arrays.stream(child.getChildren(null)) : Stream.of(child))
                                .collect(Collectors.toList())) {
        IElementType elementType = child.getElementType();
        if (pos == Pos.LEFT && elementType instanceof JactlTokenType && ((JactlTokenType)elementType).isOperator()) {
          // Once we get to the operator we need to check on newlines positions to decide how to align
          // We only align on operators when they are the first thing on a line (i.e. previous whitespace included '\n')
          // and are not followed by a newline. We also align on operator if our parent is operator aligned.
          Function<ASTNode,Boolean> hasNewLine = n -> n instanceof PsiWhiteSpace && n.getText().contains("\n");
          if (parentOpAlign != null || hasNewLine.apply(child.getTreePrev()) && !hasNewLine.apply(child.getTreeNext())) {
            operatorAlignment = parentOpAlign == null ? Alignment.createAlignment() : parentOpAlign;
            pos = Pos.RIGHT;
          }
        }
        childrenPos.put(child, pos);
      }
    }

    public Alignment getOperatorAlignment() {
      return operatorAlignment;
    }

    public Alignment getAlignment(ASTNode child) {
      if (operatorAlignment == null) {
        // If we are not aligning on the operator then align everything together
        return operandAlignment;
      }
      Pos pos = childrenPos.get(child);
      if (pos == null) {
        pos = Pos.RIGHT;
      }
      return pos == Pos.LEFT ? getAlignment() : operatorAlignment;
    }
  }

  private static boolean isBinaryOrMethodCallExpr(ASTNode child) {
    // Need to ignore bracketed BinaryExpr since they are just "(" BinaryExpr ")" and have no operator to align on
    return JactlUtils.isElementType(child, JactlExprElementType.BINARY_EXPR) &&
           !JactlUtils.isElementType(JactlUtils.getFirstChildNotWhiteSpace(child.getPsi()), LEFT_PAREN) ||
           JactlUtils.isElementType(child, JactlExprElementType.METHOD_CALL_EXPR);
  }

  class JactlTernaryExpr extends JactlBlock {
    Alignment operatorAlignment = Alignment.createAlignment();
    JactlTernaryExpr(JactlAbstractBlock parentBlock, ASTNode node, SpacingBuilder spacingBuilder, Alignment alignment) {
      super(parentBlock, node, spacingBuilder, alignment);
    }

    @Override public Alignment getAlignment(ASTNode node) {
      IElementType elementType = node.getElementType();
      if (elementType instanceof JactlTokenType && ((JactlTokenType)elementType).tokenType.is(io.jactl.TokenType.QUESTION, io.jactl.TokenType.COLON)) {
        return operatorAlignment;
      }
      return getAlignment();
    }
  }

  class JactlLeafBlock extends JactlAbstractBlock {
    Indent indent;
    JactlLeafBlock(JactlAbstractBlock parentBlock, ASTNode node, SpacingBuilder spacingBuilder, Indent indent, Alignment alignment) {
      super(parentBlock, node, Wrap.createWrap(WrapType.NORMAL, false), alignment, spacingBuilder, false);
      this.indent = indent;
    }
    @Override protected List<Block> buildChildren() { return Collections.EMPTY_LIST; }
    @Override public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) { return null; }
    @Override public @Nullable Indent getIndent() { return indent; }
    @Override public boolean isLeaf() { return true; }
  }

  class JactlStmtBlock extends JactlAbstractBlock {
    JactlStmtBlock(JactlAbstractBlock parentBlock, ASTNode node, boolean isTopLevel, SpacingBuilder spacingBuilder, Alignment alignment) {
      super(parentBlock, node, null, alignment, spacingBuilder, isTopLevel);
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
    JactlBlock(JactlAbstractBlock parentBlock, ASTNode node, SpacingBuilder spacingBuilder, Alignment alignment) {
      super(parentBlock, node, null, alignment, spacingBuilder, false);
    }
    @Override public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) { return null; }
    @Override public @Nullable Indent getIndent() { return getAlignment() == null ? Indent.getContinuationWithoutFirstIndent() : Indent.getNoneIndent(); }
    @Override public boolean isLeaf() { return false; }
  }

  class JactlCodeBlock extends JactlAbstractBlock {
    JactlCodeBlock(JactlAbstractBlock parentBlock, ASTNode node, Wrap wrap, Alignment alignment, SpacingBuilder spacingBuilder, boolean isTopLevel) {
      super(parentBlock, node, wrap, alignment, spacingBuilder, isTopLevel);
    }
    @Override public Indent getIndent() { return isTopLevel ? Indent.getNoneIndent() : Indent.getNormalIndent(); }
    @Override public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
      return spacingBuilder.getSpacing(this, child1, child2);
    }
    @Override public boolean isLeaf() { return false; }
  }
}
