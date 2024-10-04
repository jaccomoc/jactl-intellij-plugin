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

package io.jactl.intellijplugin;

import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.tree.IElementType;
import io.jactl.*;
import io.jactl.intellijplugin.psi.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.IntStream;

public class JactlTokenBuilder extends BuilderImpl {

  public interface Event {
    boolean isToken();
    boolean isDropped();
    Token getToken();
    JactlMarker getMarker();
    boolean isStart();
  }

  public static class TokenEvent implements Event {
    Token token;

    TokenEvent(Token token) {
      this.token = token;
    }

    @Override public boolean isStart()       { return false; }
    @Override public boolean isDropped()     { return false; }
    @Override public boolean isToken()       { return true; }
    @Override public String toString()       { return "TOKEN<" + token + ">"; }
    @Override public Token getToken()        { return token; }
    @Override public JactlMarker getMarker() { return null; }
  }

  public static class MarkerEvent implements Event {
    JactlMarker marker;
    public enum State {START, END};
    public MarkerEvent.State state;
    MarkerEvent(JactlMarker marker, MarkerEvent.State state) {
      this.marker = marker;
      this.state = state;
      //new Exception(this + ": Stack trace").printStackTrace();
    }

    @Override public boolean isStart()       { return state == State.START; }
    @Override public boolean isDropped()     { return marker.dropped; }
    @Override public boolean isToken()       { return false; }
    @Override public Token getToken()        { return null; }
    @Override public JactlMarker getMarker() { return marker; }
    @Override public String toString()       { return (marker.dropped ? "DROPPED" : marker.type) + "[" + marker.id + "]<" + state + ">"; }
  }

  int                   counter = 0;
  int                   indent  = 0;
  List<Event>           events  = new ArrayList<>();
  LinkedHashSet<String> errors  = new LinkedHashSet<>();

  public JactlTokenBuilder(Tokeniser tokeniser) {
    super(tokeniser);
  }

  void pushEvent(Event evt)  { events.add(evt); }
  Event popEvent()           { return events.remove(events.size() - 1); }

  public List<Event> getEvents() { return events; }
  public boolean hasErrors()     { return !errors.isEmpty(); }

  void insertEventBefore(JactlMarker newMarker, JactlMarker existing) {
    int idx = IntStream.range(0, events.size()).filter(i -> events.get(i).getMarker() == existing).filter(i -> events.get(i).isStart()).findFirst().getAsInt();
    events.add(idx, new MarkerEvent(newMarker, MarkerEvent.State.START));
  }

  void debug(String msg) {
    //System.out.println(" ".repeat(indent*2) + msg);
  }

  @Override
  public void done() {
    // Nothing to do since we don't want to throw any errors
  }

  @Override
  public Marker mark() {
    ProgressIndicatorProvider.checkCanceled();
    JactlMarker marker = new JactlMarker(super.mark(), ++counter);
    pushEvent(new MarkerEvent(marker, MarkerEvent.State.START));
    return marker;
  }

  @Override
  public Token _advance() {
    ProgressIndicatorProvider.checkCanceled();
    Token token = super._advance();
    pushEvent(new TokenEvent(token));
    //debug("Token: " + token);
    return token;
  }

  public class JactlMarker implements Marker {
    public PsiBuilder.Marker psiMarker;
    Marker              tokenMarker;
    boolean             doneFlagged = false;
    public IElementType type;
    int                 id;
    boolean             dropped = false;
    boolean             isDone  = false;
    public int          offset;      // Offset into source code
    public JactlAstKey  nameKey;     // For FunDecl and VarDecls points to identifier
    boolean             isError;
    CompileError        error;
    public JactlUserDataHolder astNode;     // Either Stmt, Expr, JactlType, or JactlName

    JactlMarker(Marker tokenMarker, int id) {
      this.tokenMarker = tokenMarker;
      this.id = id;
      //debug(String.format("%06d --> new Marker", id));
      indent++;
    }


    @Override
    public Marker precede() {
      //debug("precede()");
      JactlMarker marker = new JactlMarker(tokenMarker.precede(), ++counter);
      insertEventBefore(marker, this);
      return marker;
    }

    @Override
    public void rollback() {
      tokenMarker.rollback();
      indent--;
      while (true) {
        Event event = popEvent();
        if (event.isToken()) {
          continue;
        }
        if (((MarkerEvent) event).marker == this) {
          break;
        }
        indent--;
      }
      //debug(String.format("%06d <-- Rollback marker", id));
    }

    @Override
    public void error(CompileError err) {
      indent--;
      if (errors.add(err.getMessage())) {
        type = null;
        _done(err);
        isError = true;
        error = err;
      }
      else {
        drop();
      }
    }

    public CompileError getError() { return error; }

    @Override
    public boolean isError() {
      return isError;
    }

    @Override
    public void drop() {
      indent--;
      dropped = true;
      //debug(String.format("%06d <-- Drop marker", id));
      pushEvent(new MarkerEvent(this, MarkerEvent.State.END));
    }

