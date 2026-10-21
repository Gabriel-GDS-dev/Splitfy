# Splitfy na Vercel

O deploy inclui a API Spring Boot e o frontend do mesmo JAR. A raiz do projeto
na Vercel deve ser a raiz do repositório (Root Directory vazio), com framework
Other e sem comandos de build/install/output personalizados. A Vercel detecta
`vercel.json` declara explicitamente o serviço container e encaminha todas as
rotas a ele. `Dockerfile.vercel` compila a aplicação com Java 21. Container
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

O Flyway aplica V1 a V11 na inicialização, mantendo o histórico e validando
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
