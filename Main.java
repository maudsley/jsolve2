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
				expression = "4^(1/2)";
			}
			
			Parser parser = null;
			try {
				parser = new Parser(expression);
			} catch (Error e) {
				System.out.println(e.getMessage());
				continue;
			}

			Expression input = parser.getExpression();

			boolean simplify = false;
			
			if (args.length != 0 && args[0].equals("simplify")) {
				simplify = true;
			}
			
			if (!input.getType().equals(Expression.Type.NODE_EQUALS)) {
				simplify = true;
			}
			
			if (simplify) {
				Expression result = Simplify.simplify(input);
				result = Expander.expand(result, "x");
				result = Collector.collect(result, "x");
				result = Simplify.simplify(result);
				System.out.println(input.toString() + " -> " + result.toString());
				continue;
			}
			
			if (args.length != 0 && args[0].equals("expand")) {
				Expression result = Simplify.simplify(input);
				result = Expander.expand(result, "x");
				result = Collector.normalizeExponents(result);
				result = Simplify.simplify(result);
				System.out.println(input.toString() + " -> " + result.toString());
				continue;
			}
			
			List<Expression> solutions = Solver.solve(input, "x");

			if (solutions.isEmpty()) {
				System.out.println(input.toString() + " -> Unable to solve :(");
				continue;
			}
			
			for (int i = 0; i < solutions.size(); ++i) {
				if (i == 2) {
					System.out.println(input.toString() + " -> (" + i + " of " + solutions.size() + " solutions)");
					break;
				}
				Expression solution = solutions.get(i);
				Expression equation = Expression.equals(new Expression("x"), solution);
				if (solutions.size() > 1) {
					Integer count = new Integer(i + 1);
					System.out.println(input.toString() + " -> " + equation.toString() + " (" + count.toString() + ")");
				} else {
					System.out.println(input.toString() + " -> " + equation.toString());
				}
			}
		}
	}
}
