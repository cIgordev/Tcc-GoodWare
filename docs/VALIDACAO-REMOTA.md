> Histórico da fase anterior. Para autenticação, RLS e migrações V3–V5, prevalece AUTENTICACAO-E-IMPLANTACAO.md. Não executar baseline antigo na base atual.

# Validação Supabase remoto — 14/10/2026

Projeto: tccgoodware / tvnvjfaxuxwndlsnpiar.
Autorização explícita recebida antes da aplicação. Nenhum outro projeto alterado.

| Migração local | Nome remoto | Versão UTC |
|---|---|---|
| V1__persistent_analyses.sql | goodware_v1_persistent_analyses | 20261015001417 |
| V2__private_backend_access.sql | goodware_v2_private_backend_access | 20261015001427 |

As quatro tabelas foram criadas em goodware: analyses, analyzed_files, findings,
signature_catalog. RLS ativa em todas. Políticas exclusivas para goodware_backend;
catálogo com SELECT/INSERT; demais tabelas com DML. Grupo NOLOGIN e sem BYPASSRLS.
Privilégios de schema e SELECT negados a anon, authenticated e service_role.

Teste SQL remoto como administrador: criação de duas análises sintéticas,
arquivo e achado; leitura, atualização; FK rejeitou arquivo de outra análise;
CHECK rejeitou ../escape; exclusão em cascata removeu filhos. Transação revertida
com ROLLBACK. Consulta posterior confirmou zero análises. Nenhuma amostra de
malware nem assinatura real foi inserida.

O teste SET LOCAL ROLE goodware_backend falhou com permission denied to set role.
A identidade da integração não tem esse direito; não ampliamos seus privilégios.
Portanto, grants/políticas foram inspecionados, mas DML remoto como login restrito
continua pendente. Testes locais desse papel já passaram na entrega anterior.

Advisor de segurança: dois WARN na função preexistente public.rls_auto_enable(),
SECURITY DEFINER com EXECUTE concedido a anon e authenticated. A função não foi
criada nem alterada pelas migrações GoodWare. Requer revisão separada da finalidade
antes de mudar seus privilégios. Referências retornadas pelo advisor:
- https://supabase.com/docs/guides/database/database-linter?lint=0028_anon_security_definer_function_executable
- https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable

Pendente: login dedicado com senha, certificado, conexão JDBC/TLS a partir do
computador, upload real via Java remoto, reinício e recuperação. O histórico
Flyway não foi inicializado. Runtime deve usar GOODWARE_MIGRATE=false; baseline
explícito versão 2 somente quando for adotar futuras migrações Flyway.

Conclusão: banco remoto implementado e validado via integração; conexão da
aplicação Java com o banco remoto ainda pendente.
