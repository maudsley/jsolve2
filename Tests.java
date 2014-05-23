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
		evaluate("(-1)^(1/2)", "i");
		evaluate("e^(i*pi) + 1", "0");
	}
}
