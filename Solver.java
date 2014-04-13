package jsolve;

public class Solver {
	static Expression solve(Expression equation, String variable) {
		if (!equation.contains(variable)) {
			return null;
		}
		
		if (equation.getType().equals(Expression.Type.NODE_EQUALS)) {
			Expression subtract = new Expression(Expression.Type.NODE_SUBTRACT);
			subtract.setLeft(equation.getLeft());
			subtract.setRight(equation.getRight());
			equation = subtract; /* f(x) = y -> f(x) - y = 0 */
		}
		
		Expression result = new Expression("0");
		while (!equation.isSymbol()) {
			if (equation.isUnary()) {
				result = inverseUnary(equation, result, variable);
				equation = equation.getChild();
				continue;
			}
			
			if (equation.getLeft().contains(variable) && equation.getRight().contains(variable)) {
				equation = Expander.expand(equation, variable);
				equation = Factorizer.factorize(equation, variable);
				if (equation.getLeft().contains(variable) && equation.getRight().contains(variable)) {
					return null;
				}
			}
			
			if (equation.getLeft().contains(variable)) {
				result = inverseLeft(equation, result, variable);
				equation = equation.getLeft();
			} else {
				result = inverseRight(equation, result, variable);
				equation = equation.getRight();
			}
		}
		return result;
	}

	static Expression inverseLeft(Expression lhs, Expression rhs, String variable) {
		Expression result = null;
		switch (lhs.getType()) {
		case NODE_ADD: /* x + a = b -> x = b - a */
			result = new Expression(Expression.Type.NODE_SUBTRACT);
			result.setLeft(rhs);
			result.setRight(lhs.getRight());
			return result;
		case NODE_SUBTRACT: /* x - a = b -> x = b + a */
			result = new Expression(Expression.Type.NODE_ADD);
			result.setLeft(rhs);
			result.setRight(lhs.getRight());
			return result;
		case NODE_MULTIPLY: /* x * a = b -> x = b / a */
			result = new Expression(Expression.Type.NODE_DIVIDE);
			result.setLeft(rhs);
			result.setRight(lhs.getRight());
			return result;
		case NODE_DIVIDE: /* x / a = b -> x = b * a */
			result = new Expression(Expression.Type.NODE_MULTIPLY);
			result.setLeft(rhs);
			result.setRight(lhs.getRight());
			return result;
		case NODE_EXPONENTIATE: /* x ^ a = b -> x = b ^ -a */
			result = new Expression(Expression.Type.NODE_EXPONENTIATE);
			result.setLeft(rhs);
			Expression exponent = new Expression(Expression.Type.NODE_MINUS);
			exponent.setChild(lhs.getRight());
			result.setRight(exponent);
			return result;
		default:
			return null;
		}
	}
	
	static Expression inverseRight(Expression lhs, Expression rhs, String variable) {
		Expression result = null;
		switch (lhs.getType()) {
		case NODE_ADD: /* a + x = b -> x = b - a */
			result = new Expression(Expression.Type.NODE_SUBTRACT);
			result.setLeft(rhs);
			result.setRight(lhs.getLeft());
			return result;
		case NODE_SUBTRACT: /* a - x = b -> x = a - b */
			result = new Expression(Expression.Type.NODE_SUBTRACT);
			result.setLeft(lhs.getLeft());
			result.setRight(rhs);
			return result;
		case NODE_MULTIPLY: /* a * x = b -> x = b / a */
			result = new Expression(Expression.Type.NODE_DIVIDE);
			result.setLeft(rhs);
			result.setRight(lhs.getLeft());
			return result;
		case NODE_DIVIDE: /* a / x = b -> x = a / b */
			result = new Expression(Expression.Type.NODE_DIVIDE);
			result.setLeft(lhs.getLeft());
			result.setRight(rhs);
			return result;
		case NODE_EXPONENTIATE: /* a ^ x = b -> x = log_a(b) */
			result = new Expression(Expression.Type.NODE_LOGARITHM);
			result.setLeft(lhs.getLeft());
			result.setRight(rhs);
			return result;
		default:
			return null;
		}
	}
	
	static Expression inverseUnary(Expression lhs, Expression rhs, String variable) {
		Expression result = null;
		switch (lhs.getType()) {
		case NODE_PLUS: /* +x = b -> x = b */
			result = rhs;
			return result;
		case NODE_MINUS: /* -x = b -> x = -b */
			result = new Expression(Expression.Type.NODE_MINUS);
			result.setChild(rhs);
			return result;
		case NODE_FACTORIAL: /* x! = b -> x = InverseFactorial(b) */
			result = new Expression(Expression.Type.NODE_FACTORIAL_INVERSE);
			result.setChild(rhs);
			return result;
		default:
			return null;
		}
	}
}
