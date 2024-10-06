package io.jactl.intellijplugin;

import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ParsingTests extends BasePlatformTestCase {

  private PsiFile psiFile;

  @Override
  protected String getTestDataPath() {
    return "src/test";
  }

  private void configureWith(String text) {
    psiFile = myFixture.configureByText("script.jactl", "def f() {\n}\n");
  }

  private String getPsiTree() {
    return DebugUtil.psiToString(psiFile, true).replaceAll("0x[0-9a-f]*","");
  }

  public void testSimple() {
    configureWith("def f() {\n}\n");
    assertEquals("Jactl File[]\n" +
                 "  JactlPsiDeclarationStmtImpl(FUN_DECL)\n" +
                 "    JactlPsiTypeImpl(BUILT_IN_TYPE)\n" +
                 "      PsiElement(JactlTokenType.DEF)('def')\n" +
                 "    PsiWhiteSpace(' ')\n" +
                 "    JactlPsiNameImpl(FUNCTION)\n" +
                 "      PsiElement(JactlTokenType.IDENTIFIER)('f')\n" +
                 "    PsiElement(JactlTokenType.LEFT_PAREN)('(')\n" +
                 "    JactlPsiListImpl(LIST)\n" +
                 "      PsiElement(JactlTokenType.RIGHT_PAREN)(')')\n" +
                 "    PsiWhiteSpace(' ')\n" +
                 "    JactlPsiStmtImpl(BLOCK)\n" +
                 "      PsiElement(JactlTokenType.LEFT_BRACE)('{')\n" +
                 "      PsiWhiteSpace('\\n')\n" +
                 "      PsiElement(JactlTokenType.RIGHT_BRACE)('}')\n" +
                 "  PsiWhiteSpace('\\n')\n",
                 getPsiTree());
  }

  public void testBadRegex()             { configureWith("/x/inQ"); }
  public void testGoodRegex()            { configureWith("/x/i"); }
  public void testDoUntil()              { configureWith("do{}until(true)"); }
  public void testBackslashEOFInString() { configureWith("def a = \"{\\"); }

  public static final String ALL_SYMBOL_EXAMPLE =
    """
    package a.b.c
    import x.y.z.Base
    import static x.y.z.Base.FFF as GGG
    class CCC extends Base {
      const CONST = 123
      final def f(int i, long j, double k, Decimal d, String sss = '', String sss2 = "") {
        String s = \"""
          ${'embedded' + 'string'} : $x : ${x}\"""
        String s2 = '''
           "another string"
        '''
        Map m = [a:1, for:2, while:3]
        List l = /* comment */ [1,2,3]
        boolean b = !true && false || l.size() == 3
        int x = 1 ^ 2 | 3 & 4 & ~7  // comment
        s =~ /[abc]*.*(123)/
        s =~ /[abc]*.*(123)/i
        x = x % 5 %% 7 * i / j - k + d
        x == 7 || x < 8 && x <= 9 || x > 10 && x >= 11 and x++ or x--
        not x or x += 7
        x = true ? 8 : 9
        for (int ii = 0; ii < 10; ii++) {}
        x = switch (l) {
          [_,_,_] -> 3
          default -> 7
        }
        x = ?? m.filter{ k,v -> k == v }.size()
        if (x != 3 && (x ?: 7) < 10) x -= 3
        x += 4; x *= 7; x /= 2; x &= 15; x |= 12; x ^= 13
        x ?= l.size()
        x = x << 2
        x = x <<< 3
        x <<= 4
        x <<<= 5
        x = x >> 2
        x >>= 3
        boolean bbb = s =~ /abc/ || s !~ /xyz/ || d === d
        l.sort{ a,b -> a <=> b }
        s =~ /a(b)c/ and $1 == 'b' and return 7
        byte b = s == null ? 1 : 2
        j = 12335L + 0x1234567L
        k = 123.0D
        d = 123.456
        s =~ s/abc/xyz/g
        var g = 123.0
        Object o = g
        if (x < 3) { x++ } else { x-- }
        while (true) { do { x-- } until (x < 0) }
        for (int iii = 0; iii < 10; iii++) {
          iii % 2 or break
          continue unless iii > 9
        }
        if (3 in [1,2,3] || 3 !in [4,5,6]) {
          CCC ccc = new CCC()
          print x
          println "x=$x"
          die 'dead' unless x instanceof CCC && x !instanceof Base
        }
        eval('print x', [x:x])
      }
    }
    BEGIN { println 'begin' }
    END   { println 'end'   }
    """;

  public void testAllSymbols() {
    String text = ALL_SYMBOL_EXAMPLE;
    myFixture.configureByText("script.jactl", text);
  }
}
