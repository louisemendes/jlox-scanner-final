# Interpretador Lox – Relatório Técnico Completo  
Construção passo a passo com base no livro *Crafting Interpreters* (Capítulos 4–12)

---

## 1. Integrantes da Equipe

<table>
  <tr>
    <th>Nome Completo</th>
    <th>Usuário GitHub</th>
  </tr>
  <tr>
    <td><strong>Raianny Cristina Ferreira da Silva</strong></td>
    <td><a href="https://github.com/raianny-cristina">raianny-cristina</a></td>
  </tr>
  <tr>
    <td><strong>Louise Reis Mendes</strong></td>
    <td><a href="https://github.com/louisemendes">louisemendes</a></td>
  </tr>
</table>

---

## 2. Introdução

Este repositório documenta o desenvolvimento completo de um **interpretador para a linguagem Lox**, conforme apresentado no livro *Crafting Interpreters*, de Robert Nystrom.

A implementação segue fielmente a arquitetura descrita na obra, cobrindo todas as etapas fundamentais de um interpretador moderno: análise léxica e sintática, geração da AST (Árvore Sintática Abstrata), resolução estática de variáveis e execução.

Os capítulos principais desta entrega são:

<table>
  <tr>
    <th>Capítulo</th>
    <th>Tema</th>
  </tr>
  <tr>
    <td>10</td>
    <td>Functions</td>
  </tr>
  <tr>
    <td>11</td>
    <td>Resolving and Binding</td>
  </tr>
  <tr>
    <td>12</td>
    <td>Classes</td>
  </tr>
</table>

---

## 3. Objetivos Gerais

<table>
  <tr>
    <th>#</th>
    <th>Objetivo</th>
  </tr>
  <tr>
    <td>1</td>
    <td>Implementar um interpretador funcional completo para a linguagem Lox.</td>
  </tr>
  <tr>
    <td>2</td>
    <td>Compreender as fases internas de um interpretador (scanner → parser → resolver → runtime).</td>
  </tr>
  <tr>
    <td>3</td>
    <td>Integrar escopo léxico, funções, closures, classes e objetos.</td>
  </tr>
  <tr>
    <td>4</td>
    <td>Consolidar aprendizado através de um projeto incremental e modular.</td>
  </tr>
</table>

---

## 4. Arquitetura Geral do Interpretador

O interpretador é modular e organizado conforme as fases clássicas de construção de linguagens:

<table>
  <tr>
    <th>Módulo</th>
    <th>Papel</th>
    <th>Responsabilidade</th>
  </tr>
  <tr>
    <td><strong>Scanner</strong></td>
    <td>Análise Léxica</td>
    <td>Converte caracteres em tokens.</td>
  </tr>
  <tr>
    <td><strong>Parser</strong></td>
    <td>Análise Sintática</td>
    <td>Constrói a AST do programa.</td>
  </tr>
  <tr>
    <td><strong>Expr/Stmt (AST)</strong></td>
    <td>Representação Intermediária</td>
    <td>Modela a estrutura sintática do programa.</td>
  </tr>
  <tr>
    <td><strong>Resolver</strong></td>
    <td>Análise Semântica</td>
    <td>Resolve escopos, profundidade e validações semânticas.</td>
  </tr>
  <tr>
    <td><strong>Interpreter</strong></td>
    <td>Execução</td>
    <td>Avalia expressões e executa instruções.</td>
  </tr>
  <tr>
    <td><strong>Environment</strong></td>
    <td>Tabela de Símbolos</td>
    <td>Gerencia variáveis, escopos e closures.</td>
  </tr>
  <tr>
    <td><strong>LoxClass / LoxInstance / LoxFunction</strong></td>
    <td>Runtime (POO)</td>
    <td>Implementam classes, métodos e instâncias.</td>
  </tr>
  <tr>
    <td><strong>GenerateAst</strong></td>
    <td>Metaprogramação</td>
    <td>Gera as classes da AST automaticamente.</td>
  </tr>
  <tr>
    <td><strong>Lox</strong></td>
    <td>Interface</td>
    <td>Modo REPL, leitura de arquivos e tratamento de erros.</td>
  </tr>
