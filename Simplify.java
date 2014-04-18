package jsolve;

import java.util.ArrayList;
import java.util.List;

import jsolve.Parser.Error;

public class Simplify {
	static String[] identities = {
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

	static boolean checkIdentity(Expression expression, Expression identity) {
		if (expression.getType().equals(identity.getLeft().getType())) {
			return true;
		} else {
			return false;
		}
	}

	static Expression applyIdentities(Expression expression, Expression subExpression) {
		Expression result = expression.copy();
		if (subExpression.isBinary()) {
			result = applyIdentities(result, subExpression.getLeft());
			result = applyIdentities(result, subExpression.getRight());
		} else if (subExpression.isUnary()) {
			result = applyIdentities(result, subExpression.getChild());
		}
	
		if (result.isBinary()) {
			result.setLeft(applyIdentities(result.getLeft(), result.getLeft()));
			result.setRight(applyIdentities(result.getRight(), result.getRight()));
		} else if (result.isUnary()) {
			result.setChild(applyIdentities(result.getChild(), result.getChild()));
		}
		
		for (String identity : identities) {
			Parser parser = null;
			try {
				parser = new Parser(identity);
			} catch (Error e) {
				continue;
			}
			if (!checkIdentity(result, parser.getExpression())) {
				continue;
			}
			Expression match = Substitution.substitute(parser.getExpression(), new Expression("x"), subExpression);
			if (Canonicalizer.compare(result, match.getLeft())) {
				return match.getRight();
			}
		}

		return result;
	}
	
	static Expression fold(Expression expression) {
		Double lhs = null;
		Double rhs = null;
		Double arg = null;
		
		expression = applyIdentities(expression, expression);
	
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
	
	static List<Expression> fold(List<Expression> expressions, Expression.Type operator) {
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
	
	static Expression simplify(Expression expression) {
		if (expression.isSymbol()) {
			return expression;
		} else if (expression.isBinary()) {
			expression.setLeft(simplify(expression.getLeft()));
			expression.setRight(simplify(expression.getRight()));
		} else if (expression.isUnary()) {
			expression.setChild(simplify(expression.getChild()));
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
}
