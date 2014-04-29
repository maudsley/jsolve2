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
			/* asked to solve for a variable that does not appear in the equation */
			return new ArrayList<Expression>();
		}
	
		if (!expression.getType().equals(Expression.Type.NODE_EQUALS)) {
			/* asked to solve but the input is not an equation */
			return new ArrayList<Expression>();
		}
	
		/* f(x) = y -> f(x) - y = 0 */
		Equation equation = new Equation();
		equation.setRight(new Expression("0"));
		equation.setLeft(Expression.subtract(expression.getLeft(), expression.getRight()));
	
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
		
		Polynomial polynomial = new Polynomial(Expression.subtract(lhs, rhs), variable);
		if (polynomial.isValid()) {
			List<Equation> roots = solvePolynomial(polynomial, variable);
			if (!roots.isEmpty()) {
				return roots;
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
		
		if (isSolvable(lhs, variable)) {
			results.add(new Equation(lhs, rhs));
			return results;
		}
		
		polynomial = new Polynomial(Expression.subtract(lhs, rhs), variable);
		if (polynomial.isValid()) {
			List<Equation> roots = solvePolynomial(polynomial, variable);
			if (!roots.isEmpty()) {
				return roots;
			}
		}
		
		return results;
	}
	
	static List<Equation> solvePolynomial(Polynomial polynomial, String variable) {
		List<Equation> results = new ArrayList<Equation>();
	
		Long degree = polynomial.getDegree();
		if (degree > 3) {
			return results;
		}
		
		/* divide by the leading coefficient */
		polynomial = polynomial.divide(polynomial.getCoefficient(polynomial.getDegree()));
		
		if (degree == 2) {
			Expression factored = solveQuadratic(polynomial, variable);
			if (factored == null) {
				return results;
			} else {
				results.add(new Equation(factored, new Expression("0")));
				return results;
			}
		}
		
		/* if necessary, this substitution will eliminate the n-1 degree term */
		Expression term = polynomial.getCoefficient(degree - 1);		
		if (term != null) {
			term = Expression.divide(term, new Expression(degree.toString()));
			Expression newVariable = Expression.subtract(polynomial.getVariable(), term);
			Expression newExpression = Substitution.substitute(polynomial.getExpression(), new Expression(variable), newVariable);
			newExpression = Simplify.simplify(newExpression);
			newExpression = Expander.expand(newExpression, variable);
			newExpression = Collector.collect(newExpression, variable);
			newExpression = Simplify.simplify(newExpression);
			polynomial = new Polynomial(newExpression, variable);
			if (!polynomial.isValid()) {
				return results;
			} else if (polynomial.getDegree() != degree) {
				return results;
			} else if (polynomial.getCoefficient(degree-1) != null) {
				return results;
			}
		}
		
		if (degree != 3) {
			return results;
		}
		
		Expression result = solveCubic(polynomial, variable);
		if (result == null) {
			return results;
		}
		
		if (term != null) {
			results.add(new Equation(Expression.add(result, term), new Expression("0")));
		}

		return results;
	}
	
	static Expression solveQuadratic(Polynomial polynomial, String variable) {
		/* complete the square */
		Expression linearTerm = polynomial.getCoefficient(1);
		Expression result = Parser.parse("(_x + _a/2)^2 - (_a/2)^2");
		result = Substitution.substitute(result, new Expression("_a"), linearTerm);
		result = Substitution.substitute(result, new Expression("_x"), polynomial.getVariable());
		Expression constantTerm = polynomial.getCoefficient(0);
		if (constantTerm != null) {
			result = Expression.add(result, constantTerm);
		}

		/* verify the solution */
		Expression original = Simplify.simplify(polynomial.getExpression());
		Expression expanded = Expander.expand(result, variable);
		expanded = Collector.collect(expanded, variable);
		expanded = Simplify.simplify(expanded);
		if (Canonicalizer.compare(original, expanded)) {
			return result; /* it worked */
		}

		return null; /* it failed */
	}
	
	static Expression solveCubic(Polynomial polynomial, String variable) {
		/* make Vieta's substitution */
		Expression linear = polynomial.getCoefficient(1);
		Expression sub = Parser.parse("_x - _a/3*_x^(-1)");
		sub = Substitution.substitute(sub, new Expression("_a"), linear);
		sub = Substitution.substitute(sub, new Expression("_x"), polynomial.getVariable());
		sub = Substitution.substitute(polynomial.getExpression(), new Expression(variable), sub);
		
		/* clearing the denominator turns this into a quadratic in x^3 */
		Expression x3 = Expression.exponentiate(polynomial.getVariable(), new Expression("3"));
		sub = Expression.multiply(sub, x3);
		sub = Simplify.simplify(sub);
		sub = Expander.expand(sub, variable);
		sub = Collector.collect(sub, variable);
		sub = Simplify.simplify(sub);
		polynomial = new Polynomial(sub, variable);
		
		if (!polynomial.isValid()) {
			return null;
		}
		
		if (polynomial.getDegree() == 1) {
			return polynomial.getExpression();
		}
		
		if (polynomial.getDegree() != 2) {
			return null;
		}
		
		Expression result = solveQuadratic(polynomial, variable);
		
		return result;
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
			Long exponent = lhs.getRight().getSymbolAsInteger();
			Expression inverse = Expression.divide(new Expression("1"), lhs.getRight());
			expression = Expression.exponentiate(rhs, inverse);
			if (exponent == null) {
				results.add(expression);
			} else {
				for (Long i = new Long(1); i <= exponent; ++i) {
					Expression denominator = new Expression(i.toString());
					Expression pi2 = Expression.multiply(new Expression("2"), new Expression("pi"));
					Expression theta = Expression.divide(pi2, lhs.getRight());
					Expression root = Expression.exponentiate(Expression.exponentiate(theta), denominator);
					results.add(Expression.multiply(expression, root));
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
