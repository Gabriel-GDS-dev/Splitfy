(() => {
    "use strict";

    // Altere para a porta/URL onde o seu servidor backend Spring Boot está executando
    const API_BASE_URL = "http://localhost:8080";

    const validRoutes = new Set([
        "inicio", "musicas", "artistas", "albuns", "generos",
        "playlists", "avaliacoes", "historico", "usuarios"
    ]);

    const state = {
        route: validRoutes.has(location.hash.slice(1)) ? location.hash.slice(1) : "inicio",
        query: "",
        activeUserId: Number(localStorage.getItem("splitfy.activeUser")) || null,
        artists: [],
        albums: [],
        genres: [],
        songs: [],
        users: [],
        profiles: new Map(),
        playlists: [],
        playlistTracks: new Map(),
        selectedPlaylistId: null,
        reviews: [],
        history: [],
        currentSongId: null,
        isPlaying: false,
        progressSeconds: 0,
        editor: null,
        confirmAction: null
    };

    const elements = {
        body: document.body,
        connectionDot: document.querySelector("#connection-dot"),
        connectionLabel: document.querySelector("#connection-label"),
        search: document.querySelector("#global-search"),
        activeUser: document.querySelector("#active-user"),
        contextCreate: document.querySelector("#context-create"),
        sidebarScrim: document.querySelector("#sidebar-scrim"),
        mobileMenu: document.querySelector("#mobile-menu"),
        heroStatus: document.querySelector("#hero-status"),
        metricStrip: document.querySelector("#metric-strip"),
        popularTracks: document.querySelector("#popular-tracks"),
        nowPlayingCard: document.querySelector("#now-playing-card"),
        featuredAlbums: document.querySelector("#featured-albums"),
        songsCount: document.querySelector("#songs-count"),
        songsGenreFilter: document.querySelector("#songs-genre-filter"),
        songsAlbumFilter: document.querySelector("#songs-album-filter"),
        songsTable: document.querySelector("#songs-table"),
        artistsCount: document.querySelector("#artists-count"),
        artistsGrid: document.querySelector("#artists-grid"),
        albumsCount: document.querySelector("#albums-count"),
        albumsGrid: document.querySelector("#albums-grid"),
        genresCount: document.querySelector("#genres-count"),
        genresGrid: document.querySelector("#genres-grid"),
        playlistsCount: document.querySelector("#playlists-count"),
        playlistsList: document.querySelector("#playlists-list"),
        playlistDetail: document.querySelector("#playlist-detail"),
        reviewsCount: document.querySelector("#reviews-count"),
        reviewsGrid: document.querySelector("#reviews-grid"),
        historyCount: document.querySelector("#history-count"),
        historyList: document.querySelector("#history-list"),
        usersCount: document.querySelector("#users-count"),
        usersTable: document.querySelector("#users-table"),
        playerTrack: document.querySelector("#player-track"),
        playerToggle: document.querySelector("#player-toggle"),
        playerPrevious: document.querySelector("#player-previous"),
        playerNext: document.querySelector("#player-next"),
        playerProgress: document.querySelector("#player-progress"),
        editorDialog: document.querySelector("#editor-dialog"),
        editorForm: document.querySelector("#editor-form"),
        dialogEyebrow: document.querySelector("#dialog-eyebrow"),
        dialogTitle: document.querySelector("#dialog-title"),
        dialogFields: document.querySelector("#dialog-fields"),
        dialogFeedback: document.querySelector("#dialog-feedback"),
        confirmDialog: document.querySelector("#confirm-dialog"),
        confirmTitle: document.querySelector("#confirm-title"),
        confirmMessage: document.querySelector("#confirm-message"),
        confirmAction: document.querySelector("#confirm-action"),
        toast: document.querySelector("#toast")
    };

    let toastTimer;

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function normalizeText(value) {
        return String(value ?? "")
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase();
    }

    function matchesSearch(...values) {
        if (!state.query) return true;
        return values.some((value) => normalizeText(value).includes(normalizeText(state.query)));
    }

    function formatDuration(seconds) {
        const total = Number(seconds) || 0;
        const minutes = Math.floor(total / 60);
        return `${minutes}:${String(total % 60).padStart(2, "0")}`;
    }

    function formatNumber(value) {
        return new Intl.NumberFormat("pt-BR").format(Number(value) || 0);
    }

    function formatDate(value, withTime = false) {
        if (!value) return "Não informado";
        if (!withTime && /^\d{4}-\d{2}-\d{2}$/.test(String(value))) {
            const [year, month, day] = String(value).split("-").map(Number);
            return new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium" })
                .format(new Date(year, month - 1, day));
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return String(value);
        return new Intl.DateTimeFormat("pt-BR", withTime
            ? { dateStyle: "short", timeStyle: "short" }
            : { dateStyle: "medium" }).format(date);
    }

    function coverClass(id) {
        return `art-${Math.abs(Number(id) || 0) % 4}`;
    }

    function unwrapList(payload) {
        if (Array.isArray(payload)) return payload;
        if (Array.isArray(payload?.content)) return payload.content;
        return [];
    }

    function extractError(data, status) {
        const fieldMap = data?.fields || data?.fieldErrors;
        if (fieldMap && typeof fieldMap === "object") {
            const details = Object.values(fieldMap).filter(Boolean).join(" ");
            if (details) return details;
        }
        if (Array.isArray(data?.errosDeCampo) && data.errosDeCampo.length) {
            return data.errosDeCampo.map((item) => item.mensagem).join(" ");
        }
        return data?.message || data?.mensagem || data?.detail || `A operação falhou (${status}).`;
    }

    async function request(path, { method = "GET", body, allow404 = false } = {}) {
        const cleanPath = path.startsWith("./") ? path.slice(1) : path;
        const fullPath = cleanPath.startsWith("/") ? cleanPath : `/${cleanPath}`;
        const targetUrl = `${API_BASE_URL}${fullPath}`;

        const options = { method, headers: { Accept: "application/json" } };
        if (body !== undefined) {
            options.headers["Content-Type"] = "application/json";
            options.body = JSON.stringify(body);
        }

        const response = await fetch(targetUrl, options);
        const raw = await response.text();
        let data = null;
        if (raw) {
            try {
                data = JSON.parse(raw);
            } catch {
                data = raw;
            }
        }

        if (allow404 && response.status === 404) return null;
        if (!response.ok) throw new Error(extractError(data, response.status));
        return data;
    }

    function setConnection(mode, label) {
        elements.connectionDot.classList.remove("is-online", "is-offline");
        if (mode) elements.connectionDot.classList.add(`is-${mode}`);
        elements.connectionLabel.textContent = label;
    }

    function refreshIcons() {
        window.lucide?.createIcons({ attrs: { "aria-hidden": "true" } });
    }

    function showToast(message) {
        clearTimeout(toastTimer);
        elements.toast.textContent = message;
        elements.toast.classList.add("is-visible");
        toastTimer = setTimeout(() => elements.toast.classList.remove("is-visible"), 3400);
    }

    function emptyState(icon, title, detail) {
        return `<div class="empty-state"><div><i data-lucide="${icon}"></i><strong>${escapeHtml(title)}</strong><span>${escapeHtml(detail)}</span></div></div>`;
    }

    function artistById(id) {
        return state.artists.find((item) => item.id === Number(id));
    }

    function albumById(id) {
        return state.albums.find((item) => item.id === Number(id));
    }

    function songById(id) {
        return state.songs.find((item) => item.id === Number(id));
    }

    function userById(id) {
        return state.users.find((item) => item.id === Number(id));
    }

    function playlistById(id) {
        return state.playlists.find((item) => item.id === Number(id));
    }

    function songSubtitle(song) {
        const album = albumById(song?.albumId);
        const artist = artistById(album?.artistaId);
        return [artist?.nome, album?.titulo].filter(Boolean).join(" · ") || "Catálogo Splitfy";
    }

    async function loadUserData() {
        if (!state.activeUserId) {
            state.reviews = [];
            state.history = [];
            return;
        }

        const [reviews, history] = await Promise.all([
            Promise.all(state.songs.map((song) =>
                request(`/api/avaliacoes/${state.activeUserId}/${song.id}`, { allow404: true })
            )),
            request(`/api/usuarios/${state.activeUserId}/historico?limite=100`)
        ]);
        state.reviews = reviews.filter(Boolean);
        state.history = unwrapList(history);
    }

    async function loadProfiles() {
        const entries = await Promise.all(state.users.map(async (user) => [
            user.id,
            await request(`/api/usuarios/${user.id}/perfil`, { allow404: true })
        ]));
        state.profiles = new Map(entries);
    }

    async function loadPlaylistTracks(playlistId) {
        if (!playlistId) return [];
        const tracks = unwrapList(await request(`/api/playlists/${playlistId}/musicas`));
        state.playlistTracks.set(Number(playlistId), tracks);
        return tracks;
    }

    async function loadAll({ quiet = false } = {}) {
        if (!quiet) setConnection(null, "Sincronizando");
        try {
            const [artists, albums, genres, songs, users, playlists] = await Promise.all([
                request("/api/artistas?size=200"),
                request("/api/albuns?size=200"),
                request("/api/generos"),
                request("/api/musicas"),
                request("/api/usuarios"),
                request("/api/playlists")
            ]);

            state.artists = unwrapList(artists);
            state.albums = unwrapList(albums);
            state.genres = unwrapList(genres);
            state.songs = unwrapList(songs);
            state.users = unwrapList(users);
            state.playlists = unwrapList(playlists);

            const activeUser = state.users.find((user) => user.id === state.activeUserId && user.ativo);
            if (!activeUser) state.activeUserId = state.users.find((user) => user.ativo)?.id ?? null;
            if (state.activeUserId) localStorage.setItem("splitfy.activeUser", String(state.activeUserId));
            else localStorage.removeItem("splitfy.activeUser");

            const selectedStillExists = state.playlists.some((playlist) => playlist.id === state.selectedPlaylistId);
            if (!selectedStillExists) {
                state.selectedPlaylistId = state.playlists.find((playlist) => playlist.usuarioId === state.activeUserId)?.id
                    ?? state.playlists[0]?.id
                    ?? null;
            }

            await Promise.all([
                loadUserData(),
                loadProfiles(),
                state.selectedPlaylistId ? loadPlaylistTracks(state.selectedPlaylistId) : Promise.resolve([])
            ]);

            setConnection("online", "API conectada");
            renderAll();
        } catch (error) {
            setConnection("offline", "API indisponível");
            renderAll();
            showToast(error.message || "Não foi possível conectar à API.");
        }
    }

    function renderUserSwitcher() {
        const options = state.users.map((user) =>
            `<option value="${user.id}" ${user.id === state.activeUserId ? "selected" : ""} ${!user.ativo ? "disabled" : ""}>${escapeHtml(user.nome)}${user.ativo ? "" : " (inativo)"}</option>`
        ).join("");
        elements.activeUser.innerHTML = `<option value="">Selecionar usuário</option>${options}`;
    }

    function renderFilters() {
        const genreValue = elements.songsGenreFilter.value;
        const albumValue = elements.songsAlbumFilter.value;
        elements.songsGenreFilter.innerHTML = `<option value="">Todos os gêneros</option>${state.genres.map((genre) =>
            `<option value="${genre.id}">${escapeHtml(genre.nome)}</option>`
        ).join("")}`;
        elements.songsAlbumFilter.innerHTML = `<option value="">Todos os álbuns</option>${state.albums.map((album) =>
            `<option value="${album.id}">${escapeHtml(album.titulo)}</option>`
        ).join("")}`;
        elements.songsGenreFilter.value = genreValue;
        elements.songsAlbumFilter.value = albumValue;
    }

    function actionButton(icon, title, action, id, className = "") {
        return `<button class="table-action ${className}" type="button" title="${escapeHtml(title)}" data-action="${action}" data-id="${id}"><i data-lucide="${icon}"></i></button>`;
    }

    function trackRow(song, index, extraActions = "") {
        const album = albumById(song.albumId);
        const artist = artistById(album?.artistaId);
        return `
            <div class="track-row">
                <span class="track-index">${String(index + 1).padStart(2, "0")}</span>
                <div class="track-cover cover-art ${coverClass(song.albumId || song.id)}"></div>
                <div class="track-title"><strong>${escapeHtml(song.titulo)}</strong><span>${escapeHtml(artist?.nome || "Artista não informado")}</span></div>
                <span class="track-meta track-album">${escapeHtml(album?.titulo || "Sem álbum")}</span>
                <span class="track-meta track-plays">${formatNumber(song.reproducoes)} plays</span>
                <span class="track-meta track-duration">${formatDuration(song.duracaoSegundos)}</span>
                <div class="track-actions">
                    ${actionButton("play", "Reproduzir", "play-song", song.id, "play-action")}
                    ${extraActions || `${actionButton("heart", "Avaliar", "review-song", song.id)}${actionButton("list-plus", "Adicionar à playlist", "add-song", song.id)}${actionButton("pencil", "Editar música", "edit-song", song.id)}${actionButton("trash-2", "Excluir música", "delete-song", song.id)}`}
                </div>
            </div>`;
    }

    function renderDashboard() {
        const activeUser = userById(state.activeUserId);
        elements.heroStatus.textContent = activeUser
            ? `${activeUser.nome}, explore seu catálogo, organize playlists e acompanhe suas reproduções.`
            : "Selecione um usuário para reproduzir músicas e registrar suas preferências.";

        const totalPlays = state.songs.reduce((sum, song) => sum + (Number(song.reproducoes) || 0), 0);
        const metrics = [
            [state.songs.length, "músicas"],
            [state.albums.length, "álbuns"],
            [state.artists.length, "artistas"],
            [totalPlays, "reproduções"],
            [state.playlists.length, "playlists"]
        ];
        elements.metricStrip.innerHTML = metrics.map(([value, label]) =>
            `<div class="metric"><strong>${formatNumber(value)}</strong><span>${label}</span></div>`
        ).join("");

        const popular = [...state.songs].sort((a, b) => (b.reproducoes || 0) - (a.reproducoes || 0)).slice(0, 5);
        elements.popularTracks.innerHTML = popular.length
            ? popular.map((song, index) => trackRow(song, index, actionButton("play", "Reproduzir", "play-song", song.id, "play-action"))).join("")
            : emptyState("music-2", "Nenhuma música cadastrada", "Adicione a primeira faixa ao catálogo.");

        const albums = state.albums.slice(0, 4);
        elements.featuredAlbums.innerHTML = albums.length
            ? albums.map(albumCard).join("")
            : emptyState("disc-3", "Nenhum álbum cadastrado", "Os álbuns aparecerão aqui.");
        renderPlayer();
    }

    function renderPlayer() {
        const song = songById(state.currentSongId);
        const hasSongs = state.songs.length > 0;
        elements.playerPrevious.disabled = !hasSongs;
        elements.playerNext.disabled = !hasSongs;
        elements.playerToggle.disabled = !song;

        if (!song) {
            elements.playerTrack.innerHTML = `<div class="player-cover cover-art art-0"></div><div><strong>Nenhuma música tocando</strong><span>Escolha uma faixa</span></div>`;
            elements.playerToggle.innerHTML = `<i data-lucide="play"></i>`;
            elements.playerProgress.style.width = "0";
            elements.nowPlayingCard.className = "now-playing-card empty-slot";
            elements.nowPlayingCard.innerHTML = `<div><i data-lucide="headphones"></i><strong>O player está livre</strong><span>Escolha uma faixa para começar.</span></div>`;
            refreshIcons();
            return;
        }

        const art = coverClass(song.albumId || song.id);
        elements.playerTrack.innerHTML = `<div class="player-cover cover-art ${art}"></div><div><strong>${escapeHtml(song.titulo)}</strong><span>${escapeHtml(songSubtitle(song))}</span></div>`;
        elements.playerToggle.innerHTML = `<i data-lucide="${state.isPlaying ? "pause" : "play"}"></i>`;
        elements.playerToggle.title = state.isPlaying ? "Pausar" : "Reproduzir";
        const progress = song.duracaoSegundos ? Math.min(100, (state.progressSeconds / song.duracaoSegundos) * 100) : 0;
        elements.playerProgress.style.width = `${progress}%`;
        elements.nowPlayingCard.className = "now-playing-card";
        elements.nowPlayingCard.innerHTML = `
            <div class="now-cover cover-art ${art}"></div>
            <div class="now-playing-info"><strong>${escapeHtml(song.titulo)}</strong><span>${escapeHtml(songSubtitle(song))}</span><span>${formatNumber(song.reproducoes)} reproduções</span></div>`;
        refreshIcons();
    }

    function renderSongs() {
        const genreId = Number(elements.songsGenreFilter.value) || null;
        const albumId = Number(elements.songsAlbumFilter.value) || null;
        const songs = state.songs.filter((song) => {
            const album = albumById(song.albumId);
            return (!genreId || song.generoId === genreId)
                && (!albumId || song.albumId === albumId)
                && matchesSearch(song.titulo, song.generoNome, album?.titulo, artistById(album?.artistaId)?.nome);
        });
        elements.songsCount.textContent = `${songs.length} ${songs.length === 1 ? "faixa encontrada" : "faixas encontradas"}`;
        elements.songsTable.innerHTML = songs.length
            ? songs.map((song, index) => trackRow(song, index)).join("")
            : emptyState("search-x", "Nenhuma faixa encontrada", "Ajuste a busca ou os filtros do catálogo.");
        refreshIcons();
    }

    function artistCard(artist) {
        const albums = state.albums.filter((album) => album.artistaId === artist.id).length;
        return `
            <article class="artist-card">
                <div class="artist-avatar cover-art ${coverClass(artist.id)}"></div>
                <div class="artist-info">
                    <strong>${escapeHtml(artist.nome)}</strong>
                    <span>${escapeHtml(artist.pais || "Origem não informada")}</span>
                    <span>${albums} ${albums === 1 ? "álbum" : "álbuns"}</span>
                    <div class="card-actions">${actionButton("pencil", "Editar artista", "edit-artist", artist.id)}${actionButton("trash-2", "Excluir artista", "delete-artist", artist.id)}</div>
                </div>
            </article>`;
    }

    function renderArtists() {
        const artists = state.artists.filter((artist) => matchesSearch(artist.nome, artist.pais, artist.biografia));
        elements.artistsCount.textContent = `${artists.length} ${artists.length === 1 ? "artista" : "artistas"}`;
        elements.artistsGrid.innerHTML = artists.length
            ? artists.map(artistCard).join("")
            : emptyState("mic-2", "Nenhum artista encontrado", "Cadastre um artista ou ajuste a busca.");
        refreshIcons();
    }

    function albumCard(album) {
        const tracks = state.songs.filter((song) => song.albumId === album.id).length;
        return `
            <article class="album-card">
                <div class="album-cover cover-art ${coverClass(album.id)}"></div>
                <div class="album-info">
                    <strong>${escapeHtml(album.titulo)}</strong>
                    <span>${escapeHtml(album.artistaNome || artistById(album.artistaId)?.nome || "Artista não informado")}</span>
                    <span>${album.anoLancamento || "Ano não informado"} · ${tracks} ${tracks === 1 ? "faixa" : "faixas"}</span>
                    <div class="card-actions">${actionButton("pencil", "Editar álbum", "edit-album", album.id)}${actionButton("trash-2", "Excluir álbum", "delete-album", album.id)}</div>
                </div>
            </article>`;
    }

    function renderAlbums() {
        const albums = state.albums.filter((album) => matchesSearch(album.titulo, album.artistaNome, album.anoLancamento));
        elements.albumsCount.textContent = `${albums.length} ${albums.length === 1 ? "álbum" : "álbuns"}`;
        elements.albumsGrid.innerHTML = albums.length
            ? albums.map(albumCard).join("")
            : emptyState("disc-3", "Nenhum álbum encontrado", "Cadastre um álbum ou ajuste a busca.");
        refreshIcons();
    }

    function renderGenres() {
        const accents = ["var(--green)", "var(--coral)", "var(--cyan)", "var(--yellow)"];
        const genres = state.genres.filter((genre) => matchesSearch(genre.nome, genre.descricao));
        elements.genresCount.textContent = `${genres.length} ${genres.length === 1 ? "gênero" : "gêneros"}`;
        elements.genresGrid.innerHTML = genres.length ? genres.map((genre, index) => {
            const songs = state.songs.filter((song) => song.generoId === genre.id).length;
            return `
                <article class="genre-card" style="--genre-accent: ${accents[index % accents.length]}">
                    <div><span class="eyebrow">Gênero</span><h3>${escapeHtml(genre.nome)}</h3></div>
                    <p>${escapeHtml(genre.descricao || "Sem descrição cadastrada.")}</p>
                    <div class="genre-meta"><span class="count-badge">${songs} ${songs === 1 ? "música" : "músicas"}</span><div class="card-actions">${actionButton("pencil", "Editar gênero", "edit-genre", genre.id)}${actionButton("trash-2", "Excluir gênero", "delete-genre", genre.id)}</div></div>
                </article>`;
        }).join("") : emptyState("radio-tower", "Nenhum gênero encontrado", "Cadastre um gênero ou ajuste a busca.");
        refreshIcons();
    }

    function renderPlaylists() {
        const playlists = state.playlists.filter((playlist) => matchesSearch(playlist.nome, playlist.descricao, userById(playlist.usuarioId)?.nome));
        elements.playlistsCount.textContent = `${playlists.length} ${playlists.length === 1 ? "playlist" : "playlists"}`;
        elements.playlistsList.innerHTML = playlists.length ? playlists.map((playlist) => `
            <button class="playlist-card ${playlist.id === state.selectedPlaylistId ? "is-selected" : ""}" type="button" data-action="select-playlist" data-id="${playlist.id}">
                <div class="playlist-cover cover-art ${coverClass(playlist.id)}"></div>
                <span class="playlist-card-info"><strong>${escapeHtml(playlist.nome)}</strong><span>${escapeHtml(userById(playlist.usuarioId)?.nome || "Usuário")}</span></span>
                <i data-lucide="chevron-right"></i>
            </button>`).join("") : emptyState("list-music", "Nenhuma playlist encontrada", "Crie uma coleção para organizar suas músicas.");
        renderPlaylistDetail();
        refreshIcons();
    }

    function renderPlaylistDetail() {
        const playlist = playlistById(state.selectedPlaylistId);
        if (!playlist) {
            elements.playlistDetail.innerHTML = emptyState("list-music", "Selecione uma playlist", "Os detalhes e as faixas aparecerão aqui.");
            return;
        }
        const tracks = state.playlistTracks.get(playlist.id) || [];
        const trackRows = tracks.map((entry, index) => {
            const song = songById(entry.musicaId) || { id: entry.musicaId, titulo: entry.musicaTitulo, duracaoSegundos: 0, reproducoes: 0 };
            const actions = `${actionButton("play", "Reproduzir", "play-song", song.id, "play-action")}${actionButton("list-x", "Remover da playlist", "remove-track", song.id)}`;
            return trackRow(song, index, actions);
        }).join("");
        elements.playlistDetail.innerHTML = `
            <header class="playlist-detail-header">
                <div class="playlist-detail-cover cover-art ${coverClass(playlist.id)}"></div>
                <div class="playlist-detail-copy">
                    <span class="eyebrow">${playlist.publica ? "Playlist pública" : "Playlist privada"}</span>
                    <h2>${escapeHtml(playlist.nome)}</h2>
                    <p>${escapeHtml(playlist.descricao || "Sem descrição.")}</p>
                    <p>${escapeHtml(userById(playlist.usuarioId)?.nome || "Usuário")} · ${tracks.length} ${tracks.length === 1 ? "faixa" : "faixas"}</p>
                    <div class="playlist-detail-actions">
                        <button class="primary-button" type="button" data-action="add-track" data-id="${playlist.id}"><i data-lucide="list-plus"></i><span>Adicionar faixa</span></button>
                        <button class="secondary-button" type="button" data-action="edit-playlist" data-id="${playlist.id}"><i data-lucide="pencil"></i><span>Editar</span></button>
                        <button class="secondary-button" type="button" data-action="delete-playlist" data-id="${playlist.id}"><i data-lucide="trash-2"></i><span>Excluir</span></button>
                    </div>
                </div>
            </header>
            <div class="track-list">${trackRows || emptyState("music-2", "Playlist vazia", "Adicione faixas para começar esta coleção.")}</div>`;
    }

    function ratingMarkup(note) {
        const value = Number(note) || 0;
        return `<span class="rating" aria-label="${value} de 5 estrelas">${Array.from({ length: 5 }, (_, index) => `<i data-lucide="star" ${index < value ? 'fill="currentColor"' : ""}></i>`).join("")}</span>`;
    }

    function renderReviews() {
        if (!state.activeUserId) {
            elements.reviewsCount.textContent = "Selecione um usuário ativo";
            elements.reviewsGrid.innerHTML = emptyState("user-round", "Usuário não selecionado", "Escolha um usuário no topo para ver avaliações.");
            refreshIcons();
            return;
        }
        const reviews = state.reviews.filter((review) => {
            const song = songById(review.musicaId);
            return matchesSearch(song?.titulo, songSubtitle(song), review.nota);
        });
        elements.reviewsCount.textContent = `${reviews.length} ${reviews.length === 1 ? "avaliação" : "avaliações"} de ${userById(state.activeUserId)?.nome || "usuário"}`;
        elements.reviewsGrid.innerHTML = reviews.length ? reviews.map((review) => {
            const song = songById(review.musicaId);
            return `
                <article class="review-card">
                    <div class="review-cover cover-art ${coverClass(song?.albumId || review.musicaId)}"></div>
                    <div class="review-info">
                        <strong>${escapeHtml(song?.titulo || `Música #${review.musicaId}`)}</strong>
                        <span>${escapeHtml(songSubtitle(song))}</span>
                        ${ratingMarkup(review.nota)}
                        <span>${review.curtido ? "Curtida" : "Sem curtida"}</span>
                        <div class="card-actions">${actionButton("pencil", "Editar avaliação", "edit-review", review.musicaId)}${actionButton("trash-2", "Excluir avaliação", "delete-review", review.musicaId)}</div>
                    </div>
                </article>`;
        }).join("") : emptyState("heart", "Nenhuma avaliação", "Avalie uma música para registrar sua preferência.");
        refreshIcons();
    }

    function renderHistory() {
        if (!state.activeUserId) {
            elements.historyCount.textContent = "Selecione um usuário ativo";
            elements.historyList.innerHTML = emptyState("user-round", "Usuário não selecionado", "Escolha um usuário para consultar o histórico.");
            refreshIcons();
            return;
        }
        const history = state.history.filter((entry) => {
            const song = songById(entry.musicaId);
            return matchesSearch(song?.titulo, songSubtitle(song));
        });
        elements.historyCount.textContent = `${history.length} ${history.length === 1 ? "reprodução" : "reproduções"} recentes`;
        elements.historyList.innerHTML = history.length ? history.map((entry) => {
            const song = songById(entry.musicaId);
            return `
                <article class="timeline-item">
                    <span class="timeline-dot"><i data-lucide="play"></i></span>
                    <div class="timeline-copy"><strong>${escapeHtml(song?.titulo || `Música #${entry.musicaId}`)}</strong><span>${escapeHtml(songSubtitle(song))}</span></div>
                    <time class="timeline-time">${formatDate(entry.reproduzidoEm, true)}</time>
                </article>`;
        }).join("") : emptyState("history", "Histórico vazio", "As reproduções deste usuário aparecerão aqui.");
        refreshIcons();
    }

    function renderUsers() {
        const users = state.users.filter((user) => {
            const profile = state.profiles.get(user.id);
            return matchesSearch(user.nome, user.email, user.role, profile?.nomeExibicao);
        });
        elements.usersCount.textContent = `${users.length} ${users.length === 1 ? "usuário" : "usuários"}`;
        const header = `<div class="table-row table-head users-row"><span>Usuário</span><span class="user-email">E-mail</span><span>Perfil</span><span class="user-birth">Nascimento</span><span>Status</span><span>Ações</span></div>`;
        elements.usersTable.innerHTML = users.length ? header + users.map((user) => {
            const profile = state.profiles.get(user.id);
            return `
                <div class="table-row users-row">
                    <div class="table-primary"><strong>${escapeHtml(profile?.nomeExibicao || user.nome)}</strong><span>${escapeHtml(user.nome)}</span></div>
                    <span class="table-cell-muted user-email">${escapeHtml(user.email)}</span>
                    <span class="role-badge">${escapeHtml(user.role)}</span>
                    <span class="table-cell-muted user-birth">${formatDate(user.dataNascimento)}</span>
                    <span class="status-badge ${user.ativo ? "is-active" : "is-inactive"}">${user.ativo ? "Ativo" : "Inativo"}</span>
                    <div class="table-actions">
                        ${actionButton("contact-round", profile ? "Editar perfil" : "Criar perfil", "profile-user", user.id)}
                        ${actionButton("pencil", "Editar usuário", "edit-user", user.id)}
                        ${actionButton(user.ativo ? "user-x" : "user-check", user.ativo ? "Desativar usuário" : "Ativar usuário", "toggle-user", user.id)}
                    </div>
                </div>`;
        }).join("") : emptyState("users", "Nenhum usuário encontrado", "Cadastre um usuário ou ajuste a busca.");
        refreshIcons();
    }

    function renderAll() {
        renderUserSwitcher();
        renderFilters();
        renderDashboard();
        renderSongs();
        renderArtists();
        renderAlbums();
        renderGenres();
        renderPlaylists();
        renderReviews();
        renderHistory();
        renderUsers();
        setView(state.route, { updateHash: false });
        refreshIcons();
    }

    function renderCurrentView() {
        const renderers = {
            inicio: renderDashboard,
            musicas: renderSongs,
            artistas: renderArtists,
            albuns: renderAlbums,
            generos: renderGenres,
            playlists: renderPlaylists,
            avaliacoes: renderReviews,
            historico: renderHistory,
            usuarios: renderUsers
        };
        renderers[state.route]?.();
    }

    function setView(route, { updateHash = true } = {}) {
        if (!validRoutes.has(route)) route = "inicio";
        state.route = route;
        document.querySelectorAll("[data-view]").forEach((view) => {
            const active = view.dataset.view === route;
            view.hidden = !active;
            view.classList.toggle("is-active", active);
        });
        document.querySelectorAll(".nav-item[data-route]").forEach((item) => item.classList.toggle("is-active", item.dataset.route === route));
        elements.body.classList.remove("sidebar-open");
        if (updateHash && location.hash !== `#${route}`) history.pushState(null, "", `#${route}`);

        const createMap = {
            inicio: ["musica", "Nova música"],
            musicas: ["musica", "Nova música"],
            artistas: ["artista", "Novo artista"],
            albuns: ["album", "Novo álbum"],
            generos: ["genero", "Novo gênero"],
            playlists: ["playlist", "Nova playlist"],
            avaliacoes: ["avaliacao", "Nova avaliação"],
            usuarios: ["usuario", "Novo usuário"]
        };
        const action = createMap[route];
        elements.contextCreate.hidden = !action;
        if (action) {
            elements.contextCreate.dataset.create = action[0];
            elements.contextCreate.querySelector("span").textContent = action[1];
        }
        document.title = route === "inicio" ? "Splitfy" : `${route[0].toUpperCase()}${route.slice(1)} · Splitfy`;
    }

    function inputField(name, label, value = "", { type = "text", required = false, min, max, wide = false, placeholder = "", disabled = false } = {}) {
        return `<div class="form-field ${wide ? "is-wide" : ""}"><label for="field-${name}">${escapeHtml(label)}</label><input id="field-${name}" name="${name}" type="${type}" value="${escapeHtml(value)}" ${required ? "required" : ""} ${min !== undefined ? `min="${min}"` : ""} ${max !== undefined ? `max="${max}"` : ""} ${placeholder ? `placeholder="${escapeHtml(placeholder)}"` : ""} ${disabled ? "disabled" : ""}></div>`;
    }

    function textareaField(name, label, value = "", { required = false, wide = true } = {}) {
        return `<div class="form-field ${wide ? "is-wide" : ""}"><label for="field-${name}">${escapeHtml(label)}</label><textarea id="field-${name}" name="${name}" ${required ? "required" : ""}>${escapeHtml(value)}</textarea></div>`;
    }

    function selectField(name, label, options, value, { required = true, disabled = false, wide = false } = {}) {
        return `<div class="form-field ${wide ? "is-wide" : ""}"><label for="field-${name}">${escapeHtml(label)}</label><select id="field-${name}" name="${name}" ${required ? "required" : ""} ${disabled ? "disabled" : ""}><option value="">Selecione</option>${options.map((option) => `<option value="${option.value}" ${String(option.value) === String(value ?? "") ? "selected" : ""}>${escapeHtml(option.label)}</option>`).join("")}</select></div>`;
    }

    function toggleField(name, label, checked = false) {
        return `<label class="toggle-field"><span>${escapeHtml(label)}</span><input name="${name}" type="checkbox" ${checked ? "checked" : ""}></label>`;
    }

    function optionList(items, label = "nome") {
        return items.map((item) => ({ value: item.id, label: item[label] }));
    }

    function openEditor(kind, item = null, context = {}) {
        const isEdit = Boolean(item);
        const activeUser = state.activeUserId || state.users.find((user) => user.ativo)?.id;
        let title = "Novo item";
        let eyebrow = isEdit ? "Editar cadastro" : "Novo cadastro";
        let fields = "";

        if (kind === "artista") {
            title = isEdit ? "Editar artista" : "Novo artista";
            fields = inputField("nome", "Nome", item?.nome, { required: true, wide: true })
                + inputField("pais", "País", item?.pais)
                + inputField("inicioCarreira", "Início da carreira", item?.inicioCarreira, { type: "date", max: new Date().toISOString().slice(0, 10) })
                + textareaField("biografia", "Biografia", item?.biografia);
        } else if (kind === "album") {
            if (!state.artists.length) return showToast("Cadastre um artista antes de criar um álbum.");
            title = isEdit ? "Editar álbum" : "Novo álbum";
            fields = inputField("titulo", "Título", item?.titulo, { required: true, wide: true })
                + selectField("artistaId", "Artista", optionList(state.artists), item?.artistaId)
                + inputField("anoLancamento", "Ano de lançamento", item?.anoLancamento, { type: "number", min: 1900, max: new Date().getFullYear() })
                + inputField("capaUrl", "URL alternativa da capa", item?.capaUrl, { type: "url", wide: true, placeholder: "https://" });
        } else if (kind === "genero") {
            title = isEdit ? "Editar gênero" : "Novo gênero";
            fields = inputField("nome", "Nome", item?.nome, { required: true, wide: true })
                + textareaField("descricao", "Descrição", item?.descricao);
        } else if (kind === "musica") {
            if (!state.albums.length || !state.genres.length) return showToast("Cadastre ao menos um álbum e um gênero antes da música.");
            title = isEdit ? "Editar música" : "Nova música";
            fields = inputField("titulo", "Título", item?.titulo, { required: true, wide: true })
                + selectField("albumId", "Álbum", optionList(state.albums, "titulo"), item?.albumId)
                + selectField("generoId", "Gênero", optionList(state.genres), item?.generoId)
                + inputField("duracaoSegundos", "Duração em segundos", item?.duracaoSegundos, { type: "number", min: 1, required: true })
                + inputField("numeroFaixa", "Número da faixa", item?.numeroFaixa, { type: "number", min: 1 });
        } else if (kind === "usuario") {
            title = isEdit ? "Editar usuário" : "Novo usuário";
            fields = inputField("nome", "Nome", item?.nome, { required: true, wide: true })
                + inputField("email", "E-mail", item?.email, { type: "email", required: true, wide: true })
                + inputField("senha", isEdit ? "Nova senha (opcional)" : "Senha", "", { type: "password", required: !isEdit, min: 6 })
                + inputField("dataNascimento", "Data de nascimento", item?.dataNascimento, { type: "date", required: true })
                + selectField("role", "Permissão", [{ value: "USER", label: "Usuário" }, { value: "ADMIN", label: "Administrador" }], item?.role || "USER");
        } else if (kind === "perfil") {
            const user = userById(context.userId);
            title = item ? `Editar perfil de ${user?.nome || "usuário"}` : `Criar perfil de ${user?.nome || "usuário"}`;
            fields = inputField("nomeExibicao", "Nome de exibição", item?.nomeExibicao, { required: true, wide: true })
                + inputField("fotoUrl", "URL da foto", item?.fotoUrl, { type: "url", wide: true, placeholder: "https://" })
                + textareaField("bio", "Biografia", item?.bio);
        } else if (kind === "playlist") {
            if (!state.users.length) return showToast("Cadastre um usuário antes de criar uma playlist.");
            title = isEdit ? "Editar playlist" : "Nova playlist";
            fields = inputField("nome", "Nome", item?.nome, { required: true, wide: true })
                + selectField("usuarioId", "Responsável", optionList(state.users), item?.usuarioId || activeUser, { disabled: isEdit })
                + toggleField("publica", "Playlist pública", item?.publica)
                + textareaField("descricao", "Descrição", item?.descricao);
        } else if (kind === "avaliacao") {
            if (!state.users.length || !state.songs.length) return showToast("É necessário ter usuários e músicas cadastrados.");
            const songId = item?.musicaId || context.songId;
            title = isEdit ? "Editar avaliação" : "Nova avaliação";
            fields = selectField("usuarioId", "Usuário", optionList(state.users), item?.usuarioId || activeUser, { disabled: isEdit })
                + selectField("musicaId", "Música", optionList(state.songs, "titulo"), songId, { disabled: isEdit })
                + selectField("nota", "Nota", [1, 2, 3, 4, 5].map((value) => ({ value, label: `${value} ${value === 1 ? "estrela" : "estrelas"}` })), item?.nota, { required: false })
                + toggleField("curtido", "Marcar como curtida", item?.curtido);
        } else if (kind === "playlist-track") {
            const playlist = playlistById(context.playlistId);
            const existing = new Set((state.playlistTracks.get(playlist?.id) || []).map((entry) => entry.musicaId));
            const available = state.songs.filter((song) => !existing.has(song.id));
            if (!available.length) return showToast("Todas as músicas já estão nesta playlist.");
            title = `Adicionar em ${playlist?.nome || "playlist"}`;
            eyebrow = "Organizar playlist";
            fields = selectField("musicaId", "Música", optionList(available, "titulo"), context.songId, { wide: true });
        }

        state.editor = { kind, item, context };
        elements.dialogEyebrow.textContent = eyebrow;
        elements.dialogTitle.textContent = title;
        elements.dialogFields.innerHTML = fields;
        elements.dialogFeedback.textContent = "";
        elements.editorDialog.showModal();
        refreshIcons();
        elements.dialogFields.querySelector("input:not([type=checkbox]), select:not([disabled]), textarea")?.focus();
    }

    function formNumber(formData, name) {
        const value = formData.get(name);
        return value === null || value === "" ? null : Number(value);
    }

    async function submitEditor(event) {
        event.preventDefault();
        if (!state.editor) return;
        const submit = elements.editorForm.querySelector('[type="submit"]');
        submit.disabled = true;
        elements.dialogFeedback.textContent = "";
        const data = new FormData(elements.editorForm);
        const { kind, item, context } = state.editor;

        try {
            if (kind === "artista") {
                const body = { nome: data.get("nome"), pais: data.get("pais") || null, inicioCarreira: data.get("inicioCarreira") || null, biografia: data.get("biografia") || null };
                await request(`/api/artistas${item ? `/${item.id}` : ""}`, { method: item ? "PUT" : "POST", body });
            } else if (kind === "album") {
                const body = { titulo: data.get("titulo"), artistaId: formNumber(data, "artistaId"), anoLancamento: formNumber(data, "anoLancamento"), capaUrl: data.get("capaUrl") || null };
                await request(`/api/albuns${item ? `/${item.id}` : ""}`, { method: item ? "PUT" : "POST", body });
            } else if (kind === "genero") {
                const body = { nome: data.get("nome"), descricao: data.get("descricao") || null };
                await request(`/api/generos${item ? `/${item.id}` : ""}`, { method: item ? "PUT" : "POST", body });
            } else if (kind === "musica") {
                const body = { titulo: data.get("titulo"), albumId: formNumber(data, "albumId"), generoId: formNumber(data, "generoId"), duracaoSegundos: formNumber(data, "duracaoSegundos"), numeroFaixa: formNumber(data, "numeroFaixa") };
                await request(`/api/musicas${item ? `/${item.id}` : ""}`, { method: item ? "PUT" : "POST", body });
            } else if (kind === "usuario") {
                const body = { nome: data.get("nome"), email: data.get("email"), senha: data.get("senha") || null, dataNascimento: data.get("dataNascimento"), role: data.get("role") };
                await request(`/api/usuarios${item ? `/${item.id}` : ""}`, { method: item ? "PUT" : "POST", body });
            } else if (kind === "perfil") {
                const body = { nomeExibicao: data.get("nomeExibicao"), fotoUrl: data.get("fotoUrl") || null, bio: data.get("bio") || null };
                await request(`/api/usuarios/${context.userId}/perfil`, { method: item ? "PUT" : "POST", body });
            } else if (kind === "playlist") {
                const body = { nome: data.get("nome"), descricao: data.get("descricao") || null, publica: data.get("publica") === "on", usuarioId: item?.usuarioId || formNumber(data, "usuarioId") };
                await request(`/api/playlists${item ? `/${item.id}` : ""}`, { method: item ? "PUT" : "POST", body });
            } else if (kind === "avaliacao") {
                const body = { usuarioId: item?.usuarioId || formNumber(data, "usuarioId"), musicaId: item?.musicaId || formNumber(data, "musicaId"), nota: formNumber(data, "nota"), curtido: data.get("curtido") === "on" };
                const path = item ? `/api/avaliacoes/${item.usuarioId}/${item.musicaId}` : "/api/avaliacoes";
                await request(path, { method: item ? "PUT" : "POST", body });
                if (!item && body.usuarioId !== state.activeUserId) {
                    state.activeUserId = body.usuarioId;
                    localStorage.setItem("splitfy.activeUser", String(body.usuarioId));
                }
            } else if (kind === "playlist-track") {
                const playlist = playlistById(context.playlistId);
                await request(`/api/playlists/${playlist.id}/musicas`, { method: "POST", body: { musicaId: formNumber(data, "musicaId"), usuarioId: playlist.usuarioId } });
            }

            elements.editorDialog.close();
            await loadAll({ quiet: true });
            showToast(item ? "Alterações salvas com sucesso." : "Cadastro realizado com sucesso.");
        } catch (error) {
            elements.dialogFeedback.textContent = error.message;
        } finally {
            submit.disabled = false;
        }
    }

    function askConfirmation(title, message, action) {
        elements.confirmTitle.textContent = title;
        elements.confirmMessage.textContent = message;
        state.confirmAction = action;
        elements.confirmDialog.showModal();
        refreshIcons();
    }

    async function runConfirmedAction() {
        if (!state.confirmAction) return;
        elements.confirmAction.disabled = true;
        try {
            await state.confirmAction();
            elements.confirmDialog.close();
            state.confirmAction = null;
            await loadAll({ quiet: true });
            showToast("Ação concluída com sucesso.");
        } catch (error) {
            showToast(error.message);
        } finally {
            elements.confirmAction.disabled = false;
        }
    }

    async function selectPlaylist(id) {
        state.selectedPlaylistId = Number(id);
        try {
            await loadPlaylistTracks(state.selectedPlaylistId);
            renderPlaylists();
        } catch (error) {
            showToast(error.message);
        }
    }

    async function playSong(id) {
        const song = songById(id);
        const activeUser = userById(state.activeUserId);
        if (!song) return showToast("Música não encontrada.");
        if (!activeUser?.ativo) return showToast("Selecione um usuário ativo antes de reproduzir.");
        try {
            const updated = await request(`/api/musicas/${song.id}/play`, { method: "POST", body: { usuarioId: activeUser.id } });
            const index = state.songs.findIndex((item) => item.id === song.id);
            if (index >= 0) state.songs[index] = updated;
            state.currentSongId = song.id;
            state.isPlaying = true;
            state.progressSeconds = 0;
            state.history = unwrapList(await request(`/api/usuarios/${activeUser.id}/historico?limite=100`));
            renderAll();
        } catch (error) {
            showToast(error.message);
        }
    }

    function adjacentSong(offset) {
        if (!state.songs.length) return;
        const currentIndex = state.songs.findIndex((song) => song.id === state.currentSongId);
        const nextIndex = currentIndex < 0 ? 0 : (currentIndex + offset + state.songs.length) % state.songs.length;
        playSong(state.songs[nextIndex].id);
    }

    async function handleAction(action, id) {
        const numericId = Number(id);
        if (action === "play-song") return playSong(numericId);
        if (action === "review-song") return openEditor("avaliacao", state.reviews.find((review) => review.musicaId === numericId) || null, { songId: numericId });
        if (action === "add-song") {
            const preferred = state.playlists.find((playlist) => playlist.usuarioId === state.activeUserId) || state.playlists[0];
            if (!preferred) return showToast("Crie uma playlist antes de adicionar faixas.");
            if (!state.playlistTracks.has(preferred.id)) await loadPlaylistTracks(preferred.id);
            return openEditor("playlist-track", null, { playlistId: preferred.id, songId: numericId });
        }
        if (action === "edit-song") return openEditor("musica", songById(numericId));
        if (action === "edit-artist") return openEditor("artista", artistById(numericId));
        if (action === "edit-album") return openEditor("album", albumById(numericId));
        if (action === "edit-genre") return openEditor("genero", state.genres.find((item) => item.id === numericId));
        if (action === "edit-user") return openEditor("usuario", userById(numericId));
        if (action === "profile-user") return openEditor("perfil", state.profiles.get(numericId), { userId: numericId });
        if (action === "edit-playlist") return openEditor("playlist", playlistById(numericId));
        if (action === "edit-review") return openEditor("avaliacao", state.reviews.find((review) => review.musicaId === numericId));
        if (action === "select-playlist") return selectPlaylist(numericId);
        if (action === "add-track") return openEditor("playlist-track", null, { playlistId: numericId });

        if (action === "delete-song") {
            const song = songById(numericId);
            return askConfirmation("Excluir música", `A faixa “${song?.titulo}” será removida do catálogo.`, () => request(`/api/musicas/${numericId}`, { method: "DELETE" }));
        }
        if (action === "delete-artist") {
            const artist = artistById(numericId);
            return askConfirmation("Excluir artista", `O artista “${artist?.nome}” será removido.`, () => request(`/api/artistas/${numericId}`, { method: "DELETE" }));
        }
        if (action === "delete-album") {
            const album = albumById(numericId);
            return askConfirmation("Excluir álbum", `O álbum “${album?.titulo}” será removido.`, () => request(`/api/albuns/${numericId}`, { method: "DELETE" }));
        }
        if (action === "delete-genre") {
            const genre = state.genres.find((item) => item.id === numericId);
            return askConfirmation("Excluir gênero", `O gênero “${genre?.nome}” será removido.`, () => request(`/api/generos/${numericId}`, { method: "DELETE" }));
        }
        if (action === "delete-playlist") {
            const playlist = playlistById(numericId);
            return askConfirmation("Excluir playlist", `A playlist “${playlist?.nome}” e sua organização serão removidas.`, () => request(`/api/playlists/${numericId}?usuarioId=${playlist.usuarioId}`, { method: "DELETE" }));
        }
        if (action === "remove-track") {
            const playlist = playlistById(state.selectedPlaylistId);
            const song = songById(numericId);
            return askConfirmation("Remover da playlist", `Remover “${song?.titulo}” desta playlist?`, () => request(`/api/playlists/${playlist.id}/musicas/${numericId}?usuarioId=${playlist.usuarioId}`, { method: "DELETE" }));
        }
        if (action === "delete-review") {
            return askConfirmation("Excluir avaliação", "Sua avaliação desta música será removida.", () => request(`/api/avaliacoes/${state.activeUserId}/${numericId}`, { method: "DELETE" }));
        }
        if (action === "toggle-user") {
            const user = userById(numericId);
            const method = user.ativo ? "DELETE" : "PATCH";
            const path = user.ativo ? `/api/usuarios/${numericId}` : `/api/usuarios/${numericId}/ativar`;
            return askConfirmation(user.ativo ? "Desativar usuário" : "Ativar usuário", `${user.nome} ficará ${user.ativo ? "sem acesso às ações do player" : "disponível para uso"}.`, () => request(path, { method }));
        }
    }

    function bindEvents() {
        document.addEventListener("click", async (event) => {
            const routeButton = event.target.closest("[data-route]");
            if (routeButton) {
                event.preventDefault();
                state.query = "";
                elements.search.value = "";
                setView(routeButton.dataset.route);
                renderCurrentView();
                window.scrollTo({ top: 0, behavior: "smooth" });
                return;
            }

            const createButton = event.target.closest("[data-create]");
            if (createButton) return openEditor(createButton.dataset.create);

            const quickButton = event.target.closest("[data-quick-action]");
            if (quickButton) return openEditor(quickButton.dataset.quickAction);

            const actionButtonElement = event.target.closest("[data-action]");
            if (actionButtonElement) {
                try {
                    await handleAction(actionButtonElement.dataset.action, actionButtonElement.dataset.id);
                } catch (error) {
                    showToast(error.message);
                }
            }
        });

        elements.search.addEventListener("input", () => {
            state.query = elements.search.value.trim();
            renderCurrentView();
        });
        elements.songsGenreFilter.addEventListener("change", renderSongs);
        elements.songsAlbumFilter.addEventListener("change", renderSongs);
        document.querySelector("#clear-song-filters").addEventListener("click", () => {
            elements.songsGenreFilter.value = "";
            elements.songsAlbumFilter.value = "";
            renderSongs();
        });
        elements.activeUser.addEventListener("change", async () => {
            state.activeUserId = Number(elements.activeUser.value) || null;
            if (state.activeUserId) localStorage.setItem("splitfy.activeUser", String(state.activeUserId));
            else localStorage.removeItem("splitfy.activeUser");
            try {
                await loadUserData();
                renderAll();
            } catch (error) {
                showToast(error.message);
            }
        });
        document.querySelector("#refresh-history").addEventListener("click", async () => {
            try {
                await loadUserData();
                renderHistory();
                showToast("Histórico atualizado.");
            } catch (error) {
                showToast(error.message);
            }
        });
        elements.mobileMenu.addEventListener("click", () => elements.body.classList.add("sidebar-open"));
        elements.sidebarScrim.addEventListener("click", () => elements.body.classList.remove("sidebar-open"));
        elements.editorForm.addEventListener("submit", submitEditor);
        document.querySelectorAll("[data-dialog-close]").forEach((button) => button.addEventListener("click", () => elements.editorDialog.close()));
        document.querySelector("#confirm-cancel").addEventListener("click", () => {
            state.confirmAction = null;
            elements.confirmDialog.close();
        });
        elements.confirmAction.addEventListener("click", runConfirmedAction);
        elements.playerToggle.addEventListener("click", () => {
            if (!state.currentSongId) return;
            state.isPlaying = !state.isPlaying;
            renderPlayer();
        });
        elements.playerPrevious.addEventListener("click", () => adjacentSong(-1));
        elements.playerNext.addEventListener("click", () => adjacentSong(1));

        document.addEventListener("keydown", (event) => {
            if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
                event.preventDefault();
                elements.search.focus();
            }
            if (event.key === "Escape") elements.body.classList.remove("sidebar-open");
        });
        window.addEventListener("hashchange", () => {
            const route = location.hash.slice(1);
            if (validRoutes.has(route)) setView(route, { updateHash: false });
        });

        setInterval(() => {
            const song = songById(state.currentSongId);
            if (!state.isPlaying || !song) return;
            state.progressSeconds += 1;
            if (state.progressSeconds >= (song.duracaoSegundos || 1)) {
                state.progressSeconds = 0;
                state.isPlaying = false;
            }
            renderPlayer();
        }, 1000);
    }

    bindEvents();
    setView(state.route, { updateHash: false });
    refreshIcons();
    loadAll();
})();