# Exemplos de validação

Os ZIPs são fixtures estáticas, não programas para executar.
- projeto-seguro.zip: nenhum achado esperado; não equivale a certificado de segurança.
- projeto-vulneravel.zip: 5 achados esperados (execução Java, segredo fictício, eval, ofuscação e aviso de dependência).
- zip-slip-rejeitado.zip: rejeição pelo caminho ../escape.txt.
- invalido.zip: rejeição pelo formato.
- xss-nome-e-evidencia.zip: conteúdo deve aparecer como texto, sem alertas no navegador.

Não há amostras reais de malware nem credenciais reais. Não descompacte nem execute as fixtures hostis; envie-as ao scanner.
