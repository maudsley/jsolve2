package jsolve;

import java.util.ArrayList;
import java.util.List;

public class Simplify {
	Simplify() {
		String[] rules = {
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
	
	Expression applyIdentities(Expression expression) {
		for (Expression identity : identities) {
			if (expression.isBinary()) {
				Expression matchLeft = Substitution.substitute(identity, new Expression("x"), expression.getLeft());
				if (Canonicalizer.compare(expression, matchLeft.getLeft())) {
					return matchLeft.getRight();
				}
				Expression matchRight = Substitution.substitute(identity, new Expression("x"), expression.getRight());
				if (Canonicalizer.compare(expression, matchRight.getLeft())) {
					return matchRight.getRight();
				}
			} else if (expression.isUnary()) {
				Expression matchCild = Substitution.substitute(identity, new Expression("x"), expression.getChild());
				if (Canonicalizer.compare(expression, matchCild.getLeft())) {
					return matchCild.getRight();
				}
			}
		}
		return expression;
	}

	static long gcd(long a, long b) {
		if (b == 0) {
			return a;
		} else {
			return gcd(b, a % b);
		}
	}
	
	Expression foldFractionSum(Expression expression) {
		/* a/b +/- c/d = (ad +/- cb) / (bd) */
		Double a = expression.getLeft().getLeft().getSymbolAsFloat();
		Double b = expression.getLeft().getRight().getSymbolAsFloat();
		Double c = expression.getRight().getLeft().getSymbolAsFloat();
		Double d = expression.getRight().getRight().getSymbolAsFloat();
		if (a == null || b == null || c == null || d == null) {
			return expression;
		}
		Expression result = Parser.parse("(_a*_d + _s*_c*_b) / (_b * _d)");
		result = Substitution.substitute(result, new Expression("_a"), expression.getLeft().getLeft());
		result = Substitution.substitute(result, new Expression("_b"), expression.getLeft().getRight());
		result = Substitution.substitute(result, new Expression("_c"), expression.getRight().getLeft());
		result = Substitution.substitute(result, new Expression("_d"), expression.getRight().getRight());
		if (expression.getType().equals(Expression.Type.NODE_ADD)) {
			result = Substitution.substitute(result, new Expression("_s"), new Expression("1"));
		} else { /* subtract instead */
			result = Substitution.substitute(result, new Expression("_s"), new Expression("-1"));
		}
		return result;
	}

	Expression foldAddition(Expression lhs, Expression rhs) {
		if (lhs.isZero()) {
			return rhs; /* 0 + x = x */
		} else if (rhs.isZero()) {
			return lhs; /* x + 0 = x */
		}
		Double left = lhs.getSymbolAsFloat();
		Double right = rhs.getSymbolAsFloat();
		if (left != null && right != null) {
			return new Expression(left + right);
		}
		Expression result = Expression.add(foldConstants(lhs), foldConstants(rhs));
		if (lhs.getType().equals(Expression.Type.NODE_DIVIDE) && rhs.getType().equals(Expression.Type.NODE_DIVIDE)) {
			Expression trial = foldFractionSum(result);
			if (!trial.toString().equals(result.toString())) {
				return trial;
			}
		}
		return result;
	}
	
	Expression foldSubtraction(Expression lhs, Expression rhs) {
		if (rhs.isZero()) {
			return lhs; /* x - 0 = x */
		} else if (lhs.isZero()) {
			return Expression.negate(rhs); /* 0 - x = -x */
		} else if (Canonicalizer.compare(lhs, rhs)) {
			return new Expression("0"); /* x - x = 0 */
		}
		Double left = lhs.getSymbolAsFloat();
		Double right = rhs.getSymbolAsFloat();
		if (left != null && right != null) {
			return new Expression(left - right);
		}
		Expression result = Expression.subtract(foldConstants(lhs), foldConstants(rhs));
		if (lhs.getType().equals(Expression.Type.NODE_DIVIDE) && rhs.getType().equals(Expression.Type.NODE_DIVIDE)) {
			Expression trial = foldFractionSum(result);
			if (!trial.toString().equals(result.toString())) {
				return trial;
			}
		}
		return result;
	}

	Expression foldDivision(Expression lhs, Expression rhs) {
		/* keep numbers rational */
		Long lhsValue = lhs.getSymbolAsInteger();
		Long rhsValue = rhs.getSymbolAsInteger();
		if (lhsValue != null && rhsValue != null) {
			long product = lhsValue * rhsValue;
			lhsValue = Math.abs(lhsValue);
			rhsValue = Math.abs(rhsValue);
			long commonFactor = gcd(lhsValue, rhsValue);
			lhsValue /= commonFactor;
			rhsValue /= commonFactor;
			lhs = new Expression(lhsValue.toString());
			rhs = new Expression(rhsValue.toString());
			if (product < 0) {
				return Expression.negate(Expression.divide(lhs, rhs));
			}
		}
		return Expression.divide(foldConstants(lhs), foldConstants(rhs));
	}

	Expression foldUnaryMinus(Expression arg) {
		Double value = arg.getSymbolAsFloat();
		if (value != null) { /* move the sign onto the constant */
			return new Expression(-value);
		}
		return Expression.multiply(new Expression("-1"), foldConstants(arg));
	}

	Expression foldSin(Expression arg) {
		String[][] table = {
			{"0", "0"},
			{"pi/2", "1"},
			{"pi", "0"},
			{"pi*2/3", "3^(1/2)/2"},
			{"pi*6/4", "-1"},
			{"pi*2", "0"}
		};
		for (String[] pair : table) {
			if (Canonicalizer.compare(Parser.parse(pair[0]), arg)) {
				return Parser.parse(pair[1]);
			}
		}
		Double value = arg.getSymbolAsFloat();
		if (value != null) {
			return new Expression(Math.sin(value));
		}
		Expression result = new Expression(Expression.Type.NODE_SINE);
		result.setChild(foldConstants(arg));
		return result;
	}
	
	Expression foldCos(Expression arg) {
		String[][] table = {
			{"0", "1"},
			{"pi/2", "0"},
			{"pi", "-1"},
			{"pi*2/3", "-1/2"},
			{"pi*6/4", "0"},
			{"pi*2", "1"}
		};
		for (String[] pair : table) {
			if (Canonicalizer.compare(Parser.parse(pair[0]), arg)) {
				return Parser.parse(pair[1]);
			}
		}
		Double value = arg.getSymbolAsFloat();
		if (value != null) {
			return new Expression(Math.cos(value));
		}
		Expression result = new Expression(Expression.Type.NODE_COSINE);
		result.setChild(foldConstants(arg));
		return result;
	}

	Expression foldConstants(Expression expression) {
		/* identity folding pending rewrite */
		expression = applyIdentities(expression);

		/* new handling - do not assume the branches are constants */
		switch (expression.getType()) {
		case NODE_ADD:
			return foldAddition(expression.getLeft(), expression.getRight());
		case NODE_SUBTRACT:
			return foldSubtraction(expression.getLeft(), expression.getRight());
		case NODE_DIVIDE:
			return foldDivision(expression.getLeft(), expression.getRight());
		case NODE_PLUS:
			return expression;
		case NODE_MINUS:
			return foldUnaryMinus(expression.getChild());
		case NODE_SINE:
			return foldSin(expression.getChild());
		case NODE_COSINE:
			return foldCos(expression.getChild());
		default:
			break;
		}

		/* old handling - assume constants */
		if (expression.isBinary()) {
			Double lhs = expression.getLeft().getSymbolAsFloat();
			Double rhs = expression.getRight().getSymbolAsFloat();
			if (lhs != null && rhs != null) {
				switch (expression.getType()) {
				case NODE_ADD:
					return new Expression(lhs + rhs);
				case NODE_SUBTRACT:
					return new Expression(lhs - rhs);
				case NODE_MULTIPLY:
					return new Expression(lhs * rhs);
				case NODE_EXPONENTIATE:
					return new Expression(Math.pow(lhs,  rhs));
				case NODE_LOGARITHM:
					return new Expression(Math.log(rhs) / Math.log(lhs));
				default:
					break;
				}
			}
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
	
	Expression getNumerator(Expression expression) {
		if (expression.getType().equals(Expression.Type.NODE_DIVIDE)) {
			return expression.getLeft();
		} else {
			return expression;
		}
	}
	
	Expression getDenominator(Expression expression) {
		if (expression.getType().equals(Expression.Type.NODE_DIVIDE)) {
			return expression.getRight();
		} else {
			return new Expression("1");
		}
	}
	
	Expression foldExponential(Expression expression) {
		expression = applyIdentities(expression);
	
		Expression exponent = fold(getExponent(expression));
		if (exponent.isOne()) {
			return expression;
		}
		
		Expression base = fold(getBase(expression));
		if (base.isOne()) {
			return base;
		}
		
		Expression numerator = getNumerator(base);
		Expression denominator = getDenominator(base);
		if (numerator.getSymbolAsInteger() != null || denominator.getSymbolAsInteger() != null) {
			if (!denominator.isOne()) {
				/* (1/a)^b -> 1/a^b */
				numerator = foldConstants(Expression.exponentiate(numerator, exponent));
				denominator = foldConstants(Expression.exponentiate(denominator, exponent));
				return Expression.divide(numerator, denominator);
			}
		}
		
		if (base.isSymbol()) {
			if (base.getSymbol().equals("i")) {
				Long power = exponent.getSymbolAsInteger();
				if (power != null && power >= 0) {
					String[] powers = {"1", "i", "-1", "-i"};
					return new Expression(powers[power.intValue()%4]);
				}
			}
		}
		
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
		return Expression.multiply(foldConstants(lhs), foldConstants(rhs));
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
		result = Collector.normalizeExponents(result);
		result = foldExponential(result);

		/* addition, subtraction, and their inverses already handled */
		switch (result.getType()) {
		case NODE_ADD:
		case NODE_SUBTRACT:
		case NODE_MULTIPLY:
			return result;
		case NODE_DIVIDE:
			/* a/b becomes a*1/b, so let us see into the reciprocal */
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
