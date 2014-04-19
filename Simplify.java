package jsolve;

import java.util.ArrayList;
import java.util.List;

import jsolve.Parser.Error;

public class Simplify {
	Simplify() {
		String[] rules = {
			"x+0 = x",
			"x-0 = x",
			"0-x = -x",
			"x-x = 0",
			"x*0 = 0",
			"x*1 = x",
			"0/x = 0",
			"x/1 = x",
			"x/x = 1",
			"x*1/x = 1",
			"1/(1/x) = x",
			"x^0 = 1",
			"x^1 = x",
			"0^x = 0",
			"1^x = 1",
			"0! = 1",
			"x!*(x+1) = (x+1)!",
			"x!/x = (x-1)!"
		};
	
		identities = new ArrayList<Expression>();
		for (String rule : rules) {
			Parser parser = null;
			try {
				parser = new Parser(rule);
			} catch (Error e) {
				continue;
			}
			identities.add(parser.getExpression());
		}
	}

	boolean compareOperators(Expression a, Expression b) {
		if (a == null || b == null) {
			return a == null && b == null;
		}
		boolean result = true;
		result &= compareOperators(a.getLeft(), b.getLeft());
		result &= compareOperators(a.getLeft(), b.getLeft());
		result &= compareOperators(a.getLeft(), b.getLeft());
		if (!a.getType().equals(b.getType())) {
			result = false;
		}
		return result;
	}

	boolean checkIdentity(Expression expression, Expression subExpression, Expression identity) {
		Expression variable = new Expression(Substitution.allocateVariable(expression));
		Expression a = Substitution.substitute(expression, subExpression, variable);
		Expression b = Substitution.substitute(identity, new Expression("x"), variable);
		return compareOperators(a, b.getLeft());
	}

	Expression applyIdentitiesSubstitute(Expression expression, Expression subExpression) {
		if (expression == null || subExpression == null) {
			return expression;
		}

		Expression result = expression.copy();
		result.setLeft(applyIdentities(expression.getLeft()));
		result.setRight(applyIdentities(expression.getRight()));
		result.setChild(applyIdentities(expression.getChild()));

		boolean check = false;
		for (Expression identity : identities) {
			if (checkIdentity(expression, subExpression, identity)) {
				check = true;
				Expression match = Substitution.substitute(identity, new Expression("x"), subExpression);
				if (Canonicalizer.compare(expression, match.getLeft())) {
					return match.getRight();
				}
			}
		}
		
		if (!check) {
			return result;
		}

		return result;
	}
	
	Expression applyIdentities(Expression expression) {
		if (expression != null) {
			Expression result = expression.copy();
			result = applyIdentitiesSubstitute(result, result.getLeft());
			result = applyIdentitiesSubstitute(result, result.getRight());
			result = applyIdentitiesSubstitute(result, result.getChild());
			return result;
		}
		return null;
	}

	Expression fold(Expression expression) {
		Double lhs = null;
		Double rhs = null;
		Double arg = null;
		
		expression = applyIdentities(expression);
	
		try {
			if (expression.isBinary()) {
				if (!expression.getLeft().isSymbol() || !expression.getRight().isSymbol()) {
					return expression;
				}
				lhs = Double.parseDouble(expression.getLeft().getSymbol());
				rhs = Double.parseDouble(expression.getRight().getSymbol());
			} else if (expression.isUnary()) {
				if (!expression.getChild().isSymbol()) {
					return expression;
				}
				arg = Double.parseDouble(expression.getChild().getSymbol());
			} else {
				return expression;
			}
		} catch (NumberFormatException e) {
			return expression;
		}
	
		switch (expression.getType()) {
		case NODE_ADD:
			return new Expression(lhs + rhs);
		case NODE_SUBTRACT:
			return new Expression(lhs - rhs);
		case NODE_MULTIPLY:
			return new Expression(lhs * rhs);
		case NODE_DIVIDE:
			return new Expression(lhs / rhs);
		case NODE_EXPONENTIATE:
			return new Expression(Math.pow(lhs,  rhs));
		case NODE_PLUS:
			return new Expression(arg);
		case NODE_MINUS:
			return new Expression(-arg);
		case NODE_LOGARITHM:
			return new Expression(Math.log(rhs) / Math.log(lhs));
		case NODE_FACTORIAL:
		case NODE_FACTORIAL_INVERSE:
		default:
			break;
		}
		
		return expression;
	}
	
	List<Expression> fold(List<Expression> expressions, Expression.Type operator) {
		for (int i = 0; i < expressions.size(); ++i) {
			for (int j = 0; j < expressions.size(); ++j) {
				if (i == j) {
					continue;
				}
				Expression product = new Expression(operator);
				product.setLeft(expressions.get(i));
				product.setRight(expressions.get(j));
				Expression simplified = fold(product);
				if (!Canonicalizer.compare(product,  simplified)) {
					/* rebuild the list with our simplified factor */
					List<Expression> result = new ArrayList<Expression>();
					for (int k = 0; k < expressions.size(); ++k) {
						if (k != i && k != j) {
							result.add(expressions.get(k));
						}
					}
					result.add(simplified);
					return result;
				}
			}
		}
		return expressions;
	}
	
	Expression simplifyExpression(Expression expression) {
		if (expression.isSymbol()) {
			return expression;
		} else if (expression.isBinary()) {
			expression.setLeft(simplifyExpression(expression.getLeft()));
			expression.setRight(simplifyExpression(expression.getRight()));
		} else if (expression.isUnary()) {
			expression.setChild(simplifyExpression(expression.getChild()));
		}
	
		/* iterate while the expression keeps changing */
		String hash = Canonicalizer.toString(expression);
		while (true) {
			expression = fold(expression);
			expression = Iterator.listSum(fold(Iterator.getTerms(expression), Expression.Type.NODE_ADD));
			expression = Iterator.listProduct(fold(Iterator.getFactors(expression, 0), Expression.Type.NODE_MULTIPLY));
			String newHash = Canonicalizer.toString(expression);
			if (hash.equals(newHash)) {
				break;
			}
			hash = newHash;
		}
	
		return expression;
	}
	
	static Expression simplify(Expression expression) {
		Simplify simplify = new Simplify();
		Expression result = expression.copy();
		result = simplify.simplifyExpression(result);
		return result;
	}
	
	List<Expression> identities;
}
