package jsolve;

import java.util.ArrayList;
import java.util.List;

public class Expander {
	static Expression expand(Expression expression) {
		Expression result = expand(expression, "x");
		List<String> variables = Substitution.getSymbols(expression);
		for (String variable : variables) {
			result = expand(result, variable);
		}
		return result;
	}
	
	static Expression expand(Expression expression, String variable) {
		/* iterate while the expression keeps changing */
		String hash = Canonicalizer.toString(expression);
		while (true) {
			List<Expression> factors = Iterator.getFactors(expression, 5);
			for (int i = 0; i < factors.size(); ++i) {
				Expression factor = factors.get(i);
				if (!factor.contains(variable)) {
					continue;
				}
				List<Expression> terms = Iterator.getTerms(factor);
				if (terms.size() == 1) {
					continue;
				} else if (factors.size() == 1) {
					Expression result = null;
					List<Expression> newTerms = new ArrayList<Expression>();
					for (Expression term : terms) {
						newTerms.add(expand(term, variable));
					}
					result = Iterator.listSum(newTerms);
					if (Canonicalizer.compare(result, factor)) {
						break;
					}
					expression = result;
				} else {
					List<Expression> coefficients = new ArrayList<Expression>();
					coefficients.addAll(factors.subList(0, i));
					coefficients.addAll(factors.subList(i+1, factors.size()));
					Expression coefficient = Iterator.listProduct(coefficients);
					List<Expression> keep = new ArrayList<Expression>();
					List<Expression> replace = new ArrayList<Expression>();
					for (Expression term : terms) {
						if (term.contains(variable)) {
							replace.add(Expression.multiply(coefficient,  term));
						} else {
							keep.add(term);
						}
					}
					expression = Expression.multiply(coefficient, Iterator.listSum(keep));
					expression = Expression.add(Iterator.listSum(replace), expression);
				}
				break;
			}
			String newHash = Canonicalizer.toString(expression);
			if (hash.equals(newHash)) {
				break;
			}
			hash = newHash;
		}
		return expression;
	}
}
