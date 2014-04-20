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
			if (args.length == 0) {
				try {
					expression = bufferedReader.readLine();
				} catch (IOException e1) {
					break;
				}
			} else {
				repl = false;
				for (int i = 1; i < args.length; ++i) {
					expression += args[i] + " ";
				}
			}
			
			if (args.length != 0 && args[0].equals("debug")) {
				expression = "(e^x + a)^3 - 3*(e^x*a^2 + (e^(x*2)*a) = y";
			}
			
			Parser parser = null;
			try {
				parser = new Parser(expression);
			} catch (Error e) {
				System.out.println(e.getMessage());
				return;
			}

			Expression tree = parser.getExpression();
			
			if (args.length != 0 && args[0].equals("simplify")) {
				Expression result = Simplify.simplify(tree);
				result = Expander.expand(result, "x");
				result = Collector.collect(result, "x");
				result = Simplify.simplify(result);
				System.out.println(tree.toString() + " -> " + result.toString());
				return;
			}
			
			if (args.length != 0 && args[0].equals("expand")) {
				Expression result = Simplify.simplify(tree);
				result = Expander.expand(result, "x");
				result = Collector.normalizeExponents(result);
				result = Simplify.simplify(result);
				System.out.println(tree.toString() + " -> " + result.toString());
				return;
			}
			
			Expression solution = Solver.solve(tree, "x");

			if (solution == null) {
				System.out.println(tree.toString() + " -> Unable to solve :(");
			} else {
				solution = Simplify.simplify(solution);
				Expression output = new Expression(Expression.Type.NODE_EQUALS);
				output.setLeft(new Expression("x"));
				output.setRight(solution);
				System.out.println(tree.toString() + " -> " + output.toString());
			}
		}
	}
}