    private void _done(Object obj) {
      isDone = true;
      indent--;
      pushEvent(new MarkerEvent(this, MarkerEvent.State.END));
      //String str = obj instanceof Exception ? ((Exception) obj).getMessage() : obj == null ? "null" : obj.getClass().getName();
      //debug(String.format("%06d <-- Marker done(%s)", id, str));
    }

    @Override
    public void done(JactlName name) {
      this.type    = JactlNameElementType.getElementType(name.getType());
      this.astNode = name;
      this.offset  = name.getName().getOffset();
      _done(name);
    }

    @Override
    public void done(JactlType jactlType, Token location) {
      this.type = jactlType.is(JactlType.CLASS,JactlType.INSTANCE) ? JactlTypeElementType.CLASS_TYPE
                                                                   : JactlTypeElementType.BUILT_IN_TYPE;
      this.astNode = jactlType;
      this.offset  = location.getOffset();
      _done(jactlType);
    }

    @Override
    public void done(List list) {
      type = JactlListElementType.LIST;
      _done(list);
    }

    @Override
    public void done(Stmt stmt) {
      if (stmt == null) {
        drop();
        return;
      }
      type = stmt.accept(new Stmt.Visitor<>() {
        @Override public IElementType visitIf(Stmt.If anIf)                       { return JactlStmtElementType.IF_STMT; }
        @Override public IElementType visitClassDecl(Stmt.ClassDecl classDecl)    { return JactlStmtElementType.CLASS_DECL; }
        @Override public IElementType visitImport(Stmt.Import anImport)           { return JactlStmtElementType.IMPORT_STMT; }
        @Override public IElementType visitVarDecl(Stmt.VarDecl varDecl)          { return JactlStmtElementType.VAR_DECL; }
        @Override public IElementType visitFunDecl(Stmt.FunDecl funDecl)          { return JactlStmtElementType.FUN_DECL; }
        @Override public IElementType visitWhile(Stmt.While aWhile)               { return JactlStmtElementType.WHILE_STMT; }
        @Override public IElementType visitReturn(Stmt.Return aReturn)            { return JactlStmtElementType.RETURN_STM; }
        @Override public IElementType visitExprStmt(Stmt.ExprStmt exprStmt)       { return JactlStmtElementType.EXPR_STMT; }
        @Override public IElementType visitThrowError(Stmt.ThrowError throwError) { throw new IllegalStateException("ThrowError only for internal use"); }
        @Override public IElementType visitStmts(Stmt.Stmts stmts)                { return stmts.isSingleStmt ? JactlStmtElementType.VAR_DECL : JactlStmtElementType.BLOCK; }
        @Override public IElementType visitBlock(Stmt.Block block) {
          return switch (block.location.getType()) {
            case FOR -> JactlStmtElementType.FOR_STMT;
            case DO  -> JactlStmtElementType.DO_UNTIL_STMT;
            default  -> JactlStmtElementType.BLOCK;
          };
        }
      });
      if (type == null) {
        drop();
      }
      else {
        this.astNode = stmt;
        _done(type);
      }
    }

