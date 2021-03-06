package main;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
  private LoxClass klass;
  private final Map<String, Object> fields = new HashMap<>();


  LoxInstance(LoxClass klass) {
    this.klass = klass;
  }

  Object get(Token name) {
    // find if field/method exists
    if (fields.containsKey(name.lexeme)) {
      return fields.get(name.lexeme);
    }
    LoxFunction method = klass.findMethod(name.lexeme);
    if (method != null) {
      return method.bind(this); // bind current instance to "this" keyword
    }
    // else throw error
    throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
  }

  void set(Token name, Object value) {
    fields.put(name.lexeme, value); // assign fields to instance
  }

  @Override
  public String toString() {
    return klass.name + " instance";
  }

}
