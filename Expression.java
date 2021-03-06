package jsolve;

public class Expression {
	enum Type {
		NODE_SYMBOL,
		NODE_EQUALS,
		NODE_ADD,
		NODE_SUBTRACT,
		NODE_MULTIPLY,
		NODE_DIVIDE,
		NODE_EXPONENTIATE,
		NODE_PLUS,
		NODE_MINUS,
		NODE_FACTORIAL,
		NODE_FACTORIAL_INVERSE,
		NODE_LOGARITHM,
		NODE_SINE,
		NODE_COSINE,
		NODE_LAMBERTW
	}

	Expression(Type type) {
		type_ = type;
	}
	
	Expression(String symbol) {
		type_ = Type.NODE_SYMBOL;
		symbol_ = symbol;
	}
	
	Expression(Double constant) {
		type_ = Type.NODE_SYMBOL;
		Integer integer = constant.intValue();
		if (constant.equals(new Double(integer))) {
			symbol_ = integer.toString();
		} else {
			symbol_ = constant.toString();
		}
	}

	Expression getLeft() {
		return left_;
	}

	Type getType() {
		return type_;
	}
	
	void setType(Type type) {
		type_ = type;
	}
	
	boolean isSymbol() {
		return type_ == Type.NODE_SYMBOL;
	}
	
	boolean isUnary() {
		return child_ != null;
	}
	
	boolean isBinary() {
		return !isSymbol() && !isUnary();
	}

	void setLeft(Expression left) {
		if (left != null) {
			left_ = left.copy();
		} else {
			left_ = null;
		}
	}
	
	Expression getRight() {
		return right_;
	}
	
	void setRight(Expression right) {
		if (right != null) {
			right_ = right.copy();
		} else {
			right_ = null;
		}
	}
	
	Expression getChild() {
		return child_;
	}
	
	void setChild(Expression child) {
		if (child != null) {
			child_ = child.copy();
		} else {
			child_ = null;
		}
	}
	
	String getSymbol() {
		return symbol_;
	}
	
	void setSymbol(String symbol) {
		symbol_ = symbol;
	}
	
	Expression copy() {
		Expression duplicate = new Expression(type_);
		if (child_ != null) {
			duplicate.child_ = child_.copy();
		}
		if (left_ != null) {
			duplicate.left_ = left_.copy();
		}
		if (right_ != null) {
			duplicate.right_ = right_.copy();
		}
		duplicate.setSymbol(getSymbol());
		return duplicate;
	}
	
	public String toString() {
		if (isSymbol()) {
			Double value = getSymbolAsFloat();
			if (value != null && value < 0) {
				return "(" + getSymbol() + ")";
			} else {
				return getSymbol();
			}
		} else if (isUnary()) {
			String child = getChild().toString();
			switch (getType()) {
			case NODE_PLUS:
				return "(+" + child + ")";
			case NODE_MINUS:
				return "(-" + child + ")";
			case NODE_FACTORIAL:
				return "(" + child + "!)";
			case NODE_FACTORIAL_INVERSE:
				return "InverseFactorial(" + child + ")";
			case NODE_SINE:
				return "Sin(" + child + ")";
			case NODE_COSINE:
				return "Cos(" + child + ")";
			case NODE_LAMBERTW:
				return "LambertW(" + child + ")";
			default:
				return "(?" + child + ")";
			}
		} else {
			String left = getLeft().toString();
			String right = getRight().toString();
			switch (getType()) {
			case NODE_EQUALS:
				return "(" + left + "=" + right + ")";
			case NODE_ADD:
				return "(" + left + "+" + right + ")";
			case NODE_SUBTRACT:
				return "(" + left + "-" + right + ")";
			case NODE_MULTIPLY:
				return "(" + left + "*" + right + ")";
			case NODE_DIVIDE:
				return "(" + left + "/" + right + ")";
			case NODE_EXPONENTIATE:
				return "(" + left + "^" + right + ")";
			case NODE_LOGARITHM:
				return "(log_(" + left + ")(" + right + "))";
			default:
				return "(" + left + "?" + right + ")";
			}
		}
	}
	
