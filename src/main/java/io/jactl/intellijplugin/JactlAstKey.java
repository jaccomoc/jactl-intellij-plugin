package io.jactl.intellijplugin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.tree.IElementType;

public class JactlAstKey {
  public static final Logger LOG = Logger.getInstance(JactlParserAdapter.class);

  private IElementType type;
  private int          offset;
  private String       toString;
  private JactlFile    jactlFile;
  private VirtualFile  virtualFile;
  private Project      project;

  public JactlAstKey(JactlFile jactlFile, IElementType type, int offset) {
    this.type   = type;
    this.offset = offset;
    this.jactlFile = jactlFile;
    if (jactlFile == null) {
      //LOG.warn("JactlFile is null");
    }
    else
    if (jactlFile.getVirtualFile() == null) {
      //LOG.warn("VirtualFile is null for " + jactlFile);
    }
    else {
      this.virtualFile = jactlFile.getVirtualFile();
      this.project = jactlFile.getProject();
    }
    this.toString = type + ":" + offset;
  }

  public IElementType getType()   { return type; }
  public int          getOffset() { return offset; }
  public JactlFile    getFile()   {
    //return (JactlFile)PsiManager.getInstance(project).findFile(virtualFile);
    return jactlFile;
  }

  @Override
  public String toString() {
    return "JactlAstKey[" + toString + "]";
  }

  @Override
  public int hashCode() {
    return toString.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (obj instanceof JactlAstKey other) {
      if (type != null && !type.equals(other.type) || type == null && other.type != null) {
        return false;
      }
      return offset == other.offset;
    }
    return false;
  }
}
