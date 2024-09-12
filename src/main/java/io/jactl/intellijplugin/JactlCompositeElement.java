package io.jactl.intellijplugin;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class JactlCompositeElement extends CompositeElement {
  private JactlParserAdapter parser;
  private JactlFile          jactlFile;

  public JactlCompositeElement(@NotNull IElementType type) {
    super(type);
  }

  public void setParser(JactlParserAdapter parser) {
    this.parser = parser;
  }
  public JactlParserAdapter getParser() {
    return this.parser;
  }

  public void setJactlFile(JactlFile file) {
    this.jactlFile = file;
  }

  public JactlFile getJactlFile() {
    return jactlFile;
  }
}
