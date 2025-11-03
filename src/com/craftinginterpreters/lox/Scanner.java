package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    // 4.4: Campos de estado
    private int start = 0;
    private int current = 0;
    private int line = 1;

    // 4.7: Mapa para Palavras-Chave (Keywords)
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

    public Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            // 4.5: Tokens de um único caractere
            case '(': addToken(LEFT_PAREN); break; case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break; case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break; case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break; case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break; case '*': addToken(STAR); break;

            // 4.5: Tokens de um ou dois caracteres (usando match)
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;

            // 4.5: Barras (Divisão ou Comentário)
            case '/':
                if (match('/')) { while (peek() != '\n' && !isAtEnd()) advance(); } 
                else { addToken(SLASH); }
                break;
            
            // 4.5: Whitespace e Quebra de Linha
            case ' ': case '\r': case '\t': break; // Ignora whitespace.
            case '\n': line++; break; // Conta a linha.

            // 4.6: Strings
            case '"': string(); break; 
            
            // 4.6/4.7: Literais, Identificadores e Erros
            default:
                if (isDigit(c)) {
                    number(); // 4.6: Rotina de números
                } else if (isAlpha(c)) { 
                    identifier(); // 4.7: Rotina de identificadores/keywords
                } else {
                    Lox.error(line, "Caractere inesperado."); 
                }
                break;
        }
    }

    // 4.6: Rotinas auxiliares de Literais
    private void string() {
        while (peek() != '"' && !isAtEnd()) { if (peek() == '\n') line++; advance(); }
        if (isAtEnd()) { Lox.error(line, "String não terminada."); return; }
        advance(); // Consome aspa de fechamento
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private void number() {
        while (isDigit(peek())) advance();
        if (peek() == '.' && isDigit(peekNext())) { advance(); while (isDigit(peek())) advance(); }
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    // 4.7: Rotina de Identificadores e Palavras-Chave
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type); 
    }

    // --- Funções Auxiliares de Baixo Nível ---
    private char advance() { return source.charAt(current++); }
    private void addToken(TokenType type) { addToken(type, null); }
    private void addToken(TokenType type, Object literal) { 
        String text = source.substring(start, current); 
        tokens.add(new Token(type, text, literal, line)); 
    }
    private boolean match(char expected) {
        if (isAtEnd()) return false; if (source.charAt(current) != expected) return false;
        current++; return true;
    }
    private char peek() { if (isAtEnd()) return '\0'; return source.charAt(current); }
    private char peekNext() { if (current + 1 >= source.length()) return '\0'; return source.charAt(current + 1); }
    
    // Métodos de verificação de caracteres
    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private boolean isAlpha(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private boolean isAlphaNumeric(char c) { return isAlpha(c) || isDigit(c); }
    private boolean isAtEnd() { return current >= source.length(); }
}