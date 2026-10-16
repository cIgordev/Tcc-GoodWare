# GoodWare com autenticação — entrega atual

Leia [LEIA-ME-PRIMEIRO.md](LEIA-ME-PRIMEIRO.md) e [guia atual](docs/AUTENTICACAO-E-IMPLANTACAO.md).
Login real e isolamento implementados; uso público ainda depende das validações operacionais registradas.

---
## Documentação da versão anterior (histórico)

# GoodWare — persistência PostgreSQL / Supabase

Aplicação Java 21 / Spring Boot 3.5.16 com frontend HTML, CSS e JavaScript original
integrado à API. O scanner trata ZIPs como dados e nunca executa seu conteúdo.

**Para começar:** leia [LEIA-ME-PRIMEIRO.md](LEIA-ME-PRIMEIRO.md).

## Estrutura
- `pom.xml`: projeto Maven e dependências gerenciadas pelo Spring Boot.
- `src/main/java/br/com/goodware/controller`: API, erros e proteção da origem local.
- `src/main/java/br/com/goodware/service`: orquestração, retenção e catálogo.
- `src/main/java/br/com/goodware/engine`: leitor ZIP, regras, dependências e evidências.
- `src/main/resources/static`: páginas, estilos, ícones SVG e animações recuperados.
- `src/main/resources/rules`: 18 regras divididas entre vulnerabilidades e indicadores.
- `src/main/resources/signatures/catalog.json`: catálogo local com procedência, inicialmente vazio.
- `src/test`: testes automatizados Java; `tests/`: verificações do frontend.
- `examples`: fixtures de texto para testar o fluxo; não são programas para executar.
- `dist`: JAR executável para Java 21, gerado desta versão dos fontes.
- `docs`: arquitetura, API, segurança, catálogo, testes e recuperação.

## Funcionalidades
Upload síncrono real, estado de envio/análise, painel sem dados fictícios, busca por
SHA-256, inventário com cobertura, evidências por arquivo/linha, severidade e
confiança, recomendações, exportação JSON, revisão por aba e exclusão do relatório.

O login simulado foi substituído por uma entrada explícita para o modo local.
Não existe conta de usuário, sessão autenticada ou OAuth. A persistência agora usa PostgreSQL via JDBC; configuração obrigatória em docs/CONFIGURAR-SUPABASE.md.

## Categorias
| Categoria | Significado |
|---|---|
| POTENTIAL_VULNERABILITY | Padrão que merece revisão; não comprova exploração |
| SUSPICIOUS_INDICATOR | Comportamento ou referência suspeita; não comprova malware |
| KNOWN_SIGNATURE | Correspondência exata com entrada documentada do catálogo |
| TEST_SIGNATURE | Correspondência com fixture explicitamente marcada como teste |
| DEPENDENCY_ADVISORY | Dependência declarada em faixa de aviso público suportado |
| COVERAGE_NOTICE | Parte da análise não pôde ser realizada |

## Executar e testar
```sh
mvn clean verify
mvn spring-boot:run
```
Acesse http://localhost:8080. Alternativamente, use `iniciar-windows.bat` ou
`./iniciar.sh` com o JAR de `dist`.

Testes opcionais da interface, com servidor iniciado:
```sh
npm install
npm run test:dom
npx playwright install chromium
npm run test:browser
```

## Limitações e transparência
Não é um antivírus, SAST completo, sandbox dinâmica nem uma auditoria completa de
CVE. Não faz AST/taint tracking, não resolve dependências transitivas e não identifica
malware desconhecido. O catálogo de ameaças **não contém assinaturas reais por
padrão**. Não há consulta externa durante o scan.

O histórico é persistente, com paginação e exclusão manual; não expira ao reiniciar. O serviço deve permanecer
em localhost e em usuário único. Requisitos para evolução estão em docs/SEGURANCA.md.
A verificação limitada de Log4j é baseada em https://logging.apache.org/security.html
(CVE-2021-44228, consulta de 14/10/2026); cobertura exata em docs/ARQUITETURA.md.

## Revisão visual de 14/10/2026

As cinco páginas usam tipografia e componentes compartilhados, fontes locais e
layouts responsivos. O relatório permite busca e filtro de severidade. Consulte
`docs/DESIGN.md` e `docs/VALIDACAO-VISUAL.md` para mudanças e testes desta entrega.

## Persistência implementada em 14/10/2026

- Spring JDBC + Hikari + driver PostgreSQL; transações e migrações Flyway V1/V2.
- Análises, arquivos, achados e snapshots de catálogo com procedência.
- Histórico paginado, estados de falha, recuperação de análises interrompidas.
- Schema privado e RLS restrita ao papel backend; credenciais externas e TLS verify-full.
- Design original mantido; textos de histórico atualizados, controles de paginação.

**Antes de executar:** siga `docs/CONFIGURAR-SUPABASE.md` e `.env.example`.
Não há fallback em memória. O JAR desta entrega foi gerado com Maven/Java 21.
Execute `mvn clean verify` para reconstruir; não use um JAR anterior com estes fontes.

**Situação remota:** V1/V2 aplicadas e verificadas no `tccgoodware` após autorização
explícita. Schema privado, quatro tabelas, RLS e grants confirmados. Teste sintético
remoto de integridade aprovado com rollback. Conexão Java/JDBC remota ainda pendente.
Mantenha GOODWARE_MIGRATE=false: as tabelas já existem. Veja guia de configuração.

## Testes da integração
```sh
mvn clean verify              # Java, API, scanner e JDBC com H2 isolado
npm ci
npm run test:postgres         # PostgreSQL/WASM isolado: migrações, RLS, integridade
python3 tests/jar-smoke.py     # JAR + JDBC PostgreSQL local, HTTP, reinício, indisponibilidade
```
O último comando exige JAR atualizado em dist/, Node, Python 3 e Java 21 em PATH
ou JAVA_HOME; usa portas locais 18080/55432 e dados sintéticos temporários.
Não se conecta ao Supabase. `docs/TESTES-PERSISTENCIA.md` distingue o que foi
executado localmente do que permanece pendente no serviço remoto.

`docs/MODELAGEM.md`: tabelas, estados, catálogo e limites de autorização.
Documentos de entregas anteriores em docs/ são históricos; para persistência,
prevalecem os três guias acima e PROGRESSO_GOODWARE.md.
