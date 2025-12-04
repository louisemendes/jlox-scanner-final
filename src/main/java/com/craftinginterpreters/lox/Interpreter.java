package main.java.com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;

import java.util.List;
import java.util.Objects;

/**
 * Interpreter: implementa Expr.Visitor<Object> e Stmt.Visitor<Void> para avaliar
 * e executar o código.
 */
// Implementa as duas interfaces Visitor
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    // NOVO: Campo para o ambiente global de variáveis
    private Environment environment = new Environment();

    // NOVO: Entrypoint público para avaliar uma LISTA de instruções
    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement); // Executa cada instrução
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error); // Trata o erro de execução
        }
    }

    // --- MÉTODOS DE EXECUÇÃO AUXILIARES ---

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    // NOVO: Executa um bloco de instruções em um novo escopo
    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment; // Muda para o novo ambiente

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous; // Restaura o ambiente anterior
        }
    }

    // --- VISITORS DE EXPRESSÃO (MANTIDOS E COMPLETOS) ---

    // Entrypoint público para avaliar uma expressão (mantido)
    public Object evaluate(Expr expr) {
        return expr.accept(this);
    }
    // ... (visitLiteralExpr, visitGroupingExpr, visitUnaryExpr, visitBinaryExpr, stringify, isTruthy, isEqual, checkNumberOperand/s) ...

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -((Double) right);
            case BANG:
                return !isTruthy(right);
        }

        // unreachable
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            // arithmetic
            case PLUS:
                // suporte para concatenação de strings
                if (left instanceof Double && right instanceof Double) {
                    return (Double) left + (Double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                // também permitir number + string? não por padrão — reporta erro
                throw new RuntimeError(expr.operator,
                        "Operador '+' requer dois números ou duas strings.");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left - (Double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left * (Double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left / (Double) right;

            // comparison
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left > (Double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left >= (Double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left < (Double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left <= (Double) right;

            // equality
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
        }

        // unreachable
        return null;
    }

    // --- VISITORS DE INSTRUÇÃO (NOVOS) ---

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression); // Avalia a expressão, mas ignora o resultado
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression); // Avalia a expressão
        System.out.println(stringify(value)); // Imprime o resultado
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) { // Se houver inicializador, avalia a expressão
            value = evaluate(stmt.initializer);
        }
        // Define a variável no ambiente atual
        environment.define(stmt.name.lexeme, value);
        return null;
    }
    
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        // Cria um novo ambiente aninhado e o executa
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch); // Condição verdadeira: executa 'then'
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch); // Condição falsa: executa 'else' (se existir)
        }
        return null;
    }


    // --- UTILITÁRIOS (MANTIDOS E COMPLETOS) ---

    public String stringify(Object object) {
        if (object == null) return "nil";
        // Em Lox, números são double; remover .0 quando inteiro
        if (object instanceof Double) {
            double d = (Double) object;
            if (d == (long) d) {
                return String.format("%d", (long) d);
            }
        }
        return object.toString();
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (Boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operando deve ser um número.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operadores requerem números.");
    }
}

