package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Parser (Analisador Sintático).
 * Transforma a lista plana de Tokens gerada pelo Scanner em uma Árvore de Sintaxe Abstrata (AST).
 * Utiliza a técnica "Recursive Descent Parsing" (Análise Descendente Recursiva).
 *
 * Referência: Crafting Interpreters - Capítulos 6 (Parsing Expressions), 
 * 8 (Statements), 9 (Control Flow), 10 (Functions) e 12 (Classes).
 */
public class Parser {

    // [Cap. 6] Classe de erro simples para desenrolar a recursão em caso de erro de sintaxe.
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0; // Ponteiro para o token atual sendo analisado.

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * [Cap. 8] Método principal que inicia o parsing.
     * Em vez de retornar uma única expressão, agora retorna uma lista de declarações.
     */
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    // --- Declarações (Declarations) ---

    /**
     * [Cap. 8] Ponto de entrada para declarações.
     * Uma declaração pode ser uma definição de variável ou um comando (statement).
     * Também lida com a sincronização de erros (Panic Mode Recovery).
     */
    private Stmt declaration() {
        try {
            // [Cap. 12 - NOVO] Reconhece declaração de Classe
            if (match(CLASS)) return classDeclaration();

            // [Cap. 10] Reconhece declaração de função
            if (match(FUN)) return function("function"); 

            if (match(VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    /**
     * [Cap. 12 - NOVO] Faz o parsing de uma declaração de classe.
     * Gramática: class Nome { métodos... }
     */
    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");
        consume(LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, methods);
    }

    /**
     * [Cap. 10] Faz o parsing de uma declaração de função.
     * Gramática: fun nome(params) { corpo }
     * @param kind Passamos "function" ou "method" (para reutilizar em classes no futuro).
     */
    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");

        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();

        return new Stmt.Function(name, parameters, body);
    }

    /**
     * [Cap. 8] Declaração de Variável: 'var' nome ('=' inicializador)? ';'
     */
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    /**
     * [Cap. 8 e 9] Comandos (Statements).
     * Roteia para o tipo específico de comando baseado na palavra-chave.
     */
    private Stmt statement() {
        if (match(FOR)) return forStatement();     // [Cap. 9] Laço For
        if (match(IF)) return ifStatement();       // [Cap. 9] Condicional If
        if (match(PRINT)) return printStatement(); // [Cap. 8] Print
        if (match(RETURN)) return returnStatement(); // [Cap. 10] Return (Recuperado)
        if (match(WHILE)) return whileStatement(); // [Cap. 9] Laço While
        
        // [Cap. 8] Bloco de escopo { ... }
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    /**
     * [Cap. 10] Declaração de Retorno (return value;).
     * (Adicionado pois estava faltando no seu código base)
     */
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    /**
     * [Cap. 9] Laço 'For'.
     * O Lox não tem um nó de AST próprio para 'for'.
     * O parser "desaçucara" (desugars) o 'for' transformando-o em um 'while' equivalente.
     */
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        // 1. Inicializador (ex: var i = 0;)
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        // 2. Condição (ex: i < 10;)
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        // 3. Incremento (ex: i = i + 1)
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement();

        // Construção do While equivalente:
        
        // Se houver incremento, ele executa após o corpo.
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(
                body,
                new Stmt.Expression(increment)
            ));
        }

        // Se não houver condição, assume 'true' (loop infinito).
        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        // Se houver inicializador, ele executa antes de tudo.
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    /**
     * [Cap. 9] Condicional If: if (condicao) thenBranch (else elseBranch)?
     */
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * [Cap. 8] Comando Print: print expressao;
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    /**
     * [Cap. 9] Laço While: while (condicao) corpo
     */
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    /**
     * [Cap. 8] Bloco: { declarações... }
     */
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    /**
     * [Cap. 8] Expression Statement: expressao;
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    // --- Expressões (Precedência) ---

    /**
     * [Cap. 6] Nível Superior de Expressão.
     * Delega para assignment().
     */
    private Expr expression() {
        return assignment();
    }

    /**
     * [Cap. 8] Atribuição.
     * Precedência: Atribuição -> Lógica OR -> ...
     */
    private Expr assignment() {
        // Primeiro tenta parsear como uma lógica OR (que desce para as outras).
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            // Recursivo para permitir a = b = c
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            } 
            // [Cap. 12 - NOVO] Lógica de Set (Atribuição em Propriedade)
            // Se o parser viu "obj.prop" (Get) e agora encontrou um "=", vira um "Set".
            else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    /**
     * [Cap. 9] Lógica OR ('or').
     * Tem precedência menor que AND.
     */
    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * [Cap. 9] Lógica AND ('and').
     * Tem precedência menor que Igualdade.
     */
    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * [Cap. 6] Igualdade (==, !=).
     */
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * [Cap. 6] Comparação (>, >=, <, <=).
     */
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * [Cap. 6] Termo (Adição/Subtração).
     */
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * [Cap. 6] Fator (Multiplicação/Divisão).
     */
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * [Cap. 6] Unário (!, -).
     */
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        // [Cap. 10] Chamada de Função
        return call();
    }

    /**
     * [Cap. 10 e 12] Parsing de Chamadas e Acessos.
     * Atualizado no Cap 12 para suportar 'dot' (ponto) para propriedades.
     */
    private Expr call() {
        Expr expr = primary();

        while (true) { 
            // [Cap. 10] Chamada de função: ()
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } 
            // [Cap. 12 - NOVO] Acesso a propriedade: .identificador
            else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } 
            else {
                break;
            }
        }

        return expr;
    }

    /**
     * [Cap. 10] Auxiliar para processar os argumentos da função.
     */
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    /**
     * [Cap. 6] Primário (Literais, Variáveis, Agrupamento).
     * O nível mais alto de precedência (avaliado primeiro).
     */
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        // [Cap. 12 - NOVO] Palavra-chave 'this'
        if (match(THIS)) return new Expr.This(previous());

        // [Cap. 8] Identificadores (Uso de Variável)
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    // --- Helpers (Métodos Auxiliares) ---

    /**
     * Verifica se o token atual é de um determinado tipo.
     * Se for, consome-o e retorna true. Caso contrário, retorna false.
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Consome o token atual se for do tipo esperado.
     * Se não for, lança um erro de sintaxe.
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    /**
     * Verifica se o token atual é do tipo especificado sem consumi-lo.
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /**
     * Consome e retorna o token atual.
     */
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * Relata um erro e retorna a exceção ParseError para desenrolar a pilha.
     */
    private ParseError error(Token token, String message) {
        Lox.error(token.line, message);
        return new ParseError();
    }

    /**
     * [Cap. 6] Sincronização de Erro.
     * Descarta tokens até encontrar um limite de declaração (como ';')
     * ou o início de uma nova declaração (var, if, while, etc.).
     * Isso evita que um erro de sintaxe gere uma cascata de centenas de erros.
     */
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
