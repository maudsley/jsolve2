package jsolve;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

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
				expression = "x^2 + x = y";
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
			
			List<Expression> solutions = Solver.solve(tree, "x");

			if (solutions.isEmpty()) {
				System.out.println(tree.toString() + " -> Unable to solve :(");
				return;
			}
			
			for (int i = 0; i < solutions.size(); ++i) {
				Expression solution = solutions.get(i);
				solution = Simplify.simplify(solution);
				Expression equation = Expression.equals(new Expression("x"), solution);
				if (solutions.size() > 1) {
					Integer count = new Integer(i + 1);
					System.out.println(tree.toString() + " -> " + equation.toString() + " (" + count.toString() + ")");
				} else {
					System.out.println(tree.toString() + " -> " + equation.toString());
				}
			}
		}
	}
}
