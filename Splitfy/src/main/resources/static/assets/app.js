(() => {
    "use strict";

    // Altere para a porta/URL onde o seu servidor backend Spring Boot está executando
    const API_BASE_URL = "";

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

    const audio = new Audio();
    audio.preload = "metadata";
    audio.volume = 0.7;
    let audioUrl = null;
    let activeSource = null;
    let playbackSources = [];
    let failedSources = new Set();
    let searchResults = [];
    let searchVersion = 0;
    let playbackRequest = 0;
    let toastTimer;

    function clearAudio() {
        playbackRequest += 1;
        activeSource = null;
        window.splitfyProviders.stop();
        audio.onerror = null;
        audio.onplaying = null;
        audio.pause();
        state.isPlaying = false;
        audio.removeAttribute("src");
        audio.load();
        if (audioUrl) URL.revokeObjectURL(audioUrl);
        audioUrl = null;
    }

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

    function imageUrl(value) {
        try {
            const url = new URL(String(value || "").trim());
            if (url.protocol !== "https:" || url.username || url.password) return "";
            return url.href;
        } catch { return ""; }
    }

    function coverImage(value, label) {
        const url = imageUrl(value);
        return url ? `<img class="remote-cover" src="${escapeHtml(url)}" alt="${escapeHtml(label || "Capa")}" loading="lazy" decoding="async" referrerpolicy="no-referrer">` : "";
    }

    function albumImage(albumId, label) {
        return coverImage(albumById(albumId)?.capaUrl, label);
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
        const session = window.splitfyAuth.getSession();
        if (!session) throw new Error("Entre na sua conta para acessar a biblioteca.");
        options.headers.Authorization = `Bearer ${session.token}`;
        if (body !== undefined) {
            if (body instanceof FormData) options.body = body;
            else {
                options.headers["Content-Type"] = "application/json";
                options.body = JSON.stringify(body);
            }
        }

        const response = await fetch(targetUrl, options);
        if (window.splitfyAuth.getSession()?.token !== session.token) {
            throw new Error("A sessão foi encerrada.");
        }
        if (response.status === 401) {
            window.splitfyAuth.expire();
            throw new Error("Sua sessão expirou. Entre novamente.");
        }
        if (response.status === 403) {
            throw new Error("Sua conta não tem permissão para esta ação.");
        }
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
        const session = window.splitfyAuth.getSession();
        if (!session) {
            setConnection(null, "Entre na sua conta");
            renderAll();
            return;
        }
        if (!quiet) setConnection(null, "Sincronizando");
        try {
            const currentUser = await request("/api/auth/me");
            const [artists, albums, genres, songs, users, playlists] = await Promise.all([
                request("/api/artistas?size=200"),
                request("/api/albuns?size=200"),
                request("/api/generos"),
                request("/api/musicas"),
                session.role === "ADMIN" ? request("/api/usuarios") : Promise.resolve([currentUser]),
                request("/api/playlists")
            ]);

            if (window.splitfyAuth.getSession()?.token !== session.token) return;
            state.artists = unwrapList(artists);
            state.albums = unwrapList(albums);
            state.genres = unwrapList(genres);
            state.songs = unwrapList(songs);
            if (state.currentSongId && !state.songs.some(song => song.id === state.currentSongId)) {
                clearAudio();
                state.currentSongId = null;
                document.querySelector("#provider-panel").hidden = true;
            }
            state.users = unwrapList(users);
            state.playlists = unwrapList(playlists);

            if (session.role !== "ADMIN") state.activeUserId = session.userId;
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
            if (window.splitfyAuth.getSession()?.token !== session.token) return;
            setConnection("offline", "API indisponível");
            renderAll();
            showToast(error.message || "Não foi possível conectar à API.");
        }
    }

    function renderUserSwitcher() {
        elements.activeUser.disabled = window.splitfyAuth.getSession()?.role !== "ADMIN";
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
        if (/^(edit|delete)-(song|artist|album|genre|user)$/.test(action) || action === "toggle-user") {
            if (window.splitfyAuth.getSession()?.role !== "ADMIN") return "";
        }
        return `<button class="table-action ${className}" type="button" title="${escapeHtml(title)}" data-action="${action}" data-id="${id}"><i data-lucide="${icon}"></i></button>`;
    }

    function trackRow(song, index, extraActions = "") {
        const album = albumById(song.albumId);
        const artist = artistById(album?.artistaId);
        return `
            <div class="track-row">
                <span class="track-index">${String(index + 1).padStart(2, "0")}</span>
                <div class="track-cover cover-art ${coverClass(song.albumId || song.id)}">${albumImage(song.albumId, song.titulo)}</div>
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
        elements.playerToggle.disabled = !song || !activeSource;

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
        elements.playerTrack.innerHTML = `<div class="player-cover cover-art ${art}">${albumImage(song.albumId, song.titulo)}</div><div><strong>${escapeHtml(song.titulo)}</strong><span>${escapeHtml(songSubtitle(song))}</span></div>`;
        elements.playerToggle.innerHTML = `<i data-lucide="${state.isPlaying ? "pause" : "play"}"></i>`;
        elements.playerToggle.title = state.isPlaying ? "Pausar" : "Reproduzir";
        const progress = song.duracaoSegundos ? Math.min(100, (state.progressSeconds / song.duracaoSegundos) * 100) : 0;
        elements.playerProgress.style.width = `${progress}%`;
        elements.nowPlayingCard.className = "now-playing-card";
        elements.nowPlayingCard.innerHTML = `
            <div class="now-cover cover-art ${art}">${albumImage(song.albumId, song.titulo)}</div>
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
                <div class="album-cover cover-art ${coverClass(album.id)}">${coverImage(album.capaUrl, album.titulo)}</div>
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
                    <div class="review-cover cover-art ${coverClass(song?.albumId || review.musicaId)}">${albumImage(song?.albumId, song?.titulo)}</div>
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
        const header = `<div class="table-row table-head users-row"><span>Usuário</span><span class="user-email">E-mail</span><span>Permissão</span><span class="user-birth">Nascimento</span><span>Status</span><span>Ações</span></div>`;
        elements.usersTable.innerHTML = users.length ? header + users.map((user) => {
            const profile = state.profiles.get(user.id);
            return `
                <div class="table-row users-row">
                    <div class="table-primary">${profile?.fotoUrl ? `<span class="profile-photo cover-art">${coverImage(profile.fotoUrl, user.nome)}</span>` : ""}<strong>${escapeHtml(profile?.nomeExibicao || user.nome)}</strong><span>${escapeHtml(user.nome)}</span></div>
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
        const session = window.splitfyAuth.getSession();
        const admin = session?.role === "ADMIN";
        if (route === "usuarios" && !admin) route = "inicio";
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
        document.querySelectorAll('.nav-item[data-route="usuarios"]').forEach((item) => { item.hidden = !admin; });
        const restricted = new Set(["musica", "artista", "album", "genero", "usuario"]);
        document.querySelectorAll("[data-create], [data-quick-action]").forEach((item) => {
            const kind = item.dataset.create || item.dataset.quickAction;
            if (restricted.has(kind)) item.hidden = !admin;
        });
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
        const session = window.splitfyAuth.getSession();
        if (!session) return window.splitfyAuth.open();
        if (["artista", "album", "genero", "musica", "usuario"].includes(kind) && session.role !== "ADMIN") {
            return showToast("Somente administradores podem alterar o catálogo e os usuários.");
        }
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
                + inputField("capaUrl", "Link direto da imagem da capa (HTTPS)", item?.capaUrl, { type: "url", wide: true, placeholder: "https://" });
        } else if (kind === "genero") {
            title = isEdit ? "Editar gênero" : "Novo gênero";
            fields = inputField("nome", "Nome", item?.nome, { required: true, wide: true })
                + textareaField("descricao", "Descrição", item?.descricao);
        } else if (kind === "musica") {
            title = isEdit ? "Editar música" : "Nova música";
            fields = `<div class="form-field is-wide"><label for="music-lookup">Buscar música e cantor</label><div class="lookup-controls"><input id="music-lookup" type="search" placeholder="Ex.: INVISIBLE Duran Duran"><button type="button" class="secondary-button" id="music-lookup-button">Buscar</button></div><p id="music-lookup-status" role="status"></p><div id="music-lookup-results" class="lookup-results"></div></div>`
                + inputField("titulo", "Título", item?.titulo, { required: true, wide: true })
                + selectField("albumId", "Álbum", [{ value: "importar", label: "Criar álbum a partir da faixa encontrada" }, ...optionList(state.albums, "titulo")], item?.albumId)
                + `<div class="form-field is-wide" id="import-metadata" hidden><label for="import-artist">Cantor / artista (confirme o nome)</label><input id="import-artist" name="importArtist"><label for="import-album">Álbum</label><input id="import-album" name="importAlbum"><small>No YouTube, o resultado informa o canal. Corrija o nome do cantor se necessário.</small></div>`
                + selectField("generoId", "Gênero", [{ value: "novo", label: "Sem classificação (criar gênero)" }, ...optionList(state.genres)], item?.generoId)
                + inputField("duracaoSegundos", "Duração em segundos", item?.duracaoSegundos, { type: "number", min: 1, required: true })
                + inputField("numeroFaixa", "Número da faixa", item?.numeroFaixa, { type: "number", min: 1 })
                + inputField("spotifyUrl", "Link da faixa no Spotify", item?.spotifyUrl, { type: "url", wide: true, placeholder: "https://open.spotify.com/track/..." })
                + inputField("youtubeUrl", "Link no YouTube Music ou YouTube", item?.youtubeUrl, { type: "url", wide: true, placeholder: "https://music.youtube.com/watch?v=..." })
                + selectField("fontePreferida", "Fonte preferida", [{ value: "AUTO", label: "Automática: YouTube → Spotify → MP3" }, { value: "SPOTIFY", label: "Spotify" }, { value: "YOUTUBE", label: "YouTube" }, { value: "MP3", label: "MP3" }], item?.fontePreferida || "AUTO", { wide: true })
                + `<div class="form-field is-wide"><label for="field-arquivo">Arquivo MP3 (até 4 MB)</label><input id="field-arquivo" name="arquivo" type="file" accept=".mp3,audio/mpeg"><small>Envie uma música sua ou que você tenha autorização para disponibilizar. Ao editar, deixe vazio para manter o áudio atual.</small></div>`;
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
                + inputField("fotoUrl", "Link direto da foto (HTTPS)", item?.fotoUrl, { type: "url", wide: true, placeholder: "https://" })
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

        searchVersion += 1;
        searchResults = [];
        state.editor = { kind, item, context };
        elements.dialogEyebrow.textContent = eyebrow;
        elements.dialogTitle.textContent = title;
        elements.dialogFields.innerHTML = fields;
        elements.dialogFeedback.textContent = "";
        elements.editorDialog.showModal();
        if (kind === "musica") {
            elements.dialogFields.querySelector("#music-lookup-button").addEventListener("click", lookupMusic);
            elements.dialogFields.querySelector("#music-lookup").addEventListener("keydown", (event) => {
                if (event.key === "Enter") { event.preventDefault(); lookupMusic(); }
            });
        }
        for (const field of elements.dialogFields.querySelectorAll('[name="capaUrl"], [name="fotoUrl"]')) {
            const preview = document.createElement("div");
            preview.className = "image-preview";
            field.after(preview);
            const updatePreview = () => {
                preview.replaceChildren();
                if (!field.value.trim()) return;
                const url = imageUrl(field.value);
                if (!url) { preview.textContent = "Use um link HTTPS direto para a imagem."; return; }
                const img = new Image();
                img.alt = "Prévia da imagem";
                img.referrerPolicy = "no-referrer";
                img.addEventListener("error", () => { preview.textContent = "Não foi possível carregar. Use o endereço da imagem, não o link de uma página; o servidor também precisa permitir acesso externo."; });
                img.src = url;
                preview.appendChild(img);
            };
            field.addEventListener("input", updatePreview);
            updatePreview();
        }
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
                const arquivo = data.get("arquivo");
                if (arquivo?.size > 4 * 1024 * 1024) throw new Error("O MP3 deve ter até 4 MB.");
                if (arquivo?.size && !arquivo.name.toLowerCase().endsWith(".mp3")) throw new Error("Escolha um arquivo com extensão .mp3.");
                let albumId = formNumber(data, "albumId");
                let generoId = formNumber(data, "generoId");
                if (data.get("albumId") === "importar") {
                    if (!state.editor.importResult) throw new Error("Busque e selecione uma faixa ou escolha um álbum já cadastrado.");
                    const nome = String(data.get("importArtist") || "").trim();
                    const tituloAlbum = String(data.get("importAlbum") || "").trim();
                    if (!nome || !tituloAlbum) throw new Error("Confirme o cantor e o álbum do resultado.");
                    if (!state.editor.importAlbumId) {
                        let artist = state.artists.find((entry) => normalizeText(entry.nome) === normalizeText(nome));
                        if (!artist) {
                            artist = await request("/api/artistas", { method: "POST", body: { nome, pais: null, inicioCarreira: null, biografia: null } });
                            state.artists.push(artist);
                        }
                        let album = state.albums.find((entry) => entry.artistaId === artist.id && normalizeText(entry.titulo) === normalizeText(tituloAlbum));
                        if (!album) {
                            album = await request("/api/albuns", { method: "POST", body: { titulo: tituloAlbum, artistaId: artist.id, anoLancamento: null, capaUrl: state.editor.importResult.capaUrl || null } });
                            state.albums.push(album);
                        }
                        state.editor.importAlbumId = album.id;
                    }
                    albumId = state.editor.importAlbumId;
                }
                if (data.get("generoId") === "novo") {
                    let genre = state.genres.find((entry) => entry.nome === "Sem classificação");
                    if (!genre) {
                        genre = await request("/api/generos", { method: "POST", body: { nome: "Sem classificação", descricao: "Faixas sem gênero definido." } });
                        state.genres.push(genre);
                    }
                    generoId = genre.id;
                }
                const body = { titulo: data.get("titulo"), albumId, generoId, duracaoSegundos: formNumber(data, "duracaoSegundos"), numeroFaixa: formNumber(data, "numeroFaixa"), spotifyUrl: data.get("spotifyUrl") || null, youtubeUrl: data.get("youtubeUrl") || null, fontePreferida: data.get("fontePreferida") || "AUTO" };
                const saved = await request(`/api/musicas${item ? `/${item.id}` : ""}`, { method: item ? "PUT" : "POST", body });
                state.editor.item = saved;
                if (arquivo?.size) {
                    const upload = new FormData();
                    upload.append("arquivo", arquivo);
                    await request(`/api/musicas/${saved.id}/audio`, { method: "PUT", body: upload });
                }
            } else if (kind === "usuario") {
                const body = { nome: data.get("nome"), email: data.get("email"), senha: data.get("senha") || null, dataNascimento: data.get("dataNascimento"), role: data.get("role") };
                await request(`/api/usuarios${item ? `/${item.id}` : ""}`, { method: item ? "PUT" : "POST", body });
                if (item?.id === window.splitfyAuth.getSession()?.userId
                        && item.role !== body.role) {
                    elements.editorDialog.close();
                    window.splitfyAuth.expire();
                    showToast("Permissão alterada. Entre novamente para atualizar seu acesso.");
                    return;
                }
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

    async function lookupMusic() {
        const query = document.querySelector("#music-lookup").value.trim();
        const status = document.querySelector("#music-lookup-status");
        const results = document.querySelector("#music-lookup-results");
        const button = document.querySelector("#music-lookup-button");
        if (query.length < 2) { status.textContent = "Informe o nome da música e o cantor."; return; }
        const version = ++searchVersion;
        button.disabled = true;
        status.textContent = "Buscando nos serviços disponíveis…";
        results.replaceChildren();
        try {
            const response = await request(`/api/integracoes/musicas/buscar?q=${encodeURIComponent(query)}`);
            if (version !== searchVersion || !elements.editorDialog.open) return;
            searchResults = response.resultados;
            status.textContent = response.avisos.join(" ") || (searchResults.length ? "Escolha a faixa correta. Você pode adicionar uma fonte de cada serviço." : "Nenhuma faixa encontrada. Tente nome e cantor ou cadastre o link.");
            results.innerHTML = searchResults.map((result, index) => `<div class="lookup-result"><span class="lookup-cover cover-art">${coverImage(result.capaUrl, result.titulo)}</span><span><strong>${escapeHtml(result.titulo)}</strong><small>${escapeHtml(result.artista)} · ${result.fonte === "SPOTIFY" ? "Spotify" : "YouTube"}</small></span><div class="lookup-actions"><button type="button" class="secondary-button" data-import-result="${index}" aria-label="Usar faixa: ${escapeHtml(result.titulo)} · ${escapeHtml(result.artista)}">Usar faixa</button><button type="button" class="secondary-button" data-import-result="${index}" data-source-only="true" aria-label="Adicionar fonte ${result.fonte}: ${escapeHtml(result.titulo)}">Adicionar só a fonte</button></div></div>`).join("");
        } catch (error) { if (version === searchVersion) status.textContent = error.message; }
        finally { if (version === searchVersion) button.disabled = false; }
    }

    function availableSources(song) {
        const order = ["YOUTUBE", "SPOTIFY", "MP3"];
        if (song.fontePreferida && song.fontePreferida !== "AUTO") {
            order.splice(order.indexOf(song.fontePreferida), 1);
            order.unshift(song.fontePreferida);
        }
        return order.filter((source) => source === "YOUTUBE" ? song.youtubeUrl : source === "SPOTIFY" ? song.spotifyUrl : song.audioDisponivel);
    }

    async function registerPlayback(song, version) {
        try {
            const updated = await request(`/api/musicas/${song.id}/play`, { method: "POST", body: { usuarioId: state.activeUserId } });
            if (version !== playbackRequest) return;
            const index = state.songs.findIndex((item) => item.id === song.id);
            if (index >= 0) state.songs[index] = updated;
            state.history = unwrapList(await request(`/api/usuarios/${state.activeUserId}/historico?limite=100`));
            if (version === playbackRequest) renderAll();
        } catch (error) { if (version === playbackRequest) showToast(error.message); }
    }

    async function playSong(id, source = null, continuing = false) {
        const song = songById(id);
        if (!song) return showToast("Música não encontrada.");
        if (!userById(state.activeUserId)?.ativo) return showToast("Selecione um usuário ativo antes de reproduzir.");
        if (!continuing) failedSources = new Set();
        playbackSources = availableSources(song);
        const chosen = source || playbackSources.find((entry) => !failedSources.has(entry));
        clearAudio();
        state.currentSongId = song.id;
        state.progressSeconds = 0;
        renderPlayer();
        const panel = document.querySelector("#provider-panel");
        const status = document.querySelector("#provider-status");
        panel.hidden = false;
        document.querySelector("#provider-title").textContent = song.titulo;
        const select = document.querySelector("#provider-source");
        select.innerHTML = playbackSources.map((entry) => `<option value="${entry}">${{ YOUTUBE: "YouTube", SPOTIFY: "Spotify", MP3: "MP3" }[entry]}</option>`).join("");
        select.value = chosen || "";
        document.querySelector("#provider-fallback").disabled = playbackSources.filter((entry) => !failedSources.has(entry) && entry !== chosen).length === 0;
        if (!chosen || !playbackSources.includes(chosen)) {
            status.textContent = "Não há uma fonte disponível. Edite a música para vincular Spotify/YouTube ou enviar um MP3.";
            return;
        }
        activeSource = chosen;
        renderPlayer();
        if (!continuing) panel.scrollIntoView({ behavior: "smooth", block: "center" });
        const volume = document.querySelector('.player-volume input');
        volume.disabled = chosen === "SPOTIFY";
        volume.title = chosen === "SPOTIFY" ? "Use os controles do player Spotify." : "Volume";
        status.textContent = chosen === "SPOTIFY"
            ? "Spotify pode oferecer uma prévia ou pedir login. Se necessário, use os controles abaixo ou escolha outra fonte."
            : chosen === "YOUTUBE" ? "Se a reprodução não começar, pressione Play no vídeo abaixo." : "Carregando MP3…";
        const version = playbackRequest;
        const session = window.splitfyAuth.getSession();
        let registered = false;
        const started = () => {
            if (version !== playbackRequest || registered) return;
            registered = true;
            registerPlayback(song, version);
        };
        const fallback = (error) => {
            if (version !== playbackRequest) return;
            failedSources.add(chosen);
            const next = playbackSources.find((entry) => !failedSources.has(entry));
            if (next) { showToast("Fonte indisponível. Tentando outra opção…"); playSong(song.id, next, true); }
            else { clearAudio(); state.currentSongId = song.id; renderPlayer(); status.textContent = `${error.message} Nenhuma outra fonte disponível; cadastre outro link ou envie um MP3.`; }
        };
        try {
            if (chosen !== "MP3") {
                await window.splitfyProviders.play(chosen, chosen === "SPOTIFY" ? song.spotifyUrl : song.youtubeUrl, {
                    onStarted: started,
                    onState: (playing) => { if (version === playbackRequest) { state.isPlaying = playing; renderPlayer(); } },
                    onProgress: (position, duration) => {
                        if (version !== playbackRequest) return;
                        state.progressSeconds = position;
                        elements.playerProgress.style.width = `${duration > 0 ? Math.min(100, position / duration * 100) : 0}%`;
                    },
                    onError: fallback
                });
                return;
            }
            const response = await fetch(`/api/musicas/${song.id}/audio`, { headers: { Authorization: `Bearer ${session.token}` } });
            if (response.status === 401) { window.splitfyAuth.expire(); return; }
            if (!response.ok) throw new Error("Não foi possível carregar o MP3.");
            const blob = await response.blob();
            if (version !== playbackRequest || window.splitfyAuth.getSession()?.token !== session.token) return;
            audioUrl = URL.createObjectURL(blob);
            audio.src = audioUrl;
            audio.onerror = () => fallback(new Error("Não foi possível reproduzir este MP3."));
            audio.onplaying = started;
            await audio.play();
            if (version !== playbackRequest) return;
            started();
            status.textContent = "Reproduzindo o MP3 salvo no Splitfy.";
        } catch (error) {
            if (version !== playbackRequest) return;
            if (error.name === "NotAllowedError") status.textContent = "Pressione Play para começar a reprodução.";
            else fallback(error);
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

    function applyMusicResult(result, sourceOnly = false) {
        const status = document.querySelector("#music-lookup-status");
        if (sourceOnly && !document.querySelector('[name="titulo"]').value.trim()) {
            status.textContent = "Escolha Usar faixa primeiro; depois adicione outra fonte para essa mesma música.";
            return;
        }
        const field = result.fonte === "SPOTIFY" ? '[name="spotifyUrl"]' : '[name="youtubeUrl"]';
        if (!sourceOnly) {
            if (state.editor.importResult && state.editor.importResult.url !== result.url) {
                document.querySelector(result.fonte === "SPOTIFY" ? '[name="youtubeUrl"]' : '[name="spotifyUrl"]').value = "";
            }
            document.querySelector('[name="titulo"]').value = result.titulo;
            document.querySelector('[name="duracaoSegundos"]').value = result.duracaoSegundos || "";
            state.editor.importResult = result;
            state.editor.importAlbumId = null;
            state.editor.imported = true;
            document.querySelector("#import-metadata").hidden = false;
            document.querySelector("#import-artist").value = result.artista;
            document.querySelector("#import-album").value = result.album || result.titulo;
            document.querySelector('[name="albumId"]').value = "importar";
            if (!document.querySelector('[name="generoId"]').value) document.querySelector('[name="generoId"]').value = "novo";
        }
        document.querySelector(field).value = result.url;
        status.textContent = `${result.fonte === "SPOTIFY" ? "Spotify" : "YouTube"} vinculado: ${result.titulo} · ${result.artista}. Confira se as fontes correspondem à mesma música e revise os dados antes de salvar.`;
    }

    function bindEvents() {
        document.addEventListener("error", (event) => {
            if (event.target.matches?.("img.remote-cover")) {
                event.target.hidden = true;
                event.target.parentElement.title = "A imagem não pôde ser carregada. Verifique o link ao editar.";
            }
        }, true);
        document.addEventListener("click", async (event) => {
            const importResult = event.target.closest("[data-import-result]");
            if (importResult && state.editor?.kind === "musica") {
                const result = searchResults[Number(importResult.dataset.importResult)];
                if (!result) return;
                applyMusicResult(result, importResult.dataset.sourceOnly === "true");
                return;
            }
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
            clearAudio();
            state.currentSongId = null;
            document.querySelector("#provider-panel").hidden = true;
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
            if (activeSource && activeSource !== "MP3") { window.splitfyProviders.toggle(); return; }
            if (audio.paused) audio.play().catch(() => showToast("Não foi possível reproduzir o áudio."));
            else audio.pause();
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

        document.querySelector("#provider-source").addEventListener("change", (event) => playSong(state.currentSongId, event.target.value));
        document.querySelector("#provider-fallback").addEventListener("click", () => {
            failedSources.add(activeSource);
            const next = playbackSources.find((entry) => !failedSources.has(entry));
            if (next) playSong(state.currentSongId, next, true);
        });
        audio.addEventListener("timeupdate", () => {
            if (activeSource !== "MP3") return;
            state.progressSeconds = audio.currentTime;
            elements.playerProgress.style.width = `${Number.isFinite(audio.duration) && audio.duration > 0 ? audio.currentTime / audio.duration * 100 : 0}%`;
        });
        for (const name of ["play", "pause", "ended"]) audio.addEventListener(name, () => {
            if (activeSource !== "MP3") return;
            state.isPlaying = !audio.paused && !audio.ended;
            renderPlayer();
        });
        document.querySelector('.player-volume input').addEventListener("input", (event) => {
            audio.volume = Number(event.target.value) / 100;
            window.splitfyProviders.volume(Number(event.target.value));
        });
    }

    window.addEventListener("splitfy:session", () => {
        clearAudio();
        document.querySelector("#provider-panel").hidden = true;
        searchVersion += 1;
        state.artists = [];
        state.albums = [];
        state.genres = [];
        state.songs = [];
        state.users = [];
        state.playlists = [];
        state.reviews = [];
        state.history = [];
        state.profiles.clear();
        state.playlistTracks.clear();
        state.selectedPlaylistId = null;
        state.activeUserId = window.splitfyAuth.getSession()?.userId || null;
        localStorage.removeItem("splitfy.activeUser");
        state.currentSongId = null;
        state.isPlaying = false;
        state.progressSeconds = 0;
        elements.editorDialog.close();
        elements.confirmDialog.close();
        state.editor = null;
        state.confirmAction = null;
        renderAll();
        renderPlayer();
        loadAll();
    });
    bindEvents();
    setView(state.route, { updateHash: false });
    refreshIcons();
    loadAll();
    if (!window.splitfyAuth.getSession()) window.splitfyAuth.open();
})();
