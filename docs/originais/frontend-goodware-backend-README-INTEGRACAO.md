# GoodWare integrado — como abrir

1. Pare o backend antigo no IntelliJ (botão vermelho Stop).
2. Faça backup da pasta `goodware-backend` atual.
3. Extraia este ZIP em uma nova pasta.
4. Abra a pasta `goodware-backend` pelo IntelliJ e aguarde o Maven.
5. Execute `GoodwareBackendApplication`.
6. Acesse `http://localhost:8080/analisar.html` (NÃO abra o HTML com `file://`).
7. Selecione `teste-goodware.zip`, aceite a autorização e clique em `Iniciar análise`.
8. Ao terminar, clique em `Ver relatório`.

As páginas `painel.html` e `login.html` ainda são demonstrativas; não existe autenticação real. A análise e o histórico ficam na memória até reiniciar. O progresso indica processamento e conclusão, sem percentual fictício. Esta versão é apenas para desenvolvimento local e NÃO deve ser publicada como serviço aberto na internet.

O frontend foi preservado visualmente e os scripts `js/analisar.js` e `js/achado.js` foram conectados à API.
