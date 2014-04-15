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
	
	static Expression listSum(List<Expression> terms) {
		if (terms.size() == 0) {
			return new Expression("0"); /* empty sum */
		}
		Expression result = null;
		for (Expression term : terms) {
			if (result == null) {
				result = term;
			} else {
				result = Expression.add(result, term);
			}
		}
		return result;
	}
	
	static Expression listProduct(List<Expression> factors) {
		if (factors.size() == 0) {
			return new Expression("1"); /* empty product */
		}
		Expression result = null;
		for (Expression factor : factors) {
			if (result == null) {
				result = factor;
			} else {
				result = Expression.multiply(result, factor);
			}
		}
		return result;
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
				if (expression.getRight().isSymbol()) {
					Integer exponent = Integer.parseInt(expression.getRight().toString());
					if (exponent != null && exponent < expandLimit_) {
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
