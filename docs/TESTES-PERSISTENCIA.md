# Testes da persistência — 14/10/2026

## Escopo
Validação local com Java 21, Spring Boot 3.5.16 e Maven 3.9.9. Dados sintéticos.
H2 em modo PostgreSQL para integração Java; PostgreSQL/WASM PGlite 0.5.10
(PostgreSQL 18.6) para SQL/RLS e JDBC local. PGlite não substitui validação no
Supabase PostgreSQL 17 nem teste de carga/conexões concorrentes nativas.

## Aprovados
- `mvn verify`: 82 testes, zero falhas, zero erros, zero ignorados. Inclui os testes
  originais de ZIP bomb, Zip Slip, corrupção, arquivos seguros/vulneráveis,
  regras, assinaturas, API, origem e headers de segurança.
- Persistência/recuperação por nova instância de repositório; arquivos/achados e
  estados/datas; catálogo com procedência, idempotência e versão imutável.
- Rollback de arquivos quando achado viola FK, exclusão em cascata, paginação,
  mais de 30 análises preservadas, validação de limites de página, máscara de segredo.
- ZIP inválido gera FAILED sem filhos; RUNNING não pode ser excluído; recuperação
  de RUNNING interrompido preserva análises recentes.
- Falhas de datasource/transação retornam mensagem sanitizada sem sucesso fictício.
- `npm run test:postgres`: 10 verificações no PostgreSQL/WASM isolado. V1 e V2
  executadas; RLS nas quatro tabelas; anon/authenticated/service_role bloqueados;
  backend permitido para DML e proibido para DROP; FK entre análise/arquivo;
  path traversal bloqueado; rollback; dados preservados ao fechar/reabrir banco;
  exclusão em cascata.
- JAR construído pelo Maven e inicializado. `python3 tests/jar-smoke.py` valida
  JDBC PostgreSQL local: upload HTTP de ZIP seguro e vulnerável, arquivo inválido,
  inventário, paginação, leitura dos mesmos IDs após reiniciar o backend,
  exclusão HTTP e resposta 503 durante queda real do banco.

## Falhas encontradas e corrigidas
- Queda de conexão ao abrir transação retornava 500 em vez de 503: handler passou
  a tratar TransactionException, com teste de regressão e repetição do fluxo real.
- Preparação do ambiente: Maven inicialmente sem proxy/CA; resolvido com a
  configuração de rede e truststore do ambiente, sem desativar validação TLS.
- PGlite socket usa sessão multiplexada: statements nomeados sobreviviam ao
  restart do cliente. O teste local usa prepareThreshold=0 e pool de uma conexão.
  O runtime Supabase mantém configuração normal, com conexão direta/session pooler.

## Não executados / pendentes
- Atualização remota após autorização: V1/V2 aplicadas; testes SQL sintéticos de
  integridade aprovados como administrador, com rollback. Ver VALIDACAO-REMOTA.md.
- Spring/JDBC → Supabase, certificado TLS remoto, login de privilégio mínimo e
  RLS efetiva no serviço remoto: sem credenciais JDBC; schema já aplicado.
- Flyway sobre PostgreSQL nativo/Supabase: não executado. Flyway passou com H2;
  V1/V2 passaram diretamente no PostgreSQL/WASM. Tentativa Flyway no socket
  PGlite encontrou limitação de múltiplas sessões/bloqueio; no smoke, SQL é
  aplicado pelo próprio PGlite antes do JAR. Não afirmar que esse teste validou
  Flyway no Supabase. Flyway 11.7.2 gerenciado pelo Boot suporta PostgreSQL 17;
  PGlite anuncia 18.6 e produz aviso de versão não testada.
- Autorização entre usuários: não implementada porque aplicação não possui contas.
  Não é sistema público multiusuário. Testes de origem não equivalem a autenticação.
- Navegadores reais, Windows e stress concorrente: não reexecutados nesta entrega.
  Testes/relatórios de design anteriores são históricos, não novas evidências.

## Reproduzir
```sh
mvn clean verify
npm ci
npm run test:postgres
# copie target/goodware-backend-1.0.0.jar para dist/ antes do teste seguinte
python3 tests/jar-smoke.py
```
O smoke usa localhost, portas 55432/18080, TLS desativado SOMENTE no teste local,
fixture temporária e encerra os processos iniciados. Não passe credenciais reais.
Evidências em `docs/EVIDENCIAS-PERSISTENCIA.txt`; detalhes dos testes no código.
