package jsolve;

import java.util.ArrayList;
import java.util.List;

public class Polynomial {
	class Coefficient {
		Long getDegree() {
			return degree_;
		}
		
		void setDegree(long degree) {
			degree_ = degree;
		}
		
		Expression getExpression() {
			return expression_;
		}
		
		void setExpression(Expression expression) {
			expression_ = expression;
		}
	
		Long degree_;
		Expression expression_;
	}
	
	Polynomial(Expression expression, String variableName) {
		variableName_ = variableName;
		List<Expression> terms = Iterator.getTerms(expression);
		for (Expression term : terms) {
			List<Expression> coefficients = new ArrayList<Expression>();
			List<Expression> variables = new ArrayList<Expression>();
			List<Expression> factors = Iterator.getFactors(term, 0);
			for (Expression factor : factors) {
				if (factor.contains(variableName)) {
					variables.add(factor);
				} else {
					coefficients.add(factor);
				}
			}
			Coefficient result = new Coefficient();
			result.setDegree(0);
			result.setExpression(Iterator.listProduct(coefficients));
			Expression variable = Iterator.listProduct(variables);
			if (variables.size() != 0) {
				Long exponent = getExponent(variable);
				variable = getBase(variable);
				if (variable_ == null) {
					variable_ = variable;
					result.setDegree(exponent);
				} else if (Canonicalizer.compare(variable_, variable)) {
					result.setDegree(exponent);
				} else { /* this is not a polynomial */
					result.setExpression(term);
				}
			}
			Expression currentValue = getCoefficient(result.getDegree());
			if (currentValue == null) {
				setCoefficient(result.getDegree(), result.getExpression());
			} else { /* add it to the rest of the terms */
				List<Expression> currentTerms = Iterator.getTerms(currentValue);
				currentTerms.add(result.getExpression());
				setCoefficient(result.getDegree(), Iterator.listSum(currentTerms));
			}
		}
		
		/* factor out any common multiple from the exponents */
		Long commonFactor = null;
		for (Coefficient coefficient : coefficients_) {
			if (commonFactor == null) {
				commonFactor = coefficient.getDegree();
			} else {
				commonFactor = Simplify.gcd(commonFactor, coefficient.getDegree());
			}
		}
		if (commonFactor == 0) {
			return;
		}
		for (int i = 0; i < coefficients_.size(); ++i) {
			Coefficient coefficient = coefficients_.get(i);
			coefficient.setDegree(coefficient.getDegree() / commonFactor);
			coefficients_.set(i, coefficient);
		}
		if (commonFactor > 1) {
			Expression base = getBase(variable_);
			Long exponent = getExponent(variable_) * commonFactor;
			variable_ = Expression.exponentiate(base, new Expression(exponent.toString()));
		}
	}
	
	Polynomial(Polynomial other) {
		variableName_ = other.variableName_;
		variable_ = other.variable_.copy();
		for (Coefficient coefficient : other.coefficients_) {
			Coefficient newCoefficient = new Coefficient();
			newCoefficient.setDegree(coefficient.getDegree());
			newCoefficient.setExpression(coefficient.getExpression());
			coefficients_.add(newCoefficient);
		}
	}
	
	Expression getVariable() {
		return variable_;
	}
	
	Expression getExpression() {
		List<Expression> result = new ArrayList<Expression>();
		for (Coefficient coefficient : coefficients_) {
			Expression exponent = new Expression(coefficient.getDegree().toString());
			Expression term = variable_;
			if (!exponent.isOne()) {
				term = Expression.exponentiate(term, exponent);
			}
			Expression expression = coefficient.getExpression();
			if (exponent.isZero()) {
				term = expression;
			} else if (!expression.isOne()) {
				term = Expression.multiply(expression, term);
			}
			result.add(term);
		}
		return Iterator.listSum(result);
	}
	
	public String toString() {
		return getExpression().toString();
	}
	
	boolean isValid() {
		for (Coefficient coefficient : coefficients_) {
			if (coefficient.getExpression().contains(variableName_)) {
				return false;
			}
		}
		return true;
	}
	
	Long getDegree() {
		Long degree = Long.MIN_VALUE;
		for (Coefficient coefficient : coefficients_) {
			if (coefficient.getDegree() > degree) {
				degree = coefficient.getDegree();
			}
		}
		return degree;
	}
	
	Expression getCoefficient(long degree) {
		for (Coefficient coefficient : coefficients_) {
			if (coefficient.getDegree() == degree) {
				return coefficient.getExpression();
			}
		}
		return null;
	}
	
	void setCoefficient(long degree, Expression expression) {
		for (int i = 0; i < coefficients_.size(); ++i) {
			Coefficient coefficient = coefficients_.get(i);
			if (coefficient.getDegree() == degree) {
				coefficient.setExpression(expression);
				coefficients_.set(i, coefficient);
				return;
			}
		}
		/* that term does not exist. create it */
		Coefficient coefficient = new Coefficient();
		coefficient.setDegree(degree);
		coefficient.setExpression(expression);
		coefficients_.add(coefficient);
	}
	
	Polynomial add(Expression term) {
		Polynomial result = new Polynomial(this);
		Expression constantTerm = result.getCoefficient(0);
		if (constantTerm == null) {
			result.setCoefficient(0, term);
		} else { /* add to the constant term */
			setCoefficient(0, Expression.add(constantTerm, term));
		}
		return result;
	}
	
	Polynomial divide(Expression divisor) {
		Polynomial result = new Polynomial(this);
		List<Coefficient> newCoefficients = new ArrayList<Coefficient>();
		for (Coefficient coefficient : coefficients_) {
			Expression quotient = Expression.divide(coefficient.getExpression(), divisor);
			Coefficient newCoefficient = new Coefficient();
			newCoefficient.setExpression(quotient);
			newCoefficient.setDegree(coefficient.getDegree());
			newCoefficients.add(newCoefficient);
		}
		result.coefficients_ = newCoefficients;
		return result;		
	}
	
	private long getExponent(Expression expression) {
		/* return the product of any constant factors in the exponent: e^(2*3*x) -> 6 */
		long exponent = 1;
		if (expression.getType().equals(Expression.Type.NODE_EXPONENTIATE)) {
			List<Expression> factors = Iterator.getFactors(expression.getRight(), 0);
			for (Expression factor : factors) {
				Long value = factor.getSymbolAsInteger();
				if (value != null) {
					exponent *= value;
				}
			}
		}
		return exponent;
	}
	
	private Expression getBase(Expression expression) {
		/* remove the constant factors from the exponent: e^(2*3*x) -> e^x */
		if (expression.getType().equals(Expression.Type.NODE_EXPONENTIATE)) {
			List<Expression> newFactors = new ArrayList<Expression>();
			List<Expression> factors = Iterator.getFactors(expression.getRight(), 0);
			for (Expression factor : factors) {
				if (factor.getSymbolAsInteger() == null) {
					newFactors.add(factor);
				}
			}
			Expression newExponent = Iterator.listProduct(newFactors);
			if (newExponent.isOne()) {
				return expression.getLeft();
			}
			return Expression.exponentiate(expression.getLeft(), newExponent);
		}
		return expression;
	}

	String variableName_;
	Expression variable_;
	List<Coefficient> coefficients_ = new ArrayList<Coefficient>();
}
