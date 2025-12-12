package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Scanner (Analisador Léxico).
 *
 * Responsável por percorrer o código-fonte e convertê-lo em uma sequência linear
 * de Tokens. Implementa o comportamento descrito no Capítulo 4 do livro
 * "Crafting Interpreters", incluindo:
 *
 *  - Estratégia "maximal munch" (captura do maior lexema válido).
 *  - Lookahead com 1 e 2 caracteres.
 *  - Reconhecimento de literais, identificadores, operadores e palavras-chave.
 *  - Tratamento de comentários e whitespace.
 *  - Geração de tokens com informação de linha.
 */
public class Scanner {

    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    // Ponteiro para o início do lexema atual.
    private int start = 0;
    // Ponteiro para o caractere sendo analisado neste ciclo.
    private int current = 0;
    // Contador de linhas para diagnóstico.
    private int line = 1;

    /**
     * Palavras reservadas da linguagem Lox.
     * Esta tabela permite diferenciação rápida entre IDENTIFIER e KEYWORD.
     */
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
        // Poderia ser transformado em um Map imutável, se desejado.
    }

    public Scanner(String source) {
        this.source = source;
    }

    /**
     * Método principal do analisador léxico.
     *
     * Percorre todo o código-fonte gerando tokens até encontrar EOF.
     * Sempre retorna uma lista completa, incluindo o token EOF final.
     */
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    /**
     * Avalia o caractere atual e determina qual token iniciar.
     * Implementa exatamente o fluxograma do Capítulo 4.
     */
    private void scanToken() {
        char c = advance();

        switch (c) {
            // Tokens de caractere único
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;

            // Operadores compostos (lookahead condicional)
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;

            // Comentários e barra
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;

            // Whitespace ignorado
            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                line++;
                break;

            // Literais de string (suportam múltiplas linhas)
            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    // --- Reconhecimento de tipos específicos ---

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;

        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // Consome '.'
            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        advance(); // Fecha aspas

        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    // --- Helpers de navegação e lookahead ---

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
