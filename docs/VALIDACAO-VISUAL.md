# Validação da revisão visual — 14/10/2026

## Aprovado

| Verificação | Resultado |
|---|---|
| Compilação de todos os fontes Java | JDK 21, `javac --release 21 -parameters`, dependências originais preservadas |
| Testes Java existentes | 71 encontrados, 71 aprovados, 0 falhas/ignorados, via JUnit Platform 1.12.2 |
| Navegador + API real (`tests/browser.cjs`) | 12 verificações aprovadas |
| DOM + HTTP real (`tests/dom.cjs`) | 11 cenários aprovados |
| Responsividade e novos controles (`tests/visual.cjs`) | 9 grupos aprovados; cinco páginas em 320, 390, 768 e 1440 px |
| Inspeção visual realizada | Capturas Chromium de desktop/celular revisadas; também tela de análise em tablet e entrada em 320 px |
| API e segurança existentes | Upload, hash, inventário, exportação, exclusão, rejeição de ZIP inválido/Zip Slip e conteúdo hostil como texto |
| Filtros | Busca e severidade combinados, nenhum resultado, restauração da lista e foco no detalhe |
| Teclado | Alternância de abas, Escape no menu, foco no detalhe; estilos de foco compartilhados |
| Falha da API | HTTP 503 injetado apenas no teste: alerta e botão Tentar novamente; dados da aplicação não são simulados |
| Console JavaScript | Nenhum erro de execução nos fluxos testados |
| Empacotamento | Recursos do JAR comparados byte a byte com `src/main/resources`; fontes, CSS, HTML e JS incluídos |
| Preservação do backend | Java, regras, assinaturas e application.properties comparados ao ZIP original, sem alterações |

As capturas foram usadas somente para inspeção interna; não são necessárias para
executar o produto. A aplicação testada usou o JAR com recursos internos, sem um
servidor frontend separado e sem substituir respostas da API nos fluxos reais.
O único mock HTTP foi o cenário explícito de indisponibilidade 503.

## Compilação e limitação do Maven

O ambiente fornecia apenas Java 17 e nenhum Maven. JDK 21 e Maven foram obtidos
para validação. A resolução direta de dependências do Maven falhou por DNS e
acesso ao proxy; uma tentativa intermediada avançou, mas o ciclo completo não foi
concluído. **Não se declara `mvn clean verify` aprovado nesta revisão.**

Para entregar um executável atualizado, todos os fontes Java foram recompilados
com `javac --release 21 -parameters`, usando exclusivamente as dependências do JAR
anexado. O lançador Spring Boot e os JARs de dependências foram preservados; as
classes da aplicação e todos os recursos foram substituídos pelos novos resultados.
`scripts/recompilar-offline.py` reproduz o procedimento com JDK 21 e Python 3.

Os 71 testes existentes foram compilados e executados pela JUnit Platform com
as versões correspondentes do Spring Boot Test, Spring Test e dependências de
asserções. Isso valida os testes Java, mas não substitui validação dos plugins e
de todo o ciclo de vida Maven. O `pom.xml` original está preservado para uso normal.

## Falhas encontradas e corrigidas

- Relatório longo encolhia como item flex e cortava conteúdo: altura automática
  no documento e `flex-shrink: 0` na superfície do relatório.
- Teste aguardava visibilidade de histórico vazio que é corretamente ocultado:
  espera ajustada para atributo de conclusão no DOM, sem mudar o produto.
- Empacotamento direto inicial não tinha entradas de diretórios exigidas pelo
  classloader Spring Boot: corrigido; executável final iniciou e passou nos fluxos.
- Download padrão de Chromium falhou; Chromium Headless disponível via pacote
  de ferramentas foi usado. Nenhuma dependência de navegador entrou no produto.

## Não executado

Windows/macOS nativos, Safari/Firefox, leitor de tela real, auditoria WCAG completa,
carga prolongada, auditoria externa de segurança e ciclo Maven completo.
A revisão de foco, labels, contraste e teclado não equivale a certificação WCAG.

## Reproduzir no computador do usuário

```sh
mvn clean verify
# Atualizar dist com o JAR de target para usar os inicializadores.
java -jar dist/goodware-backend-1.0.0.jar
# Outro terminal:
npm install
npm run test:dom
npx playwright install chromium
npm run test:browser
npm run test:visual
```

`GOODWARE_TEST_URL` altera a URL local dos testes. `GOODWARE_CHROME_PATH` permite
indicar um Chromium já instalado. `GOODWARE_SCREENSHOT_DIR` é opcional e precisa
apontar para uma pasta existente.
