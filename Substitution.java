package jsolve;

import java.util.ArrayList;
import java.util.List;

public class Substitution {
	static Expression substitute(Expression a, Expression b, Expression c) {
		if (a.toString().equals(b.toString())) {
			return c;
		} else if (a.isBinary()) {
			Expression result = a.copy();
			result.setLeft(substitute(a.getLeft(), b, c));
			result.setRight(substitute(a.getRight(), b, c));
			return result;
		} else if (a.isUnary()) {
			Expression result = a.copy();
			result.setChild(substitute(a.getChild(), b, c));
			return result;
		} else {
			return a;
		}
	}

	static List<String> getSymbols(Expression expression) {
		List<String> result = new ArrayList<String>();
		if (expression.isUnary()) {
			result.addAll(getSymbols(expression.getChild()));
		} else if (expression.isBinary()) {
			result.addAll(getSymbols(expression.getLeft()));
			result.addAll(getSymbols(expression.getRight()));
		} else {
			result.add(expression.getSymbol());
		}
		return result;
	}

	static String allocateVariable(Expression expression) {
		String[] variables = {
			"a", "b", "c", "d", /*"e",*/ "f", "g", "h", /*"i",*/ "j", "k", "l",
			"m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
		};
		List<String> symbols = getSymbols(expression);
		for (String variable : variables) {
			boolean used = false;
			for (String symbol : symbols) {
				if (symbol.equals(variable)) {
					used = true;
					break;
				}
			}
			if (!used) {
				return variable;
			}
		}
		return null;
	}
}