	boolean contains(String symbol) {
		if (isBinary()) {
			return left_.contains(symbol) || right_.contains(symbol);
		} else if (isUnary()) {
			return child_.contains(symbol);
		} else if (symbol_.equals(symbol)) {
			return true;
		}
		return false;
	}
	
	Long getSymbolAsInteger() {
		if (isSymbol()) {
			try {
				Long value = Long.parseLong(getSymbol());
				return value;
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
	
	Double getSymbolAsFloat() {
		if (isSymbol()) {
			try {
				Double value = Double.parseDouble(getSymbol());
				return value;
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
	
	boolean isOne() {
		if (isSymbol()) {
			try {
				Double value = Double.parseDouble(getSymbol());
				if (value == 1.0) {
					return true;
				}
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}
	
	boolean isZero() {
		if (isSymbol()) {
			try {
				Double value = Double.parseDouble(getSymbol());
				if (value == 0.0) {
					return true;
				}
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}
	
	boolean isDegenerate() {
		if (isBinary()) {
			if (getLeft().isDegenerate()) {
				return true;
			}
			if (getRight().isDegenerate()) {
				return true;
			}
			if (Simplify.getDenominator(this).isZero()) {
				return true; /* zero divide */
			}
			if (type_.equals(Expression.Type.NODE_LOGARITHM)) {
				if (getRight().isZero()) {
					return true; /* log_n(0) */
				}
			}
		} else if (isUnary()) {
			if (getChild().isDegenerate()) {
				return true;
			}
		}
		return false;
	}

	static Expression equals(Expression a, Expression b) {
		Expression result = new Expression(Type.NODE_EQUALS);
		result.setLeft(a);
		result.setRight(b);
		return result;
	}

	static Expression add(Expression a, Expression b) {
		Expression result = new Expression(Type.NODE_ADD);
		result.setLeft(a);
		result.setRight(b);
		return result;
	}
	
	static Expression subtract(Expression a, Expression b) {
		Expression result = new Expression(Type.NODE_SUBTRACT);
		result.setLeft(a);
		result.setRight(b);
		return result;
	}
	
	static Expression multiply(Expression a, Expression b) {
		Expression result = new Expression(Type.NODE_MULTIPLY);
		result.setLeft(a);
		result.setRight(b);
		return result;
	}
	
	static Expression divide(Expression a, Expression b) {
		Expression result = new Expression(Type.NODE_DIVIDE);
		result.setLeft(a);
		result.setRight(b);
		return result;
	}
	
	static Expression exponentiate(Expression a, Expression b) {
		Expression result = new Expression(Type.NODE_EXPONENTIATE);
		result.setLeft(a);
		result.setRight(b);
		return result;
	}

	static Expression exponentiate(Expression x) {
		/* complex exponentiation */
		Expression cos = new Expression(Expression.Type.NODE_COSINE);
		cos.setChild(x);
		Expression sin = new Expression(Expression.Type.NODE_SINE);
		sin.setChild(x);
		Expression isin = Expression.multiply(new Expression("i"), sin);
		Expression result = Expression.add(cos, isin);
		return result;
	}
	
	static Expression logarithm(Expression a, Expression b) {
		Expression result = new Expression(Type.NODE_LOGARITHM);
		result.setLeft(a);
		result.setRight(b);
		return result;
	}
	
	static Expression lambertW(Expression x) {
		Expression result = new Expression(Expression.Type.NODE_LAMBERTW);
		result.setChild(x);
		return result;
	}
	
	static Expression negate(Expression x) {
		return Expression.multiply(new Expression("-1"),  x);
	}

	Type type_;
	Expression left_;
	Expression right_;
	Expression child_;
	String symbol_;
}
