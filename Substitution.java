package jsolve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	static class Candidate {
		Expression getExpression() {
			return expression_;
		}
		
		void setExpression(Expression expression) {
			expression_ = expression;
		}
		
		Integer getCount() {
			return count_;
		}
		
		void setCount(Integer count) {
			count_ = count;
		}
	
		Expression expression_;
		Integer count_;
	};

	static Expression candidate(Expression expression, String variable) {
		Map<String, Candidate> map = new HashMap<String, Candidate>();
		List<Expression> stack = new ArrayList<Expression>();
		if (expression.isBinary()) {
			stack.add(expression.getLeft());
			stack.add(expression.getRight());
		} else if (expression.isUnary()) {
			stack.add(expression.getChild());
		} else {
			return null;
		}
		while (stack.size() != 0) {
			Expression top = stack.remove(0);
			if (top.isBinary()) {
				stack.add(top.getLeft());
				stack.add(top.getRight());
			} else if (top.isUnary()) {
				stack.add(top.getChild());
			} else if (top.isSymbol()) {
				continue;
			}
			if (top.contains(variable)) {
				String hash = Canonicalizer.toString(top);
				Candidate candidate = map.get(hash);
				if (candidate == null) {
					candidate = new Candidate();
					candidate.setExpression(top);
					candidate.setCount(1);
				} else { /* seen this subexpression again */
					candidate.setCount(candidate.getCount() + 1);
				}
				map.put(hash, candidate);
			}
		}
		Expression newVariable = new Expression(allocateVariable(expression));
		Candidate candidate = null;
		for (String hash : map.keySet()) {
			Candidate next = map.get(hash);
			Expression substitution = substitute(expression, next.getExpression(), newVariable);
			if (substitution.contains(variable)) {
				continue; /* does not eliminate the original variable */
			}
			if (candidate == null || next.getCount() > candidate.getCount()) {
				candidate = next;
			}
		}
		if (candidate != null) {
			return candidate.getExpression();
		}
		return null;
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
