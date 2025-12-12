package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Classe principal do Interpretador Lox.
 * Responsável por orquestrar as fases de análise e execução.
 *
 * Referência: Crafting Interpreters - Capítulo 4 (Scanning).
 */
public class Lox {

    // [Cap. 4.1] Sinalizador de erro de sintaxe.
    // Se for true, não tentamos executar o código.
    static boolean hadError = false;

    // [Cap. 7] Sinalizador de erro de execução (Runtime).
    // Se for true, o processo termina com código de erro específico (70).
    static boolean hadRuntimeError = false;

    // [Cap. 8] Instância do Interpretador.
    // É estática para que as variáveis globais persistam enquanto o REPL (prompt) estiver aberto.
    private static final Interpreter interpreter = new Interpreter();

    /**
     * Ponto de entrada da aplicação Java.
     * @param args Caminho do arquivo de script (opcional).
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
     * [Cap. 4] Modo Arquivo: Lê o arquivo inteiro e executa.
     */
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indica erro na saída do sistema se algo falhar.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    /**
     * [Cap. 4] Modo Prompt (REPL): Lê linha por linha do terminal.
     * Útil para testes rápidos e experimentação.
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
     * Núcleo do Interpretador: Scanning -> Parsing -> Resolving -> Interpreting.
     * @param source O código fonte cru.
     */
    private static void run(String source) {
        // 1. Análise Léxica (Scanning) - [Cap. 4]
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // 2. Análise Sintática (Parsing) - [Cap. 6]
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Se houve erro de sintaxe, paramos aqui. Não tentamos interpretar.
        if (hadError) return;

        // 3. Análise Semântica (Resolving) - [Cap. 11 NOVO]
        // Executa o Resolver para calcular os escopos das variáveis antes de rodar.
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Se o Resolver encontrar erros (ex: return fora de função), paramos.
        if (hadError) return;

        // 4. Interpretação (Execution) - [Cap. 8]
        // Executa a lista de declarações gerada pelo Parser.
        interpreter.interpret(statements);
    }

    // --- Tratamento de Erros ---

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
     * [Cap. 7] Reporta erro de tempo de execução (lógica inválida).
     */
    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
            "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    /**
     * Método auxiliar para formatar a mensagem de erro no stderr.
     */
    private static void report(int line, String where, String message) {
        System.err.println(
            "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}