package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This class has a very similar process to the interpreter. It goes through the syntax tree and
 * resolves all the variable values for the interpreter instead of evaluating the code. The scopes
 * defined in this class are in sync withhow the interpreter handles environments at runtime
 */
class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

  private enum FunctionType {
    NONE, FUNCTION, METHOD, INITIALIZER
  }
  private enum ClassType {
    NONE, CLASS, SUBCLASS
  }

  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
  // we're only keeping track of these to prevent the usage of "return" or "this" outside
  // a function
  private FunctionType currentFunction = FunctionType.NONE;
  private ClassType currentClass = ClassType.NONE;



  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  private void endScope() {
    scopes.pop();
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void resolve(Stmt statement) {
    statement.accept(this);
  }

  private void resolveFunction(Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;
    beginScope();
    for (Token param : function.params) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();
    currentFunction = enclosingFunction;
  }

  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }

  private void define(Token name) {
    if (scopes.isEmpty()) {
      return;
    }
    scopes.peek().put(name.lexeme, true);
  }

  private void declare(Token name) {
    if (scopes.isEmpty()) {
      return;
    }
    Map<String, Boolean> scope = scopes.peek();
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name, "Variable with this name already declared in this scope.");
    }
    scope.put(name.lexeme, false); // false just means that it isn't initialized yet
  }

  // this method does all the work. It does the job of linking the declaration with the usage
  // by passing the distance between the environments where both actions take place
  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }

    // Not found. Assume it is global.
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);
    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Cannot return from top-level code.");
    }
    if (stmt.value != null) {
      if (currentFunction == FunctionType.INITIALIZER) {
        Lox.error(stmt.keyword, "Cannot return value from initializer");
      }
      resolve(stmt.value);
    }
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) {
      resolve(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value); // in case there are other variables in the assignment
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);
    for (Expr argument : expr.arguments) {
      resolve(argument);
    }
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      Lox.error(expr.name, "Cannot read local variable in its own initializer.");
    }
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;
    declare(stmt.name);
    define(stmt.name);
    if (stmt.superclass != null && stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
      Lox.error(stmt.superclass.name, "A class cannot inherit from itself.");
    }
    if (stmt.superclass != null) {
      currentClass = ClassType.SUBCLASS;
      resolve(stmt.superclass);
      // create a closure to bind super to
      beginScope();
      scopes.peek().put("super", true);
    }
    beginScope(); // create a new scope for the class
    scopes.peek().put("this", true); // place "this" in it
    for (Stmt.Function method : stmt.methods) {
      FunctionType declaration = FunctionType.METHOD;
      if (method.name.lexeme.equals("init")) {
        declaration = FunctionType.INITIALIZER;
      }
      resolveFunction(method, declaration);
    }
    endScope(); // get rid of it when done
    if (stmt.superclass != null) {
      endScope();
    }
    currentClass = enclosingClass;
    return null;
  }

  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitThisExpr(Expr.This expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword, "Cannot use 'this' outside of a class.");
      return null;
    }
    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitSuperExpr(Expr.Super expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword,
          "Cannot use 'super' outside of a class.");
    } else if (currentClass != ClassType.SUBCLASS) {
      Lox.error(expr.keyword,
          "Cannot use 'super' in a class with no superclass.");
    }
    resolveLocal(expr, expr.keyword);
    return null;
  }
}
