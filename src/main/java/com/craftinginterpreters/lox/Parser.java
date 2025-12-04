package main.java.com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;


public class Parser {
    // 6.1: Exceção customizada para erros de parsing
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0; // Ponteiro para o token atual

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // NOVO: Retorna uma lista de instruções (em vez de apenas uma expressão)
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration()); // O ponto de entrada principal do parser.
        }
        return statements;
    }

    // --- Métodos de Instrução e Declaração (NOVOS) ---

    // 8.3: O ponto de entrada para declarações (var, fun, class ou statement)
    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize(); // Tenta se recuperar de um erro
            return null;
        }
    }

    // 8.3: Declaração de variável (var name = expr;)
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Esperado nome da variável.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Esperado ';' após declaração de variável.");
        return new Stmt.Var(name, initializer);
    }

    // 8.2 & 9.2: Trata as instruções de nível superior (print, if, block e expression)
    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block()); // 9.2: Bloco
        if (match(IF)) return ifStatement(); // 9.3: Condicional
        
        return expressionStatement();
    }

    // 8.2: Instrução print (print expr;)
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Esperado ';' depois do valor.");
        return new Stmt.Print(value);
    }

    // 8.2: Instrução de expressão (expr;)
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Esperado ';' depois da expressão.");
        return new Stmt.Expression(expr);
    }

    // 9.2: Parsing do bloco ({ ... })
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Esperado '}' depois do bloco.");
        return statements;
    }

    // 9.3: Parsing do condicional (if (condition) then else branch)
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Esperado '(' depois de 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Esperado ')' depois da condição.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    // --- Métodos de Precedência de Expressão (MODIFICADOS) ---
    
    private Expr expression() {
        // 7.1: O ponto de entrada é agora 'assignment'
        return assignment();
    }
    
    // 7.1: Atribuição (NOVO NÍVEL DE PRECEDÊNCIA)
    private Expr assignment() {
        Expr expr = equality(); // Começa no nível de precedência mais alto

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment(); // Recurso para atribuições em cadeia (a = b = c)

            if (expr instanceof Expr.Variable) {
                // Se for uma variável, transforma-a em um nó de atribuição
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            
            // Se o lado esquerdo não for um LValue (variável), reporta erro.
            error(equals, "Alvo de atribuição inválido.");
        }

        return expr;
    }

    // ... (equality, comparison, term, factor, unary) - Mantidos ...
    
    // 7.1: 'primary' precisa de um novo caso para IDENTIFIER
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }
        
        // NOVO: Tratamento de identificadores (referência de variável)
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        // Agrupamento
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Esperado ')' depois da expressão.");
            return new Expr.Grouping(expr);
        }
        
        throw error(peek(), "Esperado expressão.");
    }
    
    // --- Funções Auxiliares de Consumo de Tokens (MANTIDAS) ---
    
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        
        throw error(peek(), message);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token.line, message);
        return new ParseError();
    }
    
    // Discarda tokens após um erro de parsing para tentar sincronizar.
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

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
    
    // ... (comparison, term, factor, unary) - Não precisam ser modificados, mantendo a estrutura original
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }
}