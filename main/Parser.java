package main;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import static main.TokenType.*;

// This class takes the tokens and produces a syntax tree according to the grammar rules

/**
 * 
 * This is the syntactical grammar that we are using for Lox. We start with the outermost
 * rule and walk down the ladder. As we make our way through the rules from top to
 * bottom the precedence increases (those further down are evaluated first).
 * It might seem confusing that we start evaluation from the rule with the lowest precedence but
 * you should take note that the lower precedence rules are formed by putting together higher
 * precedence rules. So, in reality, we are evaluating higher precedence rules first in every case
 * 
 * program → declaration* EOF ;
 * declaration → funDecl | varDecl | statement ;
 * funDecl  → "fun" function ;
 * function → IDENTIFIER "(" parameters? ")" block ;
 * parameters → IDENTIFIER ( "," IDENTIFIER )* ;
 * varDecl → "var" IDENTIFIER ( "=" expression )? ";" ;
 * statement → exprStmt | ifStmt | printStmt | returnStmt | whileStmt | forStmt | block ;
 * returnStmt → "return" expression? ";" ;
 * forStmt → "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;
 * exprStmt → expression ";" ;
 * ifStmt    → "if" "(" expression ")" statement ( "else" statement )? ;
 * printStmt → "print" expression ";" ;
 * whileStmt → "while" "(" expression ")" statement ;
 * block → "{" declaration* "}"
 * expression → assignment ;
 * assignment → IDENTIFIER "=" assignment | logic_or ;
 * logic_or → logic_or ("or " logic_and)* ;
 * logic_and → equality ("and" equality)* ;
 * equality → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison → addition ( ( ">" | ">=" | "<" | "<=" ) addition )* ;
 * addition → multiplication ( ( "-" | "+" )multiplication )* ;
 * multiplication → unary ( ( "/" | "*" ) unary )* ;
 * unary → ( "!" | "-" ) unary| call ;
 * call → primary ("("arguments?")")* ;
 * arguments → expression ( "," expression )* ;
 * primary → NUMBER | STRING | "false" | "true" | "nil" | "(" expression ")" | IDENTIFIER ;
 * 
 */
public class Parser {
  
