package jsolve;

import java.util.ArrayList;
import java.util.List;

public class Substitution {
	static Expression substitute(Expression a, Expression b, Expression c) {
		/* substitute occurrences of expression 'b' with 'c' in expression 'a' */
		if (Canonicalizer.compare(a, b)) {
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

	static List<Expression> candidates(Expression expression, String variable) {
		/* return a list of subexpressions of 'expression' containing 'variable' */
		List<Expression> result = new ArrayList<Expression>();
		if (expression.contains(variable)) {
			result.add(expression);
		}
		if (expression.isUnary()) {
			result.addAll(candidates(expression.getChild(), variable));
		} else if (expression.isBinary()) {
			result.addAll(candidates(expression.getLeft(), variable));
			result.addAll(candidates(expression.getRight(), variable));
		}
		return result;
	}

	static List<String> getSymbols(Expression expression) {
		/* return a list of all symbols used in an expression */
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
		/* generate a variable name that has not yet been used in 'expression' */
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
