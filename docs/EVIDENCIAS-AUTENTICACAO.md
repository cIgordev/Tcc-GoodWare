# Evidências desta entrega

## Java: 88/88 aprovados, zero ignorados
JDK Temurin 21.0.12.1, javac --release 21 -parameters; dependências de runtime
extraídas do JAR recebido, sem atualizar versões. JUnit Platform Console 1.12.2,
Spring Test 6.2.19/Boot Test 3.5.16 e H2 2.3.232; fontes de teste incluídos.
Maven não conseguiu conectar ao repositório nesta execução. A compilação completa
e os testes foram feitos diretamente com javac/JUnit; não alegamos build Maven.

AuthIsolationTest: acesso anônimo e identidade forjada; seis rotas de relatório
entre contas; busca por hash; histórico; exclusão; limpeza do contexto; sessão
expirada/token inválido; CSRF no login; origem externa; rotação de sessão; logout.
A API Supabase Auth é simulada nesses testes; não há credenciais reais.
Demais testes preservam scanner, uploads adversariais, persistência e falhas.

## PostgreSQL/WASM: 11/11 aprovados
`npm run test:rls` aplica exatamente V1–V5 a uma base temporária com duas contas
sintéticas locais. Verifica RLS real do PostgreSQL: proprietário, registros legados,
negação de troca de dono, filhos, exclusão, identidade ausente, reset entre
transações, cascata e bloqueio de anon/authenticated/service_role.
Não é conexão JDBC ao Supabase e não testa diferenças do ambiente hospedado.

## Frontend DOM: 9/9 aprovados
`npm run test:dom` usa jsdom e respostas simuladas. Quatro páginas carregam sem
erros de JS, scripts respeitam ordem, cadastro/login exibem retorno, requisições
incluem cabeçalho e sessão, botão de sair aparece. Não é teste visual.
Chromium indisponível; download retornou arquivo inválido. Inspeção visual em
navegador real permanece pendente, sem alegar aprovação de screenshots.

## JAR HTTP: 5/5 aprovados
JAR recompilado integralmente, iniciado com H2 sintético e migrações portáveis.
GET /login.html = 200; GET /api/analyses = 401; GET /api/analyses/history = 401;
POST /api/auth/login sem cabeçalho de proteção = 403; GET /api/health = 200.
Servidor de teste encerrado. Nenhum pacote de usuário executado.

## Supabase remoto
Projeto autorizado confirmado ACTIVE_HEALTHY. Migrações V3/V4/V5 aplicadas com
sucesso e histórico confirmado. Quatro tabelas com RLS ativa, FK owner_id presente,
políticas de proprietário conferidas, EXECUTE público da função removido.
Advisor de segurança: zero avisos. Uma análise legada preservada sem dono.
Auth settings: email habilitado, signup habilitado, confirmação obrigatória,
usuários anônimos desabilitados. Não criamos contas ou dados sintéticos remotos.
Validação comportamental como login JDBC restrito e login real pendentes.
