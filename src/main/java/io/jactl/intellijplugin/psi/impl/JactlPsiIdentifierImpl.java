package io.jactl.intellijplugin.psi.impl;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import io.jactl.intellijplugin.JactlAstKey;
import io.jactl.intellijplugin.JactlCachedValue;
import io.jactl.intellijplugin.JactlFile;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.psi.*;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiIdentifier;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiName;

public class JactlPsiIdentifierImpl extends LeafPsiElement implements JactlPsiIdentifier {
//  private final JactlCachedValue<JactlFile>         jactlFile;
//  private final JactlCachedValue<JactlPsiReference> reference;

  public JactlPsiIdentifierImpl(IElementType type,  CharSequence text) {
    super(type, text);
//    this.reference = new JactlCachedValue<>(() -> new JactlPsiReference(this, getAstKey()));
//    this.jactlFile = new JactlCachedValue<>(this::_getFile);
  }

  public JactlAstKey getAstKey() {
    if (getParent() instanceof JactlPsiIdentifierExprImpl) {
      JactlPsiIdentifierExprImpl parent = (JactlPsiIdentifierExprImpl) getParent();
      return parent.getAstKey();
    }
    if (getParent().getNode().getElementType() == JactlExprElementType.CLASS_PATH_EXPR) {
      return ((JactlPsiElement)getParent()).getAstKey();
    }
    if (getParent() instanceof JactlPsiTypeImpl) {
      JactlPsiTypeImpl type = (JactlPsiTypeImpl) getParent();
      return type.getAstKey();
    }
    return new JactlAstKey(getFile(), getNode().getElementType(), getNode().getStartOffset());
  }

  @Override public PsiReference getReference() {
    PsiElement parent = getParent();
    // If we are already a type of name (e.g. class or variable) then we don't refer to anything.
    // Only exception is for packages where we want the identifier to refer to the PsiDirectory for
    // the package.
    if (parent instanceof JactlPsiName) {
      JactlPsiName psiName = (JactlPsiName) parent;
      if (psiName.getType() == JactlNameElementType.PACKAGE) {
        return new JactlPackageReference(this);
      }
      return null;
    }
    // If we are package part of a CLASS_PATH_EXPR (i.e. not last identifier)
    if (parent.getNode().getElementType() == JactlExprElementType.CLASS_PATH_EXPR) {
      if (JactlUtils.getLastChild(parent, JactlPsiIdentifier.class) != this) {
        return new JactlPackageReference(this);
      }
    }
    return new JactlPsiReference(this);
  }

//  @Override
//  public String toString() {
//    PsiReference reference = getReference();
//    PsiElement   resolved  = reference == null ? null : reference.resolve();
//    String resolvedToString = "";
//    if (resolved != null) {
//      int startOffset = resolved.getNode().getStartOffset();
//      resolvedToString = ",ref=" + resolved.toString() + "(" + startOffset + "," + (startOffset + resolved.getNode().getTextLength()) + ")";
//    }
//    return "JactlPsiIdentifierImpl(" + getElementType() + resolvedToString + ")";
//  }

  @Override
  public JactlFile getFile() {
//   return jactlFile.getValue();
    return _getFile();
  }

  public void subtreeChanged() {
//    reference.clear();
//    jactlFile.clear();
  }
}
