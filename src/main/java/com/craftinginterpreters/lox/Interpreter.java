package com.craftinginterpreters.lox;

import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Interpreter (Interpretador).
 * Responsável por executar a Árvore de Sintaxe Abstrata (AST) gerada pelo Parser.
 * Implementa o padrão Visitor para percorrer os nós da árvore e calcular os resultados.
 *
 * Referências:
 * - Cap. 7 (Evaluating Expressions)
 * - Cap. 8 (Statements and State)
 * - Cap. 9 (Control Flow)
 */
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    // [Cap. 8] Ambiente Global/Atual.
    // Começa apenas com o escopo global. Conforme blocos são entrados, 
    // este campo aponta para o novo ambiente local.
    private Environment environment = new Environment();

    /**
     * [Cap. 8] Ponto de entrada para execução de uma lista de comandos.
     */
    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    // --- Execução de Statements (Comandos) ---

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    /**
     * [Cap. 8] Executa um bloco de código dentro de um escopo específico.
     * Salva o ambiente anterior, troca para o novo, executa, e restaura o anterior.
     */
    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        // Se houver inicializador (var a = 1;), avalia.
        // Se não (var a;), o valor permanece null (nil).
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        // [Cap. 9] Executa o corpo repetidamente enquanto a condição for verdadeira.
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    // --- Avaliação de Expressões ---

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        // [Cap. 8] Avalia o valor e o atribui à variável existente.
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                // [Cap. 7] Sobrecarga do operador + (Soma numérica ou Concatenação).
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator,
                    "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        // [Cap. 9] Lógica de Curto-Circuito (Short-circuit).
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            // Se for OR e o esquerdo já for verdadeiro, retorna o esquerdo.
            if (isTruthy(left)) return left;
        } else {
            // Se for AND e o esquerdo for falso, retorna o esquerdo (falso).
            if (!isTruthy(left)) return left;
        }

        // Só avalia o lado direito se necessário.
        return evaluate(expr.right);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        // [Cap. 8] Busca o valor da variável no ambiente.
        return environment.get(expr.name);
    }

    // --- Utilitários (Helpers) ---

    /**
     * [Cap. 7] Verifica operandos numéricos para operações unárias (-).
     */
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    /**
     * [Cap. 7] Verifica operandos numéricos para operações binárias (+, -, *, /).
     */
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    /**
     * [Cap. 7] Regra de 'Truthiness' do Lox.
     * - false e nil são falsos.
     * - Tudo o mais é verdadeiro.
     */
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    /**
     * [Cap. 7] Converte o resultado de volta para String para impressão.
     * Trata o caso de inteiros (remover o .0 do double).
     */
    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
}