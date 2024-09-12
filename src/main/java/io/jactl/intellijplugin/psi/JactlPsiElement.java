package io.jactl.intellijplugin.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import io.jactl.JactlUserDataHolder;
import io.jactl.intellijplugin.JactlAstKey;
import io.jactl.intellijplugin.JactlFile;
import io.jactl.intellijplugin.JactlParserAdapter;

public interface JactlPsiElement extends PsiElement  {

  Logger LOG = Logger.getInstance(JactlPsiElement.class);

  JactlFile   getFile();
  JactlAstKey getAstKey();

  default JactlFile _getFile() {
    return (JactlFile)getContainingFile();
//    PsiElement parent = null;
//    for (parent = getParent(); parent != null && !(parent instanceof JactlFile); parent = parent.getParent()) {
//    }
//    if (parent == null) {
//      LOG.error("Could not find parent which is a JactlFile: psi=" + this + ", node=" + getNode());
//    }
//    return (JactlFile)parent;
  }

  default String getSourceCode() {
    JactlFile file = getFile();
    if (file == null) {
      return null;
    }
    return file.getText();
  }

  default JactlUserDataHolder getJactlAstNode() {
    return JactlParserAdapter.getJactlAstNode(getFile(), getSourceCode(), getAstKey());
  }
}
