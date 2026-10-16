# GoodWare Backend — starter

Backend inicial para o frontend GoodWare usando Java 21 e Spring Boot.

## Incluído
- `GET /api/health`
- `POST /api/analyses` para enviar um arquivo ZIP e analisar seu conteúdo
- `GET /api/analyses` para listar análises em memória
- `GET /api/analyses?sha256=<hash>` para buscar por hash
- `GET /api/analyses/{id}`
- `GET /api/analyses/{id}/findings`
- Hash SHA-256 do pacote e dos arquivos
- Regras heurísticas simples para alguns padrões de código potencialmente inseguros
- Lista local de hashes conhecidos em `src/main/resources/signatures/sha256.txt`

## Executar
Requer JDK 21. No Windows, abra esta pasta no IntelliJ IDEA e execute `GoodwareBackendApplication`.
Ou, se Maven estiver instalado: `mvn spring-boot:run`.

API local: `http://localhost:8080/api/health`.

## Importante
- Esta é uma base de desenvolvimento, não um antivírus e não pronta para exposição pública.
- A análise é síncrona e os resultados ficam apenas na memória; reiniciar a aplicação apaga o histórico.
- A versão inicial aceita somente `.zip`; `.tar.gz` deve ser desabilitado no frontend até implementarmos extração segura desse formato.
- Não executa arquivos enviados. Os bytes são lidos como dados.
- A extração não grava os arquivos no disco, reduzindo risco de path traversal; caminhos inválidos são rejeitados.
- Hashes conhecidos são apenas exemplos cadastráveis. O arquivo de assinaturas vem vazio por padrão, pois não devemos inventar hashes de malware.
- As regras heurísticas são demonstrativas e podem gerar falsos positivos/negativos. Achados não confirmam malware.
- Antes de uso real, adicione autenticação/autorização, rate limiting, limites de infraestrutura, auditoria, retenção/remoção de dados, isolamento e testes de segurança.
