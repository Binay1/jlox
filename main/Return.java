package main;

class Return extends RuntimeException {
  
  final Object value;
  
  Return(Object value) {
    // this disables all of the exception-y stuff like stack traces
    // we are using this class purely as a wrapper for the value so that
    // we can move it along the callstack
    super(null,null,false,false);
    this.value = value;
  }

}
