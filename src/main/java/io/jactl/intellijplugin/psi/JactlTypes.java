package io.jactl.intellijplugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import io.jactl.intellijplugin.JactlFileElementType;
import io.jactl.intellijplugin.JactlParserAdapter;
import io.jactl.intellijplugin.JactlParserDefinition;
import io.jactl.intellijplugin.extensions.debugger.JactlCodeFragment;
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
      if (type instanceof JactlNameElementType) {
        JactlNameElementType nameType = (JactlNameElementType) type;
        return new JactlPsiNameImpl(node, nameType);
      }

//      if (type instanceof IFileElementType) {
//        JactlParserDefinition jactlParserDefinition = new JactlParserDefinition();
//        Project project = type instanceof JactlFileElementType ? ((JactlFileElementType) type).getProject() : null;
//        if (project == null) {
//          project = node.getTreeParent().getPsi().getProject();
//        }
//        if (project == null) {
//          throw new IllegalStateException("Could not locate project for JactlFileElementType");
//        }
//        PsiBuilderImpl psiBuilder = new PsiBuilderImpl(project, jactlParserDefinition, jactlParserDefinition.createLexer(project), node, node.getText());
//        return new JactlParserAdapter(project).parse(null, psiBuilder).getFirstChildNode().getPsi();
//      }
      return new ASTWrapperPsiElement(node);
      //throw new IllegalStateException("Unexpected type of node.getElementType(): " + type.getClass().getName());
    }
  }
}
