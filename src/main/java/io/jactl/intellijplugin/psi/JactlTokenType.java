package io.jactl.intellijplugin.psi;

import com.intellij.psi.tree.IElementType;
import io.jactl.TokenType;
import io.jactl.intellijplugin.JactlLanguage;

import java.util.HashMap;
import java.util.Map;

public class JactlTokenType extends IElementType {

  static Map<TokenType,IElementType> tokenTypeMap = new HashMap<>();

  public TokenType tokenType;

  public JactlTokenType(String debugName, TokenType tokenType) {
    super(debugName, JactlLanguage.INSTANCE);
    this.tokenType = tokenType;
    tokenTypeMap.put(tokenType, this);
  }

  public static IElementType getIElementType(TokenType type) {
    return tokenTypeMap.get(type);
  }

  @Override public String toString() {
    return "JactlTokenType." + super.toString();
  }
}
