> Documento histórico da versão anterior. Para persistência, estado atual e testes desta entrega, consulte MODELAGEM.md, CONFIGURAR-SUPABASE.md e TESTES-PERSISTENCIA.md.

# Relatório de testes — 14/10/2026

## Resultado final aprovado
**71 testes Java aprovados; zero falhas, zero erros e zero ignorados.**
**11 cenários de DOM + HTTP real aprovados.**

Ambiente: Linux, Amazon Corretto OpenJDK 21.0.12.1, Maven 3.9.9,
Spring Boot 3.5.16. Build final: `mvn clean verify`, com JAR executável gerado.
O ambiente começou com Java 17 e sem Maven; as ferramentas necessárias foram
preparadas fora do projeto. Não foram incluídas no ZIP da entrega.

| Suíte Java | Casos | Resultado |
|---|---:|---|
| SafeZipReaderTest | 24 | aprovados |
| SignatureServiceTest | 6 | aprovados |
| RuleModuleTest | 12 | aprovados |
| DependencyModuleTest | 17 | aprovados |
| AnalysisApiTest | 11 | aprovados |
| GoodwareBackendApplicationTests | 1 | aprovado |
| Total | 71 | aprovado |

Cobertura exercitada: ZIP STORED/DEFLATE, nomes UTF-8, diretórios, ZIP vazio/texto,
truncamento, CRC, caminhos absolutos/relativos perigosos, barras ambíguas, links,
criptografia, colisão de caixa, profundidade, quantidade, compressão excessiva,
tamanho declarado, timeout, split ZIP, offsets inválidos, hashes exatos de fixtures,
limites de host/URL, categorias de teste, comentários, SQL parametrizado, credenciais
redigidas, placeholders, IDs por arquivo, eval/ofuscação, desserialização, TLS,
chave privada, limite de achados, RegExp.exec legítimo, faixas de Log4j, XML com DTD,
XXE, XML malformado, upload, consulta, exportação, inventário, exclusão, headers,
origem/Host, cobertura parcial, retenção e contexto Spring.

## Frontend + servidor HTTP real
`tests/dom.cjs` executado com jsdom 26.1.0 e o JAR final iniciado em localhost.
Os scripts reais das páginas fazem fetch para a API real. O teste não usa respostas
simuladas. Confirmados:
1. Landing carrega scripts e navegação.
2. Entrada local não pede senha nem finge autenticação.
3. Upload da fixture → 5 achados / 4 arquivos → JSON → revisão por aba; segredo mascarado.
4. Busca real por hash.
5. Painel alimentado pela API.
6. Arquivo seguro com zero achados e ressalva.
7. ZIP inválido com erro real.
8. Zip Slip rejeitado na interface.
9. Nomes e evidências hostis inseridos como texto, sem elemento img criado.
10. Relatório excluído aparece indisponível.
11. Nenhum erro JavaScript no DOM.

Além disso: `node --check` nos sete scripts do frontend e validação dos caminhos
locais referenciados por HTML. Esses testes não comprovam renderização visual.

## Falhas encontradas durante o desenvolvimento e resolvidas
- Maven inicialmente sem proxy/certificado do ambiente: dependências baixadas após
  configuração local, sem desabilitar verificação TLS.
- Mockito inline não conseguiu autoanexar agente no ambiente: configurado
  `mock-maker-subclass` para a suíte, que não depende de mocking de classes finais.
- Primeira versão do executor de DOM acumulava conexões HTTP keep-alive e excedia
  o limite do servidor: executor corrigido para fechar conexões de teste.
- Detector JS confundia `pattern.exec()` de RegExp com execução de shell: regra
  ajustada e teste de regressão adicionado.

**Não restaram testes Java/DOM reprovados no estado final.**

## Não executados / bloqueados
- **Navegador Chromium e revisão visual desktop/mobile:** o browser foi baixado,
  mas o ambiente negou o socket de processo (`Operation not permitted`). Não foi
  possível executar as asserções de Playwright nem produzir inspeção visual.
  O teste `tests/browser.cjs` está pronto para execução externa; não é contado como aprovado.
- Execução nativa dos inicializadores no Windows e em macOS.
- Teste de carga prolongado, fuzzing abrangente e auditoria externa de segurança.
- Varredura completa das dependências do próprio GoodWare por scanner SCA remoto.
- Inteligência de ameaças real: catálogo entregue vazio; testes usam fixtures inofensivas.

## Reproduzir
```sh
mvn clean verify
mvn spring-boot:run
# Em outro terminal, na mesma pasta:
npm install
npm run test:dom
npx playwright install chromium
npm run test:browser
```
DOM não testa CSS, renderização ou garantias de sandbox de um navegador. Os testes
de XSS confirmam uso de nós de texto neste fluxo, não uma auditoria geral contra XSS.
Os casos de falso positivo documentam limites: strings contendo código de exemplo
podem disparar regex, e não há análise de fluxo ou confirmação de exploração.

O relatório `examples/relatorio-exemplo.json` foi produzido pelo servidor final a
partir de `examples/projeto-vulneravel.zip`. Não é usado como resposta pela interface.
IDs e horários variam a cada execução.
