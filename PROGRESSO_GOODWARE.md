# Progresso GoodWare — autenticação e isolamento

Atualizado em 14/10/2026 (America/Sao_Paulo), 15/10 UTC.
Base: GoodWare-Supabase(2).zip. O outro anexo contém frontend anterior; não substituiu a base integrada.

## Concluído nesta entrega
- Login/cadastro Supabase Auth, sessão HttpOnly/SameSite=Strict, logout e expiração.
- Validação server-side de identidade; tokens não acessíveis pelo frontend.
- Autorização por dono em histórico, hash, status, resumo, relatórios, filhos e exclusão.
- Contexto SQL transacional, RLS por proprietário e FK auth.users, índices por dono.
- Proteção de origem/CSRF, limite conservador de login, bloqueio de credencial JDBC privilegiada.
- Frontend integrado ao login real; sessão expirada redireciona; botão sair.
- Migrações incrementais V3–V5 aplicadas somente a tvnvjfaxuxwndlsnpiar.
- Avisos preexistentes de EXECUTE em public.rls_auto_enable corrigidos sem alterar o trigger.
- Configurações HTTPS/proxy, Dockerfile, documentação e JAR recompilado.
- 88 testes Java, 11 testes PostgreSQL/WASM, 9 verificações DOM e 5 verificações HTTP aprovados.

## Estado remoto
V3 20261015010759; V4 20261015010805; V5 20261015010810.
V1/V2 anteriores mantidas. Advisor segurança vazio; quatro tabelas com RLS.
Uma análise antiga com owner_id NULL preservada e invisível a novas contas.
Nenhuma conta/dado real excluído. Nenhuma credencial rotacionada. Nada publicado.
Mantenha GOODWARE_MIGRATE=false. JAR antigo incompatível com novas políticas.

## Pendências reais e próxima ação
1. Usuário instala este pacote mantendo suas variáveis JDBC/certificado locais.
2. Cria/confirma uma conta e valida login, upload e persistência. Não precisamos
   receber sua senha. A conta deve ser criada no formulário/painel, não no chat.
3. Testa isolamento com segunda conta. A conexão JDBC remota não foi validada aqui;
   não há credencial disponível e a integração não pode SET ROLE goodware_backend.
4. SMTP de produção, recuperação de senha, cotas/retencão/backup e proxy precisam
   de preparação operacional antes de disponibilizar ao público.
5. Teste visual real pendente: Chromium não estava instalado e download falhou.
6. Definir com evidência o dono da análise legada antes de eventual atribuição.
7. Publicação gratuita no Render autorizada posteriormente; domínio próprio e contratação paga continuam fora do escopo.

## Continuação da implantação — 16/10/2026 UTC
- Autorização explícita recebida para publicar o código atual no repositório público cIgordev/Tcc-GoodWare e implantar no Render gratuito. Bloqueio anterior de aprovação resolvido.
- Código atual publicado em main no commit 4909b9dd4fd81904ce31901ca3923be98de199a7; 133 arquivos. O commit inicial foi preservado como pai.
- Workspace My workspace confirmado. Serviço goodware criado no plano free, região Ohio, ID srv-db8mpvrbc2fs73bebe2g.
- URL reservada: https://goodware-fstf.onrender.com — funcionamento público ainda não validado.
- Painel: https://dashboard.render.com/web/srv-db8mpvrbc2fs73bebe2g
- Docker compila fonte Java 21 e executa testes. Primeiro deploy dep-db8mq0jbc2fs73bebho0: Maven BUILD SUCCESS, 88 testes, zero falhas/erros/ignorados. Nove verificações DOM/autenticação também passaram localmente.
- Perfil render usa PORT, RENDER_EXTERNAL_URL, cookie Secure e processamento nativo de proxy; TLS JDBC permanece verify-full. Migrações automáticas desabilitadas.
- Consulta remota confirmou goodware_app com LOGIN, sem SUPERUSER/BYPASSRLS e membro de goodware_backend. Nenhuma credencial lida ou alterada.
- BLOQUEIO REAL: configurar GOODWARE_DB_PASSWORD no painel Render e adicionar Secret File prod-ca-2021.crt. Não enviar senha pelo chat. Valores restantes já configurados.
- Após configurar: verificar inicialização, HTTPS, login, upload, persistência, isolamento entre contas e URLs de confirmação do Supabase. A aprovação dos testes de build não substitui essa validação remota.
- Guia operacional: docs/RENDER.md. Nenhum serviço pago, novo banco ou domínio contratado.

## Limites documentados
Sessões em memória, expiração máxima 30 minutos sem refresh automático; uma
instância/scan simultâneo. Sem fila durável, reset de senha na UI ou catálogo
real de ameaças. Scanner heurístico não garante ausência de vulnerabilidades.
Recuperação de análises interrompidas agora ocorre por dono ao abrir o histórico.
Maven indisponível por rede; javac/JUnit e recompilador offline usados com sucesso.

Guia atual: docs/AUTENTICACAO-E-IMPLANTACAO.md.
Evidências atuais: docs/EVIDENCIAS-AUTENTICACAO.md.
Documentos anteriores devem ser lidos como histórico quando divergirem deste guia.

## Verificação de 16/10/2026 01:09 UTC
- Nova implantação dep-db8nhqui0phs738bdrug iniciada após usuário salvar certificado.
- O certificado passou da etapa anterior: a conexão alcançou a autenticação PostgreSQL.
- Bloqueio atual confirmado: password authentication failed for user goodware_app. Corrigir GOODWARE_DB_PASSWORD diretamente no Render, com a senha desse usuário. Nenhuma senha lida ou alterada pelo assistente.
- Site ainda não validado como operacional.

## Credencial corrigida — 16/10/2026 01:13 UTC
- Usuário corrigiu a credencial diretamente no Render. Deploy dep-db8njgnf3r2c73eo4i80 confirmado Live.
- Nova instância 7stln registrou conexão PostgreSQL adicionada e Hikari Start completed, às 01:12:43 UTC. Bloqueio de senha anterior resolvido.
- URL pública: https://goodware-fstf.onrender.com. Login/upload e isolamento remoto entre duas contas ainda requerem validação.
- Verificação HTTP pública: página inicial, login e health retornaram 200; análises sem sessão retornaram 401; origem externa retornou 403.

## Retomada do Render — 16/10/2026 11:03 UTC
- Implantacao permanece Live no plano free. Nova instancia q6dvp iniciou e conectou ao Supabase com sucesso.
- Primeiras requisicoes expiraram durante retomada apos inatividade; banco reconectado as 11:02:34 UTC. Origem externa rejeitada com 403.
- Uso publico independe de PowerShell e do computador local: https://goodware-fstf.onrender.com.
- Cadastro/login/upload autenticados e isolamento remoto entre duas contas ainda pendentes de validacao.
