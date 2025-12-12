package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Classe principal (Main) do Interpretador Lox.
 * Responsável por iniciar a aplicação e orquestrar o pipeline de execução:
 * 1. Análise Léxica (Scanning)
 * 2. Análise Sintática (Parsing)
 * 3. Análise Semântica (Resolving)
 * 4. Interpretação (Interpreting)
 *
 * Referência: Crafting Interpreters - Capítulo 4 (Scanning).
 */
public class Lox {

    // [Cap. 4.1] Sinalizador de erro de sintaxe.
    // Se for true, interrompe o processo antes da execução para evitar falhas em cascata.
    static boolean hadError = false;

    // [Cap. 7] Sinalizador de erro de tempo de execução (Runtime).
    // Se for true, o processo termina com código de erro específico (70).
    static boolean hadRuntimeError = false;

    // [Cap. 8] Instância única do Interpretador.
    // É estática para que o estado (variáveis globais) persista durante uma sessão REPL.
    private static final Interpreter interpreter = new Interpreter();

    /**
     * Ponto de entrada da aplicação Java.
     * Suporta dois modos:
     * 1. Arquivo: jlox [caminho/arquivo.lox]
     * 2. REPL: jlox (sem argumentos)
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64); // [Cap. 4] Código padrão UNIX para erro de uso (EX_USAGE).
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    /**
     * [Cap. 4] Modo Arquivo: Lê o arquivo inteiro do disco e executa.
     */
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indica erro na saída do sistema se algo falhar.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    /**
     * [Cap. 4] Modo Prompt (REPL): Lê linha por linha do terminal (stdin).
     * Útil para testes rápidos e experimentação interativa.
     */
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break; // Ctrl+D encerra o loop.
            
            run(line);
            
            // [Cap. 4] Importante: Resetar o erro.
            // Se o usuário errar uma linha, não queremos matar a sessão inteira.
            hadError = false;
        }
    }

    /**
     * Núcleo do Interpretador.
     * Executa o código fonte passando por todas as fases da linguagem.
     * @param source O código fonte cru (String).
     */
    private static void run(String source) {
        // 1. Análise Léxica (Scanning) - [Cap. 4]
        // Transforma o texto bruto em uma lista de tokens.
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // 2. Análise Sintática (Parsing) - [Cap. 6]
        // Transforma a lista de tokens em uma Árvore de Sintaxe Abstrata (AST).
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Se houver erro de sintaxe, paramos aqui. Não tentamos analisar ou executar.
        if (hadError) return;

        // 3. Análise Semântica (Resolving) - [Cap. 11]
        // Passe estático que resolve os escopos das variáveis antes da execução.
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Se o Resolver encontrar erros (ex: return fora de função), paramos.
        if (hadError) return;

        // 4. Interpretação (Execution) - [Cap. 8]
        // Executa a AST percorrendo os nós e realizando as operações.
        interpreter.interpret(statements);
    }

    // --- Tratamento de Erros e Relatórios ---

    /**
     * [Cap. 4] Reporta erro de análise léxica (linha específica).
     */
    static void error(int line, String message) {
        report(line, "", message);
    }

    /**
     * [Cap. 6] Reporta erro de análise sintática (token específico).
     */
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    /**
     * [Cap. 7] Reporta erro de tempo de execução (RuntimeError).
     * Exibe a mensagem e a linha onde ocorreu o erro.
     */
    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
            "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    /**
     * Método auxiliar para formatar a mensagem de erro na saída de erro padrão (stderr).
     */
    private static void report(int line, String where, String message) {
        System.err.println(
            "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}