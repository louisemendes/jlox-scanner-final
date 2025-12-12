package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Scanner (Analisador Léxico).
 * Responsável por ler o código-fonte (String) e transformá-lo em uma sequência de Tokens.
 * * Conceito chave: "Maximal Munch" (O Scanner sempre tenta capturar o maior token possível).
 * Referência: Crafting Interpreters - Capítulo 4 (Scanning).
 */
public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    // [Livro 4.4] Gerenciamento de Estado (Ponteiros)
    // 'start': Aponta para o primeiro caractere do lexema que está sendo processado.
    private int start = 0;
    // 'current': Aponta para o caractere atual sendo analisado (o cursor).
    private int current = 0;
    // 'line': Rastreia a linha atual do arquivo (crucial para mensagens de erro).
    private int line = 1;

    // [Livro 4.7] Palavras Reservadas (Keywords)
    // Mapa estático para busca rápida (O(1)) de palavras-chave da linguagem.
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

    /**
     * [Livro 4.4] Método principal do Scanner.
     * Itera sobre todo o código-fonte até encontrar o fim (EOF).
     * @return Uma lista contendo todos os tokens encontrados.
     */
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // No início de cada loop, estamos prontos para ler o próximo lexema.
            // O ponteiro 'start' alcança o 'current'.
            start = current;
            scanToken();
        }

        // [Livro 4.4] Sempre adicionamos um token de EOF ao final.
        // Isso ajuda o Parser a saber que o arquivo terminou de forma limpa.
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    /**
     * [Livro 4.5] Reconhecimento de Lexemas.
     * Analisa o caractere atual e decide qual tipo de token ele inicia.
     */
    private void scanToken() {
        char c = advance(); // Consome o caractere atual

        switch (c) {
            // [Livro 4.5] Tokens de caractere único
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

            // [Livro 4.5] Operadores que podem ter dois caracteres (! vs !=)
            // Utilizamos 'match' para olhar o próximo caractere condicionalmente.
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

            // [Livro 4.5] Barras: Podem ser divisão (/) ou comentário (//)
            case '/':
                if (match('/')) {
                    // Um comentário vai até o final da linha.
                    // Note que usamos 'peek' pois não queremos incluir o \n no comentário.
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;

            // [Livro 4.5] Caracteres em branco (Whitespace)
            // Simplesmente ignoramos para permitir indentação livre.
            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                line++; // Incrementa contador de linha
                break;

            // [Livro 4.6] Literais de String
            case '"': string(); break;

            default:
                // [Livro 4.6] Dígitos (Números)
                if (isDigit(c)) {
                    number();
                } 
                // [Livro 4.7] Letras (Identificadores ou Keywords)
                else if (isAlpha(c)) {
                    identifier();
                } 
                else {
                    // [Livro 4.1] Tratamento de erro léxico
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    // --- Rotinas Específicas de Tipo (Literais e Identificadores) ---

    /**
     * [Livro 4.7] Processa Identificadores e Palavras Reservadas.
     * Lê caracteres alfanuméricos consecutivamente. Ao final, verifica se
     * o texto formado é uma palavra reservada (ex: 'var'). Se não for, é um IDENTIFIER.
     */
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        
        if (type == null) type = IDENTIFIER;
        
        addToken(type);
    }

    /**
     * [Livro 4.6] Processa Literais Numéricos.
     * Lida com números inteiros e ponto flutuante.
     */
    private void number() {
        while (isDigit(peek())) advance();

        // Procura pela parte fracionária
        // Exige que haja um dígito APÓS o ponto (lookahead de 2 caracteres)
        // Isso evita confundir '123.method()' com um número.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consome o '.'
            advance();

            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /**
     * [Livro 4.6] Processa Literais de String.
     * Suporta strings multilinhas, mantendo a contagem de linhas correta.
     */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // O fechamento da aspa (")
        advance();

        // Trim das aspas ao redor: captura apenas o miolo da string.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    // --- Métodos Auxiliares de Navegação (Helpers) ---

    /**
     * Verifica se o caractere atual é o esperado.
     * Se for, consome-o (avança current) e retorna true.
     * Útil para operadores compostos (ex: '!=').
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    /**
     * Lookahead (1 caractere): Retorna o caractere atual sem consumi-lo.
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * Lookahead (2 caracteres): Retorna o próximo caractere sem consumi-lo.
     * Necessário para distinguir números decimais de chamadas de método.
     */
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

    /**
     * Consome o caractere atual e retorna-o, avançando o cursor.
     */
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