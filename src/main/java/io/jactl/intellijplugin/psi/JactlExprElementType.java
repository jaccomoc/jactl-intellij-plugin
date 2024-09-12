package io.jactl.intellijplugin.psi;

import com.intellij.psi.tree.IElementType;
import io.jactl.Expr;
import io.jactl.intellijplugin.JactlLanguage;

public class JactlExprElementType extends IElementType {
  public static IElementType TYPE                = new JactlExprElementType("TYPE");
  public static IElementType IDENTIFIER          = new JactlExprElementType("IDENTIFIER");
  public static IElementType VAR_DECL_EXPR       = new JactlExprElementType("VAR_DECL_EXPR");
  public static IElementType FUN_DECL_EXPR       = new JactlExprElementType("FUN_DECL_EXPR");
  public static IElementType VAR_ASSIGN          = new JactlExprElementType("VAR_ASSIGN");
  public static IElementType VAR_OP_ASSIGN       = new JactlExprElementType("VAR_OP_ASSIGN");
  public static IElementType FIELD_ASSIGN_EXPR   = new JactlExprElementType("FIELD_ASSIGN_EXPR");
  public static IElementType FIELD_OP_ASSIGN_EXPR= new JactlExprElementType("FIELD_OP_ASSIGN_EXPR");
  public static IElementType LITERAL             = new JactlExprElementType("LITERAL");
  public static IElementType LIST_LITERAL        = new JactlExprElementType("LIST_LITERAL");
  public static IElementType MAP_LITERAL         = new JactlExprElementType("MAP_LITERAL");
  public static IElementType BINARY_EXPR         = new JactlExprElementType("BINARY_EXPR");
  public static IElementType TERNARY_EXPR        = new JactlExprElementType("TERNARY_EXPR");
  public static IElementType EXPR_STRING         = new JactlExprElementType("EXPR_STRING");
  public static IElementType REGEX_MATCH         = new JactlExprElementType("REGEX_MATCH");
  public static IElementType REGEX_SUBST         = new JactlExprElementType("REGEX_SUBST");
  public static IElementType CAST_EXPR           = new JactlExprElementType("CAST_EXPR");
  public static IElementType PREFIX_UNARY_EXPR   = new JactlExprElementType("PREFIX_UNARY_EXPR");
  public static IElementType POSTFIX_UNARY_EXPR  = new JactlExprElementType("POSTFIX_UNARY_EXPR");
  public static IElementType CALL_EXPR           = new JactlExprElementType("CALL_EXPR");
  public static IElementType METHOD_CALL_EXPR    = new JactlExprElementType("METHOD_CALL_EXPR");
  public static IElementType CLASS_PATH_EXPR     = new JactlExprElementType("CLASS_PATH_EXPR");
  public static IElementType CLOSURE             = new JactlExprElementType("CLOSURE");
  public static IElementType PRINT_EXPR          = new JactlExprElementType("PRINT_EXPR");
  public static IElementType BREAK_EXPR          = new JactlExprElementType("BREAK_EXPR");
  public static IElementType CONTINUE_EXPR       = new JactlExprElementType("CONTINUE_EXPR");
  public static IElementType RETURN_EXPR         = new JactlExprElementType("RETURN_EXPR");
  public static IElementType DIE_EXPR            = new JactlExprElementType("DIE_EXPR");
  public static IElementType EVAL_EXPR           = new JactlExprElementType("EVAL_EXPR");
  public static IElementType BLOCK_EXPR          = new JactlExprElementType("BLOCK_EXPR");
  public static IElementType TYPE_EXPR           = new JactlExprElementType("TYPE_EXPR");
  public static IElementType EXPR_LIST           = new JactlExprElementType("EXPR_LIST");
  public static IElementType INVOKE_NEW          = new JactlExprElementType("INVOKE_NEW");
  public static IElementType SWITCH_EXPR         = new JactlExprElementType("SWITCH_EXPR");
  public static IElementType SWITCH_CASE_EXPR    = new JactlExprElementType("SWITCH_CASE_EXPR");
  public static IElementType SWITCH_CONSTRUCTOR_PATTERN_EXPR = new JactlExprElementType("SWITCH_CONSTRUCTOR_PATTERN_EXPR");

  public JactlExprElementType(String name) {
    super(name, JactlLanguage.INSTANCE);
  }
}
