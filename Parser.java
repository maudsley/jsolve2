package jsolve;

import java.util.ArrayDeque;
import java.util.Deque;

public class Parser {
	static class Error extends Throwable {
		Error(String message) {
			message_ = message;
		}
		
		public String getMessage() {
			return message_;
		}
		
		String message_;
		
		private static final long serialVersionUID = 1L;
	}

	Parser(String expression) throws Error {
		expression_ = parseExpression(new Lexer(expression));
		expression_ = sanitize(expression_);
	}
	
	Expression sanitize(Expression expression) {
		/* remove unary plus and minus from the expression */
		if (Operator.isBinary(expression)) {
			expression.setLeft(sanitize(expression.getLeft()));
			expression.setRight(sanitize(expression.getRight()));
		} else {
			switch (expression.getType()) {
			case NODE_PLUS:
				expression = sanitize(expression.getChild());
			case NODE_MINUS:
				expression = Expression.negate(sanitize(expression.getChild()));
			default:
				break;
			}
		}
		return expression;
	}

	Expression parseExpression(Lexer scanner) {
		Deque<Expression> operands = new ArrayDeque<Expression>();
		Deque<Expression> operators = new ArrayDeque<Expression>();
		Token token = scanner.getNextToken();
		if (token == null) {
			return null;
		}
		boolean acceptUnaryOperator = true;
		while (token != null) {
			if (token.getType().equals(Token.Type.TOKEN_CLOSE_PARENTHESES)) {
				break;
			} else if (token.getType().equals(Token.Type.TOKEN_OPEN_PARENTHESES)) {
				operands.push(parseExpression(scanner));
				acceptUnaryOperator = false;
			} else if (token.getType().equals(Token.Type.TOKEN_SYMBOL)) {
				operands.push(new Expression(token.getSymbol()));
				acceptUnaryOperator = false;
			} else {
				Expression expression = tokenToNode(token, acceptUnaryOperator);
				Operator a = Operator.fromExpression(expression);
				if (a.getArity().equals(Operator.Arity.UNARY_RIGHT)) {
					expression.setChild(operands.pop());
					operands.push(expression);
					acceptUnaryOperator = false;
				} else if (a.getArity().equals(Operator.Arity.UNARY_LEFT)) {
					operators.push(expression);
					acceptUnaryOperator = true;
				} else { /* binary */
					while (operators.size() > 0) {
						Operator b = Operator.fromExpression(operators.peek());
						if (a.getPrecedence() == b.getPrecedence()) {
							if (a.getOrder().equals(Operator.Order.RIGHT_TO_LEFT)) {
								break;
							}
						} else if (a.getPrecedence() > b.getPrecedence()) {
							break;
						}
						Expression operand = operators.pop();
						if (Operator.isBinary(operand)) {
							operand.setRight(operands.pop());
							operand.setLeft(operands.pop());
						} else {
							operand.setChild(operands.pop());
						}
						operands.push(operand);
					}
					operators.push(expression);
					acceptUnaryOperator = true;
				}
			}
			token = scanner.getNextToken();
		}
		while (operators.size() > 0) {
			Expression operand = operators.pop();
			if (Operator.isBinary(operand)) {
				operand.setRight(operands.pop());
				operand.setLeft(operands.pop());
			} else {
				operand.setChild(operands.pop());
			}
			operands.push(operand);
		}
		return operands.pop();
	}

	Expression tokenToNode(Token token, boolean unary) {
		switch (token.getType()) {
		case TOKEN_SYMBOL:
			return new Expression(token.getSymbol());
		case TOKEN_EQUALS:
			return new Expression(Expression.Type.NODE_EQUALS);
		case TOKEN_PLUS:
			if (unary) {
				return new Expression(Expression.Type.NODE_PLUS);
			} else {
				return new Expression(Expression.Type.NODE_ADD);
			}
		case TOKEN_MINUS:
			if (unary) {
				return new Expression(Expression.Type.NODE_MINUS);
			} else {
				return new Expression(Expression.Type.NODE_SUBTRACT);
			}
		case TOKEN_MULTIPLY:
			return new Expression(Expression.Type.NODE_MULTIPLY);
		case TOKEN_DIVIDE:
			return new Expression(Expression.Type.NODE_DIVIDE);
		case TOKEN_EXPONENTIATE:
			return new Expression(Expression.Type.NODE_EXPONENTIATE);
		case TOKEN_BANG:
			return new Expression(Expression.Type.NODE_FACTORIAL);
		default:
			return null;
		}
	}

	Expression getExpression() {
		return expression_;
	}
	
	Expression expression_;
}
