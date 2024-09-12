package io.jactl.intellijplugin.psi;

import com.intellij.psi.tree.IElementType;
import io.jactl.intellijplugin.JactlLanguage;

public class JactlStmtElementType extends IElementType {
  public static IElementType IMPORT_STMT   = new JactlStmtElementType("IMPORT_STMT");
  public static IElementType CLASS_DECL    = new JactlStmtElementType("CLASS_DECL");
  public static IElementType BLOCK         = new JactlStmtElementType("BLOCK");
  public static IElementType PARAMS        = new JactlStmtElementType("PARAMS");
  public static IElementType EXPR_STMT     = new JactlStmtElementType("EXPR_STMT");
  public static IElementType VAR_DECL      = new JactlStmtElementType("VAR_DECL");
  public static IElementType FUN_DECL      = new JactlStmtElementType("FUN_DECL");
  public static IElementType IF_STMT       = new JactlStmtElementType("IF_STMT");
  public static IElementType WHILE_STMT    = new JactlStmtElementType("WHILE_STMT");
  public static IElementType FOR_STMT      = new JactlStmtElementType("FOR_STMT");
  public static IElementType DO_UNTIL_STMT = new JactlStmtElementType("DO_WHILE_STMT");
  public static IElementType RETURN_STM    = new JactlStmtElementType("RETURN_STMT");

  public JactlStmtElementType(String name) {
    super(name, JactlLanguage.INSTANCE);
  }
}
