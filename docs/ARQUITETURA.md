> Histórico da fase anterior. Para autenticação, RLS e migrações V3–V5, prevalece AUTENTICACAO-E-IMPLANTACAO.md. Não executar baseline antigo na base atual.

> Documento histórico da versão anterior. Para persistência, estado atual e testes desta entrega, consulte MODELAGEM.md, CONFIGURAR-SUPABASE.md e TESTES-PERSISTENCIA.md.

# Arquitetura e decisões

## Fluxo real
`POST /api/analyses` recebe multipart → valida limites → calcula SHA-256 do ZIP →
SafeZipReader verifica estrutura e percorre bytes descomprimidos → calcula SHA-256
por arquivo → módulos de regras e assinaturas → inventário e relatório → retenção
limitada em memória → resposta 201 ao navegador.

A API é síncrona. A interface mostra “Enviando / analisando” até receber a resposta;
não existe percentual ou progresso por etapa fabricado. O botão de retorno fica
indisponível durante a requisição. Fechar a aba não cancela processamento já iniciado.

## Separação de responsabilidades
- **SafeZipReader**: diretório central, cabeçalhos locais, CRC e limites. Nunca extrai
  caminhos de entrada no sistema de arquivos. O multipart pode ser temporariamente
  armazenado pelo servlet; a aplicação não guarda nem executa o código-fonte.
- **AnalysisService**: uma análise de cada vez, orçamento de tempo cooperativo,
  cobertura, hashes, agregação e histórico. Excesso simultâneo devolve HTTP 429.
- **ScanModule**: contrato para adicionar módulos sem misturar os significados dos achados.
- **RuleModule/vulnerabilities.json**: padrões de risco por linguagem.
- **RuleModule/indicators.json**: ofuscação, PowerShell codificado e referências onion.
- **SignatureService**: SHA-256 e IOC de rede com procedência, sem visita, DNS ou download.
- **DependencyModule**: leitura XML endurecida do pom.xml, independente das regex de código.
- **Evidence**: IDs estáveis por regra/arquivo/linha/evidência, mascaramento e limite do trecho.
- **LocalSecurityFilter**: localhost, verificação de origem, proteção de Host e cabeçalhos.

## Regras
18 regras: processos Java/Python/JS/PHP; SQL Java concatenado; eval; desserialização
Java/Python/PHP; HTML dinâmico; TLS desabilitado; hashes legados; segredos literais
em código/configuração; marcador de chave privada; decodificação seguida de eval;
PowerShell codificado; URL onion. Os JSONs incluem linguagens, categorias,
severidade, confiança, expressão, impacto e recomendação.

Comentários simples e comentários de bloco C são desconsiderados nas linguagens
suportadas. Os números de linha originais são mantidos. Há limites documentados:
strings com código de exemplo podem disparar regras; comentários/triple strings
Python e templates complexos não são tratados como AST. Código em várias linhas,
aliases, fluxo entre funções e variantes ofuscadas podem não disparar.

Confiança representa a qualidade da inferência; severidade representa impacto
potencial. Não aumentamos confiança para HIGH simplesmente por encontrar `exec`.
A evidência de segredos descreve a atribuição e oculta o valor inteiro.

## Cobertura de arquivos
Todos os arquivos aceitos recebem hash. Texto UTF-8 de extensões suportadas até
2 MiB recebe regras. Binários, codificação não UTF-8, texto maior e arquivos sem
extensão suportada ficam HASH_ONLY, com motivo no inventário. Linhas >4096 caracteres
são omitidas das regex e sinalizadas PARTIAL. ZIPs/JARs internos não são abertos.
“TEXT_SCANNED” significa aplicação do conjunto disponível, nunca análise completa.

## Dependências
Apenas `pom.xml`, coordenadas explícitas `org.apache.logging.log4j:log4j-core` e
CVE-2021-44228. Faixas estáveis implementadas:
- [2.0, 2.3.1)
- [2.4, 2.12.2)
- [2.13.0, 2.15.0)

Fonte Apache: https://logging.apache.org/security.html — consultada em 14/10/2026.
O aviso também abrange pré-lançamentos a partir de 2.0-beta9; o MVP não resolve
pré-lançamentos, propriedades, intervalos, herança ou BOM e emite aviso de versão
não resolvida. DTD/entidades externas são rejeitadas. XML inválido gera aviso de
cobertura. `log4j-api` isolado não dispara a regra. Dependências em perfis ou
`dependencyManagement` também são declarações: o relatório não prova que estejam
presentes em runtime. Recomenda-se versão mantida pelo fornecedor, sem afirmar que
as primeiras versões corrigidas sejam livres de outras vulnerabilidades.

NPM, pip, Gradle e dependências transitivas não são auditados por CVE. Futuro módulo
SCA deve usar parser próprio, lockfiles e catálogo com intervalos/versionamento.
Nunca rodar Maven, npm, pip ou scripts a partir de pacotes recebidos.

## Dados e frontend
Fonte de verdade: JSON da API. Dados não confiáveis entram no DOM via textContent.
Não existe `innerHTML` alimentado por resultados. Revisão de achados usa
sessionStorage por aba e não altera a análise original. Relatório JSON contém versão
do motor/catálogo, contagens, hashes, evidências e avisos.
