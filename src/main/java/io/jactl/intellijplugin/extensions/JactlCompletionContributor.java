/*
 * Copyright © 2022,2023,2024  James Crawford
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.jactl.intellijplugin.extensions;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.util.ProcessingContext;
import io.jactl.*;
import io.jactl.intellijplugin.JactlAstKey;
import io.jactl.intellijplugin.JactlFile;
import io.jactl.intellijplugin.JactlParserAdapter;
import io.jactl.intellijplugin.JactlUtils;
import io.jactl.intellijplugin.common.JactlPlugin;
import io.jactl.intellijplugin.psi.*;
import io.jactl.intellijplugin.psi.impl.JactlPsiIdentifierExprImpl;
import io.jactl.intellijplugin.psi.impl.JactlPsiTypeImpl;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiExpr;
import io.jactl.intellijplugin.psi.interfaces.JactlPsiIdentifierExpr;
import io.jactl.runtime.ClassDescriptor;
import io.jactl.runtime.FunctionDescriptor;
import io.jactl.runtime.Functions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.jactl.intellijplugin.psi.JactlNameElementType.PARAMETER;

public class JactlCompletionContributor extends CompletionContributor {
  public static final Logger LOG = Logger.getInstance(JactlCompletionContributor.class);

  List<LookupElementBuilder> builtinTypeLookups = Stream.of(JactlUtils.BUILTIN_TYPES)
                                                        .map(type -> LookupElementBuilder.create(type).withIcon(AllIcons.Nodes.Type))
                                                        .toList();

  List<LookupElementBuilder> beginningKeywords = Stream.of(JactlUtils.BEGINNING_KEYWORDS)
                                                       .map(LookupElementBuilder::create)
                                                       .toList();

  public JactlCompletionContributor() {
    List<LookupElementBuilder> globalFunctionNames = Functions.getGlobalFunctionNames()
                                                              .stream()
                                                              .map(Functions::getGlobalFunDecl)
                                                              .map(JactlCompletionContributor::createFunctionLookup)
                                                              .toList();

    // We have an identifier in an expression not immediately after a '.' or '?.'
    // Add all visible variables/fields, functions/methods, global functions, and any class static functions we can find.
    // If the identifier is the only element in the expression then it could be the start of a variable declaration
    // so also add all builtin types and all classes and also add keywords that can begin a statement.
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(JactlTokenTypes.IDENTIFIER)
                                                 .withParent(JactlPsiIdentifierExprImpl.class)
                                                 .andNot(PlatformPatterns.psiElement().withAncestor(2, PlatformPatterns.psiElement(JactlPsiTypeImpl.class)))
                                                 .andNot(PlatformPatterns.psiElement().afterLeaf(".", "?.")),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
               JactlPsiElement element          = (JactlPsiElement) parameters.getPosition();
               JactlPsiElement parent           = (JactlPsiElement) element.getParent();
               JactlPsiElement       grandParent      = (JactlPsiElement) parent.getParent();
               List<ClassDescriptor> classDescriptors = JactlParserAdapter.getClasses(parent.getFile(), parent.getSourceCode(), grandParent.getAstKey());
               ClassDescriptor       owningClass      = JactlParserAdapter.getClass(parent.getFile(), parent.getSourceCode(), parent.getAstKey());
               List<ClassDescriptor> baseClasses = owningClass == null ? List.of()
                                                                       : Stream.concat(Stream.of(owningClass), JactlUtils.stream(owningClass, ClassDescriptor::getBaseClass)).toList();
               Runnable addTypes = () -> {
                 result.addAllElements(builtinTypeLookups);
                 result.addAllElements(classDescriptors.stream().map(JactlCompletionContributor::createLookup).toList());
               };

               // If we are only identifier in expr then we need to add built-in types and any visible
               // top level classes as user might be about to define a variable/function.
               PsiElement firstChild = JactlUtils.getFirstChild(grandParent, JactlExprElementType.IDENTIFIER);
               if (JactlUtils.isElementType(grandParent, JactlStmtElementType.EXPR_STMT, JactlStmtElementType.VAR_DECL) &&
                   firstChild == parent &&
                   JactlUtils.getNextSibling(parent, JactlTokenTypes.IDENTIFIER) == null) {
                 addTypes.run();
                 PsiElement classDecl        = JactlUtils.getAncestor(grandParent, JactlStmtElementType.CLASS_DECL);
                 PsiElement greatGrandParent = grandParent.getParent();
                 if (classDecl != null && greatGrandParent.getParent() == classDecl) {
                   // We are at field/method level of a class declaration so add "static" and "const" and
                   // "class" and then return as there are no other completions available
                   result.addElement(LookupElementBuilder.create(TokenType.STATIC.asString));
                   result.addElement(LookupElementBuilder.create(TokenType.CONST.asString));
                   result.addElement(LookupElementBuilder.create(TokenType.CLASS.asString));
                   return;
                 }
                 else if (JactlUtils.isElementType(grandParent, JactlStmtElementType.EXPR_STMT) &&
                          JactlUtils.getFirstChildNotWhiteSpace(grandParent) == parent) {
                   // If we are inside a statement block (not a class declaration) and we are at the start of the EXPR_STMT
                   // then also add statement-beginning keywords since we are at the start of a statement.
                   result.addAllElements(beginningKeywords);
                   // If top level of script add "class" to completion list
                   IElementType type = greatGrandParent.getNode().getElementType();
                   if (type == JactlNameElementType.JACTL_FILE || type instanceof IFileElementType) {
                     result.addElement(LookupElementBuilder.create(TokenType.CLASS.asString));
                     // If first element in script then allow 'package' in completions
                     PsiElement prevSibling = JactlUtils.getPrevSibling(grandParent);
                     if (prevSibling == null) {
                       result.addElement(LookupElementBuilder.create(TokenType.PACKAGE.asString));
                     }
                     // If prev was package or we are first then also allow 'import'
                     if (prevSibling == null || JactlUtils.isElementType(prevSibling, JactlNameElementType.PACKAGE, JactlStmtElementType.IMPORT_STMT)) {
                       result.addElement(LookupElementBuilder.create(TokenType.IMPORT.asString));
                     }
                   }
                 }
               }
               else if (JactlUtils.isElementType(parent, JactlExprElementType.IDENTIFIER) &&
                        JactlUtils.isElementType(JactlUtils.getPrevSibling(parent), JactlTokenTypes.LEFT_PAREN)) {
                 // Immediately after '(' so add types since this might be a cast
                 addTypes.run();
               }

               // Add global functions
               result.addAllElements(globalFunctionNames);

               // Add variables and functions visible
               result.addAllElements(JactlParserAdapter.getVariablesAndFunctions(parent.getFile(), parent.getSourceCode(), parent.getAstKey())
                                                       .stream()
                                                       .map(JactlCompletionContributor::createLookup)
                                                       .toList());

               // Add class static methods for other classes (not our class or one of our base classes)
               result.addAllElements(classDescriptors.stream()
                                                     .filter(c -> baseClasses.stream().noneMatch(baseClass -> c.getPackagedName().equals(baseClass.getPackagedName())))
                                                     .flatMap(descriptor -> descriptor.getAllMethods()
                                                                                      .filter(entry -> entry.getValue().isStatic)
                                                                                      .map(entry -> createStaticFunctionLookup(descriptor, entry.getValue())))
                                                     .toList());

               // Add any global variables if we are in a script
               JactlFile file = element.getFile();
               if (file.isScriptFile()) {
                 Map<String, Object> globals = JactlUtils.getGlobals(element.getProject());
                 if (globals != null) {
                   globals.keySet().forEach(key -> result.addElement(LookupElementBuilder.create(key)));
                 }
               }
             }
           });

    // Potential parameter type
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(JactlTokenTypes.IDENTIFIER)
                                                 .withParent(PlatformPatterns.psiElement(PARAMETER))
                                                 .andOr(PlatformPatterns.psiElement()
                                                                        .withAncestor(4, PlatformPatterns.psiElement(JactlStmtElementType.FUN_DECL)),
                                                        PlatformPatterns.psiElement()
                                                                        .withAncestor(4, PlatformPatterns.psiElement(JactlExprElementType.CLOSURE))),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
               // If we know we are a type then complete accordingly (built-ins plus package/classes) or if we are a
               // single identifier then we could be a parameter name or we might be a type since user could be about
               // to type the parameter name next.
               JactlPsiElement element = (JactlPsiElement) parameters.getPosition();
               if (JactlUtils.getPrevSibling(element.getParent()) == null) {
                 IElementType ancestorType = JactlUtils.getAncestor(element, JactlStmtElementType.FUN_DECL, JactlExprElementType.CLOSURE).getNode().getElementType();
                 addBuiltinsAndClasses(result, element, ancestorType, true, null);
               }
             }
           });

    // Class type without class path (no package prefix)
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(JactlTokenTypes.IDENTIFIER)
                                                 .andNot(PlatformPatterns.psiElement().withParent(PlatformPatterns.psiElement(JactlExprElementType.CLASS_PATH_EXPR)))
                                                 .withAncestor(2, PlatformPatterns.psiElement(JactlTypeElementType.CLASS_TYPE)),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
               JactlPsiElement element = (JactlPsiElement) parameters.getPosition();
               // If we know we are a class type then complete with known classes. Make sure to exclude current class
               // if we happen to be in an "extends".
               PsiElement classType       = JactlUtils.getAncestor(element, JactlTypeElementType.CLASS_TYPE);
               PsiElement prevToClassType = JactlUtils.getPrevSibling(classType);
               boolean    isExtends       = JactlUtils.isElementType(prevToClassType, JactlTokenTypes.EXTENDS);
               String className = isExtends ? JactlUtils.getPrevSibling(prevToClassType).getText() : "";
               addBuiltinsAndClasses(result, element, JactlTypeElementType.CLASS_TYPE, !isExtends, className);
             }
           });

    // Complete "extends"
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(JactlTokenTypes.IDENTIFIER)
                                                 .withParent(PlatformPatterns.psiElement(PsiErrorElement.class)
                                                                             .afterSibling(PlatformPatterns.psiElement(JactlNameElementType.CLASS)))
                                                 .withAncestor(2, PlatformPatterns.psiElement(JactlStmtElementType.CLASS_DECL)),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
               result.addElement(LookupElementBuilder.create(TokenType.EXTENDS.asString));
             }
           });

    // Class path like a.b.c.X: add classes and packages
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(JactlTokenTypes.IDENTIFIER)
                                                 .withParent(PlatformPatterns.psiElement(JactlExprElementType.CLASS_PATH_EXPR)),
           new CompletionProvider<CompletionParameters>() {
             @Override protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
               JactlPsiElement element = (JactlPsiElement)parameters.getPosition();
               // Build package path for preceding nodes
               StringBuilder pathBuilder = new StringBuilder();
               for (PsiElement child = element.getParent().getFirstChild(); child != element; child = child.getNextSibling()) {
                 pathBuilder.append(child.getText());
               }
               String path = JactlPlugin.removeSuffix(pathBuilder.toString());
               if (JactlUtils.pkgNames(element.getProject()).contains(path)) {
                 // Package name so complete with all sub packages and classes in this package
                 result.addAllElements(JactlUtils.packageContents(element.getProject(), path)
                                                 .stream()
                                                 .map(entry -> createClassOrPackageLookup(path, entry))
                                                 .toList());
               }
             }
           });

    // Type after "const": we only support simple types for consts (boolean, byte, int, long, double, Decimal, String)
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(JactlTokenTypes.IDENTIFIER)
                                                 .afterLeaf(TokenType.CONST.asString),
           new CompletionProvider<CompletionParameters>() {
             @Override protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
               result.addAllElements(Stream.of(JactlUtils.SIMPLE_TYPES)
                                           .map(type -> LookupElementBuilder.create(type).withIcon(AllIcons.Nodes.Type))
                                           .toList());
             }
           });


    // Look for expressions of the form something.x
    // Need to get type of left hand side so that we can work out what completions to offer
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(JactlTokenTypes.IDENTIFIER)
                                                 .withParent(JactlPsiIdentifierExpr.class)
                                                 .withAncestor(2, PlatformPatterns.psiElement(JactlExprElementType.BINARY_EXPR))
                                                 .afterLeaf(".", "?."),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
               JactlPsiElement element = (JactlPsiElement)parameters.getPosition();
               PsiElement      lhs     = element.getParent().getParent();
               if (lhs instanceof JactlPsiExpr) {
                 JactlPsiExpr        expr      = (JactlPsiExpr) lhs;
                 JactlUserDataHolder jactlNode = expr.getJactlAstNode();
                 if (jactlNode instanceof Expr.Binary && ((Expr.Binary) jactlNode).left.type != null) {
                   Expr.Binary     jactlExpr       = (Expr.Binary) jactlNode;
                   JactlType       type            = jactlExpr.left.type;
                   ClassDescriptor classDescriptor = type.getClassDescriptor();
                   if (type.is(JactlType.INSTANCE, JactlType.CLASS) && type.getClassDescriptor() != null) {
                     // Add methods if instance or static if class
                     result.addAllElements(classDescriptor.getAllMethods()
                                                          .filter(entry -> !entry.getKey().startsWith(Utils.JACTL_PREFIX))
                                                          .filter(entry -> type.is(JactlType.INSTANCE) || entry.getValue().isStatic)
                                                          .map(Map.Entry::getValue)
                                                          .map(JactlCompletionContributor::createFunctionLookup)
                                                          .toList());
                     // Add fields if instance
                     if (type.is(JactlType.INSTANCE)) {
                       result.addAllElements(classDescriptor.getAllFieldsStream()
                                                            .map(entry -> LookupElementBuilder.create(entry.getKey())
                                                                                              .withTypeText(entry.getValue().toString()))
                                                            .toList());
                     }
                     else {
                       // Add inner classes if class
                       result.addAllElements(classDescriptor.getInnerClasses()
                                                            .stream()
                                                            .map(JactlCompletionContributor::createLookup)
                                                            .toList());
                     }
                   }
                   // Add any builtin methods based on type or on last assigned type if we have a variable
                   if (!type.is(JactlType.CLASS)) {
                     List<FunctionDescriptor> builtinMethods = Functions.getAllMethods(type);
                     result.addAllElements(builtinMethods.stream()
                                                         .map(JactlCompletionContributor::createFunctionLookup)
                                                         .toList());
                     // Add any additional methods based on last type assigned to variable
                     if (jactlExpr.left instanceof Expr.Identifier) {
                       Expr.Identifier identifier       = (Expr.Identifier) jactlExpr.left;
                       JactlType       lastAssignedType = identifier.varDecl != null ? identifier.varDecl.lastAssignedType : null;
                       if (lastAssignedType != null && !type.equals(lastAssignedType)){
                         List<FunctionDescriptor> newMethods = Functions.getAllMethods(lastAssignedType).stream().filter(f -> builtinMethods.stream().noneMatch(b -> b.name.equals(f.name))).toList();
                         result.addAllElements(newMethods.stream().map(JactlCompletionContributor::createFunctionLookup).toList());
                       }
                     }

                   }
                 }
               }
             }
           });

    // Import statements: first identifier
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(JactlTokenTypes.IDENTIFIER)
                                                 .withParent(PlatformPatterns.psiElement(JactlStmtElementType.IMPORT_STMT)),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
               JactlPsiElement element = (JactlPsiElement) parameters.getPosition();
               // Add top level packages and 'static' keyword
               result.addAllElements(JactlUtils.packageContents(element.getProject(), "")
                                               .stream()
                                               .filter(entry -> entry.isPackage())
                                               .map(entry -> createClassOrPackageLookup("", entry))
                                               .toList());
               // If we don't already have 'static' then add it
               if (JactlUtils.getFirstChild(element.getParent(), JactlTokenTypes.STATIC) == null) {
                 result.addElement(LookupElementBuilder.create(TokenType.STATIC.asString));
               }
             }
           });

    // Import statements
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(JactlTokenTypes.IDENTIFIER)
                                                 .withParent(JactlPsiIdentifierExprImpl.class)
                                                 .withSuperParent(2, PlatformPatterns.psiElement(JactlStmtElementType.IMPORT_STMT)),
           new CompletionProvider<CompletionParameters>() {
             @Override protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
               JactlPsiElement element = (JactlPsiElement)parameters.getPosition();
               JactlPsiElement importStmt = (JactlPsiElement)element.getParent().getParent();
               // Build package path for preceding nodes
               List<String> path = new ArrayList<>();
               for (PsiElement child = JactlUtils.getFirstChild(importStmt, JactlPsiIdentifierExprImpl.class); child != element.getParent(); child = child.getNextSibling()) {
                 if (child instanceof JactlPsiIdentifierExprImpl) {
                   path.add(child.getText());
                 }
               }
               // Check for ClassPath
               JactlPsiElement classPathElement = (JactlPsiElement) JactlUtils.getFirstChild(importStmt, JactlExprElementType.CLASS_PATH_EXPR);
               if (classPathElement == null) {
                 String packageName = String.join(".'", path);
                 if (JactlUtils.pkgNames(element.getProject()).contains(packageName)) {
                   // Package name so complete with all sub packages and classes in this package
                   result.addAllElements(JactlUtils.packageContents(element.getProject(), packageName)
                                                   .stream()
                                                   .map(entry -> createClassOrPackageLookup(packageName, entry))
                                                   .toList());
                 }
               }
               else {
                 Expr.ClassPath astNode     = (Expr.ClassPath)classPathElement.getJactlAstNode();
                 String         parentClass = astNode.fullClassName();
                 Stmt.ClassDecl classDecl = JactlParserAdapter.getClassDecl(element.getProject(), parentClass);
                 // Find the inner class we are at based on current path
                 for (String next: path) {
                   classDecl = classDecl == null ? null : classDecl.innerClasses.stream().filter(inner -> inner.name.getStringValue().equals(next)).findFirst().orElse(null);
                   if (classDecl == null) {
                     return;
                   }
                 }
                 if (classDecl != null) {
                   result.addAllElements(classDecl.innerClasses.stream()
                                                               .map(inner -> createLookup(inner.name.getStringValue(), parentClass))
                                                               .toList());
                   if (JactlUtils.getFirstChild(importStmt, JactlTokenTypes.STATIC) != null) {
                     result.addAllElements(classDecl.methods.stream()
                                                            .map(funDecl -> funDecl.declExpr)
                                                            .filter(Expr.FunDecl::isStatic)
                                                            .map(funDecl -> funDecl.varDecl)
                                                            .map(decl -> createLookup(decl, false))
                                                            .toList());
                     result.addAllElements(classDecl.fields.stream()
                                                           .map(decl -> decl.declExpr)
                                                           .filter(declExpr -> declExpr.isConstVar)
                                                           .map(JactlCompletionContributor::createLookup)
                                                           .toList());
                   }
                 }
               }
             }
           });

    // Package statement
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(JactlTokenTypes.IDENTIFIER)
                                                 .withParent(PlatformPatterns.psiElement(JactlNameElementType.PACKAGE)),
           new CompletionProvider<CompletionParameters>() {
             @Override protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
               JactlPsiElement element = (JactlPsiElement)parameters.getPosition();
               // Build package path for preceding nodes
               String packageName = JactlUtils.parentPackage(element, ".");
               if (packageName.isEmpty() || JactlUtils.pkgNames(element.getProject()).contains(packageName)) {
                 // Package name so complete with all sub packages in this package
                 result.addAllElements(JactlUtils.packageContents(element.getProject(), packageName)
                                                 .stream()
                                                 .filter(JactlUtils.PackageEntry::isPackage)
                                                 .map(entry -> createClassOrPackageLookup(packageName, entry))
                                                 .toList());
               }
             }
           });

  }

  private void addBuiltinsAndClasses(CompletionResultSet result, JactlPsiElement element, IElementType parentType, boolean includeBuiltins, String excludeName) {
    if (includeBuiltins) {
      result.addAllElements(builtinTypeLookups);
    }
    JactlFile   file       = element.getFile();
    JactlAstKey astKey     = ((JactlPsiElement) JactlUtils.getAncestor(element, parentType)).getAstKey();
    String      sourceCode = element.getSourceCode();
    result.addAllElements(JactlParserAdapter.getClasses(file, sourceCode, astKey)
                                            .stream()
                                            .filter(descriptor -> !descriptor.getClassName().equals(excludeName))
                                            .map(JactlCompletionContributor::createLookup)
                                            .toList());
  }

  private static LookupElementBuilder createLookup(String name, String text) {
    return LookupElementBuilder.create(name)
                               .withTypeText(text)
                               .withIcon(AllIcons.Nodes.Class);
  }

  private static LookupElementBuilder createLookup(Object obj) {
    return createLookup(obj, true);
  }

  private static LookupElementBuilder createLookup(Object obj, boolean addParentheses) {
    if (obj instanceof Expr.VarDecl) {
      Expr.VarDecl varDecl = (Expr.VarDecl) obj;
      if (varDecl.funDecl != null) {
        return createFunctionLookup(varDecl, addParentheses);
      }
      return LookupElementBuilder.create(varDecl.name.getStringValue())
                                 .withTypeText(varDecl.type.toString())
                                 .withIcon(AllIcons.Nodes.Variable);
    }

    if (obj instanceof JactlParserAdapter.FieldDescriptor) {
      JactlParserAdapter.FieldDescriptor field = (JactlParserAdapter.FieldDescriptor) obj;
      return LookupElementBuilder.create(field.name())
                                 .withTypeText(field.type().toString())
                                 .withIcon(AllIcons.Nodes.Variable);
    }

    if (obj instanceof FunctionDescriptor) {
      FunctionDescriptor funcDesc = (FunctionDescriptor) obj;
      return createFunctionLookup(funcDesc);
    }

    // Must be a class
    if (obj instanceof ClassDescriptor) {
      ClassDescriptor descriptor = (ClassDescriptor) obj;
      return LookupElementBuilder.create(descriptor.getClassName())
                                 .withTypeText(descriptor.getPackageName())
                                 .withIcon(AllIcons.Nodes.Class);
    }

    LOG.warn("Unexpected type for completion: " + obj.getClass().getName());
    return LookupElementBuilder.create(obj.getClass().getName());
  }

  private static LookupElementBuilder createFunctionLookup(Expr.VarDecl f) {
    return createFunctionLookup(f, true);
  }

  private static LookupElementBuilder createFunctionLookup(Expr.VarDecl f, boolean addParentheses) {
    LookupElementBuilder lookup = LookupElementBuilder.create(f.name.getStringValue())
                                                      .appendTailText(JactlUtils.getParameterText(f.funDecl), true)
                                                      .withTypeText(f.funDecl.returnType.toString())
                                                      .withIcon(AllIcons.Nodes.Function);
    if (addParentheses) {
      lookup = lookup.withInsertHandler((context, item) -> {
        context.getDocument().insertString(context.getTailOffset(), hasMandatoryParams(f) ? "(" : "()");
        context.getEditor().getCaretModel().moveToOffset(context.getTailOffset());
        context.commitDocument();
      });
    }
    return lookup;
  }

  private static boolean hasMandatoryParams(Expr.VarDecl f) {
    return f.funDecl.parameters.stream().anyMatch(p -> p.declExpr.initialiser instanceof Expr.Noop && ((Expr.Noop) p.declExpr.initialiser).originalExpr == null);
  }

  private static LookupElementBuilder createFunctionLookup(FunctionDescriptor f) {
    return createFunctionLookupWithPrefix(null, f);
  }
  private static LookupElementBuilder createFunctionLookupWithPrefix(String prefix, FunctionDescriptor f) {
    return LookupElementBuilder.create(prefix == null ? f.name : prefix + "." + f.name)
                               .appendTailText("(" +
                                               IntStream.range(0,f.paramNames.size())
                                                        .mapToObj(i -> f.paramNames.get(i) + " " + f.paramTypes.get(i).toString())
                                                        .collect(Collectors.joining(", "))
                                               + ")", true)
                               .withTypeText(f.returnType.toString())
                               .withIcon(AllIcons.Nodes.Function)
                               .withInsertHandler((context, item) -> {
                                 context.getDocument().insertString(context.getTailOffset(), f.mandatoryParams.isEmpty() ? "()" : "(");
                                 context.getEditor().getCaretModel().moveToOffset(context.getTailOffset());
                                 context.commitDocument();
                               });
  }

  private static LookupElementBuilder createStaticFunctionLookup(ClassDescriptor owningClass, FunctionDescriptor f) {
    return createFunctionLookupWithPrefix(owningClass.getClassName(), f);
  }

  private static LookupElementBuilder createClassOrPackageLookup(String packageName, JactlUtils.PackageEntry entry) {
    if (entry.isPackage()) {
      return LookupElementBuilder.create(entry.name())
                                 .withIcon(AllIcons.Nodes.Package);
    }
    return LookupElementBuilder.create(entry.name())
                               .withTypeText(packageName)
                               .withIcon(AllIcons.Nodes.Class);
  }

  private static JactlType getType(JactlUserDataHolder node) {
    if (node instanceof JactlType) {
      JactlType type = (JactlType) node;
      return type;
    }
    if (node instanceof Expr) {
      Expr expr = (Expr) node;
      return expr.type;
    }
    return null;
  }
}
