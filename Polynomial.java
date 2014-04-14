package jsolve;

import java.util.List;

public class Polynomial {
	class Coefficient {
		Long getDegree() {
			return degree_;
		}
		
		void setDegree(Long degree) {
			degree_ = degree;
		}
		
		Expression getExpression() {
			return expression_;
		}
		
		void setCoefficient(Expression expression) {
			expression_ = expression;
		}
	
		Long degree_;
		Expression expression_;
	}
	
	Polynomial(Expression expression) {
	
	}
	
	Expression getExpression() {
		Expression result = null;
		for (Coefficient coefficient : coefficients_) {
			Expression exponent = new Expression(coefficient.getDegree().toString());
			Expression term = Expression.exponentiate(variable_, exponent);
			term = Expression.multiply(coefficient.getExpression(), term);
			if (result == null) {
				result = term;
			} else {
				result = Expression.add(result, term);
			}
		}
		return result;
	}

	Expression variable_;
	List<Coefficient> coefficients_;
}
