package jsolve;

import jsolve.Parser.Error;

public class Main {
	public static void main(String[] args) {
		String expression = "";
		for (String arg : args) {
			expression += arg;
		}
		if (expression.isEmpty()) {
			expression = "x + x + x = y";
		}
		
		Parser parser = null;
		try {
			parser = new Parser(expression);
		} catch (Error e) {
			System.out.println(e.getMessage());
			return;
		}

		Expression tree = parser.getExpression();

		System.out.println(tree.toString());

		Expression solution = Solver.solve(tree, "x");
		
		Expression output = new Expression(Expression.Type.NODE_EQUALS);
		output.setLeft(new Expression("x"));
		output.setRight(solution);
		
		System.out.println(output.toString());
	}
}
