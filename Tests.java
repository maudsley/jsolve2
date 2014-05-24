package jsolve;

public class Tests {
	static void evaluate(String test, String result) {
		Expression expression = Parser.parse(test);
		expression = Simplify.evaluate(expression);
		if (expression.toString().equals(result.toString())) {
			System.out.println("PASS: " + test + " = " + result);
		} else {
			System.out.println("FAIL: " + test + " = " + result + ", instead: " + expression.toString());
		}
	}
	public static void main(String[] args) {
		evaluate("1 + 1", "2");
		evaluate("3/4 + 4/3", "(25/12)");
		evaluate("1 + 1/2", "(3/2)");
		evaluate("1/2 + 1", "(3/2)");
		evaluate("3/2 * 2/3", "1");
		evaluate("3/2 * (3/2)^(-1)", "1");
		evaluate("2^a * 2^b", "(2^(a+b))");
		evaluate("(-1)^(1/2)", "i");
		evaluate("e^(i*pi) + 1", "0");
		evaluate("x*x", "(x^2)");
		evaluate("3/x + 7/x", "(10/x)");
		evaluate("1/3 - 3^(-1)", "0");
		evaluate("(a + 1/x) * x^2", "((a*(x^2))+x)");
		evaluate("(x - 1/3)^3 + (x - 1/3)^2 + (x - 1/3)", "((((x^3)+((2*x)/3))-(2/9))-(1/27))");
		evaluate("(x - 1/3*x^(-1))^3 + (x - 1/3*x^(-1))", "((x^3)-(1/(27*(x^3))))");
		evaluate("(x - 1/(3*x))^3 + (x - 1/(3*x))", "((x^3)-(1/(27*(x^3))))");
		evaluate("(x - a/3*x^(-1))^3 + a*(x - a/3*x^(-1))", "((x^3)-((a^3)/(27*(x^3))))");
		evaluate("(x - a/(3*x))^3 + a*(x - a/(3*x))", "((x^3)-((a^3)/(27*(x^3))))");
		evaluate("((x - 1/3*x^(-1))^3 + (x - 1/3*x^(-1))) * x^3", "((x^6)-(1/27))");
		evaluate("((x - 1/(3*x))^3 + (x - 1/(3*x))) * x^3", "((x^6)-(1/27))");
		evaluate("((x - a/3*x^(-1))^3 + a*(x - a/3*x^(-1))) * x^3", "((x^6)-((a^3)/27))");
		evaluate("((x - a/(3*x))^3 + a*(x - a/(3*x))) * x^3", "((x^6)-((a^3)/27))");
		evaluate("((x - a/3*x^(-1))^3 + a*(x - a/3*x^(-1)) + b) * x^3", "(((x^6)+(b*(x^3)))-((a^3)/27))");
	}
}
