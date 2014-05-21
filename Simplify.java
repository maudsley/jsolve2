package jsolve;

import java.util.ArrayList;
import java.util.List;

public class Simplify {
	static long gcd(long a, long b) {
		if (b == 0) {
			return a;
		} else {
			return gcd(b, a % b);
		}
	}
	
	Expression signSubstitute(Expression where, Expression with) {
		if (with.getType().equals(Expression.Type.NODE_ADD)) {
			return Substitution.substitute(where, new Expression("_s"), new Expression("1"));
		} else if (with.getType().equals(Expression.Type.NODE_SUBTRACT)) { /* subtract instead */
			return Substitution.substitute(where, new Expression("_s"), new Expression("-1"));
		} else {
			return where;
		}
	}
	
	Expression foldLeftFractionSum(Expression expression) {
		/* a/b +/- v = (a +/- v*b) / b */
		if (!expression.getLeft().getType().equals(Expression.Type.NODE_DIVIDE)) {
			return expression;
		}
		
		Double a = expression.getLeft().getLeft().getSymbolAsFloat();
		Double b = expression.getLeft().getRight().getSymbolAsFloat();
		Double v = expression.getRight().getSymbolAsFloat();
		if (a == null || b == null || v == null) {
			return expression;
		}

		Expression result = Parser.parse("(_a + _s*_v*_b) / _b");
		result = Substitution.substitute(result, new Expression("_v"), expression.getRight());
		result = Substitution.substitute(result, new Expression("_a"), expression.getLeft().getLeft());
		result = Substitution.substitute(result, new Expression("_b"), expression.getLeft().getRight());
		return signSubstitute(result, expression);
	}
	
	Expression foldRightFractionSum(Expression expression) {
		/* a/b +/- v = (a +/- v*b) / b */
		if (!expression.getRight().getType().equals(Expression.Type.NODE_DIVIDE)) {
			return expression;
		}
		
		Double u = expression.getLeft().getSymbolAsFloat();
		Double c = expression.getRight().getLeft().getSymbolAsFloat();
		Double d = expression.getRight().getRight().getSymbolAsFloat();
		if (u == null || c == null || d == null) {
			return expression;
		}

		Expression result = Parser.parse("(_u*_d + _s*_c) / _d");
		result = Substitution.substitute(result, new Expression("_u"), expression.getLeft());
		result = Substitution.substitute(result, new Expression("_c"), expression.getRight().getLeft());
		result = Substitution.substitute(result, new Expression("_d"), expression.getRight().getRight());
		return signSubstitute(result, expression);
	}
	
