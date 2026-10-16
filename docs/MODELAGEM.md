> Histórico da fase anterior. Para autenticação, RLS e migrações V3–V5, prevalece AUTENTICACAO-E-IMPLANTACAO.md. Não executar baseline antigo na base atual.

# Modelagem e decisões

## Arquitetura
Frontend existente → API Spring Boot → serviço de análise → JdbcTemplate/Hikari →
PostgreSQL. Scanner continua lendo ZIP apenas em memória, sem executar conteúdos.
A persistência não usa Hibernate. SQL parametrizado; Flyway controla o esquema.

| Tabela | Chave | Conteúdo |
|---|---|---|
| goodware.analyses | UUID | nome, SHA-256, bytes do ZIP, estado, datas, erro seguro, versões, duração, avisos |
| goodware.analyzed_files | analysis_id + path | inventário, tamanho, SHA-256, linguagem, cobertura e motivo |
| goodware.findings | analysis_id + id | categoria/classificação analítica, severidade, confiança, regra/versão, linha, evidência, impacto, recomendação |
| goodware.signature_catalog | catalog_version + id | snapshots de assinaturas locais validadas com tipo, valor, fonte, data, confiança, classificação, licença |

`category` já representa a classificação analítica no contrato original:
POTENTIAL_VULNERABILITY, SUSPICIOUS_INDICATOR, KNOWN_SIGNATURE, TEST_SIGNATURE,
DEPENDENCY_ADVISORY e COVERAGE_NOTICE. Não duplicamos essa informação em outra
coluna. Classificação da assinatura é MALWARE/SUSPICIOUS/TEST, vinda do catálogo.
Nunca inferimos família de malware nem inventamos hashes/fonte. Catálogo real vazio.

Resumo por categoria/severidade é calculado em SQL (`/{id}/summary`). Quantidades
por arquivo e severidade são derivadas dos registros, evitando contadores
persistidos inconsistentes. Avisos são uma lista JSON serializada em TEXT; não
armazenamos o relatório completo nem duplicamos o conteúdo dos arquivos.

Achados de pacote usam `file_path=NULL`, retornado pela API como `[pacote ZIP]`.
FK composta impede que um achado aponte a arquivo de outra análise. `ON DELETE
CASCADE` elimina arquivos/achados ao excluir a análise, sem tocar no catálogo.
Índices de histórico, hash, FK e agrupamentos atendem as consultas principais.
UUID não substitui autorização: este é um sistema individual, sem usuários.

## Transações e estado
Após limites iniciais de upload, grava RUNNING antes do scan. Rejeições anteriores
(nome/extensão/tamanho/vazio) não criam registros. ZIP malformado registra FAILED.
Resultado final: bloqueio da análise + arquivos + achados + COMPLETED em uma única
transação. Em falha, rollback dos filhos e tentativa independente de FAILED.
Se o banco estiver indisponível, retorna 503; não retorna sucesso em memória.
Uma falha de rede após commit pode tornar a confirmação ambígua: consulte o histórico
antes de reenviar. A API não promete idempotência; uploads repetidos geram novos IDs.

Análises interrompidas com RUNNING há mais de 10 minutos são marcadas FAILED pelo
agendador a cada minuto. O scanner mantém o prazo original de 30 segundos;
o bloqueio do pai serializa a recuperação com a conclusão transacional.
Não há retomada do scan: reenvie o ZIP, pois seu conteúdo não é armazenado.
A exclusão de RUNNING retorna conflito. Leituras de resultado usam REPEATABLE READ.

## API e interface
POST mantém resposta síncrona 201 apenas após commit. Não retorna ID antecipado.
Enquanto POST está em curso, o painel lista RUNNING; o ID pode ser consultado
por `GET /api/analyses/{id}/status`. O navegador de upload mantém seu estado visual.
Não é fila assíncrona nem progresso percentual. O painel precisa ser atualizado
para ver mudança de estado; a página de resultado não exibe FAILED como seguro.

`GET /api/analyses` mantém array legado dos 30 mais recentes (agora sem expiração).
`GET /api/analyses/history?page=0&size=20` retorna items/page/size/total.
Página é zero-based, size 1–100; ordenação created_at DESC,id DESC. Painel usa essa
rota e mostra métricas da página claramente. Paginação por offset pode deslocar
itens se houver novos uploads entre consultas; não é snapshot de todo o histórico.
GET individual/files/findings/report, SHA-256 e DELETE preservados.

## Catálogo
O catálogo local validado permanece fonte do scanner; uma cópia versionada é
persistida ao iniciar. Não há consulta externa. Novas assinaturas exigem editar
catalog.json, aumentar a versão e reconstruir/reiniciar. Não editar entradas
já publicadas sob a mesma versão. Alterações diretas no banco não ativam regras.
Esse é um catálogo auditável de snapshots, não um painel de edição remota.
As versões de regra são a versão do motor para heurísticas e versão do catálogo
para assinaturas; o motor original não tem versão individual por regra.

## Privilégios
Schema `goodware` privado, sem privilégios para PUBLIC/anon/authenticated/service_role.
RLS nas quatro tabelas, políticas somente para `goodware_backend`, grupo NOLOGIN.
Políticas permitem o conjunto de dados ao backend porque não há múltiplos usuários;
não são políticas públicas. Tabelas não devem ser adicionadas aos esquemas expostos.
Runtime: DML nas análises/arquivos/achados, SELECT/INSERT no catálogo; sem DDL.
Administrador proprietário/superuser/BYPASSRLS pode ignorar RLS. Por isso não deve
ser a identidade do backend. Não há chaves privilegiadas no frontend.

## Privacidade e limites
Persistimos caminhos e pequenos trechos redigidos, nunca ZIP nem conteúdo completo.
A máscara remove atribuições conhecidas de segredos, credenciais de URL, Bearer,
JWTs e literais entre aspas. É minimização heurística: caminhos, metadados e textos
não reconhecidos podem conter informação sensível. Não há garantia universal de
anonimização. Restrinja acesso ao banco e defina sua retenção; exclusão manual sem
expiração automática nesta versão. Logs de falha da API não imprimem SQL/credenciais.
Frontend continua escapando dados com textContent. Controles ZIP e origem preservados.
