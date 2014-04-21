package jsolve;

import java.util.ArrayList;
import java.util.List;

public class Solver {
	static class Equation {
		Equation() {
		}
	
		Equation(Expression left, Expression right) {
			setLeft(left);
			setRight(right);
		}
	
		Expression getLeft() {
			return left_;
		}
		
		void setLeft(Expression left) {
			left_ = left.copy();
		}
		
		Expression getRight() {
			return right_;
		}
		
		void setRight(Expression right) {
			right_ = right.copy();
		}
	
		Expression left_;
		Expression right_;
	}

	static List<Expression> solve(Expression expression, String variable) {
		if (!expression.contains(variable)) {
			return null;
		}
	
		/* f(x) = y -> f(x) - y = 0 */
		Equation equation = new Equation();
		equation.setRight(new Expression("0"));
		if (expression.getType().equals(Expression.Type.NODE_EQUALS)) {
			equation.setLeft(Expression.subtract(expression.getLeft(), expression.getRight()));
		} else {
			equation.setLeft(expression);
		}
	
		List<Expression> results = new ArrayList<Expression>();
		List<Equation> equations = new ArrayList<Equation>();
		equations.add(equation);
		
		while (!equations.isEmpty()) {
			equation = equations.remove(0);
			Expression lhs = equation.getLeft();
			Expression rhs = equation.getRight();
			
			if (lhs.isSymbol()) {
				results.add(rhs);
				continue;
			}
			
			if (!isSolvable(lhs, variable)) {
				List<Equation> solutions = factorize(lhs, rhs, variable);
				for (Equation solution : solutions) {
					equations.add(solution);
				}
				continue;
			}

			if (lhs.isUnary()) {
				rhs = inverseUnary(lhs, rhs, variable);
				lhs = lhs.getChild();
				equations.add(new Equation(lhs, rhs));
				continue;
			}
			
			if (lhs.getLeft().contains(variable)) {
				rhs = inverseLeft(lhs, rhs, variable);
				lhs = lhs.getLeft();
				equations.add(new Equation(lhs, rhs));
				continue;
			} else {
				rhs = inverseRight(lhs, rhs, variable);
				lhs = lhs.getRight();
				equations.add(new Equation(lhs, rhs));
				continue;
			}
		}
		
		return results;
	}
	
	static int solvedCount(List<Equation> equations, String variable) {
		int count = 0;
		for (Equation equation : equations) {
			if (equation.getLeft().isSymbol()) {
				if (equation.getLeft().getSymbol().equals(variable)) {
					count++;
				}
			}
		}
		return count;
	}

	static List<Equation> factorize(Expression lhs, Expression rhs, String variable) {
		List<Equation> results = new ArrayList<Equation>();
	
		/* try to eliminate common subexpressions: (x^2 + x)^2 + (x^2 + x) -> u^2 + u */
		Expression subexpression = Substitution.candidate(lhs, variable);
		if (subexpression != null) {
			String newVariable = Substitution.allocateVariable(lhs);
			Expression substitution = Substitution.substitute(lhs, subexpression, new Expression(newVariable));
			substitution = Expression.subtract(substitution, rhs);
			List<Expression> solutions = Solver.solve(substitution, newVariable);
			for (Expression solution : solutions ) {
				results.add(new Equation(subexpression, solution));
			}
			if (!results.isEmpty()) {
				return results;
			}
		}

		/* solve for the factors independently f(x)*g(x)*h(x) = 0 */
		rhs = Simplify.simplify(rhs);
		if (rhs.isZero()) {
			List<Expression> factors = Iterator.getFactors(lhs, 0);
			if (factors.size() > 1) {
				for (Expression factor : factors) {
					if (factor.contains(variable)) {
						List<Expression> solutions = Solver.solve(factor, variable);
						for (Expression solution : solutions ) {
							results.add(new Equation(new Expression(variable), solution));
						}
					}
				}
			}
			if (!results.isEmpty()) {
				return results;
			}
		}

		lhs = Simplify.simplify(lhs);
		if (isSolvable(lhs, variable)) {
			results.add(new Equation(lhs, rhs));
			return results;
		}
		
		lhs = Expander.expand(lhs, variable);
		if (isSolvable(lhs, variable)) {
			results.add(new Equation(lhs, rhs));
			return results;
		}
		
		lhs = Collector.collect(lhs, variable);
		if (isSolvable(lhs, variable)) {
			results.add(new Equation(lhs, rhs));
			return results;
		}
		
		lhs = Simplify.simplify(lhs);
		if (isSolvable(lhs, variable)) {
			results.add(new Equation(lhs, rhs));
			return results;
		}
		
		if (lhs.getType().equals(Expression.Type.NODE_DIVIDE)) {
			/* f(x) / g(x) = h(x) -> f(x) = h(x) * g(x) */
			rhs = Expression.multiply(rhs, lhs.getRight());
			lhs = Expression.subtract(lhs.getLeft(), rhs);
			rhs = new Expression("0");
			results.add(new Equation(lhs, rhs));
			return results;
		}
		
		Polynomial polynomial = new Polynomial(lhs, variable);
		if (polynomial.isValid()) {
			lhs = polynomial.factorize();
		}
		
		if (isSolvable(lhs, variable)) {
			results.add(new Equation(lhs, rhs));
			return results;
		}
		
		return results;
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
		case NODE_EXPONENTIATE: /* x ^ a = b -> x = b ^ 1/a */
			result = new Expression(Expression.Type.NODE_EXPONENTIATE);
			result.setLeft(rhs);
			Expression exponent = new Expression(Expression.Type.NODE_DIVIDE);
			exponent.setLeft(new Expression("1"));
			exponent.setRight(lhs.getRight());
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
	
	static boolean isSolvable(Expression expression, String variable) {
		if (expression.isBinary()) {
			if (expression.getLeft().contains(variable) && expression.getRight().contains(variable)) {
				return false;
			}
		}
		return true;
	}
}
