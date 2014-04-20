package jsolve;

import java.util.ArrayList;
import java.util.List;

public class Simplify {
	Simplify() {
		String[] rules = {
			"x+0 = x",
			"x-0 = x",
			"0-x = -x",
			"x-x = 0",
			"x*0 = 0",
			"x*1 = x",
			"0/x = 0",
			"x/1 = x",
			"x/x = 1",
			"x*1/x = 1",
			"1/(1/x) = x",
			"x^0 = 1",
			"x^1 = x",
			"0^x = 0",
			"1^x = 1",
			"0! = 1",
			"x!*(x+1) = (x+1)!",
			"x!/x = (x-1)!"
		};
	
		identities = new ArrayList<Expression>();
		for (String rule : rules) {
			identities.add(Parser.parse(rule));
		}
	}

	boolean compareOperators(Expression a, Expression b) {
		if (a == null || b == null) {
			return a == null && b == null;
		}
		boolean result = true;
		result &= compareOperators(a.getLeft(), b.getLeft());
		result &= compareOperators(a.getLeft(), b.getLeft());
		result &= compareOperators(a.getLeft(), b.getLeft());
		if (!a.getType().equals(b.getType())) {
			result = false;
		}
		return result;
	}

	boolean checkIdentity(Expression expression, Expression subExpression, Expression identity) {
		Expression variable = new Expression(Substitution.allocateVariable(expression));
		Expression a = Substitution.substitute(expression, subExpression, variable);
		Expression b = Substitution.substitute(identity, new Expression("x"), variable);
		return compareOperators(a, b.getLeft());
	}

	Expression applyIdentitiesSubstitute(Expression expression, Expression subExpression) {
		if (expression == null || subExpression == null) {
			return expression;
		}

		Expression result = expression.copy();
		result.setLeft(applyIdentities(expression.getLeft()));
		result.setRight(applyIdentities(expression.getRight()));
		result.setChild(applyIdentities(expression.getChild()));

		for (Expression identity : identities) {
			Expression match = Substitution.substitute(identity, new Expression("x"), subExpression);
			if (Canonicalizer.compare(expression, match.getLeft())) {
				return match.getRight();
			}
		}

		return result;
	}
	
	Expression applyIdentities(Expression expression) {
		if (expression != null) {
			Expression result = expression.copy();
			result = applyIdentitiesSubstitute(result, result.getLeft());
			result = applyIdentitiesSubstitute(result, result.getRight());
			result = applyIdentitiesSubstitute(result, result.getChild());
			return result;
		}
		return null;
	}

	Expression foldConstants(Expression expression) {
		Double lhs = null;
		Double rhs = null;
		Double arg = null;
		
		expression = applyIdentities(expression);

		try {
			if (expression.isBinary()) {
				if (!expression.getLeft().isSymbol() || !expression.getRight().isSymbol()) {
					return expression;
				}
				lhs = Double.parseDouble(expression.getLeft().getSymbol());
				rhs = Double.parseDouble(expression.getRight().getSymbol());
			} else if (expression.isUnary()) {
				if (!expression.getChild().isSymbol()) {
					return expression;
				}
				arg = Double.parseDouble(expression.getChild().getSymbol());
			} else {
				return expression;
			}
		} catch (NumberFormatException e) {
			return expression;
		}
	
		switch (expression.getType()) {
		case NODE_ADD:
			return new Expression(lhs + rhs);
		case NODE_SUBTRACT:
			return new Expression(lhs - rhs);
		case NODE_MULTIPLY:
			return new Expression(lhs * rhs);
		case NODE_DIVIDE:
			return new Expression(lhs / rhs);
		case NODE_EXPONENTIATE:
			return new Expression(Math.pow(lhs,  rhs));
		case NODE_PLUS:
			return new Expression(arg);
		case NODE_MINUS:
			return new Expression(-arg);
		case NODE_LOGARITHM:
			return new Expression(Math.log(rhs) / Math.log(lhs));
		case NODE_FACTORIAL:
		case NODE_FACTORIAL_INVERSE:
		default:
			break;
		}
		
		return expression;
	}
	
	Expression foldSum(List<Expression> terms) {
		for (int i = 0; i < terms.size(); ++i) {
			for (int j = 0; j < terms.size(); ++j) {
				if (i == j) {
					continue;
				}
				List<Expression> twoTerms = new ArrayList<Expression>();
				twoTerms.add(terms.get(i));
				twoTerms.add(terms.get(j));
				Expression sum = Iterator.listSum(twoTerms);
				Expression simplified = foldConstants(sum);
				if (!sum.toString().equals(simplified.toString())) {
					/* rebuild the list with our simplified term  */
					terms.set(i, simplified);
					terms.remove(j);
					if (terms.size() > 1) {
						return foldSum(terms);
					}
				}
			}
		}
		return Iterator.listSum(terms);
	}
	
