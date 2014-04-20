jsolve
======

What is this
------------

This is a symbolic equation solver written in Java.

How to build
------------

You need 'ant' installed. Then:

ant && ant run

To enter the REPL

How to use
----------

At the REPL, type an equation followed by ENTER. If possible, the solution will be printed.

It is hardcoded to solve for the variable 'x'.

Variables are strings of any length.

Multiplication is not implied, so a2 is a variable named 'a2' and not the expression a*2.

The following operators are supported:

 * + (addition)
 * - (subtraction)
 * * (multiplication)
 * / (divison)
 * ^ (exponentation)
 * ! (factorial)

Examples
--------

a*x + b = 0

a*x^2 + b*x + c = 0
