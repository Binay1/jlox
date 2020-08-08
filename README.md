# jlox

This is my tree-walk interpreter for the Lox programming language created while following Robert Nystrom's "Crafting Interpreters". The grammar for the language has been defined by Robert Nystrom (at least as far as I know).The implementation has been completed. It has a working scanner, parser and interpreter with support for variables, if statements, for/while loops, functions, classes, local scope in the form of block statements and inheritance. 

The current workflow of the project is to take the source file, read it as a string and convert it to "tokens" (basically atttaching a meaning to groups of text: what type they are, value, etc.). The next step is to take the tokens and create "expressions", "statements" out of them. What that means is to take a series of tokens and attach some more meaning to them (grouping them together and defining what the tokens represent in a larger context based on the language grammar that we have defined). This is very similar to the previous step, we're just forming expressions/statements out of sequence of tokens instead of tokens out of sequence of characters. In terms of heirarchy, statements come above expressions. Expressions are like a sub-class (not in the literal java sense) of statements. We have an "expression statement" that acts like a wrapper for all expressions. The Parser converts the text to a "syntax tree" made up of a few basic types of expression and statement nodes that we have defined and it's job ends. Before we send the syntax tree to the interpreter, we do a variable resolution pass through the tree once. It basically tells the interpreter which variable corresponds to which declaration and does a few error checks (like no return statements outside functions) so that the interpreter doesn't encounter anything weird.The interpreter class then takes the syntax-tree as input and then evaluates it.

# Docs

This isn't going to be nearly as good as the documentation that you usually find for programming languages. I understand the importance of writing good and detailed docs (I wouldn't survuve a day without them), but is a very time consuming process and I believe my time would be more effectively utilized working on something else. My only aim is to acquaint the reader enough with the language so that you can see this project in action.

Some things to get out of the way right in the beginning:

- Every statement must end with a semicolon(;)
- Lox is dynamically typed so we don't have to worry about any data types. Strings need to be surrounded in quotes("") though.
- Each block({}) houses its own scope and variables declared inside can not be accessed from outside of it
- 'nil' is the Lox version of null
- We only have syntax for single line comments. It goes like this:

	// This is a comment

## Print

The print statement, as you might have already guessed, allows you to print stuff.

	print "Hello world";

## Logical Operators

Lox supports three logical operators: and, or, not. The syntax is as follows:

- and : and
- or : or
- not : !

## Variable declaration

You can declare a variable by using a 'var' keyword. You can choose to declare it with an initial value or assign one later.

Example 1:

	var variableName = value;
	
Example 2:

	var variableName;
	variableName = value;
	
## Loops

Lox only supports two types of loops:

For Loop:

The syntax is exactly like what you would see in javascript.

	for(var i=0;i<5;) {
		print i;
	}

While Loop:

Again, the syntax is exactly the same as that of javascript.
	
	var x=5;
	while(x>0) {
		print x;
		x=x-1;
	}

## If-Else

There is no switch statement or conditional operator in Lox, so it's just plain old if-else. The syntax is just like in other programming languages.

	if(val1>val2) {
		print(val1);
	}
	else {
		print val2;
	}

## Functions

We begin a function declaration by using the 'fun' keyword. The arguments are optional.

	fun FunName(arg1, arg2) {
		return arg1>arg2;
	}
	
You don't necessarily have to return something. The function returns nil by default. A function call is as simple as typing the name and following it with parentheses that contain arguments, like so:

	FunName(5,2);

## Classes

A class, by default, only has methods associated with it. You can't declare fields when defining a class and there are no fields "associated" with it (basically, no static fields allowed). The class is only meant to group behavior. Methods are just functions declared within a class that can be accessed by its instances. You don't need to use the 'fun' keyword before declaring a method.

	class person {		
		sayHello() {
			print "hello";
		}	
	}
	
The way to create a new instance is to simply follow the class name with parentheses:

	var me = person();
	
Accessing a method is also simple: just type a dot(.) following the instance name and call it like a function:

	me.sayHello();

The way to attach new fields is to follow instance name by a dot(.), write the property name and assign a value.

	me.name = "Binay";
	
We can access the instance properties in class methods by using the 'this' keyword. 

	class person {		
		sayHello() {
			print "hello, " + "my name is " + this.name;
		}	
	}

Coming back to fields, I know that it might seem weird for there to be no structure regarding fields but there is a crucial part of classes that we have not covered yet that wil make you realize that it isn't completely structure-less. Coming to constructors: these methods are run every time an instance is created and are named 'init'. One very helpful usecase is to initialize each object with the required fields like so: 

	class person {
	  init(name, home) {
	    this.name = name;
	    this.home = home;
	  }
	
	  sayHello() {
	    print this.name + " lives in " + this.home;
	  }
	}
	
	var me = person("Binay", "Delhi");
	me.sayHello();

This allows you to make sure that every object has the required data to perform the actions that you have defined. You can also attach other fields to objects individually after they have been initialized and I think that's a great feature to have.

The last part that we have to cover now is inheritance. When declaring a subclass, follow the subclass' name with a lesser-than(<) sign and write the superclass name. You can access the
superclass' methods by using the 'super' keyword.

	class Doughnut {
	  cook() {
	    print "Fry until golden brown.";
	  }
	}
	
	class BostonCream < Doughnut {
	  cook() {
	    super.cook();
	    print "Pipe full of brownie and coat with chocolate.";
	  }
	}
	
	BostonCream().cook();

I took the above example from the Crafting Interpreters Book because I was running out of creativity with my person examples.

# How to use

Fire up your favorite Java IDE (I used eclipse to build this) and open the project in it (Eclipse doesn't let you "import" the project for some reason. I'm guessing it's because I uploaded just the src directory to the repo. There were probably some hidden files in the base project folder. So for Eclipse, just go File -> Open Projects from File System). Run Lox.java without any arguments to get a shell-like mode (REPL) or provide it with a filepath to your Lox program (you can just write it in a .txt file somewhere). I've only been running it using eclipse so I'll figure out how to compile and run the project straight from cmd and upload instructions soon.