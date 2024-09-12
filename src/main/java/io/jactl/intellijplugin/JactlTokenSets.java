package io.jactl.intellijplugin;

import com.intellij.psi.tree.TokenSet;
import io.jactl.intellijplugin.psi.JactlTokenTypes;

public interface JactlTokenSets {
  TokenSet IDENTIFIERS     = TokenSet.create(JactlTokenTypes.IDENTIFIER, JactlTokenTypes.DOLLAR_IDENTIFIER);
  TokenSet STRING_LITERALS = TokenSet.create(JactlTokenTypes.STRING_CONST);
  TokenSet COMMENT         = TokenSet.create(JactlTokenTypes.COMMENT);
  TokenSet WHITESPACE      = TokenSet.create(JactlTokenTypes.WHITESPACE);
}
