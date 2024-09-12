package io.jactl.intellijplugin.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import io.jactl.Expr;
import io.jactl.JactlType;
import io.jactl.JactlUserDataHolder;
import io.jactl.Stmt;
import io.jactl.intellijplugin.*;
import io.jactl.intellijplugin.common.JactlPlugin;
import io.jactl.intellijplugin.psi.impl.JactlPsiNameImpl;
import io.jactl.resolver.Resolver;
import io.jactl.runtime.ClassDescriptor;
import io.jactl.runtime.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JactlPsiReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  public static final Logger LOG = Logger.getInstance(JactlPsiReference.class);

  protected JactlPsiElement psiElement;

  private final JactlCachedValue<PsiElement> resolvedElement;

  public JactlPsiReference(JactlPsiElement psi) {
    // TextRange must be relative to start of this psi
    super(psi, TextRange.from(0, psi.getTextLength()));
    this.psiElement = psi;
    this.resolvedElement = new JactlCachedValue<>(this::_resolve);
  }

  @Override
  public boolean isReferenceTo(@NotNull PsiElement element) {
    JactlAstKey     elementAstKey = null;
    if (element instanceof JactlFile file) {
      var classDecl = file.getTopLevelClass();
      elementAstKey = classDecl == null ? null : classDecl.getUserData(JactlAstKey.class);
    }
    else {
      elementAstKey = element instanceof JactlPsiElement psi ? psi.getAstKey() : null;
    }
    PsiElement value = resolvedElement.getValue();
    boolean equals = elementAstKey != null && value instanceof JactlPsiElement jactlPsi && elementAstKey.equals(jactlPsi.getAstKey());
    return equals;
  }

  @Override
  public @NotNull PsiElement getElement() {
    return psiElement;
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    // For file rename remove .jactl suffix
    newElementName = JactlPlugin.removeSuffix(newElementName);
    PsiElement newElement = JactlUtils.newReferenceElement(psiElement.getProject(), ((JactlPsiNameImpl)resolvedElement.getValue()).getType(), newElementName, psiElement.getClass());
    psiElement = (JactlPsiElement)psiElement.replace(newElement);
    return psiElement;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    if (element instanceof PsiFile file && psiElement.getParent().getNode().getElementType() == JactlExprElementType.CLASS_PATH_EXPR) {
      psiElement = JactlUtils.createNewPackagePath(file.getContainingDirectory(), psiElement);
      return this.psiElement;
    }
    return super.bindToElement(element);
  }

  @Override
  public @Nullable PsiElement resolve() {
    return resolvedElement.getValue();
  }

  protected @Nullable PsiElement _resolve() {
    String              source  = psiElement.getSourceCode();
    JactlUserDataHolder astNode = JactlParserAdapter.getJactlAstNode(psiElement.getFile(), source, psiElement.getAstKey());
    if (astNode == null) {
      //LOG.warn("Could not find Jactl AST Node for " + psiElement);
      return null;
    }

    // If we are a field after a '.'
    if (astNode instanceof Expr expr && expr.parentType != null) {
      var descriptor = expr.parentType.getClassDescriptor();
      if (descriptor == null) {
        return null;
      }
      var classDecl = descriptor.getUserData(Stmt.ClassDecl.class);
      if (classDecl == null) {
        LOG.warn("ClassDescriptor for parent (" + descriptor.getPrettyName() + ") has no ClassDecl");
        return null;
      }
      String fieldName = psiElement.getText();
      JactlUserDataHolder decl = classDecl.fieldVars.get(fieldName);
      if (decl == null) {
        var funDecl = classDecl.methods.stream().filter(f -> f.declExpr.nameToken.getStringValue().equals(fieldName)).findFirst().orElse(null);
        if (funDecl == null) {
          // Look for built-in method
          var builtin = Functions.lookupMethod(expr.parentType, fieldName);
          if (builtin != null) {
            return psiElement;    // Nowhere to point to so just return current field so at least we don't get an error during highlighting
          }
          // Look for inner class
          var inner = classDecl.innerClasses.stream().filter(c -> c.name.getStringValue().equals(fieldName)).findFirst().orElse(null);
          if (inner == null) {
            return null;
          }
          return JactlUtils.getNameElementForPsiElementInTree(inner.getUserData(JactlAstKey.class));
        }
        decl = funDecl.declExpr.varDecl;
      }
      var key = decl.getUserData(JactlAstKey.class);
      return JactlUtils.getNameElementForPsiElementInTree(key);
    }

    // If we are part of a class path expression then see if we point to a valid class
    if (astNode instanceof Expr.ClassPath expr) {
      Stmt.ClassDecl classDecl = null;
      if (expr.type != null) {
        ClassDescriptor descriptor = expr.type.getClassDescriptor();
        if (descriptor != null) {
          classDecl = descriptor.getUserData(Stmt.ClassDecl.class);
        }
      }
      if (classDecl == null) {
        classDecl = JactlParserAdapter.getClassDecl(getElement().getProject(), expr.fullClassName());
      }
      if (classDecl == null) {
        return null;
      }
      return JactlUtils.getNameElementForPsiElementInTree(classDecl.getUserData(JactlAstKey.class));
    }

    if (astNode instanceof JactlType jactlType) {
      if (!jactlType.is(JactlType.CLASS,JactlType.INSTANCE)) {
        return null;
      }
      var descriptor = jactlType.getClassDescriptor();
      if (descriptor == null) {
        return null;
      }
      Stmt.ClassDecl classDecl = descriptor.getUserData(Stmt.ClassDecl.class);
      //Stmt.ClassDecl classDecl = JactlParserAdapter.getClassDecl(psiElement.getFile(), source, descriptor.getNamePath());
      if (classDecl == null) {
        LOG.warn("ClassDescriptor has no ClassDecl");
        return null;
      }
      JactlAstKey declarationKey = classDecl.getUserData(JactlAstKey.class);
      if (declarationKey == null) {
        LOG.warn("Declaration has no AST Key set: declaration=" + classDecl);
        return null;
      }

      return JactlUtils.getNameElementForPsiElementInTree(declarationKey);
    }

    // Key to node where varDecl or funDecl or classDecl is
    var declaration = astNode.getDeclaration();
    if (declaration == Resolver.ERROR_VARDECL) {
      return null;
    }

    if (declaration instanceof ClassDescriptor descriptor) {
      declaration = JactlParserAdapter.getClassDecl(psiElement.getFile(), source, descriptor.getNamePath());
    }

    // If we point to a class
    if (declaration instanceof Expr.VarDecl varDecl && varDecl.classDescriptor != null) {
      declaration = JactlParserAdapter.getClassDecl(psiElement.getFile(), source, varDecl.classDescriptor.getNamePath());
    }

    if (declaration == null) {
      // Special case for class name on its own as user types a VarDecl that is incomplete
      if (astNode instanceof Expr.Identifier expr && expr.type != null && expr.type.is(JactlType.CLASS)) {
        declaration = expr.type.getClassDescriptor().getUserData(Stmt.ClassDecl.class);
      }
      if (declaration == null) {
        return null;
      }
    }

    JactlAstKey declarationKey = declaration.getUserData(JactlAstKey.class);
    if (declarationKey == null) {
      LOG.warn("Declaration has no AST Key set: declaration=" + declaration);
      return null;
    }

    // Now we need to find the PSI element in our PSI tree
    return JactlUtils.getNameElementForPsiElementInTree(declarationKey);
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    PsiElement resolved = resolve();
    return new ResolveResult[] {
      new ResolveResult() {
        @Override public @Nullable PsiElement getElement() { return resolved; }
        @Override public boolean isValidResult()           { return resolved != null; }
      }
    };
  }
}
