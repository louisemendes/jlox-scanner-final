package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

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

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            // Tokens de um único caractere
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

            // Tokens de um ou dois caracteres
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;

            // Barras (Divisão ou Comentário)
            case '/':
                if (match('/')) {
                    // É um comentário. Consome tudo até o fim da linha (mas não o \n).
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
            
            // Whitespace e Quebra de Linha (Descartados ou contados)
            case ' ':
            case '\r':
            case '\t':
                // Ignora o whitespace.
                break;

            case '\n':
                line++; // Incrementa a linha ao encontrar a quebra de linha.
                break;

            // 4.6: Strings
            case '"': string(); break; 
            
            default:
                if (isDigit(c)) {
                    number(); 
                } else if (isAlpha(c)) { // 💡 Adiciona a verificação para letras
                    identifier(); 
                } else {
                    Lox.error(line, "Caractere inesperado."); 
                }
                break;
        }
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    // Consome o caractere atual e avança o ponteiro
    private char advance() {
        return source.charAt(current++);
    }

    // Cria e adiciona um Token (sem valor literal)
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    // Cria e adiciona um Token com um valor literal
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    // Testa se o caractere atual corresponde ao esperado. Se sim, consome e retorna true.
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    // Olha o caractere atual sem consumir (peek)
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    // Olha o próximo caractere após o atual (peekNext - necessário para 4.6)
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }
    
// 4.6: Rotina para reconhecer Strings
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++; // Suporta strings multi-linha
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "String não terminada.");
            return;
        }

        advance(); // Consome a aspa de fechamento

        // Extrai o valor do literal (sem as aspas)
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    // 4.6: Rotina para reconhecer Números
    private void number() {
        // Consome a parte inteira
        while (isDigit(peek())) advance();

        // Verifica se há parte fracionária (o ponto e pelo menos um dígito)
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // Consome o '.'
            // Consome os dígitos da parte fracionária
            while (isDigit(peek())) advance();
        }

        // Converte o lexema (string) para Double e adiciona como literal
        addToken(NUMBER, Double.parseDouble(
            source.substring(start, current)));
    }

    // 4.7: Rotina para reconhecer Identificadores e Palavras-Chave
    private void identifier() {
        while (isAlphaNumeric(peek())) advance(); // Consome toda a palavra
        
        String text = source.substring(start, current);
        
        // Verifica no mapa se a palavra é uma palavra-chave (keyword)
        TokenType type = keywords.get(text);
        
        // Se a busca retornar null, não é palavra-chave, é um IDENTIFIER.
        if (type == null) type = IDENTIFIER;
        
        addToken(type); 
    }

    // Métodos de verificação de caracteres (necessários para 4.6 e 4.7)
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
    
}
