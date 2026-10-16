# Autenticação, isolamento e preparação operacional

Atualização: 14/10/2026 (Brasil), migrações de 15/10/2026 UTC.

## Arquitetura implementada

Frontend HTML/CSS/JS e Java 21/Spring Boot existentes preservados. Login/cadastro
usam a API oficial Supabase Auth do projeto tvnvjfaxuxwndlsnpiar. Email está
habilitado, cadastro aberto, confirmação de email obrigatória e usuários anônimos
desabilitados, conforme endpoint público de configurações consultado nesta entrega.
Não alteramos essas configurações nem criamos contas remotas.

O navegador recebe somente um cookie de sessão HttpOnly/SameSite=Strict. Tokens
Supabase ficam na memória do servidor; não são enviados ao JS nem ao localStorage.
Sessão com expiração absoluta de até 30 minutos e limite pela validade do token.
Não há renovação automática: entre novamente após expirar. Reinício encerra sessões.
Login recria a sessão; logout invalida a sessão local e solicita logout no Supabase.
Falha remota no logout não restaura a sessão local já invalidada.

Em cada chamada protegida, o servidor consulta `/auth/v1/user` com o token e usa
somente o UUID retornado. Não confia em user_metadata, cabeçalhos de usuário,
parâmetros owner_id ou IDs enviados pelo frontend para decidir propriedade.
Falha do provedor resulta em erro fechado; não existe modo sem autenticação.

Toda escrita HTTP exige X-GoodWare-Request: 1 e mesma origem permitida. Não há
CORS amplo. Formulários externos não conseguem acrescentar esse cabeçalho sem
preflight autorizado. Respostas têm no-store, CSP e proteções de conteúdo/frame.
Em HTTPS há HSTS. Login/cadastro possuem teto global de 30 tentativas/minuto por
instância; esse limite conservador pode afetar usuários legítimos sob abuso.

## Banco

Schema privado goodware continua inacessível a anon/authenticated/service_role.
O Java conecta com login restrito, membro de goodware_backend, sem BYPASSRLS,
sem superuser e sem propriedade de tabelas. Inicialização recusa credenciais
que contornem as políticas. Não use postgres no runtime.

V3 acrescenta owner_id nullable e índices compostos por proprietário.
V4 liga owner_id a auth.users e restringe políticas RLS de análises e filhos.
A identidade é aplicada com set_config(..., true) dentro de cada transação JDBC,
na conexão da transação; não permanece na conexão devolvida ao pool. Consultas
Java também usam owner_id explicitamente. Políticas de escrita impedem mudar o
dono ou anexar filhos a análise alheia. Respostas para recursos alheios são 404.

A identidade SQL é fornecida pelo backend confiável, não pelo cliente PostgREST.
Quem obtiver a senha JDBC pode escolher esse contexto; portanto ela é um segredo
de servidor. RLS não substitui proteção da credencial ou correção do backend.

Registros legados NULL permanecem intactos e invisíveis às contas. Foi encontrado
um registro nessa situação. Atribuição futura exige identificar o dono verdadeiro;
não há ferramenta pública para reivindicar análises. FK usa NO ACTION ao excluir
contas: exclusão definitiva precisa de fluxo administrativo de retenção antes.

Recuperação de RUNNING expirado (>10 min) ocorre ao abrir histórico/listagem do
próprio usuário. Removido o recuperador global sem identidade. Não há worker privilegiado.

V5 revoga EXECUTE público de public.rls_auto_enable(), mantendo seu corpo, dono e
trigger de ativação automática de RLS. Advisor de segurança após aplicação: vazio.

## Migrações remotas aplicadas

| Arquivo Flyway | Versão Supabase |
|---|---|
| V1 persistent_analyses | 20261015001417 |
| V2 private_backend_access | 20261015001427 |
| V3 analysis_owner | 20261015010759 |
| V4 owner_rls | 20261015010805 |
| V5 restrict_event_trigger_execute | 20261015010810 |

Mantenha GOODWARE_MIGRATE=false no projeto existente. O histórico Supabase e o
Flyway são separados. Orientações antigas de baseline na versão 2 estão superadas:
um administrador deve verificar as cinco migrações antes de eventual baseline 5.
Não repetir V1–V5 nem ativar baseline automático. Nova base isolada usa V1–V5 na
ordem, com auth.users existente (Supabase). Não foi criada outra base remota.

## Validação e limites

Veja EVIDENCIAS-AUTENTICACAO.md. Foram executados testes de unidade/integração
com provedor Auth simulado, PostgreSQL/WASM isolado e smoke HTTP do JAR com H2.
Não equivalem a um login real + JDBC remoto. A integração administrativa remota
não pode SET ROLE goodware_backend; seus privilégios não foram ampliados para testar.
Não houve acesso a senhas JDBC ou contas reais, nem remoção de dados reais.

O scanner continua heurístico, síncrono, com limite de um scan simultâneo por
instância, limites de ZIP e 30 segundos de processamento. Não há fila durável,
cotas persistidas por conta, SCA abrangente ou catálogo real de malware preenchido.
Nenhum achado não comprova segurança. Código enviado jamais é executado.

## Preparação para implantação (não publicada)

Incluídos Dockerfile com usuário não-root e perfil application-public.properties.
Build da imagem e execução em provedor NÃO foram realizados. Antes de usar:

1. Validar duas contas confirmadas: upload A; histórico B vazio; acesso B por ID/hash
   negado; excluir A; reiniciar e conferir persistência de outra análise própria.
2. Confirmar conexão JDBC restrita com TLS verify-full e certificado correto.
3. Configurar SMTP próprio no painel Supabase, remetente, templates e limites;
   validar cadastro/confirmação e definir fluxo de recuperação de senha (ainda não há UI).
4. Definir HTTPS, domínio e proxy confiável. Publicação/domínio/custos exigem nova
   autorização. Não usamos nenhum serviço pago nesta entrega.
5. No perfil public, configurar GOODWARE_PUBLIC_ORIGIN como origem HTTPS exata,
   GOODWARE_TRUSTED_PROXY_REGEX para IPs reais do proxy e segredos fora da imagem.
   Cookie Secure é obrigatório nesse perfil. Bloquear acesso direto à porta do Java.
   Somente o proxy confiável pode fornecer headers encaminhados. Não usar regex ampla.
6. Limitar uploads no proxy, armazenamento temporário, tempo/conexões e requisições;
   estabelecer cotas por conta, retenção, exclusão de conta, backup/restauração,
   monitoramento e política de privacidade antes de acesso público amplo.
7. Fixar digest da imagem base aprovada e auditar dependências/imagem antes da implantação.

Uma instância é o escopo atual. Múltiplas réplicas precisam de estratégia explícita
para sessões e limites distribuídos; não basta multiplicar containers.

## Referências verificadas

- https://supabase.com/changelog.md
- https://supabase.com/changelog/postgres-15-19-17-11-breaking-changes
- https://supabase.com/docs/reference/javascript/auth-getuser
- https://supabase.com/docs/reference/javascript/auth-signinwithpassword
- https://supabase.com/docs/guides/database/postgres/row-level-security
- https://supabase.com/docs/guides/database/database-linter?lint=0028_anon_security_definer_function_executable

Changelog consultado, incluindo mudança PostgreSQL 17.11; a implementação usa
schema privado e não depende de exposição automática de novas tabelas à Data API.
