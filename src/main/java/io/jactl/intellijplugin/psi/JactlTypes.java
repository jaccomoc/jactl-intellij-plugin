package io.jactl.intellijplugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import io.jactl.intellijplugin.psi.impl.*;

public interface JactlTypes {
  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type instanceof JactlTypeElementType)          { return new JactlPsiTypeImpl(node, type == JactlTypeElementType.BUILT_IN_TYPE); }
      if (type == JactlStmtElementType.VAR_DECL)         { return new JactlPsiDeclarationStmtImpl(node); }
      if (type == JactlStmtElementType.FUN_DECL)         { return new JactlPsiDeclarationStmtImpl(node); }
      if (type instanceof JactlStmtElementType)          { return new JactlPsiStmtImpl(node); }
      if (type == JactlExprElementType.IDENTIFIER)       { return new JactlPsiIdentifierExprImpl(node); }
      if (type instanceof JactlExprElementType)          { return new JactlPsiExprImpl(node); }
      if (type instanceof JactlListElementType)          { return new JactlPsiListImpl(node); }
      if (type instanceof JactlNameElementType nameType) { return new JactlPsiNameImpl(node, nameType); }

      throw new IllegalStateException("Unexpected type of node.getElementType(): " + type.getClass().getName());
    }
  }
}
