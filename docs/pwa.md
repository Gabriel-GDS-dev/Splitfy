# Instalar o Splitfy

O Splitfy é um PWA servido em HTTPS. Abra https://splitfy-kappa.vercel.app/ e
use **Instalar Splitfy** no menu lateral. Quando o navegador permitir, o botão
abre a confirmação nativa; nos demais casos, mostra as instruções da plataforma.

- Android: Chrome ou outro navegador compatível → menu → Instalar aplicativo
  ou Adicionar à tela inicial.
- iPhone/iPad: Safari → Compartilhar → Adicionar à Tela de Início → Adicionar.
  Se aparecer Abrir como App da Web, mantenha ativado.
- Windows/Linux: Chrome ou Edge → ícone de instalação na barra de endereço ou
  menu → Instalar aplicativo.
- Mac: Chrome/Edge → instalação pelo navegador; Safari → Arquivo → Adicionar ao Dock.

Os nomes e a disponibilidade das opções variam conforme o navegador e sua versão.
O app usa ícones próprios, abre em modo standalone e oferece atalhos para Músicas
e Playlists nos sistemas que suportam atalhos de PWA. Não é um instalador APK/EXE.

## Conexão e atualizações

Após uma primeira abertura com internet, o service worker salva apenas os
arquivos públicos da interface. Sem conexão, o app pode abrir e mostra um aviso.
Catálogo, login e reprodução (YouTube, Spotify e MP3 no banco) precisam de internet.
Não há download offline de músicas.

Tokens, respostas da API, dados de contas, MP3s e pedidos a serviços externos
nunca são interceptados ou guardados pelo service worker. O cache usa uma lista
explícita de arquivos públicos. Com internet, consulta primeiro a rede para
evitar servir versões antigas do cadastro.

O service worker é entregue com Cache-Control no-cache/no-store. Quando houver
um novo worker aguardando, o menu apresenta **Atualização disponível**; o clique
ativa a atualização e recarrega o app. Ao alterar a lista do shell, incremente
o nome CACHE em `sw.js`. Caches antigos do Splitfy são removidos na ativação.

## Publicação pelo GitHub

As integrações de Spotify, YouTube e MP3 devem estar na branch `main` junto com
o frontend e as migrações. Uma implantação manual na Vercel não sincroniza o
código com o GitHub. Antes de publicar, combine as mudanças remotas, execute os
testes e envie os commits; publicar uma branch antiga pode remover os campos.

Os campos de fontes e envio de MP3 ficam em **Músicas → Nova música/Editar**,
para contas ADMIN. Contas USER podem ouvir, mas não alterar o catálogo.

Validação local: `./mvnw test` e `node --test src/test/frontend/*.test.cjs`,
dentro de `Splitfy/`. Os testes verificam os ícones, o manifesto, as regras de
cache e os fluxos de instalação, além da autenticação e das fontes musicais.
