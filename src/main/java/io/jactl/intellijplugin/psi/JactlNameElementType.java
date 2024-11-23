package io.jactl.intellijplugin.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import io.jactl.JactlName;
import io.jactl.intellijplugin.JactlFileElementType;
import io.jactl.intellijplugin.JactlLanguage;

public class JactlNameElementType extends IElementType {

  public static IFileElementType     JACTL_FILE = new JactlFileElementType();
  public static JactlNameElementType CLASS      = new JactlNameElementType("CLASS");
  public static JactlNameElementType PACKAGE    = new JactlNameElementType("PACKAGE");
  public static JactlNameElementType FUNCTION   = new JactlNameElementType("FUNCTION");
  public static JactlNameElementType METHOD     = new JactlNameElementType("METHOD");
  public static JactlNameElementType PARAMETER  = new JactlNameElementType("PARAMETER");
  public static JactlNameElementType VARIABLE   = new JactlNameElementType("VARIABLE");
  public static JactlNameElementType FIELD      = new JactlNameElementType("FIELD");

  public JactlNameElementType(String name) {
    super(name, JactlLanguage.INSTANCE);
  }

  public static IElementType getElementType(JactlName.NameType nameType) {
    switch (nameType) {
      case FILE:      return JACTL_FILE;
      case PACKAGE:   return PACKAGE;
      case CLASS:     return CLASS;
      case FUNCTION:  return FUNCTION;
      case METHOD:    return METHOD;
      case VARIABLE:  return VARIABLE;
      case FIELD:     return FIELD;
      case PARAMETER: return PARAMETER;
      default:        throw new IllegalArgumentException();
    }
  }

  public static JactlName.NameType getNameType(JactlNameElementType type) {
    if (type == PACKAGE)    return JactlName.NameType.PACKAGE;
    if (type == CLASS)      return JactlName.NameType.CLASS;
    if (type == FUNCTION)   return JactlName.NameType.FUNCTION;
    if (type == METHOD)     return JactlName.NameType.METHOD;
    if (type == VARIABLE)   return JactlName.NameType.VARIABLE;
    if (type == FIELD)      return JactlName.NameType.FIELD;
    if (type == PARAMETER)  return JactlName.NameType.PARAMETER;
    throw new IllegalStateException("Unknown type " + type);
  }
}
