# Splitfy

API de um sistema de música, desenvolvida em equipe como uma aplicação Spring Boot com um banco PostgreSQL. O projeto usa Spring Data JPA e Flyway. O código da aplicação está na pasta [`Splitfy/`](Splitfy/).

Este README descreve a versão integrada dos módulos da equipe. O Documento de Análise e o DER já foram entregues, conforme informado pela equipe. A segunda entrega foi alterada para conter somente o script de criação do banco, com prazo em 22/10/2026. As funcionalidades citadas abaixo podem continuar evoluindo sem antecipar etapas futuras.

## Como executar

Requisitos: Java 21 ou superior e PostgreSQL. O projeto inclui o Maven Wrapper, então não é necessário instalar o Maven separadamente.

1. Tenha um banco PostgreSQL chamado `Splitfy`, ou ajuste a URL de conexão para o nome do seu banco.
2. Confira usuário, senha e URL em [`application.properties`](Splitfy/src/main/resources/application.properties). Para usar uma senha local sem editar esse arquivo, defina `SPRING_DATASOURCE_PASSWORD` no ambiente. Também é possível definir `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_URL`.
3. Defina uma chave para assinar os JWTs. No PowerShell, gere uma chave para o ambiente local antes de iniciar a aplicação:

   ```powershell
   $env:SPLITFY_JWT_SECRET = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
   ```

4. Na pasta `Splitfy/`, execute:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

O Flyway aplica as migrations de [`db/migration`](Splitfy/src/main/resources/db/migration/) na inicialização. A API fica em `http://localhost:8080/api`. Para rodar o teste existente, use `.\mvnw.cmd test` na mesma pasta; esse teste usa H2 e não substitui uma validação com PostgreSQL.

