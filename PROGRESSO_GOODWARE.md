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
- Integrações Render e GitHub disponíveis. Repositório cIgordev/Tcc-GoodWare verificado: main contém somente README.md; código da aplicação ainda não enviado.
- Workspace Render My workspace confirmado explicitamente pelo usuário. Consulta inicial não retornou serviços existentes.
- Dockerfile atualizado para compilar o fonte Java 21 e executar testes Maven; perfil render usa PORT, origem pública automática e cookie Secure. Histórico Git anterior será preservado.
- Próximas etapas: confirmar workspace, preparar build Docker a partir do fonte e porta PORT, enviar código sem segredos, criar serviço free e configurar origem HTTPS.
- Senha JDBC e certificado TLS precisam ser configurados com segurança no ambiente de hospedagem; não solicitar senha no chat. Implantação e validação remota ainda não realizadas.

## Limites documentados
Sessões em memória, expiração máxima 30 minutos sem refresh automático; uma
instância/scan simultâneo. Sem fila durável, reset de senha na UI ou catálogo
real de ameaças. Scanner heurístico não garante ausência de vulnerabilidades.
Recuperação de análises interrompidas agora ocorre por dono ao abrir o histórico.
Maven indisponível por rede; javac/JUnit e recompilador offline usados com sucesso.

Guia atual: docs/AUTENTICACAO-E-IMPLANTACAO.md.
Evidências atuais: docs/EVIDENCIAS-AUTENTICACAO.md.
Documentos anteriores devem ser lidos como histórico quando divergirem deste guia.
