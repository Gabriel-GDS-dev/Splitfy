# Splitfy na Vercel

O deploy inclui a API Spring Boot em container e o frontend estático na CDN,
sem esperar a inicialização do Java para exibir a página. O JAR também inclui
o frontend para desenvolvimento local. A raiz do projeto
na Vercel deve ser a raiz do repositório (Root Directory vazio), com framework
Other e sem comandos de build/install/output personalizados. O arquivo
`vercel.json` declara o serviço estático para a interface e o container para
as rotas da API e do Swagger. `Dockerfile.vercel` compila a aplicação com Java 21. Container
Runtime e Services são recursos beta da Vercel, disponíveis em todos os planos.
O JRE limita a compilação inicial ao nível 1 para reduzir o tempo de partida
quando a Vercel inicia uma nova instância depois de um período sem acessos.

## Banco e segredos

Conecte a integração Prisma Postgres ao projeto. A URL PostgreSQL deve ficar
somente no servidor. Configure estas variáveis no ambiente de destino:

| Variável | Conteúdo |
| --- | --- |
| `SPRING_DATASOURCE_URL` | URL JDBC do pool: `jdbc:postgresql://pooled.db.prisma.io:5432/postgres?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Usuário da conexão com pool |
| `SPRING_DATASOURCE_PASSWORD` | Senha da conexão com pool |
| `SPRING_FLYWAY_URL` | URL JDBC direta: `jdbc:postgresql://db.prisma.io:5432/postgres?sslmode=require` |
| `SPRING_FLYWAY_USER` | Usuário da conexão direta |
| `SPRING_FLYWAY_PASSWORD` | Senha da conexão direta |
| `SPLITFY_JWT_SECRET` | Chave aleatória de pelo menos 32 bytes |
| `PORT` | `8080` |

As credenciais vêm das URLs disponibilizadas pela integração: `DATABASE_URL`
para o pool e `POSTGRES_URL` para a conexão direta. Não coloque usuário/senha
na URL JDBC nem em arquivos versionados. Armazene credenciais e chave JWT como
Secret na Vercel. Use outro banco para Preview antes de habilitar esse ambiente.

O Flyway aplica V1 a V13 na inicialização, mantendo o histórico e validando
migrations já aplicadas. Não habilite `baseline-on-migrate`, não use `clean`
e não recrie um banco que já contenha dados. O pool limita cada instância a três
conexões. A integração cria um banco vazio; ela não copia registros do Postgres
local automaticamente.

## Publicação e verificação

1. Execute os testes em `Splitfy/`: `./mvnw test` e
   `node --test src/test/frontend/auth.test.cjs`.
2. Vincule a pasta raiz ao projeto correto com `vercel link` e confirme com
   `vercel project inspect --non-interactive`.
3. Publique com `vercel deploy --prod --skip-domain`.
4. Verifique a página inicial, o Swagger, cadastro, login e consultas autenticadas
   na URL gerada. Endpoints privados sem token devem responder 401.
5. Promova o deploy validado com `vercel promote <url>`.

Cadastre a conta pela interface. Para preparar o primeiro administrador, promova
apenas a conta desejada diretamente no banco e faça login novamente:

```sql
UPDATE usuario SET role = 'ADMIN' WHERE email = 'seu-email';
```

O catálogo aparece conforme os registros salvos no banco. Não há criação
automática de contas, músicas ou senhas de demonstração.

## Arquivos MP3

Entre como administrador e use Músicas → Nova música ou Editar música.
Selecione um MP3 de até 4 MB no campo Arquivo MP3 e salve. Deixar o campo vazio
mantém o áudio existente. É possível enviar gravações próprias ou de terceiros
que você tenha autorização para disponibilizar.

`PUT /api/musicas/{id}/audio` recebe multipart com campo `arquivo`, apenas para
ADMIN. `GET /api/musicas/{id}/audio` exige autenticação e retorna `audio/mpeg`,
com suporte a Range. Os bytes ficam na tabela `musica_audio`, vinculada ao
cadastro com exclusão em cascata, sem depender do disco temporário do container.
As consultas ao catálogo não carregam os bytes dos arquivos.

Os cadastros antigos são preservados. O player anterior apenas simulava plays:
cadastros sem MP3 continuam sem áudio até que o arquivo original seja enviado.
A ausência da miniatura no painel da Vercel não é controlada por uma imagem
no projeto; conferir novamente após a geração da nova implantação estática.

Para busca de músicas no Spotify/YouTube e imagens por link, consulte
[integrações musicais](integracoes-musicais.md).
