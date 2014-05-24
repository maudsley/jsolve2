package jsolve;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Collector {
	static class ExpressionMultiple extends Expression {
		ExpressionMultiple(Expression lhs) {
			super(Expression.Type.NODE_MULTIPLY);
			setChild(lhs);
			count_ = new Integer(0);
		}
		Expression getExpression(Expression.Type type) {
			Expression expression = new Expression(type);
			expression.setLeft(getChild());
			expression.setRight(new Expression(count_.toString()));
			return expression;
		}
		void raise(Integer count) {
			count_ += count;
		}
		Integer getMultiple() {
			return count_;
		}
		Integer count_;
	}
	
	static class ExpressionMap {
		ExpressionMap(Expression owner) {
			owner_ = owner;
			factors_ = new HashMap<String, ExpressionMultiple>();
		}
		
		Expression getOwner() {
			return owner_;
		}
		
		boolean hasOwner() {
			return owner_ != null;
		}
		
		ExpressionMultiple get(String key) {
			return factors_.get(key);
		}
		
		void put(String key, ExpressionMultiple expression) {
			factors_.put(key, expression);
		}
		
		Collection<ExpressionMultiple> getFactors() {
			return factors_.values();
		}
	
		Expression owner_;
		Map<String, ExpressionMultiple> factors_;
	}

	static Expression collect(Expression expression) {
		Expression result = collect(expression, "x");
		List<String> variables = Substitution.getSymbols(expression);
		for (String variable : variables) {
			result = collect(result, variable);
		}
		return result;
	}

	static Expression collect(Expression expression, String variable) {
		expression = normalizeExponents(expression);
		Map<String, ExpressionMap> map = new HashMap<String, ExpressionMap>();
		List<Expression> terms = Iterator.getTerms(expression);
		for (Expression term : terms) {
			Expression coefficient = new Expression("1");
			Expression owner = null;
			List<Expression> factors = Iterator.getFactors(term, 0);
			for (Expression factor : factors) {
				if (factor.contains(variable)) {
					if (owner == null) {
						owner = factor;
						continue;
					} else if (owner.toString().equals(factor.toString())) {
						continue;
					}
				}
				if (coefficient == null) {
					coefficient = factor;
				} else {
					coefficient = Expression.multiply(coefficient, factor);
				}
			}
			ExpressionMap entry = null;
			if (owner == null) {
				entry = map.get(coefficient.toString());
				if (entry == null) {
					entry = new ExpressionMap(null); /* indicates that this is a factor in the constant term */
				}
				owner = coefficient;
			} else {
				entry = map.get(owner.toString());
				if (entry == null) {
					entry = new ExpressionMap(owner);
				}
			}
			ExpressionMultiple multiple = entry.get(coefficient.toString());
			if (multiple == null) {
				multiple = new ExpressionMultiple(coefficient);
			}
			multiple.raise(1);
			entry.put(coefficient.toString(), multiple);
			map.put(owner.toString(), entry);
		}
		expression = null;
		for (ExpressionMap newTerms : map.values()) {
			Expression factor = null;
			for (ExpressionMultiple coefficient : newTerms.getFactors()) {
				Expression multiple = coefficient.getExpression(Expression.Type.NODE_MULTIPLY);
				if (factor == null) {
					factor = multiple;
				} else {
					factor = Expression.add(factor, multiple);
				}
			}
			if (newTerms.hasOwner()) {
				factor = Expression.multiply(newTerms.getOwner(), factor);
			}
			if (expression == null) {
				expression = factor;
			} else {
				expression = Expression.add(expression, factor);
			}
		}
		return expression;
	}

	static Expression normalizeExponents(Expression expression) {
		List<Expression> terms = Iterator.getTerms(expression);
		List<Expression> newTerms = new ArrayList<Expression>();
		for (Expression term : terms) {
			Map<String, ExpressionMultiple> exponents = new HashMap<String, ExpressionMultiple>();
			List<Expression> factors = Iterator.getFactors(term, 5);
			for (Expression factor : factors) {
				if (factor.getType().equals(Expression.Type.NODE_DIVIDE)) {
					if (factor.getRight().isSymbol()) {
						if (factor.getLeft().isOne()) {
							Expression base = factor.getRight();
							ExpressionMultiple value = exponents.get(base.toString());
							if (value == null) {
								value = new ExpressionMultiple(base);
							}
							value.raise(-1);
							exponents.put(base.toString(), value);
							continue;
						}
					}
				} else if (factor.getType().equals(Expression.Type.NODE_EXPONENTIATE)) {
					if (factor.getRight().isSymbol()) {
						Expression base = factor.getLeft();
						Integer power;
						try {
							power = Integer.parseInt(factor.getRight().getSymbol());
						} catch (NumberFormatException e) {
							power = null;
						}
						if (power != null) {
							ExpressionMultiple value = exponents.get(base.toString());
							if (value == null) {
								value = new ExpressionMultiple(base);
							}
							value.raise(power);
							exponents.put(base.toString(), value);
							continue;
						}
					}
				}
				ExpressionMultiple value = exponents.get(factor.toString());
				if (value == null) {
					value = new ExpressionMultiple(factor);
				}
				value.raise(1);
				exponents.put(factor.toString(), value);
			}
			ArrayList<Expression> product = new ArrayList<Expression>();
			for (ExpressionMultiple factor : exponents.values()) {
				if (factor.getMultiple() == 1) {
					product.add(factor.getChild());
				} else if (factor.getMultiple() < 0) {
					Double absPower = -1.0 * factor.getMultiple();
					Expression power = Expression.exponentiate(factor.getChild(), new Expression(absPower));
					product.add(Expression.divide(new Expression("1"), power));
				} else {
					product.add(factor.getExpression(Expression.Type.NODE_EXPONENTIATE));
				}
			}
			newTerms.add(Iterator.listProduct(product));
		}
		return Iterator.listSum(newTerms);
	}
}
