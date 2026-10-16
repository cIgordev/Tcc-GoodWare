> Histórico da fase anterior. Para autenticação, RLS e migrações V3–V5, prevalece AUTENTICACAO-E-IMPLANTACAO.md. Não executar baseline antigo na base atual.

# Configurar PostgreSQL / Supabase

## Estado desta entrega
V1/V2 aplicadas no `tccgoodware` (tvnvjfaxuxwndlsnpiar) em 14/10/2026 após
sua autorização explícita. As quatro tabelas, RLS e grants foram verificados.
Histórico remoto: 20261015001417 / 20261015001427. Consulte VALIDACAO-REMOTA.md.
Ainda falta conectar o Java em seu computador: não houve teste JDBC/TLS remoto.

**Para esse projeto, pule a criação das tabelas: mantenha GOODWARE_MIGRATE=false.**
Prossiga com o login restrito, certificado e variáveis. Não execute V1/V2 novamente.

## 1. Destino e credenciais
O destino autorizado e preparado é tccgoodware (tvnvjfaxuxwndlsnpiar).
No painel Supabase, abra Connect. Prefira conexão direta (IPv6) ou Session pooler
(porta 5432) quando sua rede só tiver IPv4. Não use transaction pooler neste guia.
Copie o host real, sem adivinhar região ou prefixo. Baixe o certificado raiz em
Database Settings / SSL Configuration. Não precisa de chave anon ou service_role.

## 2. Migrações e histórico
V1 e V2 já foram aplicadas pela integração e estão no histórico do Supabase.
Para a aplicação usar essas tabelas agora, basta GOODWARE_MIGRATE=false.
Não é necessário executar Flyway para iniciar o runtime.

### Antes de uma futura atualização pelo Flyway
O histórico Supabase e o histórico Flyway são distintos. A integração não criou
`goodware.flyway_schema_history`. Para adotar Flyway, primeiro confira que V1/V2
continuam correspondendo ao esquema e que nenhuma alteração externa ocorreu.
Então faça um bootstrap único, com administrador, TLS verify-full e sem HTTP:

```sh
export GOODWARE_MIGRATE=true
export GOODWARE_BASELINE_EXISTING=true
java -jar dist/goodware-backend-1.0.0.jar --spring.main.web-application-type=none
```

A configuração usa baseline-version=2, reconhecendo que V1/V2 já estão presentes.
Use também no datasource credenciais temporárias do administrador somente durante
esse bootstrap. O URL GOODWARE_MIGRATION_URL deve conter explicitamente
`sslmode=verify-full&sslrootcert=...` (as propriedades Hikari não se aplicam ao Flyway).
Confira o log de baseline versão 2, encerre com Ctrl+C e remova as credenciais
administrativas. Volte GOODWARE_BASELINE_EXISTING=false e GOODWARE_MIGRATE=false.
As próximas migrações serão V3 em diante. O baseline não revalida checksums de
V1/V2, por isso a conferência anterior é obrigatória. Não edite esses arquivos.

Em outro banco realmente novo, use GOODWARE_BASELINE_EXISTING=false e
GOODWARE_MIGRATE=true para executar V1/V2 normalmente como administrador.

## 3. Login restrito, depois de V1/V2
Usando administrador no SQL Editor/psql, crie um login dedicado sem conceder
propriedade das tabelas, superuser ou BYPASSRLS. Exemplo **sem senha**:

```sql
CREATE ROLE goodware_app LOGIN INHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
GRANT goodware_backend TO goodware_app;
```

Defina senha forte fora do código (por exemplo `\password goodware_app` em psql).
A migração cria `goodware_backend` NOLOGIN como grupo de permissões.
Na conexão direta o usuário é `goodware_app`; no Session pooler use o formato
indicado pelo painel para o usuário personalizado (`goodware_app.<project-ref>`).
Verifique como administrador que esse login não recebeu outros papéis privilegiados.
O runtime só deve pertencer ao grupo GoodWare. Não conceda esse grupo a anon,
authenticated, service_role ou a papéis de outros sistemas.

## 4. Executar
```sh
mvn clean verify
java -jar dist/goodware-backend-1.0.0.jar
```
Para usar o build feito por você, copie `target/goodware-backend-1.0.0.jar` para
`dist/` antes de executar, ou execute diretamente o JAR de `target`.
Acesse http://localhost:8080. Envie `examples/projeto-seguro.zip`, consulte o
histórico, reinicie o backend e consulte o mesmo ID. Exclua apenas essa análise
de teste. Faça o mesmo com o ZIP vulnerável e com o inválido.

Credenciais ausentes ou banco inacessível não ativam fallback em memória. Erros
HTTP de persistência retornam 503 sem dados de conexão. Não exponha este backend
publicamente: ainda não existe autenticação nem separação de usuários.

## Referências oficiais consultadas em 14/10/2026
- https://supabase.com/docs/guides/database/connecting-to-postgres
- https://supabase.com/docs/guides/platform/ssl-enforcement
- https://supabase.com/docs/guides/database/postgres/row-level-security
- https://jdbc.postgresql.org/documentation/ssl/
- https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html
- https://supabase.com/changelog.md (mudança recente de cifras de APIs HTTP não altera JDBC)
