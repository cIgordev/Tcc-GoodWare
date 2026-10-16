> Histórico da fase anterior. Para autenticação, RLS e migrações V3–V5, prevalece AUTENTICACAO-E-IMPLANTACAO.md. Não executar baseline antigo na base atual.

> Documento histórico da versão anterior. Para persistência, estado atual e testes desta entrega, consulte MODELAGEM.md, CONFIGURAR-SUPABASE.md e TESTES-PERSISTENCIA.md.

# Segurança e limites do GoodWare

## Limites da versão 1.0
| Recurso | Limite / comportamento |
|---|---|
| ZIP enviado | 100 MiB |
| Requisição multipart | 102 MiB, incluindo campos e cabeçalhos |
| Entradas, incluindo diretórios | 2000 |
| Arquivo descomprimido | 10 MiB |
| Soma descomprimida | 200 MiB |
| Caminho | 240 caracteres, até 20 diretórios |
| Compressão | até 100:1 em entradas maiores que 1 MiB |
| Texto por arquivo | 2 MiB; acima disso somente hash |
| Linha textual | 4096 caracteres; linhas maiores são sinalizadas e omitidas |
| Achados | 300 por módulo/arquivo e 1000 por pacote; exceder rejeita a análise |
| Tempo | 30 segundos, verificação cooperativa entre leituras/módulos |
| Concorrência | uma análise; demais recebem 429 |
| Retenção | 30 relatórios, 24 horas em memória; reiniciar apaga |

Não há extração de arquivos internos no disco. Verificam-se central directory,
cabeçalhos locais, descritores, método de compressão, tamanhos e CRC. Entradas
sobrepostas, inconsistentes ou duplicadas por caixa, links/arquivos especiais,
nomes absolutos, `..`, `.` e separadores ambíguos são rejeitados. Bytes extras no
fim e ZIP sem diretório central também são rejeitados. Limites se aplicam aos bytes
efetivamente descomprimidos, além dos metadados declarados.

ZIP64, ZIP criptografado, ZIP dividido, codificações de nomes não UTF-8 e métodos
diferentes de STORED/DEFLATE não são suportados. ZIPs autoextraíveis são rejeitados.
Alguns ZIPs legítimos fora desse subconjunto precisam ser recompactados.

## Fronteira de segurança
Servidor vinculado a 127.0.0.1 e acesso de Host/origem local. Requisições cross-site
são rejeitadas. Não se deve alterar o bind, remover o filtro ou publicar por túnel
sem implementar autenticação/autorização, isolamento por usuário, quotas e retenção
apropriada. O filtro não substitui autenticação contra outros processos ou usuários
no mesmo computador.

CSP, nosniff, bloqueio de frames, no-store e no-referrer nas respostas. Nomes,
fragmentos e achados são exibidos como texto. O catálogo e as regex são configuração
confiável do operador e não podem ser substituídos pelo ZIP recebido.

Nenhum `ProcessBuilder`, Runtime.exec, subprocesso, compilador ou interpretador é
invocado pelo motor de análise. A regra reconhece essas sequências como texto.
O parser XML proíbe entidades externas, DTD e acesso a schemas externos.

## Privacidade
Scans não enviam hashes, código nem indicadores a terceiros. As páginas preservam
fontes Google do design original: o navegador pode requisitar fontes, mas o motor
não transmite conteúdo do projeto. As fontes possuem fallback local.
O servlet pode usar arquivo temporário de upload, eliminado ao encerrar a requisição.
O conteúdo bruto não é guardado no histórico; hashes, nomes e evidências redigidas
permanecem em memória. O mascaramento é heurístico e não garante a remoção de todo
segredo possível. Relatórios exportados podem conter informações do projeto.

## Fora do escopo / trabalho futuro
- Autenticação real, autorização, TLS de produção e isolamento multiusuário.
- Banco de dados com política explícita de retenção e criptografia.
- Worker isolado com limite duro de CPU/memória. O timeout atual é cooperativo e
  não interrompe arbitrariamente um parser/regex em execução.
- Quotas de upload antes do parsing multipart e proteção de disco em servidor público.
- AST, taint analysis, mais linguagens, suporte a lockfiles e SCA abrangente.
- Sincronização de inteligência somente com consentimento, licença, revisão,
  autenticação e limites de API; sem enviar fonte de usuário por padrão.
- Auditoria independente, teste de carga prolongado e fuzzing contínuo.

A versão Spring Boot foi atualizada de 3.5.6 para 3.5.16, disponível no Maven Central
na verificação desta entrega. Isso não é uma declaração de ausência de CVEs em
todas as dependências; mantenha revisões de atualização como parte do DevSecOps.
