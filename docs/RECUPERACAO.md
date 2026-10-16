> Histórico da fase anterior. Para autenticação, RLS e migrações V3–V5, prevalece AUTENTICACAO-E-IMPLANTACAO.md. Não executar baseline antigo na base atual.

# Recuperação das versões anexadas

## Arquivos recebidos
- **goodware-backend-starter(4).zip**: 13 entradas; contém pom.xml, configurações,
  classes Java, catálogo legado vazio e teste de contexto. Não contém frontend.
- **GoodWare-integrado-SpringBoot(4).zip**: 21 entradas; contém páginas HTML,
  estilos CSS, JavaScript e guias. Não contém pom.xml nem classes Java.

Os anexos eram complementares, não dois backends concorrentes. Todos os arquivos
foram inventariados; hashes individuais estão em INVENTARIO-ANEXOS.md. A aplicação
original não podia ser compilada usando apenas o ZIP “integrado”.

## Base escolhida e preservação
Maven/Java do starter + frontend do integrado. Mantidos os caminhos do pacote
`br.com.goodware` e da API `/api/analyses`, nomes de páginas e design existente.
SVGs, paleta, fontes, animações e estilos originais foram conservados; estilos
adicionais somente para resumo, inventário, quebra de texto e atributos hidden.
As referências/fontes externas já existentes continuam com fallbacks.

Correções necessárias:
- Ordem correta do parent no pom e atualização de Spring Boot 3.5.6 → 3.5.16.
- ZIP inválido antes podia resultar em análise vazia bem-sucedida; agora é rejeitado.
- Contagem agora exclui diretórios e existe inventário por arquivo.
- IDs de achados incluem arquivo e evidência; não colidem entre arquivos com mesma linha.
- Painel deixou de listar projetos fictícios e consulta a API.
- Upload/remove/retorno têm estados coerentes; não há etapas temporizadas simuladas.
- Login fictício removido; página visual preservada como entrada do modo local.
- Botões sem função de conta/contato agora levam a recursos existentes.
- Depoimentos da landing já eram identificados como ilustrativos; não são análises.
- Visão do relatório não exibe “Concluída” antes da carga real.
- Retenção limitada e erros sem stack trace.

Os guias originais estão em `docs/originais` como histórico, não como instrução
atual. A referência atual é LEIA-ME-PRIMEIRO.md e os documentos nesta pasta.

## Continuidade
Leia PROGRESSO_GOODWARE.md, execute `mvn clean verify` e continue a partir das
pendências documentadas. O pacote inclui fontes completos, testes, fixtures e JAR.
Não é necessário reconstruir o projeto a partir dos ZIPs antigos.
Não há promessa de retomada automática por renovação de limites nesta sessão.
