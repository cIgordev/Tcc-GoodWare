> Documento histórico da versão anterior. Para persistência, estado atual e testes desta entrega, consulte MODELAGEM.md, CONFIGURAR-SUPABASE.md e TESTES-PERSISTENCIA.md.

# Catálogo local e procedência

`src/main/resources/signatures/catalog.json` é carregado ao iniciar. O catálogo de
produção desta entrega é **vazio de propósito**. Nunca se deve atribuir nome de
família ou classificar malware com hash inventado. Os testes criam hashes de textos
inofensivos com classification=TEST, exclusivamente em memória.

Formato raiz: `version` (string) e `signatures` (array). Cada registro exige:

| Campo | Conteúdo |
|---|---|
| id | Identificador estável, letras/números/hífen/underscore |
| type | SHA256, DOMAIN, IPV4 ou URL |
| value | Hash hexadecimal, host exato ou URL exata |
| description | Descrição que não extrapole a evidência da fonte |
| source | URL HTTPS da fonte pública/autorizada que sustenta o registro |
| updatedAt | Data ISO yyyy-MM-dd |
| confidence | LOW, MEDIUM ou HIGH |
| classification | MALWARE, SUSPICIOUS ou TEST |
| license | Licença/condições e permissão de utilização |

Não incluímos um hash fictício utilizável como ameaça. Para treinamento, gere um
hash de uma fixture inofensiva, marque TEST e mantenha em catálogo de testes separado.
Veja SignatureServiceTest.java para o procedimento sem gravar uma ameaça fictícia.

As validações de formato não provam que a fonte esteja correta. Cabe ao operador
revisar evidências, validade, licença e falsos positivos antes de importar. Catálogo
inválido impede a inicialização; entradas duplicadas e tipos não suportados são erro.

SHA-256 compara bytes completos do arquivo e também do pacote ZIP. DOMAIN/IPV4 são
comparados ao host exato de URLs HTTP(S) presentes no texto; não há subdomínios
implícitos, DNS, abertura do endereço ou consulta externa. URL compara texto exato,
sem normalização de query/path. Cobertura textual obedece aos limites do motor.
Uma referência catalogada não comprova que o programa a utilize nem que seja malware.

`signatures/sha256.txt` é referência do formato antigo, não é carregado. A base legada
não tinha assinaturas úteis; a recuperação está documentada em RECUPERACAO.md.

## Evolução de inteligência de ameaças
1. Escolher fonte confiável e verificar licença/redistribuição e retenção.
2. Criar importador separado do fluxo de upload, sob controle do operador.
3. Autenticar por variável de ambiente, aplicar rate limit/backoff e validar esquema.
4. Deduplicar, revisar procedência, data e confiança; expirar IOCs antigos.
5. Escrever nova versão de catálogo de forma atômica e validar antes de ativar.
6. Nunca transmitir código enviado pelo usuário; consultas por hash exigem política
   explícita de privacidade e autorização. Manter modo offline como padrão.
7. Registrar auditoria do importador e rollback de versão. Não criar famílias por inferência.

O contrato ScanModule e o serviço de assinatura permitem módulos adicionais. Nenhum
cliente remoto, chave de API ou sincronização periódica está habilitado nesta entrega.
