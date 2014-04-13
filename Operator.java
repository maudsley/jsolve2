package jsolve;

class Operator {
	enum Arity {
		BINARY,
		UNARY_LEFT,
		UNARY_RIGHT
	}

	enum Order {
		LEFT_TO_RIGHT,
		RIGHT_TO_LEFT,
		NOT_APPLICABLE
	}

	static Operator[] operators = {
		new Operator(0, Expression.Type.NODE_EQUALS, Operator.Arity.BINARY, Operator.Order.LEFT_TO_RIGHT),
		new Operator(1, Expression.Type.NODE_ADD, Operator.Arity.BINARY, Operator.Order.LEFT_TO_RIGHT),
		new Operator(1, Expression.Type.NODE_SUBTRACT, Operator.Arity.BINARY, Operator.Order.LEFT_TO_RIGHT),
		new Operator(2, Expression.Type.NODE_MULTIPLY, Operator.Arity.BINARY, Operator.Order.LEFT_TO_RIGHT),
		new Operator(2, Expression.Type.NODE_DIVIDE, Operator.Arity.BINARY, Operator.Order.LEFT_TO_RIGHT),
		new Operator(3, Expression.Type.NODE_PLUS, Operator.Arity.UNARY_LEFT, Operator.Order.NOT_APPLICABLE),
		new Operator(3, Expression.Type.NODE_MINUS, Operator.Arity.UNARY_LEFT, Operator.Order.NOT_APPLICABLE),
		new Operator(4, Expression.Type.NODE_EXPONENTIATE,Operator.Arity.BINARY, Operator.Order.RIGHT_TO_LEFT),
		new Operator(5, Expression.Type.NODE_FACTORIAL, Operator.Arity.UNARY_RIGHT, Operator.Order.NOT_APPLICABLE)
	};

	Operator(int precedence, Expression.Type type, Arity arity, Order order) {
		precedence_ = precedence;
		type_ = type;
		arity_ = arity;
		order_ = order;
	}
	
	int getPrecedence() {
		return precedence_;
	}
	
	Expression.Type getType() {
		return type_;
	}
	
	Arity getArity() {
		return arity_;
	}

	Order getOrder() {
		return order_;
	}
	
	static Operator fromExpression(Expression expression) {
		for (Operator operator : operators) {
			if (operator.getType().equals(expression.getType())) {
				return operator;
			}
		}
		return null;
	}
	
	static boolean isBinary(Expression expression) {
		if (expression.isSymbol()) {
			return false;
		}
		return fromExpression(expression).getArity().equals(Operator.Arity.BINARY);
	}

	int precedence_;
	Expression.Type type_;
	Arity arity_;
	Order order_;
}
