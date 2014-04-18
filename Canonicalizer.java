package jsolve;

import java.util.ArrayList;
import java.util.List;

public class Canonicalizer {
	static boolean order(Expression a, Expression b) {
		return a.toString().compareTo(b.toString()) < 0;
	}

	static List<Expression> sort(List<Expression> expressions) {
		if (expressions.size() <= 1) {
			return expressions;
		}
		List<Expression> less = new ArrayList<Expression>();
		List<Expression> greater = new ArrayList<Expression>();
		int pivotIndex = expressions.size() / 2;
		Expression pivot = expressions.get(pivotIndex);
		for (int i = 0; i < expressions.size(); ++i) {
			if (i != pivotIndex) {
				if (order(expressions.get(i), pivot)) {
					less.add(expressions.get(i));
				} else {
					greater.add(expressions.get(i));
				}
			}
		}
		List<Expression> result = new ArrayList<Expression>();
		result.addAll(sort(less));
		result.add(pivot);
		result.addAll(sort(greater));
		return result;
	}

	static Expression canonicalize(Expression expression) {
		if (expression.isBinary()) {
			expression.setLeft(canonicalize(expression.getLeft()));
			expression.setRight(canonicalize(expression.getRight()));
		} else if (expression.isUnary()) {
			expression.setChild(canonicalize(expression.getChild()));
		}
		expression = Iterator.listProduct(sort(Iterator.getFactors(expression, 0)));
		expression = Iterator.listSum(sort(Iterator.getTerms(expression)));
		return expression;
	}
}
