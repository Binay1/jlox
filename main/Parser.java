package main;

import java.util.List;
import static main.TokenType.*;

// This class takes the tokens and produces a syntax tree according to the grammar rules

/**
 * This is the syntactical grammar that we are using for Lox. We start with the outermost
 * rule(expression) and walk down the ladder. As we make our way through the rules from top to
 * bottom the precedence increases. It might seem confusing that we start evaluation from the rule
 * with the lowest precedence but you should take note that the lower precedence expressions are
 * formed by putting together higher precedence expressions. So, in reality, we are evaluating
 * higher precedence expressions first in every case
 * 
 * expression → equality ;
 * equality → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison → addition ( ( ">" | ">=" | "<" | "<=" ) addition )* ;
 * addition → multiplication ( ( "-" | "+" )multiplication )* ;
 * multiplication → unary ( ( "/" | "*" ) unary )* ;
 * unary → ( "!" | "-" ) unary| primary ; 
 * primary → NUMBER | STRING | "false" | "true" | "nil" | "(" expression ")" ;
 */
public class Parser {
  
  private static class ParseError extends RuntimeException {
    
  }

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
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
    // we've got an unexpected token
    throw error(peek(), "Expect expression.");
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }
    return primary();
  }

  // lowest precedence, grammatically on top
  private Expr expression() {
    return equality();
  }
  
  Expr parse() {
    try {
      return expression();
    } catch(ParseError error) {
        return null;
    }
  }
}
