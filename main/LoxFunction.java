package main;

import java.util.List;

class LoxFunction implements LoxCallable {

  private final Stmt.Function declaration;
  private final Environment closure;
  private final boolean isInitializer;

  LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
    this.declaration = declaration;
    this.closure = closure;
    this.isInitializer = isInitializer;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    // create an enclosing environment to house all parameter values
    Environment environment = new Environment(this.closure);
    // assign the parameter name with its respective argument value
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, arguments.get(i));
    }
    try {
      // execute the function/method body
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      if(isInitializer) {
        return closure.getAt(0,"this"); // return instance reference
      }
      return returnValue.value; // return what we got
    }
    if(isInitializer) {
      return closure.getAt(0, "this");
    }
    return null;
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }

  // whenever you get a property from an instance using ".", create a
  // new enclosing environment to house a "this" variable that points to the
  // calling instance
  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);
    return new LoxFunction(declaration, environment, isInitializer);
  }

}
