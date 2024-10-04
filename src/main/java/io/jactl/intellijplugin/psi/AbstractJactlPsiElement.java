package io.jactl.intellijplugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import io.jactl.JactlUserDataHolder;
import io.jactl.intellijplugin.JactlAstKey;
import io.jactl.intellijplugin.JactlFile;
import io.jactl.intellijplugin.JactlParserAdapter;
import io.jactl.intellijplugin.psi.impl.JactlPsiIdentifierImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for Jactl PSI elements
 */
public abstract class AbstractJactlPsiElement extends ASTWrapperPsiElement implements JactlPsiElement {

  public AbstractJactlPsiElement(@NotNull ASTNode node) {
    super(node);
  }

  public JactlAstKey getAstKey() {
    return new JactlAstKey(getFile(), getNode().getElementType(), getNode().getStartOffset());
  }

  @Override
  public JactlFile getFile() {
    return _getFile();
  }

  @Override
  public void subtreeChanged() {
    super.subtreeChanged();
    // Check for leaf nodes since Intellij doesn't automatically invoke anything for tree change
    // events on leaf nodes
    for (PsiElement child = getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof JactlPsiIdentifierImpl) {
        ((JactlPsiIdentifierImpl) child).subtreeChanged();
      }
    }
  }
}