	Expression foldFractionSum(Expression expression) {
		expression = foldLeftFractionSum(foldRightFractionSum(expression));
		
		if (!expression.getLeft().getType().equals(Expression.Type.NODE_DIVIDE)) {
			return expression;
		}
		
		if (!expression.getRight().getType().equals(Expression.Type.NODE_DIVIDE)) {
			return expression;
		}
		
		Double a = expression.getLeft().getLeft().getSymbolAsFloat();
		Double b = expression.getLeft().getRight().getSymbolAsFloat();
		Double c = expression.getRight().getLeft().getSymbolAsFloat();
		Double d = expression.getRight().getRight().getSymbolAsFloat();
	
		/* a/b +/- c/d = (ad +/- cb) / (bd) */
		if (a != null && b != null && c != null && d != null) {
			Expression result = Parser.parse("(_a*_d + _s*_c*_b) / (_b * _d)");
			result = Substitution.substitute(result, new Expression("_a"), expression.getLeft().getLeft());
			result = Substitution.substitute(result, new Expression("_b"), expression.getLeft().getRight());
			result = Substitution.substitute(result, new Expression("_c"), expression.getRight().getLeft());
			result = Substitution.substitute(result, new Expression("_d"), expression.getRight().getRight());
			return signSubstitute(result, expression);
		}
		
		return expression;
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
		Expression trial = foldFractionSum(result);
		if (!trial.toString().equals(result.toString())) {
			return trial;
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
		Expression trial = foldFractionSum(result);
		if (!trial.toString().equals(result.toString())) {
			return trial;
		}
		return result;
	}
	
	Expression foldExponents(Expression expression) {
		Double expLhs = getExponent(expression.getLeft()).getSymbolAsFloat();
		Double expRhs = getExponent(expression.getRight()).getSymbolAsFloat();
		if (expLhs == null || expRhs == null) {
			return expression;
		}
		Expression baseLhs = getBase(expression.getLeft());
		Expression baseRhs = getBase(expression.getRight());
		if (Canonicalizer.compare(baseLhs, baseRhs)) {
			switch (expression.getType()) {
			case NODE_MULTIPLY: /* x^a * x^b = x^(a+b) */
				return Expression.exponentiate(baseLhs, new Expression(expLhs + expRhs));
			case NODE_DIVIDE: /* x^a / x^b = x^(a-b) */
				return Expression.exponentiate(baseLhs, new Expression(expLhs - expRhs));
			default:
				break;
			}
		}
		return expression;
	}

	Expression foldMultiplication(Expression expression) {
		expression = foldExponents(expression);
		if (!expression.getType().equals(Expression.Type.NODE_MULTIPLY)) {
			return expression;
		}
		
		Expression lhs = expression.getLeft();
		Expression rhs = expression.getRight();
		Double lhsValue = lhs.getSymbolAsFloat();
		if (lhsValue != null) {
			if (lhsValue == 0) {
				return new Expression("0"); /* 0 * x = 0 */
			} else if (lhsValue == 1) {
				return rhs; /* 1 * x = x */
			}
		}
		
		Double rhsValue = rhs.getSymbolAsFloat();
		if (rhsValue != null) {
			if (rhsValue == 0) {
				return new Expression("0"); /* x * 0 = 0 */
			} else if (rhsValue == 1) {
				return lhs; /* x * 1 = x */
			}
		}
		
		if (lhsValue != null && rhsValue != null) {
			return new Expression(lhsValue * rhsValue);
		}
		
		return Expression.multiply(foldConstants(lhs), foldConstants(rhs));
	}

	Expression foldDivision(Expression expression) {
		expression = foldExponents(expression);
		if (!expression.getType().equals(Expression.Type.NODE_DIVIDE)) {
			return expression;
		}
		
		Expression lhs = expression.getLeft();
		Expression rhs = expression.getRight();
		if (Canonicalizer.compare(lhs, rhs)) {
			return new Expression("1"); /* x / x = 1 */
		}

		Long lhsValue = lhs.getSymbolAsInteger();
		Long rhsValue = rhs.getSymbolAsInteger();
		
		if (lhsValue != null && lhsValue == 0) {
			return lhs; /* 0 / x = 0 */
		}
		
		if (lhsValue == null || rhsValue == null) {
			return Expression.divide(foldConstants(lhs), foldConstants(rhs));
		}
		
		if (rhsValue == 1) {
			return lhs; /* x / 1 = x */
		}
		
		long product = lhsValue * rhsValue;
		lhsValue = Math.abs(lhsValue);
		rhsValue = Math.abs(rhsValue);

		/* keep numbers rational */
		long commonFactor = gcd(lhsValue, rhsValue);
		lhsValue /= commonFactor;
		rhsValue /= commonFactor;

		lhs = new Expression(lhsValue.toString());
		rhs = new Expression(rhsValue.toString());

		Expression result = Expression.divide(lhs, rhs);
		if (product < 0) {
			result = Expression.negate(result);
		}

		return result;
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
	
	Expression exactExponentation(Double base, Double expNumerator, Double expDenominator) {
		Expression exponent = Expression.divide(new Expression(expNumerator), new Expression(expDenominator));
		Expression exponentation = Expression.exponentiate(new Expression(base), exponent);
		if (expDenominator == 0) {
			return exponentation; /* cannot simplify due to zero divide */
		}
		if (expNumerator == 0) {
			return new Expression("1"); /* x^0 = 1 */
		}
		Double result = Math.floor(Math.pow(base, expNumerator/expDenominator));
		if (Math.pow(result, expDenominator/expNumerator) == base) {
			return new Expression(result);
		}
		result = Math.floor(Math.pow(base, 1/expDenominator));
		if (Math.pow(result, expDenominator) == base) { /* x^(a/b) -> [x^(1/b)]^a -> y^a */
			return Expression.exponentiate(new Expression(result), new Expression(expNumerator));
		}
		result = Math.floor(Math.pow(base, expNumerator));
		if (Math.pow(result, 1.0/expNumerator) == base) { /* x^(a/b) -> (x^a)^(1/b) -> y^(1/b) */
			Expression denominator = Expression.divide(new Expression("1"), new Expression(expDenominator));
			return Expression.exponentiate(new Expression(result), denominator);
		}		
		return exponentation; /* unable to simplify because an exact result could not be obtained */
	}
	
	Expression foldExponential(Expression expression) {	
		Expression exponent = fold(getExponent(expression));
		Expression base = fold(getBase(expression));
		
		if (exponent.isZero()) {
			return new Expression("1"); /* x^0 = 1 */
		} else if (exponent.isOne()) {
			return base; /* x^1 = x */
		}

		if (base.isZero()) {
			return new Expression("0"); /* 0^x = 0 */
		} else if (base.isOne()) {
			return base; /* 1^x = 1 */
		}
		
		Double baseValue = base.getSymbolAsFloat();
		if (baseValue != null) { /* attempt to fold numeric constants */
			if (baseValue < 0 && baseValue != -1) {
				Expression lhs = Expression.exponentiate(new Expression("-1"), exponent);
				Expression rhs = Expression.exponentiate(new Expression(Math.abs(baseValue)), exponent);
				return Expression.multiply(foldExponential(lhs), foldExponential(rhs));
			}
			Double expNumValue = getNumerator(exponent).getSymbolAsFloat();
			Double expDenValue = getDenominator(exponent).getSymbolAsFloat();
			if (expNumValue != null && expDenValue != null) {
				if (baseValue > 0) {
					return exactExponentation(baseValue, expNumValue, expDenValue);
				} else if (baseValue == -1) {
					Expression pi = new Expression("pi"); /* ln(-1) = pi */
					Expression complexExponent = Expression.multiply(pi, exponent);
					return Expression.exponentiate(complexExponent);
				}
			}
		}
		
		if (base.isSymbol()) {
			if (base.getSymbol().equals("i")) {
				Long power = exponent.getSymbolAsInteger();
				if (power != null && power >= 0) {
					String[] powers = {"1", "i", "-1", "-i"};
					return new Expression(powers[power.intValue()%4]);
				}
			} else if (base.getSymbol().equals("e")) {
				List<Expression> exponentFactors = Iterator.getFactors(exponent, 5);
				List<Expression> realFactors = new ArrayList<Expression>();
				boolean imaginary = false;
				for (Expression factor : exponentFactors) {
					if (factor.toString().equals("i")) {
						imaginary = true;
					} else {
						realFactors.add(factor);
					}
				}
				if (imaginary) {
					Expression realExponent = Iterator.listProduct(realFactors);
					return Expression.exponentiate(realExponent);
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
		
		if (constants.size() == 0 || variables.size() == 0) {
			if (getNumerator(base).isOne()) { /* (1/x)^n -> 1/x^n */
				Expression denom = Expression.exponentiate(getDenominator(base), exponent);
				return Expression.divide(new Expression("1"), denom);
			} else {
				return Expression.exponentiate(base, exponent);
			}
		}
		
		/* factor (a*b)^c into a^c*b^c, if b is a constant */
		Expression variable = Iterator.listProduct(variables);
		Expression lhs = Expression.exponentiate(variable, exponent);
		Expression constant = Iterator.listProduct(constants);
		Expression rhs = Expression.exponentiate(constant, exponent);
		return Expression.multiply(lhs, rhs);
	}
	
	Expression foldLogarithm(Expression lhs, Expression rhs) {
		Double lhsValue = lhs.getSymbolAsFloat();
		Double rhsValue = rhs.getSymbolAsFloat();
		if (lhsValue != null && rhsValue != null) {
			if (lhsValue >= 0) {
				return new Expression(Math.log(rhsValue) / Math.log(lhsValue));
			}
		}
		return Expression.logarithm(lhs, rhs);
	}

	Expression foldUnaryMinus(Expression arg) {
		Double value = arg.getSymbolAsFloat();
		if (value != null) { /* move the sign onto the constant */
			return new Expression(-value);
		}
		return Expression.negate(foldConstants(arg));
	}

	Expression foldSin(Expression arg) {
		String[][] table = {
			{"0", "0"},
			{"pi/3", "3^(1/2)/2"},
			{"pi/2", "1"},
			{"pi", "0"},
			{"pi*2/3", "3^(1/2)/2"},
			{"pi*3/2", "-1"},
			{"pi*2", "0"},
			{"pi*3", "0"},
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
			{"pi/3", "1/2"},
			{"pi/2", "0"},
			{"pi", "-1"},
			{"pi*2/3", "-1/2"},
			{"pi*3/2", "0"},
			{"pi*2", "1"},
			{"pi*3", "-1"}
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
		switch (expression.getType()) {
		case NODE_ADD:
			return foldAddition(expression.getLeft(), expression.getRight());
		case NODE_SUBTRACT:
			return foldSubtraction(expression.getLeft(), expression.getRight());
		case NODE_MULTIPLY:
			return foldMultiplication(expression);
		case NODE_DIVIDE:
			return foldDivision(expression);
		case NODE_EXPONENTIATE:
			return foldExponential(expression);
		case NODE_LOGARITHM:
			return foldLogarithm(expression.getLeft(), expression.getRight());
		case NODE_PLUS:
			return expression.getChild();
		case NODE_MINUS:
			return foldUnaryMinus(expression.getChild());
		case NODE_SINE:
			return foldSin(expression.getChild());
		case NODE_COSINE:
			return foldCos(expression.getChild());
		default:
			return expression;
		}
	}
	
	Expression foldSum(List<Expression> terms) {
		for (int i = 0; i < terms.size(); ++i) {
			for (int j = i+1; j < terms.size(); ++j) {
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
			for (int j = i+1; j < factors.size(); ++j) {
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
	
	Expression fold(Expression expression) {
		if (expression == null) {
			return null;
		}
		
		if (expression.isSymbol()) {
			return expression;
		}
	
		Expression result = Collector.normalizeExponents(expression);

		/* fold sums */
		List<Expression> terms = Iterator.getTerms(result);
		if (terms.size() > 1) {
			for (int i = 0; i < terms.size(); ++i) {
				terms.set(i, fold(terms.get(i)));
			}
			result = foldSum(terms);
		}
		
		/* fold products */
		List<Expression> factors = Iterator.getFactors(result, 0);
		if (factors.size() > 1) {
			for (int i = 0; i < factors.size(); ++i) {
				factors.set(i, fold(factors.get(i)));
			}
			result = foldProduct(factors);
		}
		
		/* recurse into other types of operator */
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
