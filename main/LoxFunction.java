package main;

import java.util.List;

class LoxFunction implements LoxCallable {

  private final Stmt.Function declaration;
  
  LoxFunction(Stmt.Function declaration) {
    this.declaration = declaration;
  }
  
  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    // making global the parent would prevent us from defining functions inside functions
    // I know most languages don't allow that but some like javascript do and I kind of like that
    // Might change this later
    Environment environment = new Environment(interpreter.globals);
    for(int i=0;i<declaration.params.size();i++) {
      environment.define(declaration.params.get(i).lexeme, arguments.get(i));
    }
    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch(Return returnValue) {
      return returnValue.value;
    }
    return null;
  }
  
  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }

}