  private static class ParseError extends RuntimeException {
    
  }

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }
  
  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after expression");
    return new Stmt.Print(value);
  }
  
  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after expression");
    return new Stmt.Expression(expr);
    
  }

  // Check if we are at end of file
  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  // we don't just "throw" an error because we need to decide how much damage control we need to do
  // instead of just handing it off to java and finishing execution
  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }
  
  // when we get an error, we want to synchronize the program state by 
  // dropping all remaining tokens in the current statement and moving to the next one
  // as it is likely for more errors to be detected in the current statement that are caused due
  // to the parser's confusion after encountering the first error (this can often be misleading as
  // all of these stem from a single mistake and it only serves to obscure what we're trying to 
  // communicate to the user). Another aim is to discover as many errors as possible in one go. 
  private void synchronize() {
    advance();
    
    while(!isAtEnd()) {
      if(previous().type==SEMICOLON) { // semicolon signals end of statement
        return;
      }
      // these keywords are often used in the beginning of a new statement
      switch(peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }
      advance(); // keep going till you get a semicolon or one of the keywords above
    }
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) {
      return advance();
    }
    throw error(peek(), message);
  }

  // return token at current position
  private Token peek() {
    return tokens.get(current);
  }

  // return token at previous position
  private Token previous() {
    return tokens.get(current - 1);
  }

  // consume the next token in line and return it
  private Token advance() {
    if (!isAtEnd()) {
      current++;
    }
    return previous();
  }

  // check if the next token is the one we want
  private boolean check(TokenType type) {
    if (isAtEnd()) {
      return false;
    } else {
      return peek().type == type;
    }
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) { // if the next token is the right one, consume it
        advance();
        return true;
      }
    }
    return false;
  }
  
  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();
    
    while(!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }
    
    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }
  
  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");
    
    Expr initializer = null;
    if(match(EQUAL)) {
      initializer = expression();
    }
    
    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }
  
  private Stmt forStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'for'.");

    Stmt initializer;
    if(match(SEMICOLON)) {
      initializer=null;
    } 
    else if(match(VAR)) {
      initializer = varDeclaration();
    } 
    else {
      initializer = expressionStatement();
    }
    
    Expr condition = null;
    if(!check(SEMICOLON)) {
      condition = expression();
    }
    consume(SEMICOLON, "Expect ';' after loop condition.");
    
    Expr increment = null;
    if(!check(RIGHT_PAREN)) {
      increment = expression();
    }
    consume(RIGHT_PAREN, "Expect ')' after for clauses.");

    Stmt body = statement();
    
    if (increment!=null) {
      // add increment to end of body
      body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
    }
    
    if(condition==null) {
      condition = new Expr.Literal(true);
    }
    
    body = new Stmt.While(condition, body); // create a while loop with the given condition and body
    
    if(initializer!=null) {
      body = new Stmt.Block(Arrays.asList(initializer, body)); // add initializer to beginning of body
                                                               // (outside of while loop)
    }
    
    return body;
    
  }
  
  private Stmt statement() {
    if(match(FOR)) {
      return forStatement();
    }
    if (match(IF)) {
      return ifStatement();
    }
    if(match(PRINT)) {
      return printStatement();
    }
    if(match(WHILE)) {
      return whileStatement();
    }
    if (match(RETURN)) {
      return returnStatement();
    }

    if(match(LEFT_BRACE)) {
      return new Stmt.Block(block());
    }
    return expressionStatement();
    
  }
  
  private Stmt returnStatement() {
    Token keyword = previous(); // just for error reporting. We have no other use for return keyword
    Expr value = null;
    if(!check(SEMICOLON)) { // check to see if we have something to return
      value = expression();
    }
    consume(SEMICOLON, "Expect ';' after return value.");
    return new Stmt.Return(keyword, value);
  }
  
  private Stmt ifStatement() {
    
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition.");
    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if(match(ELSE)) {
      elseBranch = statement();
    }
    return new Stmt.If(condition, thenBranch, elseBranch);
  }
  
  private Stmt whileStatement() {
    
    consume(LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after condition.");
    Stmt body = statement();
    
    return new Stmt.While(condition, body);
  }
  
  private Stmt.Function function(String kind) {
    Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
    consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
    List<Token> parameters = new ArrayList<>();
    if(!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Cannot have more than 255 parameters.");
        }        
        parameters.add(consume(IDENTIFIER, "Expect parameetr name."));
      } while(match(COMMA));
    }
    consume(RIGHT_PAREN, "Expect ')' after parameters.");
    consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
  }
  
  private Stmt declaration() {
    try {
      if (match(FUN)) {
        return function("function");
      }
      if(match(VAR)) {
        return varDeclaration();
      }
      return statement();
    } catch(ParseError error) {
      synchronize();
      return null;
    }
  }

  // almost identical to equality(). The only difference is type of operands
  // Check those comments to get a gist of the general process
  private Expr addition() {
    Expr expr = multiplication();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = multiplication();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  // almost identical to equality(). The only difference is type of operands
  // Check those comments to get a gist of the general process
  private Expr multiplication() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  // almost identical to equality(). The only difference is type of operands
  // Check those comments to get a gist of the general process
  private Expr comparison() {
    Expr expr = addition();
    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = addition();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  // The logic here is used for all binary operations
  private Expr equality() {
    Expr expr = comparison(); // left operand
    while (match(BANG_EQUAL, EQUAL_EQUAL)) { // if we have an equality token in line
      Token operator = previous(); // operator
      Expr right = comparison(); // right operand
      // Take both operands, the operator and package them together into a binary syntax tree node
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  // highest precedence, grammatically on bottom
  private Expr primary() {
    if (match(FALSE)) {
      return new Expr.Literal(false);
    }
    if (match(TRUE)) {
      return new Expr.Literal(true);
    }
    if (match(NIL)) {
      return new Expr.Literal(null);
    }
    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }
    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression."); // Must find a right paren, otherwise
                                                            // throw error
      return new Expr.Grouping(expr);
    }
    if(match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }
    // we've got an unexpected token
    throw error(peek(), "Expect expression.");
  }
  
  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if(!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255) {
          error(peek(), "Cannot have more than 255 arguments.");
        }
        arguments.add(expression());
      } while(match(COMMA));
    }
    Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
    return new Expr.Call(callee, paren, arguments);
  }
  
  private Expr call() {
    Expr expr = primary();
    while(true) {
      if(match(LEFT_PAREN)) {
        expr = finishCall(expr);
      }
      else {
        break;
      }
    }
    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }
    return call();
  }
  
  private Expr and() {
    Expr expr = equality();
    
    while(match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }
  
  private Expr or() {
    Expr expr = and();
    
    while(match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }
  
  private Expr assignment() {
    Expr expr = or(); // parse left side
    
    if(match(EQUAL)) { // find equal
      Token equals = previous();
      Expr value = assignment();
      
      if(expr instanceof Expr.Variable) { // if parsed left-side is a variable
        Token name  = ((Expr.Variable)expr).name; // we need to cast because at compile-time,
                                                  // java doesn't know the "type" of expr
        return new Expr.Assign(name, value); // perform assignment
      }
      error(equals, "Invalid assignment target"); // report if it isn't
    }
    
    return expr; // continue on your merry path if you don't find "="
  }

  // lowest precedence, grammatically on top
  private Expr expression() {
    return assignment();
  }
  
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while(!isAtEnd()) {
      statements.add(declaration());
    }
    return statements;
  }
}