	Expression foldProduct(List<Expression> factors) {
		for (int i = 0; i < factors.size(); ++i) {
			for (int j = 0; j < factors.size(); ++j) {
				if (i == j) {
					continue;
				}
				List<Expression> twoFactors = new ArrayList<Expression>();
				twoFactors.add(factors.get(i));
				twoFactors.add(factors.get(j));
				Expression product = Iterator.listProduct(twoFactors);
				Expression simplified = foldConstants(product);
				if (!product.toString().equals(simplified.toString())) {
					/* rebuild the list with our simplified factor */
					factors.set(i, simplified);
					factors.remove(j);
					if (factors.size() > 1) {
						return foldProduct(factors);
					}
				}
			}
		}
		return Iterator.listProduct(factors);
	}
	
	Expression getBase(Expression expression) {
		if (expression.getType().equals(Expression.Type.NODE_EXPONENTIATE)) {
			return getBase(expression.getLeft());
		}
		return expression;
	}

	Expression getExponent(Expression expression) {
		if (expression.getType().equals(Expression.Type.NODE_EXPONENTIATE)) {
			Expression exponent = getExponent(expression.getLeft());
			if (exponent.isOne()) {
				return expression.getRight();
			} else {
				return Expression.multiply(expression.getRight(), exponent);
			}
		}
		return new Expression("1");
	}
	
	Expression foldExponential(Expression expression) {
		Expression exponent = getExponent(expression);
		if (exponent.isOne()) {
			return expression;
		}

		Expression base = getBase(expression);
		List<Expression> factors = Iterator.getFactors(base, 0);
		List<Expression> variables = new ArrayList<Expression>();
		List<Expression> constants = new ArrayList<Expression>();
		for (Expression factor : factors) {
			if (factor.isSymbol()) {
				try {
					Double.parseDouble(factor.getSymbol());
					constants.add(factor);
					continue;
				} catch (NumberFormatException e) {
				}
			}
			variables.add(factor);
		}
		
		if (constants.size() == 0) {
			return Expression.exponentiate(getBase(expression), exponent);
		}
		
		/* factor (a*b)^c into a^c*b^c, if b is a constant */
		Expression variable = Iterator.listProduct(variables);
		Expression lhs = Expression.exponentiate(variable, exponent);
		Expression constant = Iterator.listProduct(constants);
		Expression rhs = Expression.exponentiate(constant, exponent);
		return Expression.multiply(lhs, rhs);
	}
	
	Expression fold(Expression expression) {
		if (expression == null) {
			return null;
		}
		
		if (expression.isSymbol()) {
			return expression;
		}
	
		Expression result = expression.copy();
	
		/* fold sums */
		List<Expression> terms = Iterator.getTerms(result);
		if (terms.size() > 1) {
			for (int i = 0; i < terms.size(); ++i) {
				terms.set(i, foldConstants(fold(terms.get(i))));
			}
			result = foldSum(terms);
		}
		
		/* fold products */
		List<Expression> factors = Iterator.getFactors(result, 0);
		if (factors.size() > 1) {
			for (int i = 0; i < factors.size(); ++i) {
				factors.set(i, foldConstants(fold(factors.get(i))));
			}
			result = foldProduct(factors);
		}
		
		/* fold exponents */
		result = foldExponential(result);

		/* addition, subtraction, and their inverses already handled */
		switch (result.getType()) {
		case NODE_ADD:
		case NODE_SUBTRACT:
		case NODE_MULTIPLY:
			return result;
		case NODE_DIVIDE:
			if (!result.getLeft().isOne()) {
				return result;
			}
		default:
			break;
		}
		
		/* recurse into other types of operator */
		result.setLeft(fold(result.getLeft()));
		result.setRight(fold(result.getRight()));
		result.setChild(fold(result.getChild()));
		return foldConstants(result);
	}

	Expression simplifyExpression(Expression expression) {	
		/* iterate while the expression keeps changing */
		String hash = Canonicalizer.toString(expression);
		while (true) {
			expression = fold(expression);
			String newHash = Canonicalizer.toString(expression);
			if (hash.equals(newHash)) {
				break;
			}
			hash = newHash;
		}
		return expression;
	}
	
	static Expression simplify(Expression expression) {
		Simplify simplify = new Simplify();
		Expression result = expression.copy();
		result = simplify.simplifyExpression(result);
		return result;
	}
	
	List<Expression> identities;
}
