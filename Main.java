package jsolve;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jsolve.Parser.Error;

public class Main {
	public static void main(String[] args) {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		boolean repl = true;
		while (repl) {
			String expression = "";
			if (args.length != 0) {
				if (args[0].equals("repl")) {
					try {
						expression = bufferedReader.readLine();
					} catch (IOException e1) {
						break;
					}
				} else {
					for (String arg : args) {
						expression += arg + " ";
					}
					repl = false;
				}
			}
			
			if (expression.isEmpty()) {
				repl = false;
				expression = "x!/x";
			}
			
			Parser parser = null;
			try {
				parser = new Parser(expression);
			} catch (Error e) {
				System.out.println(e.getMessage());
				return;
			}

			Expression tree = parser.getExpression();

			System.out.println("Input: " + tree.toString());
			System.out.println("Simplified: " + Simplify.simplify(tree));

			Expression solution = Solver.solve(tree, "x");

			if (solution == null) {
				System.out.println("Solution: Unable to solve :(");
			} else {
				solution = Simplify.simplify(solution);
				Expression output = new Expression(Expression.Type.NODE_EQUALS);
				output.setLeft(new Expression("x"));
				output.setRight(solution);
				System.out.println("Solution: " + output.toString());
			}
		}
	}
}
