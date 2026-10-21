# Spotify, YouTube e MP3

Em **Músicas → Nova música**, busque pelo nome da faixa e cantor, escolha o
resultado e confira os dados antes de salvar. É possível selecionar um resultado
de cada serviço para a mesma faixa. A primeira seleção preenche título, duração,
artista, álbum e capa; confirme o nome do cantor, pois o YouTube informa o canal.
O álbum e o artista podem ser criados durante o cadastro ou selecionados entre
os já existentes. Se o gênero não for conhecido, selecione Sem classificação.

A ordem automática é YouTube → Spotify → MP3. A fonte preferida pode ser alterada.
Erros de carregamento, vídeos indisponíveis e bloqueios de incorporação reportados
pelo player acionam a próxima fonte cadastrada. O botão **Tentar outra fonte**
permite trocar quando o serviço não informa um erro ou limita a reprodução.

Os players são os oficiais: as músicas não são baixadas dos serviços nem copiadas
para o banco. O YouTube Music usa os IDs de vídeos no player oficial do YouTube,
que permanece visível. O Spotify Embed pode oferecer prévia, exigir login ou
limitar a reprodução conforme a conta, região e disponibilidade. O navegador
pode exigir um clique no Play. O controle de volume Spotify pertence ao embed.

## Ativar a busca por nome

As credenciais ficam somente no servidor, em **Vercel → Splitfy → Settings →
Environment Variables**, ambiente **Production**. Não as coloque no frontend,
no Git ou em mensagens. Depois de salvar as variáveis, faça uma nova implantação
para que o container as receba. Basta configurar um serviço para começar.

### YouTube

1. Em https://console.cloud.google.com/, crie ou selecione um projeto.
2. Em APIs e serviços → Biblioteca, habilite **YouTube Data API v3**.
3. Em Credenciais → Criar credenciais → Chave de API, crie a chave.
4. Restrinja a chave à YouTube Data API v3. A busca é feita pelo backend;
   não use restrição por referenciador HTTP de navegador para esta chave.
5. Salve como `YOUTUBE_API_KEY` na Vercel e publique novamente.

A aplicação usa `search.list` com `type=video`, categoria Música e
`videoEmbeddable=true`, seguido de `videos.list` para consultar a duração.
As chamadas consomem a cota do projeto Google. Somente ADMIN pode pesquisar;
resultados bem-sucedidos ficam em cache por cinco minutos, até 100 consultas
por instância, para reduzir chamadas repetidas.

### Spotify

1. Abra https://developer.spotify.com/dashboard e crie um aplicativo com Web API.
2. A conta deve atender aos requisitos atuais do modo de desenvolvimento,
   incluindo a assinatura exigida pelo Spotify.
3. Salve `SPOTIFY_CLIENT_ID` e `SPOTIFY_CLIENT_SECRET` na Vercel e publique novamente.

A busca usa Client Credentials e `/v1/search?type=track`; não exige que cada
visitante conceda acesso à conta. O token é mantido no servidor até pouco antes
da expiração. Se as regras da conta impedirem a consulta, o serviço retorna
um aviso e mantém os resultados do YouTube disponíveis.

Sem credenciais, a interface explica a configuração pendente. Links diretos de
faixas continuam funcionando nos players, assim como o MP3 enviado ao banco.

## Imagens por link

Use **Editar álbum → Link direto da imagem da capa** ou **Editar perfil → Link
direto da foto**. O link deve ser HTTPS e apontar para a imagem, não para uma
página de busca, postagem ou compartilhamento. O editor apresenta uma prévia e
avisa se o endereço não carregar. O host precisa permitir que a imagem seja
exibida em outros sites. São aceitos links de até 2048 caracteres.

As capas aparecem no álbum, nas faixas, nas avaliações e no player. Falhas de
carregamento preservam a arte padrão. Não há proxy de imagens no servidor.

Referências oficiais:

- https://developer.spotify.com/documentation/embeds/references/iframe-api
- https://developer.spotify.com/documentation/web-api/tutorials/client-credentials-flow
- https://developers.google.com/youtube/iframe_api_reference
- https://developers.google.com/youtube/v3/getting-started