    @Override
    public void done(Expr expr) {
      if (expr == null) {
        drop();
        return;
      }
      type = expr.accept(new Expr.Visitor<>() {
        @Override public IElementType visitBinary(Expr.Binary binary)                          { return JactlExprElementType.BINARY_EXPR; }
        @Override public IElementType visitRegexMatch(Expr.RegexMatch regexMatch)              { return JactlExprElementType.REGEX_MATCH; }
        @Override public IElementType visitRegexSubst(Expr.RegexSubst regexSubst)              { return JactlExprElementType.REGEX_SUBST; }
        @Override public IElementType visitTernary(Expr.Ternary ternary)                       { return JactlExprElementType.TERNARY_EXPR; }
        @Override public IElementType visitPrefixUnary(Expr.PrefixUnary prefixUnary)           { return JactlExprElementType.PREFIX_UNARY_EXPR; }
        @Override public IElementType visitPostfixUnary(Expr.PostfixUnary postfixUnary)        { return JactlExprElementType.POSTFIX_UNARY_EXPR; }
        @Override public IElementType visitCast(Expr.Cast cast)                                { return JactlExprElementType.CAST_EXPR; }
        @Override public IElementType visitCall(Expr.Call call)                                { return JactlExprElementType.CALL_EXPR; }
        @Override public IElementType visitMethodCall(Expr.MethodCall methodCall)              { return JactlExprElementType.METHOD_CALL_EXPR; }
        @Override public IElementType visitLiteral(Expr.Literal literal)                       { return literal.isField ? JactlExprElementType.IDENTIFIER
                                                                                                                        : JactlExprElementType.LITERAL; }
        @Override public IElementType visitListLiteral(Expr.ListLiteral listLiteral)           { return JactlExprElementType.LIST_LITERAL; }
        @Override public IElementType visitMapLiteral(Expr.MapLiteral mapLiteral)              { return JactlExprElementType.MAP_LITERAL; }
        @Override public IElementType visitIdentifier(Expr.Identifier identifier)              { return JactlExprElementType.IDENTIFIER; }
        @Override public IElementType visitClassPath(Expr.ClassPath classPath)                 { return JactlExprElementType.CLASS_PATH_EXPR; }
        @Override public IElementType visitExprString(Expr.ExprString exprString)              { return JactlExprElementType.EXPR_STRING; }
        @Override public IElementType visitVarDecl(Expr.VarDecl varDecl)                       { return JactlExprElementType.VAR_DECL_EXPR; }
        @Override public IElementType visitFunDecl(Expr.FunDecl funDecl)                       { return JactlExprElementType.FUN_DECL_EXPR; }
        @Override public IElementType visitVarAssign(Expr.VarAssign varAssign)                 { return JactlExprElementType.VAR_ASSIGN; }
        @Override public IElementType visitVarOpAssign(Expr.VarOpAssign varOpAssign)           { return JactlExprElementType.VAR_OP_ASSIGN; }
        @Override public IElementType visitFieldAssign(Expr.FieldAssign fieldAssign)           { return JactlExprElementType.FIELD_ASSIGN_EXPR; }
        @Override public IElementType visitFieldOpAssign(Expr.FieldOpAssign fieldOpAssign)     { return JactlExprElementType.FIELD_OP_ASSIGN_EXPR; }
        @Override public IElementType visitNoop(Expr.Noop noop)                                { return null; }
        @Override public IElementType visitClosure(Expr.Closure closure)                       { return JactlExprElementType.CLOSURE; }
        @Override public IElementType visitReturn(Expr.Return aReturn)                         { return JactlExprElementType.RETURN_EXPR; }
        @Override public IElementType visitBreak(Expr.Break aBreak)                            { return JactlExprElementType.BREAK_EXPR; }
        @Override public IElementType visitContinue(Expr.Continue aContinue)                   { return JactlExprElementType.CONTINUE_EXPR; }
        @Override public IElementType visitPrint(Expr.Print print)                             { return JactlExprElementType.PRINT_EXPR; }
        @Override public IElementType visitDie(Expr.Die die)                                   { return JactlExprElementType.DIE_EXPR; }
        @Override public IElementType visitEval(Expr.Eval eval)                                { return JactlExprElementType.EVAL_EXPR; }
        @Override public IElementType visitBlock(Expr.Block block)                             { return JactlExprElementType.BLOCK_EXPR; }
        @Override public IElementType visitSwitch(Expr.Switch aSwitch)                         { return JactlExprElementType.SWITCH_EXPR; }
        @Override public IElementType visitSwitchCase(Expr.SwitchCase switchCase)              { return JactlExprElementType.SWITCH_CASE_EXPR; }
        @Override public IElementType visitConstructorPattern(Expr.ConstructorPattern constructorPattern)  { return JactlExprElementType.SWITCH_CONSTRUCTOR_PATTERN_EXPR; }
        @Override public IElementType visitTypeExpr(Expr.TypeExpr typeExpr)                    { return JactlExprElementType.TYPE_EXPR; }
        @Override public IElementType visitExprList(Expr.ExprList exprList)                    { return JactlExprElementType.EXPR_LIST; }
        @Override public IElementType visitInvokeNew(Expr.InvokeNew invokeNew)                 { return JactlExprElementType.INVOKE_NEW; }
        @Override public IElementType visitArrayLength(Expr.ArrayLength arrayLength)           { throw new IllegalStateException("internal use only"); }
        @Override public IElementType visitArrayGet(Expr.ArrayGet arrayGet)                    { throw new IllegalStateException("internal use only"); }
        @Override public IElementType visitLoadParamValue(Expr.LoadParamValue loadParamValue)  { throw new IllegalStateException("internal use only"); }
        @Override public IElementType visitInvokeFunDecl(Expr.InvokeFunDecl invokeFunDecl)     { throw new IllegalStateException("internal use only"); }
        @Override public IElementType visitInvokeInit(Expr.InvokeInit invokeInit)              { throw new IllegalStateException("internal use only"); }
        @Override public IElementType visitInvokeUtility(Expr.InvokeUtility invokeUtility)     { throw new IllegalStateException("internal use only"); }
        @Override public IElementType visitDefaultValue(Expr.DefaultValue defaultValue)        { throw new IllegalStateException("internal use only"); }
        @Override public IElementType visitInstanceOf(Expr.InstanceOf instanceOf)              { throw new IllegalStateException("internal use only"); }
        @Override public IElementType visitCheckCast(Expr.CheckCast checkCast)                 { throw new IllegalStateException("internal use only"); }
        @Override public IElementType visitConvertTo(Expr.ConvertTo convertTo)                 { throw new IllegalStateException("internal use only"); }
        @Override public IElementType visitSpecialVar(Expr.SpecialVar specialVar)              { throw new IllegalStateException("internal use only"); }
        @Override public IElementType visitStackCast(Expr.StackCast stackCast)                 { throw new IllegalStateException("internal use only"); }
      });
      this.astNode = expr;
      if (type == null) {
        drop();
      }
      else {
        _done(expr);
      }
    }
  }
}
