# Splitfy

API de um sistema de música, desenvolvida em equipe como uma aplicação Spring Boot com um banco PostgreSQL. O projeto usa Spring Data JPA e Flyway. O código da aplicação está na pasta [`Splitfy/`](Splitfy/).

Este README descreve a versão integrada dos módulos da equipe. O Documento de Análise e o DER já foram entregues, conforme informado pela equipe. A segunda entrega foi alterada para conter somente o script de criação do banco, com prazo em 22/10/2026. As funcionalidades citadas abaixo podem continuar evoluindo sem antecipar etapas futuras.

## Como executar

Requisitos: Java 21 ou superior e PostgreSQL. O projeto inclui o Maven Wrapper, então não é necessário instalar o Maven separadamente.

1. Tenha um banco PostgreSQL chamado `Splitfy`, ou ajuste a URL de conexão para o nome do seu banco.
2. Confira usuário, senha e URL em [`application.properties`](Splitfy/src/main/resources/application.properties). Para usar uma senha local sem editar esse arquivo, defina `SPRING_DATASOURCE_PASSWORD` no ambiente. Também é possível definir `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_URL`.
3. Na pasta `Splitfy/`, execute:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

O Flyway aplica as migrations de [`db/migration`](Splitfy/src/main/resources/db/migration/) na inicialização. A API fica em `http://localhost:8080/api`. Para rodar o teste existente, use `.\mvnw.cmd test` na mesma pasta; esse teste usa H2 e não substitui uma validação com PostgreSQL.

## Responsabilidades e estado atual

Cada integrante mantém sua seção abaixo. **"Banco disponível"** significa que a tabela já tem migration; **"API disponível"** significa que há código Java para atender requisições nesta branch. A presença da tabela, por si só, não indica que o CRUD esteja implementado.

### Luis Gabriel — segurança e histórico de reprodução

- **Banco disponível:** a migration `V10__create_historico_reproducao.sql` define usuário, música, data/hora e índice para consulta por usuário.
- **API disponível nesta branch:** `POST /api/musicas/{id}/play` recebe `{"usuarioId": 1}`, incrementa o contador da música e grava uma reprodução na mesma transação. `GET /api/usuarios/{usuarioId}/historico` lista as reproduções da mais recente para a mais antiga; o parâmetro opcional `limite` vai de 1 a 100, com padrão 20.
- **Pré-requisito:** usuário e música precisam existir no banco. Nesta etapa, o ID do usuário é informado na requisição. O histórico registra e consulta reproduções; não oferece atualização ou exclusão de registros.
- **Validação:** exemplos em [`historico-reproducao.http`](Splitfy/http/historico-reproducao.http). O histórico, o contador e o desfazimento conjunto em caso de falha foram verificados em PostgreSQL 17.4 temporário.
- **Pendente nesta branch:** login com JWT, Spring Security e autorização USER/ADMIN.

### Gabriel — gênero e música

- **Banco disponível:** migrations `V5__create_genero.sql` e `V6__create_musica.sql`.
- **API disponível nesta branch:** cadastro, consulta, atualização e exclusão de gêneros em `/api/generos` e de músicas em `/api/musicas`. A música possui busca por título, gênero e álbum. O play simulado fica em `/api/musicas/{id}/play` e recebe a integração com o histórico descrita na seção de Luis Gabriel.
- **Integração:** cadastrar uma música exige um álbum e um gênero existentes. A API valida essas referências antes de salvar.
- **Para ampliar:** incluir exemplos de requisições e validações do catálogo quando esse trabalho for documentado.

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
