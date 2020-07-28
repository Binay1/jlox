package main;

class RuntimeError extends RuntimeException {
  final Token token;
  
  RuntimeError(Token token, String message) {
    super(message); // run RuntimeError constructor with message
    this.token = token;
  }
}
