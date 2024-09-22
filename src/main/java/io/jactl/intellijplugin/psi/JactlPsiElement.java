package io.jactl.intellijplugin.psi;

import com.intellij.psi.PsiElement;
import io.jactl.JactlUserDataHolder;
import io.jactl.intellijplugin.JactlAstKey;
import io.jactl.intellijplugin.JactlFile;
import io.jactl.intellijplugin.JactlParserAdapter;

public interface JactlPsiElement extends PsiElement  {
  JactlFile   getFile();
  JactlAstKey getAstKey();

  default JactlFile _getFile() {
    return (JactlFile) getContainingFile();
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
