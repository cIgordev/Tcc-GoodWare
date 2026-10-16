# Atualização de autenticação

Todas as rotas de análises exigem cookie de sessão. Escritas exigem
`X-GoodWare-Request: 1`. POST /api/auth/login e /signup aceitam JSON email/password;
GET /api/auth/me retorna id; POST /api/auth/logout encerra sessão. Sem token na resposta.
401 sem sessão; 404 para recurso inexistente ou alheio; 403 para origem/CSRF;
429 para limite; 503 se provedor de autenticação estiver indisponível.

---
## Contratos anteriores preservados (agora autenticados)

# API atual

Uso individual em http://localhost:8080. Sem autenticação de usuários.

| Método/rota | Resposta |
|---|---|
| POST /api/analyses | multipart `file`, ZIP; 201 com AnalysisResult após commit |
| GET /api/analyses | array legado, últimos 30; não limita a retenção no banco |
| GET /api/analyses?sha256=... | última análise desse hash; 404 se ausente |
| GET /api/analyses/history?page=0&size=20 | items/page/size/total, size 1–100 |
| GET /api/analyses/{id} | resultado ou estado parcial (RUNNING/FAILED) |
| GET /api/analyses/{id}/status | estado, datas, tamanho e erro seguro |
| GET /api/analyses/{id}/summary | agrupamento category/severity/total |
| GET /api/analyses/{id}/files | inventário |
| GET /api/analyses/{id}/findings | achados |
| GET /api/analyses/{id}/report | download JSON |
| DELETE /api/analyses/{id} | 204, filhos em cascata; 409 se RUNNING |
| GET /api/health | liveness do processo; não é readiness do banco |

400: entrada inválida; 403: origem proibida; 404: ausente; 413: tamanho;
429: scanner ocupado; 503: falha de persistência/transação sem confirmação.
Erros seguem application/problem+json com status/detail, sem stacktrace.
POST continua síncrono, sem ID prévio nem porcentagem; painel permite consultar
análises em andamento quando o registro RUNNING já existe. Não tratar FAILED ou
RUNNING sem achados como projeto seguro. Cliente deve consultar status.
