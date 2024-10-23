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
    "package a.b.c\n" +
    "import x.y.z.Base\n" +
    "import static x.y.z.Base.FFF as GGG\n" +
    "class CCC extends Base {\n" +
    "  const CONST = 123\n" +
    "  final def f(int i, long j, double k, Decimal d, String sss = '', String sss2 = \"\") {\n" +
    "    String s = \"\"\"\n" +
    "      ${'embedded' + 'string'} : $x : ${x}\"\"\"\n" +
    "    String s2 = '''\n" +
    "       \"another string\"\n" +
    "    '''\n" +
    "    Map m = [a:1, for:2, while:3]\n" +
    "    List l = /* comment */ [1,2,3]\n" +
    "    boolean b = !true && false || l.size() == 3\n" +
    "    int x = 1 ^ 2 | 3 & 4 & ~7  // comment\n" +
    "    s =~ /[abc]*.*(123)/\n" +
    "    s =~ /[abc]*.*(123)/i\n" +
    "    x = x % 5 %% 7 * i / j - k + d\n" +
    "    x == 7 || x < 8 && x <= 9 || x > 10 && x >= 11 and x++ or x--\n" +
    "    not x or x += 7\n" +
    "    x = true ? 8 : 9\n" +
    "    for (int ii = 0; ii < 10; ii++) {}\n" +
    "    x = switch (l) {\n" +
    "      [_,_,_] -> 3\n" +
    "      default -> 7\n" +
    "    }\n" +
    "    x = ?? m.filter{ k,v -> k == v }.size()\n" +
    "    if (x != 3 && (x ?: 7) < 10) x -= 3\n" +
    "    x += 4; x *= 7; x /= 2; x &= 15; x |= 12; x ^= 13\n" +
    "    x ?= l.size()\n" +
    "    x = x << 2\n" +
    "    x = x <<< 3\n" +
    "    x <<= 4\n" +
    "    x <<<= 5\n" +
    "    x = x >> 2\n" +
    "    x >>= 3\n" +
    "    boolean bbb = s =~ /abc/ || s !~ /xyz/ || d === d\n" +
    "    l.sort{ a,b -> a <=> b }\n" +
    "    s =~ /a(b)c/ and $1 == 'b' and return 7\n" +
    "    byte b = s == null ? 1 : 2\n" +
    "    j = 12335L + 0x1234567L\n" +
    "    k = 123.0D\n" +
    "    d = 123.456\n" +
    "    s =~ s/abc/xyz/g\n" +
    "    var g = 123.0\n" +
    "    Object o = g\n" +
    "    if (x < 3) { x++ } else { x-- }\n" +
    "    while (true) { do { x-- } until (x < 0) }\n" +
    "    for (int iii = 0; iii < 10; iii++) {\n" +
    "      iii % 2 or break\n" +
    "      continue unless iii > 9\n" +
    "    }\n" +
    "    if (3 in [1,2,3] || 3 !in [4,5,6]) {\n" +
    "      CCC ccc = new CCC()\n" +
    "      print x\n" +
    "      println \"x=$x\"\n" +
    "      die 'dead' unless x instanceof CCC && x !instanceof Base\n" +
    "    }\n" +
    "    eval('print x', [x:x])\n" +
    "  }\n" +
    "}\n" +
    "BEGIN { println 'begin' }\n" +
    "END   { println 'end'   }\n";

  public void testAllSymbols() {
    String text = ALL_SYMBOL_EXAMPLE;
    myFixture.configureByText("script.jactl", text);
  }
}
