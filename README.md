# jlox

This is my implementation of jlox created while following Robert Nystrom's "Crafting Interpreters".
In it's current state, the project has a working scanner with a clear lexical grammar that takes the
source text and converts it to tokens. There is also some code to generate an Abstract Syntax Tree that
will be used to lay down the syntactical grammar to generate an Abstract Syntax Tree.

Some explanation of the jargon above: the current workflow of the project is to take the source file, read it
as a string and convert them to "tokens" (basically atttaching a meaning to groups of text). The next step is 
to take the tokens and create "expressions" out of them. What that means is to take a series of tokens and
attach some meaning to them (which, here, means defining the rules on how to evaluate them). 
This is very similar to the previous step, we're just forming expressions out of tokens instead of tokens out
of strings of characters. It's a pattern, at every stage we add more and more data to the simple text to make it
consumable for the interpreter. The way that we evaluate expressions is going to be through an Abstract Syntax Tree
(more detail on that later, still working on completely understanding it myself).
[This is all the progress I have right now. The file will be updated further in future]