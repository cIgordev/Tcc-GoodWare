# GoodWare — interface local revisada

## Páginas e componentes

| Página | Papel | Estilos específicos | JavaScript |
|---|---|---|---|
| index.html | Apresentação e exemplo explicitamente ilustrativo | landing.css | landing.js |
| login.html | Entrada no modo local, sem conta ou senha | login.css | Nenhum necessário |
| painel.html | Indicadores e histórico obtidos da API | app-shell.css, dashboard.css | user-menu.js, dashboard.js |
| analisar.html | Upload ZIP autorizado e consulta SHA-256 | app-shell.css, analisar.css | user-menu.js, analisar.js |
| achado.html | Relatório, filtros, evidências e inventário | achado.css | achado.js |

Todas carregam `fonts.css` e `base.css`. O primeiro registra as fontes locais;
o segundo concentra cores, famílias, escala responsiva, espaçamento, bordas,
raios, botões, badges, foco, link de salto e preferência de movimento reduzido.
O cabeçalho das duas telas operacionais compartilha estilos e comportamento.
A aplicação continua usando HTML, CSS e JavaScript separados, sem framework novo.

## Decisões visuais

- Fundo #0A0A0C, superfícies #17141F e #201B2B, destaque #7B61FF.
- Texto principal #F4F1FF, secundário #A7A0B8. Roxo claro para links.
- Botão primário com texto escuro para contraste sobre roxo.
- DM Sans em títulos e interface; IBM Plex Mono em código, hashes e dados técnicos.
- Fontes incluídas em `static/fonts`, com licenças OFL: nenhuma conexão com
  Google Fonts é necessária. Fallbacks de sistema continuam definidos.
- Indicadores numéricos usam DM Sans; severidades têm rótulos, não só cores.
- Superfícies estáveis, sem flutuação no relatório ou animação contínua no console.
- Controles com altura mínima de 44 px, foco visível, títulos responsivos.

## Experiência

A página inicial apresenta capacidades reais e limitações sem prometer proteção
absoluta. Console e depoimentos mantêm identificação ilustrativa. O acesso é
explicitamente local, sem credenciais e sem autenticação simulada.

O painel diferencia carregamento, histórico vazio e falha HTTP; a falha oferece
nova tentativa. Nenhum número é inventado enquanto o serviço não responde.

O upload mantém 100 MiB, consentimento e validação reais. A consulta de hash tem
rótulo visível, erro associado ao campo e estado de busca. Uma falha de upload
interrompe o indicador de processamento; não há percentuais ou etapas fictícias.

O relatório separa resumo, limitações, severidades, ações, inventário e achados.
Busca por título, caminho, regra ou categoria combina-se com filtro de severidade.
A seleção mostra título e arquivo; categoria, confiança e severidade aparecem
separadamente. Ao selecionar um achado, o foco vai para seu detalhe. Em telas
pequenas, o detalhe é trazido à área visível. A tabela e o código podem rolar
internamente, sem alargar a página. Nomes, hashes e evidências longas são preservados.

## Contratos preservados

Não houve alteração em Java, regras, assinaturas, limites ou configuração de segurança.
As requisições continuam na mesma origem `/api/analyses`. Upload multipart com
campo `file`; busca por `sha256`; detalhes por ID; exportação em `/report`;
exclusão via DELETE. Histórico permanece temporário. Dados não confiáveis são
inseridos com `textContent`, `createTextNode` e `createElement`, nunca como HTML.
A marcação de revisão continua restrita à aba; filtros não alteram o relatório exportado.

## Executar

Use `LEIA-ME-PRIMEIRO.md`. Com Java 21, abra `iniciar-windows.bat` ou execute
`bash iniciar.sh`. Para desenvolver e reconstruir: `mvn clean verify` e copie o
JAR gerado em `target` para `dist`. Acesse http://localhost:8080, sem abrir os HTML
isoladamente. Nenhuma publicação externa faz parte desta entrega.

## Validação

Veja `VALIDACAO-VISUAL.md` para resultados desta revisão. `docs/TESTES.md` e
`docs/EVIDENCIAS-TESTES.txt` documentam a entrega anterior e não substituem esse registro.
