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
			if (solution.isDegenerate()) {
				continue; /* solution contains zero divide */
			}
			Expression sub = Substitution.substitute(expression, new Expression("x"), solution);
			if (sub.isDegenerate()) {
				continue; /* solution causes zero divide */
			}
			String hash = Canonicalizer.toString(solution);
			if (duplicates.get(hash) == null) {
				solutions.add(solution);
				duplicates.put(hash, true);
			}
		}
		
		return solutions;
	}

	static List<Equation> factorize(Expression lhs, Expression rhs, String variable) {
		List<Equation> results = new ArrayList<Equation>();
	
		/* try to eliminate common subexpressions: (x^2 + x)^2 + (x^2 + x) -> u^2 + u */
		Expression subexpression = Substitution.candidate(lhs, variable);
		if (subexpression != null) {
			String newVariable = Substitution.allocateVariable(lhs);
			Expression substitution = Substitution.substitute(lhs, subexpression, new Expression(newVariable));
			substitution = Expression.equals(substitution, rhs);
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
						Expression equation = Expression.equals(factor, new Expression("0"));
						List<Expression> solutions = Solver.solve(equation, variable);
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
		lhs = Simplify.simplify(lhs);
		if (isSolvable(lhs, variable)) {
			results.add(new Equation(lhs, rhs));
			return results;
		}
		
		Expression rationalSolve = solveRational(lhs, rhs, variable);
		if (rationalSolve != null) {
			results.add(new Equation(rationalSolve, new Expression("0")));
			return results;
		}
		
		polynomial = new Polynomial(Expression.subtract(lhs, rhs), variable);
		if (polynomial.isValid()) {
			List<Equation> roots = solvePolynomial(polynomial, variable);
			if (!roots.isEmpty()) {
				return roots;
			}
		}
		
		Expression lambertWSolve = solveLambertW(Expression.subtract(lhs, rhs), variable);
		if (lambertWSolve != null) {
			lambertWSolve = Expression.subtract(new Expression(variable), lambertWSolve);
			if (isSolvable(lambertWSolve, variable)) {
				results.add(new Equation(lambertWSolve, new Expression("0")));
				return results;
			}
		}
		
		return results;
	}
	
	static List<Equation> solvePolynomial(Polynomial polynomial, String variable) {
		List<Equation> results = new ArrayList<Equation>();
	
		Long degree = polynomial.getDegree();
		if (degree < 2 || degree > 3) {
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

		List<Expression> solutions = solveCubic(polynomial, variable);
		for (Expression solution : solutions) {
			if (term != null) { /* reverse the substitution performed above */
				solution = Expression.add(solution, term);
			}
			results.add(new Equation(new Expression(variable), solution));
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
	
	static List<Expression> solveCubic(Polynomial polynomial, String variable) {
		/* make Vieta's substitution */
		Expression linear = polynomial.getCoefficient(1);
		Expression sub = Parser.parse("_x - _a/3*_x^(-1)");
		sub = Substitution.substitute(sub, new Expression("_a"), linear);
		Expression result = Substitution.substitute(polynomial.getExpression(), polynomial.getVariable(), sub);
		
		/* clearing the denominator turns this into a quadratic in x^3 */
		Expression x3 = Parser.parse("_x^3");
		result = Expression.multiply(result, x3);
		
		/* fold the expression back into polynomial form */
		result = Simplify.simplify(result);
		result = Expander.expand(result, "_x");
		result = Collector.collect(result, "_x");
		result = Substitution.substitute(result, new Expression("_x"), polynomial.getVariable());
		result = Simplify.simplify(result);
		result = Expander.expand(result, "_x");
		result = Simplify.simplify(result);
		result = Collector.collect(result, "_x");
		result = Simplify.simplify(result);

		polynomial = new Polynomial(result, variable);
		if (!polynomial.isValid()) {
			return null;
		}
		
		if (polynomial.getDegree() == 2) {
			/* a degree 2 polynomial in x^3 */
			result = solveQuadratic(polynomial, variable);
		}

		result = Expression.equals(result, new Expression("0"));
		List<Expression> roots = Solver.solve(result, variable);
		List<Expression> solutions = new ArrayList<Expression>();
		for (Expression root : roots) {
			/* reverse Vieta's substitution */
			solutions.add(Substitution.substitute(sub, new Expression("_x"), root));
		}

		return solutions;
	}
	
	static Expression denominatorExpression(Expression expression, String variable) {
		if (expression == null) {
			return null;
		}
		if (expression.getType().equals(Expression.Type.NODE_DIVIDE)) {
			if (expression.getRight().contains(variable)) {
				return expression.getRight();
			}
		}
		Expression lhs = denominatorExpression(expression.getLeft(), variable);
		if (lhs != null) {
			return lhs;
		}
		Expression rhs = denominatorExpression(expression.getRight(), variable);
		if (rhs != null) {
			return rhs;
		}
		return null;
	}
	
	static Expression solveRational(Expression lhs, Expression rhs, String variable) {
		/* x + a/x -> x^2 + a */
		Expression denominator = denominatorExpression(lhs, variable);
		if (denominator == null) {
			return null;
		}
		int iterationCount = 0;
		Expression newLhs = lhs;
		Expression newRhs = rhs;
		while (denominator != null) {
			if (++iterationCount == 4) {
				return null;
			}
			newRhs = Expression.multiply(newRhs, denominator);
			List<Expression> terms = Iterator.getTerms(lhs);
			List<Expression> newTerms = new ArrayList<Expression>();
			for (Expression term : terms) {
				newTerms.add(Expression.multiply(term, denominator));
			}
			newLhs = Iterator.listSum(newTerms);
			newLhs = Simplify.simplify(newLhs);
			denominator = denominatorExpression(newLhs, variable);
		}
		return Expression.subtract(newLhs, newRhs);
	}

	static Expression solveLambertW(Expression expression, String variable) {
		Expression result = Collector.collect(expression, variable);
		result = Simplify.simplify(result);
		boolean normalize = false;
		List<Expression> newTerms = new ArrayList<Expression>();
		List<Expression> terms = Iterator.getTerms(result);
		for (Expression term : terms) {
			if (Iterator.hasFactor(term, new Expression(variable))) {
				Expression norm = Expression.divide(term, new Expression(variable));
				norm = Simplify.simplify(norm);
				if (Iterator.hasFactor(norm, new Expression(variable))) {
					return null; /* failed to eliminate factor */
				}
				if (!Simplify.getExponent(norm).toString().equals(variable)) {
					if (normalize) {
						return null; /* unable to solve */
					}
					normalize = true; /* an expression of the form a^x=b*x */
				}
				newTerms.add(norm);
			} else {
				newTerms.add(term);
			}
		}
		Expression coefficient = newTerms.get(0);
		Expression base = Simplify.getBase(newTerms.get(1));
		Expression exponent = Simplify.getExponent(newTerms.get(1));
		if (!exponent.toString().equals(variable)) {
			coefficient = newTerms.get(1);
			base = Simplify.getBase(newTerms.get(0));
			exponent = Simplify.getExponent(newTerms.get(0));
			if (!exponent.toString().equals(variable)) {
				return null; /* unable to solve */
			}
		}
		if (normalize) { /* a^x=b*x -> a^x/x=b -> x/a^x=1/b -> -u=x and u*a^u=-1/b */
			coefficient = Expression.divide(new Expression("1"), coefficient);
		} else { /* we actually deal with a^x-b*x, not a^x=b*x */
			coefficient = Expression.negate(coefficient);
		}
		/* u*a^u=c -> u*e^[ln(a)*u]=c -> ln(a)*u*e^[ln(a)*u]=ln(a)*c */
		Expression logBase = Expression.logarithm(new Expression("e"), base);
		coefficient = Expression.multiply(coefficient, logBase);
		/* k*x*e^(k*x)=c -> x=W(c)/k */
		result = Expression.lambertW(coefficient);
		result = Expression.divide(result, logBase);
		if (normalize) { /* -u=x -> x=-u */
			result = Expression.negate(result);
		}
		return Simplify.simplify(result);
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
