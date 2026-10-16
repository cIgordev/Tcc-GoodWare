# GoodWare — atualização com autenticação

Esta versão exige uma conta Supabase confirmada. Leia `docs/AUTENTICACAO-E-IMPLANTACAO.md`.

1. Encerre o GoodWare antigo e extraia este pacote em uma pasta nova.
2. Mantenha no seu computador as variáveis GOODWARE_DB_URL, GOODWARE_DB_USER,
   GOODWARE_DB_PASSWORD e GOODWARE_DB_SSL_ROOT_CERT que você já configurou.
   O ZIP não contém suas credenciais nem seu certificado local.
3. Use Java 21. Execute `iniciar-windows.bat` ou `bash iniciar.sh`.
4. Mantenha **GOODWARE_MIGRATE=false**: as migrações V1–V5 já foram aplicadas
   ao projeto tccgoodware. Não execute baseline/reaplicação automática.
5. Abra http://localhost:8080/login.html, crie uma conta, confirme o email e entre.
   Se o email não chegar, verifique as configurações SMTP/limites do Supabase.
6. Envie `examples/projeto-seguro.zip` ou `examples/projeto-vulneravel.zip`.

O banco tem uma análise legada sem proprietário. Ela foi preservada e fica
oculta das contas novas. Não atribuímos sua propriedade automaticamente.
O JAR antigo é incompatível com as novas políticas; use o JAR deste pacote.

**Estado:** implementação e testes locais concluídos; validação real de login +
JDBC no seu ambiente e preparação operacional pública ainda pendentes.
Nada foi publicado. Não envie senhas ou chaves privadas no chat.
