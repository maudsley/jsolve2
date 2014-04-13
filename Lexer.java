package jsolve;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
	Lexer(String expression) {
		tokens_ = new ArrayList<Token>();
		expression_ = expression;
		constant_ = -1;
	}

	Token getNextToken() {
		Token a = getNextTokenBuffered();
		if (a == null) {
			return null;
		}
		
		Token b = getNextTokenBuffered();
		if (b == null) {
			return a;
		}
		
		switch (a.getType()) {
		case TOKEN_EQUALS:
			switch (b.getType()) {
			case TOKEN_EQUALS:
				return new Token(Token.Type.TOKEN_EQUALS_STRONG);
			default:
				break;
			}
		case TOKEN_LESSTHAN:
			switch (b.getType()) {
			case TOKEN_EQUALS:
				return new Token(Token.Type.TOKEN_LESSTHAN_EQUAL_TO);
			default:
				break;
			}
		case TOKEN_GREATERTHAN:
			switch (b.getType()) {
			case TOKEN_EQUALS:
				return new Token(Token.Type.TOKEN_GREATERTHAN_EQUAL_TO);
			default:
				break;
			}
		case TOKEN_MULTIPLY:
			switch (b.getType()) {
			case TOKEN_MULTIPLY:
				return new Token(Token.Type.TOKEN_EXPONENTIATE);
			default:
				break;
			}
		default:
			break;
		}
		
		tokens_.add(0, b);
		return a;
	}

	private Token getNextTokenBuffered() {
		if (!tokens_.isEmpty()) {
			return tokens_.remove(0);
		}
		readNextToken();
		if (!tokens_.isEmpty()) {
			return tokens_.remove(0);
		}
		return null;
	}

	private void readNextToken() {
		Token token = null;
		while (token == null) {
			if (cursor_ == expression_.length()) {
				break;
			}
			switch (expression_.charAt(cursor_)) {
			case '(':
				token = new Token(Token.Type.TOKEN_OPEN_PARENTHESES);
				break;
			case ')':
				token = new Token(Token.Type.TOKEN_CLOSE_PARENTHESES);
				break;
			case '=':
				token = new Token(Token.Type.TOKEN_EQUALS);
				break;
			case '<':
				token = new Token(Token.Type.TOKEN_LESSTHAN);
				break;
			case '>':
				token = new Token(Token.Type.TOKEN_GREATERTHAN);
				break;
			case '+':
				token = new Token(Token.Type.TOKEN_PLUS);
				break;
			case '-':
				token = new Token(Token.Type.TOKEN_MINUS);
				break;
			case '*':
				token = new Token(Token.Type.TOKEN_MULTIPLY);
				break;
			case '/':
				token = new Token(Token.Type.TOKEN_DIVIDE);
				break;
			case '^':
				token = new Token(Token.Type.TOKEN_EXPONENTIATE);
				break;
			case '!':
				token = new Token(Token.Type.TOKEN_BANG);
				break;
			case ' ':
				if (constant_ >= 0) {
					token = new Token(expression_.substring(constant_, cursor_));
					constant_ = -1;
				}
				break;
			default:
				if (constant_ < 0) {
					constant_ = cursor_;
				}
				break;
			}
			if (token != null) {
				if (constant_ >= 0) {
					tokens_.add(new Token(expression_.substring(constant_, cursor_)));
					constant_ = -1;
				}
				if (token != null) {
					tokens_.add(token);
				}
			}
			cursor_++;
		}
		if (constant_ >= 0) {
			tokens_.add(new Token(expression_.substring(constant_)));
			constant_ = -1;
		}
	}

	int cursor_;
	int constant_;
	String expression_;
	List<Token> tokens_;
}
