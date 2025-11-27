package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    // 4.1.1: Sinalizador de erro estático
    static boolean hadError = false;
    static boolean hadRuntimeError = false; // NOVO: Sinalizador para erros de execução

    // 5.1.4: O interpretador deve ser uma instância persistente
    // Nota: Criamos a instância aqui para ser usada na classe Lox.
    private static final Interpreter interpreter = new Interpreter(); // NOVO: Instância do Interpretador
    
    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Uso: jlox [script]");
            System.out.println(64); // EX_USAGE
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        
        if (hadError) System.exit(65); // Erro de sintaxe/análise
        if (hadRuntimeError) System.exit(70); // NOVO: Código de saída para erro de execução
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false; // Reseta o erro no REPL
            // O hadRuntimeError NÃO é resetado aqui porque o método 'run' chama 'interpret'
            // que já trata e reporta o erro, mas em modo REPL, não é estritamente necessário resetar a flag 
            // após cada linha, pois ela só é checada ao sair do programa.
        }
    }

    private static void run(String source) {
        // 4.4: Inicia o Scanner
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        
        // NOVO: Inicia o Parser
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();

        // Se houve erro de sintaxe, para.
        if (hadError) return;
        
        // NOVO: Executa o Interpretador
        interpreter.interpret(expression); // O interpretador fará a chamada final para 'System.out.println'
    }

    // 4.1.1: Funções de Tratamento de Erro (para erros de scanning/parsing)
    public static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println(
            "[linha " + line + "] Erro" + where + ": " + message);
        hadError = true;
    }
    
    // NOVO: Manipulador de erros de execução (chamado pelo Interpreter)
    public static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
            "\n[linha " + error.token.line + "]");
        hadRuntimeError = true;
    }
}