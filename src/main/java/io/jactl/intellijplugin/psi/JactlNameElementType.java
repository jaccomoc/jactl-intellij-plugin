package io.jactl.intellijplugin.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import io.jactl.JactlName;
import io.jactl.intellijplugin.JactlLanguage;

public class JactlNameElementType extends IElementType {

  public static IElementType         JACTL_FILE = new IFileElementType("FILE", JactlLanguage.INSTANCE);
  public static JactlNameElementType CLASS      = new JactlNameElementType("CLASS");
  public static JactlNameElementType PACKAGE    = new JactlNameElementType("PACKAGE");
  public static JactlNameElementType FUNCTION   = new JactlNameElementType("FUNCTION");
  public static JactlNameElementType METHOD     = new JactlNameElementType("METHOD");
  public static JactlNameElementType PARAMETER  = new JactlNameElementType("PARAMETER");
  public static JactlNameElementType VARIABLE   = new JactlNameElementType("VARIABLE");

  public JactlNameElementType(String name) {
    super(name, JactlLanguage.INSTANCE);
  }

  public static IElementType getElementType(JactlName.NameType nameType) {
    return switch (nameType) {
      case FILE      -> JACTL_FILE;
      case PACKAGE   -> PACKAGE;
      case CLASS     -> CLASS;
      case FUNCTION  -> FUNCTION;
      case METHOD    -> METHOD;
      case VARIABLE  -> VARIABLE;
      case PARAMETER -> PARAMETER;
    };
  }

  public static JactlName.NameType getNameType(JactlNameElementType type) {
    if (type == JACTL_FILE) return JactlName.NameType.FILE;
    if (type == PACKAGE)    return JactlName.NameType.PACKAGE;
    if (type == CLASS)      return JactlName.NameType.CLASS;
    if (type == FUNCTION)   return JactlName.NameType.FUNCTION;
    if (type == METHOD)     return JactlName.NameType.METHOD;
    if (type == VARIABLE)   return JactlName.NameType.VARIABLE;
    if (type == PARAMETER)  return JactlName.NameType.PARAMETER;
    throw new IllegalStateException("Unknown type " + type);
  }
}
