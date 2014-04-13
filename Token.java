package jsolve;

public class Token {
	enum Type {
		TOKEN_SYMBOL,
		TOKEN_OPEN_PARENTHESES,
		TOKEN_CLOSE_PARENTHESES,
		TOKEN_EQUALS,
		TOKEN_EQUALS_STRONG,
		TOKEN_LESSTHAN,
		TOKEN_GREATERTHAN,
		TOKEN_LESSTHAN_EQUAL_TO,
		TOKEN_GREATERTHAN_EQUAL_TO,
		TOKEN_PLUS,
		TOKEN_MINUS,
		TOKEN_MULTIPLY,
		TOKEN_DIVIDE,
		TOKEN_EXPONENTIATE,
		TOKEN_BANG
	}
	
	Token(Type type) {
		type_ = type;
	}
	
	Token(String constant) {
		type_ = Type.TOKEN_SYMBOL;
		symbol_ = constant;
	}
	
	Type getType() {
		return type_;
	}
	
	void setType(Type type) {
		type_ = type;
	}
	
	String getSymbol() {
		return symbol_;
	}
	
	void setSymbol(String symbol) {
		symbol_ = symbol;
	}
	
	Type type_;
	String symbol_;
}
