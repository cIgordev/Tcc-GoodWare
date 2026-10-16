# GoodWare — front-end

Plataforma web para análise estática de pacotes de projetos de software (TCC).
Este repositório contém **apenas o front-end** (HTML, CSS e JavaScript puros, sem build).
O sistema trata os arquivos enviados como dados: nada do que é enviado é executado.

## Como abrir

Abra a pasta com um servidor local (recomendado). O cálculo do SHA-256 na página
de análise exige HTTPS ou `localhost`.

```bash
python3 -m http.server 8000
# acesse http://localhost:8000
```

## Estrutura

```
index.html        Landing page (pública)
login.html        Entrar
painel.html       Resumo e lista de análises
analisar.html     Nova análise: enviar pacote, buscar por hash, andamento
achado.html       Detalhe de achados (relatório de exemplo)
css/
  base.css        Tokens, tipografia, botões, campos, badges (telas do app)
  app-shell.css   Topbar e área de conteúdo (painel + analisar)
  landing.css     Landing page
  login.css · dashboard.css · analisar.css · achado.css   Estilos de cada página
js/
  user-menu.js    Menu do usuário (painel + analisar)
  landing.js · login.js · dashboard.js · analisar.js · achado.js
```

Regra do projeto: sem estilo ou script inline; cada página tem seu CSS/JS.

## Fluxo entre as páginas

```
index ──► login ──► painel ──► analisar ──► achado
  │          ▲         │  ▲                    │
  │          │         │  └────────────────────┤ (Voltar)
  │          └─ Sair ──┘                       │
  └──────────────► achado (“Veja uma prévia”) ◄┘
```

- `index` → `login` (Login, Comece agora, Comece grátis, Experimente grátis) e `achado` (Veja uma prévia).
- `login` → `painel` após entrar; a marca leva de volta ao `index`.
- `painel` → `analisar` (Nova análise) e `achado` (Ver relatório); “Sair” → `login`.
- `analisar` → `achado` (Ver relatório / Abrir relatório) e `painel` (menu e marca).
- `achado` → página de origem (Voltar); sem origem, `painel`.

## Estado atual (protótipo)

Os dados e o andamento da análise são **simulados**. Pontos de integração estão
marcados com `TODO API` no código:

| Onde | Integração pendente |
| --- | --- |
| `login.js` | `POST /api/auth/login` |
| `dashboard.js` | `GET /api/analyses` |
| `analisar.js` | `GET /api/analyses?sha256=`, `POST /api/analyses`, acompanhar `GET /api/analyses/:id`, cancelar |
| `achado.js` | `GET /api/analyses/:id/findings` |

Links ainda sem destino (`href="#"`): Criar conta, Esqueci minha senha, Continuar com Google,
Perfil, Configurações e “Fale com um atendente”.

## Segurança no front-end

- A validação no navegador (extensão, tamanho, hash) é só para UX. O servidor deve repetir tudo:
  tipo real do arquivo, tamanho após extração, nº de arquivos, profundidade, path traversal e zip bomb.
- Nome de arquivo, hash digitado e dados da API são conteúdo não confiável e entram na página
  somente por `textContent`/`createElement` (ou escapados), nunca por `innerHTML` sem escape.
- Não há controle de acesso no front: o acesso a `painel`, `analisar` e `achado` precisa ser
  protegido pelo backend (sessão/autorização).
- Um resultado sem achados não garante que o projeto seja seguro; um achado é indicador, não confirmação.

## Observações

- Os depoimentos da landing são ilustrativos e devem ser substituídos por feedbacks reais.
- Os achados e o hash de demonstração são dados fictícios, não resultados de testes.
