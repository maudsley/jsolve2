jsolve
======

What is this
------------

This is a symbolic equation solver written in Java.

How to build
------------

You need 'ant' installed. Then:

ant && ant run

How to use
----------

At the REPL, type an equation followed by ENTER. If possible, the solutions will be printed.

It is hardcoded to solve for the variable 'x' and all variables are assumed to be real numbers.

Variable names are strings of any length and multiplication is not implied, so a2 is a variable named 'a2' and not the expression a*2.

If the input is an expression, f(x), without an equals sign, the solver will assume you meant f(x) = 0.

The following operators are supported:

 * + (addition)
 * - (subtraction)
 * * (multiplication)
 * / (divison)
 * ^ (exponentation)
 * ! (factorial)

Functions, e.g trig functions, are not supported.

Examples
--------

* a*x + b = 0
* a*x^2 + b*x + c = 0
* e^(2*x) / (a + e^x) = y