</table>

---

## 5. Desenvolvimento por Capítulos

### 5.1 Capítulo 4 — Lox Core  
- Classe principal `Lox`  
- REPL  
- Execução de arquivos  
- Tratamento de erros  

---

### 5.2 Capítulo 5 — Scanner  

O scanner reconhece:

<table>
  <tr>
    <th>Categoria</th>
    <th>Exemplos</th>
  </tr>
  <tr>
    <td>Números</td>
    <td>123, 3.14</td>
  </tr>
  <tr>
    <td>Strings</td>
    <td>"texto"</td>
  </tr>
  <tr>
    <td>Identificadores</td>
    <td>nome, contador</td>
  </tr>
  <tr>
    <td>Palavras-chave</td>
    <td>var, fun, class, if, else</td>
  </tr>
  <tr>
    <td>Operadores</td>
    <td>==, !=, <=, >=, +, -, *, /</td>
  </tr>
</table>

---

### 5.3 Capítulos 6–7 — Parser e Precedência  
Implementações:

- binários e unários  
- agrupamento  
- chamadas de função  
- AST gerada automaticamente com `GenerateAst`  

---

### 5.4 Capítulo 8 — Variáveis e Statements  
- `var`  
- `print`  
- blocos `{}`  
- escopo léxico (Environment)  

---

### 5.5 Capítulo 9 — Controle de Fluxo  
- `if / else`  
- `while`  
- transformação de `for` → `while`  
- `and`, `or`  

---

### 5.6 Capítulo 10 — Funções

<table>
  <tr>
    <th>Recurso</th>
    <th>Descrição</th>
  </tr>
  <tr>
    <td>Declaração</td>
    <td><code>fun nome(params) { ... }</code></td>
  </tr>
  <tr>
    <td>Retorno</td>
    <td><code>return valor;</code></td>
  </tr>
  <tr>
    <td>Closures</td>
    <td>Funções capturam o ambiente externo.</td>
  </tr>
  <tr>
    <td>LoxFunction</td>
    <td>Implementa a lógica de chamadas e binding.</td>
  </tr>
</table>

---

### 5.7 Capítulo 11 — Resolver (Escopo Estático)

<table>
  <tr>
    <th>Validação</th>
    <th>Descrição</th>
  </tr>
  <tr>
    <td>Variáveis não inicializadas</td>
    <td>Erro ao usar variável antes de definir.</td>
  </tr>
  <tr>
    <td><code>return</code> inválido</td>
    <td>Não permitido fora de funções.</td>
  </tr>
  <tr>
    <td><code>this</code> fora de classe</td>
    <td>Detecta uso ilegal.</td>
  </tr>
  <tr>
    <td>Profundidade de escopo</td>
    <td>O resolver informa ao interpretador quantos escopos pular.</td>
  </tr>
</table>

---

### 5.8 Capítulo 12 — Classes, Propriedades e Métodos

<table>
  <tr>
    <th>Recurso</th>
    <th>Descrição</th>
  </tr>
  <tr>
    <td>Classes</td>
    <td><code>class Pessoa { ... }</code></td>
  </tr>
  <tr>
    <td>Inicializador</td>
    <td>Método <code>init</code> funciona como construtor.</td>
  </tr>
  <tr>
    <td>Métodos</td>
    <td>São funções com binding automático ao <code>this</code>.</td>
  </tr>
  <tr>
    <td>Instâncias</td>
    <td>Guardam campos dinâmicos.</td>
  </tr>
  <tr>
    <td>Acesso a propriedades</td>
    <td><code>obj.prop</code> e <code>obj.prop = valor</code></td>
  </tr>
</table>

---

## 6. Execução

### 6.1 Compilação

```bash
mvn clean compile
