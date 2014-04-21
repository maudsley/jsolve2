package jsolve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
			return new ArrayList<Expression>();
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
				results.add(rhs); /* this one was solved */
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
				List<Expression> solutions = inverseUnary(lhs, rhs, variable);
				lhs = lhs.getChild();
				for (Expression solution : solutions) {
					equations.add(new Equation(lhs, solution));
				}
			} else if (lhs.getLeft().contains(variable)) {
				List<Expression> solutions = inverseLeft(lhs, rhs, variable);
				lhs = lhs.getLeft();
				for (Expression solution : solutions) {
					equations.add(new Equation(lhs, solution));
				}
			} else {
				List<Expression> solutions = inverseRight(lhs, rhs, variable);
				lhs = lhs.getRight();
				for (Expression solution : solutions) {
					equations.add(new Equation(lhs, solution));
				}
			}
		}
		
		/* simplify the solutions and filter duplicates */
		List<Expression> solutions = new ArrayList<Expression>();
		Map<String, Boolean> duplicates = new HashMap<String, Boolean>();
		for (int i = 0; i < results.size(); ++i) {
			Expression solution = results.get(i);
			solution = Simplify.simplify(solution);
			String hash = Canonicalizer.toString(solution);
			if (duplicates.get(hash) == null) {
				solutions.add(solution);
				duplicates.put(hash, true);
			}
		}
		
		return solutions;
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

	static List<Expression> inverseLeft(Expression lhs, Expression rhs, String variable) {
		List<Expression> results = new ArrayList<Expression>();
		Expression expression = null;
		switch (lhs.getType()) {
		case NODE_ADD: /* x + a = b -> x = b - a */
			results.add(Expression.subtract(rhs, lhs.getRight()));
			break;
		case NODE_SUBTRACT: /* x - a = b -> x = b + a */
			results.add(Expression.add(rhs, lhs.getRight()));
			break;
		case NODE_MULTIPLY: /* x * a = b -> x = b / a */
			results.add(Expression.divide(rhs, lhs.getRight()));
			break;
		case NODE_DIVIDE: /* x / a = b -> x = b * a */
			results.add(Expression.multiply(rhs, lhs.getRight()));
			break;
		case NODE_EXPONENTIATE: /* x ^ a = b -> x = b ^ 1/a */
			Expression exponent = Expression.divide(new Expression("1"), lhs.getRight());
			expression = Expression.exponentiate(rhs, exponent);
			results.add(expression);
			Long power = lhs.getRight().getSymbolAsInteger();
			if (power != null) {
				if (power % 2 == 0) { /* +/- sqrt(x) */
					results.add(Expression.negate(expression));
				}
			}
			break;
		default:
			break;
		}
		return results;
	}
	
	static List<Expression> inverseRight(Expression lhs, Expression rhs, String variable) {
		List<Expression> results = new ArrayList<Expression>();
		Expression expression = null;
		switch (lhs.getType()) {
		case NODE_ADD: /* a + x = b -> x = b - a */
			results.add(Expression.subtract(rhs, lhs.getLeft()));
			break;
		case NODE_SUBTRACT: /* a - x = b -> x = a - b */
			results.add(Expression.subtract(lhs.getLeft(), rhs));
			break;
		case NODE_MULTIPLY: /* a * x = b -> x = b / a */
			results.add(Expression.divide(rhs, lhs.getLeft()));
			break;
		case NODE_DIVIDE: /* a / x = b -> x = a / b */
			results.add(Expression.divide(lhs.getLeft(), rhs));
			break;
		case NODE_EXPONENTIATE: /* a ^ x = b -> x = log_a(b) */
			expression = new Expression(Expression.Type.NODE_LOGARITHM);
			expression.setLeft(lhs.getLeft());
			expression.setRight(rhs);
			results.add(expression);
			break;
		default:
			break;
		}
		return results;
	}
	
	static List<Expression>  inverseUnary(Expression lhs, Expression rhs, String variable) {
		List<Expression> results = new ArrayList<Expression>();
		Expression expression = null;
		switch (lhs.getType()) {
		case NODE_PLUS: /* +x = b -> x = b */
			expression = rhs;
			results.add(expression);
			break;
		case NODE_MINUS: /* -x = b -> x = -b */
			expression = new Expression(Expression.Type.NODE_MINUS);
			expression.setChild(rhs);
			results.add(expression);
			break;
		case NODE_FACTORIAL: /* x! = b -> x = InverseFactorial(b) */
			expression = new Expression(Expression.Type.NODE_FACTORIAL_INVERSE);
			expression.setChild(rhs);
			results.add(expression);
			break;
		default:
			break;
		}
		return results;
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
