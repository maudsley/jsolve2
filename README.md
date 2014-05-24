What is this
------------

This is a symbolic equation solver written in Java.

How to build
------------

You need 'ant' installed. Then:

ant && ant run

How to use
----------

At the prompt, type an equation followed by ENTER. If possible, the solutions will be printed.

It is hardcoded to solve for the variable 'x' and all variables are assumed to be real numbers, except 'i', where i^2 = -1. Also, 'e' is understood to be the base of the natural logarithm and e^(i*x) will expand appropriately.

Variable names are strings of any length and multiplication is not implied, so a2 is a variable named 'a2' and not the expression a*2.

Variables with an underscore prefix are used internally for substitutions and it will probably break things if you use them.

If the input is an expression without an equals sign, the program will try to simplify it and print the result.

The following operators are supported:

 * Addition (+)
 * Subtraction (-)
 * Multiplication (*)
 * Divison (/)
 * Exponentation (^)
 * Factorial (!)

Functions, e.g trig functions, are not supported. To input sqrt(x) use x^(1/2).

Examples
--------

* a*x + b = 0
* a*x^2 + b*x + c = 0
* e^(2*x) / (a + e^x) = y
* e^(x+a)! = b*(x+a)!

How to run tests
----------------

To run test cases use:

ant && ant test
