package jsolve;

import java.util.ArrayList;
import java.util.List;

public class Iterator {
	private static void sumIterator(Expression expression, boolean negative, Iterator callback) {
		switch (expression.getType()) {
		case NODE_ADD:
			sumIterator(expression.getLeft(), negative, callback);
			sumIterator(expression.getRight(), negative, callback);
			break;
		case NODE_SUBTRACT:
			sumIterator(expression.getLeft(), negative, callback);
			sumIterator(expression.getRight(), !negative, callback);
			break;
		default:
			callback.term(expression, negative);
			break;
		}
	}

	void term(Expression expression, boolean negative) {
		/* called for each term in a sum */
	}

	public static void sumIterator(Expression expression, Iterator callback) {
		sumIterator(expression, false, callback);
	}

	private static void productIterator(Expression expression, boolean inverse, Iterator callback) {
		switch (expression.getType()) {
		case NODE_MULTIPLY:
			productIterator(expression.getLeft(), inverse, callback);
			productIterator(expression.getRight(), inverse, callback);
			break;
		case NODE_DIVIDE:
			productIterator(expression.getLeft(), inverse, callback);
			productIterator(expression.getRight(), !inverse, callback);
			break;
		default:
			callback.factor(expression, inverse);
			break;
		}
	}
	
	void factor(Expression expression, boolean inverse) {
		/* called for each factor in a product */
	}
	
	public static void productIterator(Expression expression, Iterator callback) {
		productIterator(expression, false, callback);
	}
	
	static int getSign(Expression expression) {
		int sign = 1;
		List<Expression> factors = getFactors(expression, 0);
		for (Expression factor : factors) {
			if (factor.isSymbol()) {
				Double value = 1.0;
				try {
					value = Double.parseDouble(factor.getSymbol());
				} catch (NumberFormatException e) {
				}
				if (value < 0) {
					sign = -sign;
				}
			}
		}
		return sign;
	}
	
	static Expression getAbs(Expression expression) {
		List<Expression> newFactors = new ArrayList<Expression>();
		List<Expression> factors = getFactors(expression, 0);
		for (Expression factor : factors) {
			if (factor.isSymbol()) {
				try {
					Double value = Double.parseDouble(factor.getSymbol());
					if (value != -1.0) {
						newFactors.add(new Expression(Math.abs(value)));
					}
					continue;
				} catch (NumberFormatException e) {
				}
			}
			newFactors.add(factor);
		}
		return listProduct(newFactors);
	}
	
	static Expression listSum(List<Expression> terms) {
		if (terms.size() == 0) {
			return new Expression("0"); /* empty sum */
		}
		List<Expression> positive = new ArrayList<Expression>();
		List<Expression> negative = new ArrayList<Expression>();
		for (Expression term : terms) {
			if (getSign(term) == 1) {
				positive.add(term);
			} else {
				negative.add(getAbs(term));
			}
		}
		Expression result = null;
		for (Expression term : positive) {
			if (result == null) {
				result = term;
			} else {
				result = Expression.add(result, term);
			}
		}
		for (Expression term : negative) {
			if (result == null) {
				result = Expression.negate(term);
			} else {
				result = Expression.subtract(result, term);
			}
		}
		return result;
	}
	
	static Expression removeRecipricol(Expression expression) {
		/* if the expression is a quotient of the form 1/f(x), return f(x) */
		if (expression.getType().equals(Expression.Type.NODE_DIVIDE)) {
	 		if (expression.getLeft().isOne()) {
	 			return expression.getRight();
	 		}
		}
		return expression;
	}
	
	static boolean hasRecipricol(Expression expression) {
		Expression result = removeRecipricol(expression);
		return !result.toString().equals(expression.toString());
	}
	
	static Expression listProduct(List<Expression> factors) {
		if (factors.size() == 0) {
			return new Expression("1"); /* empty product */
		}
		List<Expression> numerators = new ArrayList<Expression>();
		List<Expression> denominators = new ArrayList<Expression>();
		for (Expression factor : factors) {
			if (!hasRecipricol(factor)) {
				numerators.add(factor);
			} else {
				denominators.add(removeRecipricol(factor));
			}
		}
		Expression numerator = null;
		for (Expression factor : numerators) {
			if (numerator == null) {
				numerator = factor;
			} else {
				numerator = Expression.multiply(numerator, factor);
			}
		}
		Expression denominator = null;
		for (Expression factor : denominators) {
			if (denominator == null) {
				denominator = factor;
			} else {
				denominator = Expression.multiply(denominator, factor);
			}
		}
		if (numerator == null) {
			numerator = new Expression("1");
		}
		if (denominator == null) {
			return numerator;
		}
		return Expression.divide(numerator, denominator);
	}

	static class TermCollector extends Iterator {
		void term(Expression expression, boolean negative) {
			if (negative) {
				terms.add(Expression.negate(expression));
			} else {
				terms.add(expression);
			}
		}
		List<Expression> terms = new ArrayList<Expression>();
	}
	
	static List<Expression> getTerms(Expression sum) {
		TermCollector collector = new TermCollector();
		Iterator.sumIterator(sum, collector);
		return collector.terms;
	}
	
	static class ProductCollector extends Iterator {
		ProductCollector(int expandLimit) {
			expandLimit_ = expandLimit;
		}
		void factor(Expression expression, boolean inverse) {
			if (expression.getType().equals(Expression.Type.NODE_EXPONENTIATE)) {
				if (expression.getRight().isSymbol() && expandLimit_ != 0) {
					Integer exponent;
					try {
						exponent = Integer.parseInt(expression.getRight().toString());
					} catch (NumberFormatException e) {
						exponent = null;
					}
					if (exponent != null && exponent > 0 && exponent < expandLimit_) {
						/* return x^2 as x*x in two calls */
						for (int i = 0; i < exponent; ++i) {
							factor(expression.getLeft(), inverse);
						}
						return;
					}
				}
			}
			if (inverse) {
				factors.add(Expression.divide(new Expression("1"), expression));
			} else {
				factors.add(expression);
			}
		}
		int expandLimit_;
		List<Expression> factors = new ArrayList<Expression>();
	}
	
	static List<Expression> getFactors(Expression product, int expandLimit) {
		ProductCollector collector = new ProductCollector(expandLimit);
		Iterator.productIterator(product, collector);
		return collector.factors;
	}
}
