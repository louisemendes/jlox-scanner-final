package main.java.com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;

import java.util.Objects;

/**
 * Interpreter: implementa Expr.Visitor<Object> para avaliar expressões.
 * Suporta: Literal, Grouping, Unary, Binary (com +,-,*,/, comparações, igualdade).
 */
public class Interpreter implements Expr.Visitor<Object> {

    // Entrypoint público para avaliar uma expressão
    public Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    // Converte valor Lox para string para imprimir no REPL
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

    // --- VISITORS ---

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

    // --- UTILITÁRIOS ---

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

