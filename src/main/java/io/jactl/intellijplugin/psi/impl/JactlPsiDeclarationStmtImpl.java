package io.jactl.intellijplugin.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import io.jactl.intellijplugin.psi.AbstractJactlPsiStmt;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiStmt;
import org.jetbrains.annotations.NotNull;

public class JactlPsiDeclarationStmtImpl extends AbstractJactlPsiStmt implements JactlPsiStmt {

  private PsiElement identifier;

  public JactlPsiDeclarationStmtImpl(@NotNull ASTNode node) {
    super(node);
  }

//  @Override
//  public @Nullable PsiElement getNameIdentifier() {
//    if (identifier == null) {
//      for(PsiElement element = getFirstChild(); element != null; element = element.getNextSibling()){
//        if (element.getNode().getElementType() instanceof JactlNameElementType) {
//          identifier = element;
//          break;
//        }
//      }
//    }
//    return identifier;
//  }
//
//  @Override
//  public @Nullable PsiElement getIdentifyingElement() {
//    return getNameIdentifier();
//  }
//
//  @Override
//  public @Nullable @NlsSafe String getName() {
//    String name = getNameIdentifier().getText();
//    return name;
//  }

//  @Override
//  public int getTextOffset() {
//    // Need to return offset of our identifier within us apparently
//    int textOffset = getNameIdentifier().getTextOffset();
//    int thisOffset = super.getTextOffset();
//    int diff = textOffset - thisOffset;
//    return diff;
//  }

//  @Override
//  public PsiElement setName(@NlsSafe @NotNull String name) throws IncorrectOperationException {
//    throw new UnsupportedOperationException("Not yet implemented");
//  }
}
