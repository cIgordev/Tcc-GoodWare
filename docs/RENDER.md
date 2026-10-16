# Implantação no Render

Este repositório contém o fonte atual do GoodWare. O Dockerfile compila com Java
21 e executa os testes Maven antes de gerar a imagem; não usa o JAR de uma entrega
anterior. O histórico Git anterior é preservado.

Serviço Docker, plano free, região Ohio, branch main. Use
SPRING_PROFILES_ACTIVE=render. A porta vem de PORT e a origem permitida de
RENDER_EXTERNAL_URL, ambas fornecidas pelo Render. Cookies são Secure/HttpOnly.
Tomcat usa processamento nativo de proxy com sua lista padrão de IPs privados;
nenhuma confiança irrestrita em proxies foi habilitada. Validar login via HTTPS
após o primeiro deploy, incluindo rejeição de origem externa.

Variáveis de execução:
- GOODWARE_DB_URL=jdbc:postgresql://aws-0-us-east-2.pooler.supabase.com:5432/postgres
- GOODWARE_DB_USER=goodware_app.tvnvjfaxuxwndlsnpiar
- GOODWARE_DB_PASSWORD: configurar exclusivamente no painel Render.
- GOODWARE_MIGRATE=false (migrações já aplicadas ao projeto autorizado).
- GOODWARE_DB_SSL_ROOT_CERT=/etc/secrets/prod-ca-2021.crt

Adicionar o certificado CA do projeto como Secret File com nome
prod-ca-2021.crt. A verificação TLS permanece verify-full. Não substituir pela
senha administrativa postgres nem desabilitar RLS para resolver falhas.

O serviço não estará operacional até receber senha e certificado válidos.
Após iniciar, verificar /api/health, cadastro/confirmação de e-mail, login,
upload, histórico e isolamento de duas contas. Ajustar Site URL/redirects do
Supabase para o endereço público antes de validar e-mails de confirmação.

O plano gratuito pode suspender a instância por inatividade. Sessões ficam em
memória: reinícios exigem novo login e podem interromper análises. O histórico
persistido continua no Supabase. Nenhum disco pago ou novo banco é necessário.

Referências: https://render.com/docs/environment-variables,
https://render.com/docs/web-services,
https://docs.spring.io/spring-boot/3.5/how-to/webserver.html
