package com.craftinginterpreters.lox;

// Necessário para o campo 'List<Expr>' (futuramente para o parsing de statements)
import java.util.List; 

// A classe base abstrata para todas as expressões
abstract class Expr {

    // 5.2.3: Interface Visitor - Define um método para cada classe de expressão
    interface Visitor<R> {
        R visitAssignExpr(Assign expr); // NOVO
        R visitBinaryExpr(Binary expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal expr);
        R visitUnaryExpr(Unary expr);
        R visitVariableExpr(Variable expr); // NOVO
    }

    // 5.2.2: Classes Geradas (Subclasses estáticas)

    // NOVO: Representa a atribuição de valor a uma variável (name = value)
    static class Assign extends Expr {
        Assign(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssignExpr(this);
        }

        final Token name;
        final Expr value;
    }

    // Representa operações binárias (1 + 2)
    static class Binary extends Expr {
        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor) { // Método accept para o Visitor
            return visitor.visitBinaryExpr(this);
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }

    // Representa expressões agrupadas por parênteses ((1 + 2))
    static class Grouping extends Expr {
        Grouping(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }

        final Expr expression;
    }

    // Representa valores literais (123, "string", true, nil)
    static class Literal extends Expr {
        Literal(Object value) {
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }

        final Object value;
    }

    // Representa operações unárias (!true, -1)
    static class Unary extends Expr {
        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }

        final Token operator;
        final Expr right;
    }

    // NOVO: Representa a referência a uma variável (retorna o valor)
    static class Variable extends Expr {
        Variable(Token name) {
            this.name = name;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }

        final Token name;
    }

    // 5.2.2: Método Abstrato de aceitação
    abstract <R> R accept(Visitor<R> visitor);
}