Abra o frontend em [http://localhost:8080/](http://localhost:8080/), com a aplicação em execução. Clique em **Entrar** e informe o e-mail e a senha de um usuário existente. A sessão fica nesta aba; sair ou expirar o token exige novo login. USER acessa a biblioteca e seu histórico; a administração de usuários e alterações do catálogo exigem ADMIN.

Os testes de autenticação da interface podem ser executados com Node.js: `node --test src/test/frontend/auth.test.cjs`, dentro de `Splitfy/`.

## Responsabilidades e estado atual

Cada integrante mantém sua seção abaixo. **"Banco disponível"** significa que a tabela já tem migration; **"API disponível"** significa que há código Java para atender requisições nesta branch. A presença da tabela, por si só, não indica que o CRUD esteja implementado.

### Luis Gabriel — segurança e histórico de reprodução

- **Banco disponível:** a migration `V10__create_historico_reproducao.sql` define usuário, música, data/hora e índice para consulta por usuário.
- **API disponível nesta branch:** `POST /api/musicas/{id}/play` recebe `{"usuarioId": 1}`, incrementa o contador da música e grava uma reprodução na mesma transação. `GET /api/usuarios/{usuarioId}/historico` lista as reproduções da mais recente para a mais antiga; o parâmetro opcional `limite` vai de 1 a 100, com padrão 20.
- **Pré-requisito:** usuário e música precisam existir no banco. Nesta etapa, o ID do usuário é informado na requisição. O histórico registra e consulta reproduções; não oferece atualização ou exclusão de registros.
- **Validação:** exemplos em [`historico-reproducao.http`](Splitfy/http/historico-reproducao.http). O histórico, o contador e o desfazimento conjunto em caso de falha foram verificados em PostgreSQL 17.4 temporário.
- **Segurança:** `POST /api/auth/login` recebe `{"email":"usuario@exemplo.com","senha":"suaSenha"}` e devolve `tokenAcesso`, `tipo` (`Bearer`) e `expiraEmSegundos` (3600). Envie `Authorization: Bearer <tokenAcesso>` nas demais chamadas da API. O token é assinado com `SPLITFY_JWT_SECRET`; não há refresh token.
- **Senhas e papéis:** novos cadastros e alterações de senha usam BCrypt. Senhas antigas em texto puro são convertidas para BCrypt no primeiro login correto. `POST /api/usuarios` permanece público para cadastrar usuários USER; somente um ADMIN autenticado pode atribuir ADMIN. Para preparar o primeiro administrador em um banco novo, cadastre um usuário e promova-o diretamente no banco com `UPDATE usuario SET role = 'ADMIN' WHERE email = 'usuario@exemplo.com';`, depois faça login novamente.
- **Autorização:** listar, consultar, atualizar e ativar/desativar usuários, além de alterar o catálogo, exige ADMIN. Histórico e play aceitam o próprio usuário ou ADMIN. Os demais endpoints da API exigem um token válido. Alterações de role ou desativação passam a valer para tokens já emitidos quando eles expirarem. O frontend envia o token obtido no login, trata a expiração da sessão e evita consultas administrativas para contas USER.
- **Validação:** exemplos em [`autenticacao.http`](Splitfy/http/autenticacao.http) e [`historico-reproducao.http`](Splitfy/http/historico-reproducao.http).

### Gabriel — gênero e música

- **Banco disponível:** migrations `V5__create_genero.sql` e `V6__create_musica.sql`.
- **API disponível nesta branch:** cadastro, consulta, atualização e exclusão de gêneros em `/api/generos` e de músicas em `/api/musicas`. A música possui busca por título, gênero e álbum. O play simulado fica em `/api/musicas/{id}/play` e recebe a integração com o histórico descrita na seção de Luis Gabriel.
- **Integração:** cadastrar uma música exige um álbum e um gênero existentes. A API valida essas referências antes de salvar.
- **Swagger:** com `SplitfyApplication` em execução, abra [Swagger UI](http://localhost:8080/swagger-ui.html). A documentação está limitada a gênero e música, incluindo filtros, exemplos JSON, respostas e registro de reprodução. As interfaces `GeneroControllerDoc` e `MusicaControllerDoc` seguem o padrão do exemplo fornecido; o springdoc 3.1.1 é compatível com Spring Boot 4.
- **Execução local no IntelliJ:** `application.properties` importa opcionalmente `application-local.properties`. Esse arquivo fica fora do Git e pode definir `splitfy.jwt.secret` com uma chave aleatória de pelo menos 32 bytes. A variável `SPLITFY_JWT_SECRET`, quando informada, tem prioridade na configuração local. Em outra máquina, configure a variável conforme as instruções acima ou crie seu próprio arquivo local; não publique a chave.
- **Autenticação no Swagger:** em **Select a definition**, selecione **seguranca-historico**. Em **Autenticação → POST /api/auth/login → Try it out**, informe o `email` e a `senha` de um usuário existente e clique em **Execute**. Na resposta 200, copie somente o valor de `tokenAcesso`, sem aspas. Volte para **generos-e-musicas**, abra **Authorize**, remova o valor antigo com **Logout** se houver e cole somente o token, sem `Bearer` e sem URL. Confirme com **Authorize → Close**. O endereço `/.well-known/oauth-protected-resource` é metadado da API, não um token. O login no site não autoriza automaticamente o Swagger. Consultas exigem USER ou ADMIN; cadastro, edição e exclusão exigem ADMIN. Em `/play`, USER informa seu próprio `usuarioId`; ADMIN pode informar outro usuário.
- **OpenAPI:** JSON do catálogo em `/v3/api-docs/generos-e-musicas`, autenticação/histórico em `/v3/api-docs/seguranca-historico` e documentação geral em `/v3/api-docs`. A configuração OpenAPI base é compartilhada; cada grupo delimita suas rotas. A interface e a documentação são públicas; os endpoints da API mantêm a autorização existente.
- **Validação:** `SwaggerIntegracaoTest` verifica as 12 operações, contratos, autenticação Bearer, abertura pública do Swagger e proteção da API. Execute `mvnw.cmd test` dentro da pasta `Splitfy`.

### Karen — artista e álbum

- **Banco disponível:** migrations `V3__create_artista.sql` e `V4__create_album.sql`.
- **API disponível:** CRUD de artistas em `/api/artistas` e de álbuns em `/api/albuns`. Também estão disponíveis `/api/artistas/{id}/albuns` e `/api/albuns/{id}/musicas`.
- **Regras:** um artista com álbuns e um álbum com músicas não podem ser excluídos antes de suas dependências.

### Airon — usuário e perfil

- **Banco disponível:** migrations `V1__create_usuario.sql` e `V2__create_perfil.sql`. A tabela `usuario` já permite a referência usada pelo histórico.
- **API disponível:** cadastro, consulta, atualização e desativação de usuários em `/api/usuarios`. A reativação usa `PATCH /api/usuarios/{id}/ativar`.
- **Perfil:** CRUD em `/api/usuarios/{usuarioId}/perfil`, com um perfil por usuário.

### Victor — playlist e avaliação/curtida

- **Banco disponível:** migrations `V7__create_playlist.sql`, `V8__create_playlist_musica.sql` e `V9__create_avaliacao.sql`.
- **API disponível:** CRUD de playlists em `/api/playlists`, listagem por usuário em `/api/usuarios/{usuarioId}/playlists` e inclusão/remoção de músicas em `/api/playlists/{id}/musicas`.
- **Avaliação/curtida:** operações em `/api/avaliacoes` e contagem de curtidas em `/api/avaliacoes/musicas/{musicaId}/curtidas`.

## Como manter este README

Ao integrar uma parte nova, o responsável atualiza sua própria seção com o estado real do banco e da API, os endpoints disponíveis, as dependências de outros módulos e um exemplo de validação. Se mudar o modo de executar a aplicação, atualize também **Como executar**. Documente como concluído apenas o comportamento já presente e validado no código integrado.
