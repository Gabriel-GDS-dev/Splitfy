const demoGenres = [
    { id: 1, nome: "Indie Pop", descricao: "Melodias leves, sintetizadores e clima urbano." },
    { id: 2, nome: "MPB", descricao: "Cancao brasileira, arranjos organicos e letras fortes." },
    { id: 3, nome: "Rock Alternativo", descricao: "Guitarras, energia e producao moderna." }
];

const demoSongs = [
    {
        id: 1,
        titulo: "Noite Neon",
        duracaoSegundos: 213,
        numeroFaixa: 1,
        reproducoes: 18,
        albumId: 1,
        generoId: 1,
        generoNome: "Indie Pop",
        criadoEm: null
    },
    {
        id: 2,
        titulo: "Cidade Inteira",
        duracaoSegundos: 184,
        numeroFaixa: 4,
        reproducoes: 31,
        albumId: 2,
        generoId: 2,
        generoNome: "MPB",
        criadoEm: null
    },
    {
        id: 3,
        titulo: "Frequencia Livre",
        duracaoSegundos: 246,
        numeroFaixa: 2,
        reproducoes: 11,
        albumId: 1,
        generoId: 3,
        generoNome: "Rock Alternativo",
        criadoEm: null
    }
];

const state = {
    apiBase: localStorage.getItem("splitfyApiBase") || "/api",
    useMock: false,
    genres: [],
    songs: [],
    selectedGenreId: null,
    latestPlayId: null,
    activeView: "home",
    remoteSearchResults: null
};

const els = {};

document.addEventListener("DOMContentLoaded", init);

async function init() {
    cacheElements();
    bindEvents();
    els.apiBase.value = state.apiBase;
    setView(location.hash.replace("#", "") || "home");
    await loadCatalog();
    render();
}

function cacheElements() {
    els.navItems = document.querySelectorAll("[data-view]");
    els.panels = document.querySelectorAll("[data-view-panel]");
    els.apiSettings = document.querySelector("#api-settings");
    els.apiBase = document.querySelector("#api-base");
    els.connectionStatus = document.querySelector("#connection-status");
    els.statsGenres = document.querySelector("#stats-genres");
    els.statsSongs = document.querySelector("#stats-songs");
    els.statsPlays = document.querySelector("#stats-plays");
    els.featuredStrip = document.querySelector("#featured-strip");
    els.latestPlay = document.querySelector("#latest-play");
    els.genreForm = document.querySelector("#genre-form");
    els.genreFormTitle = document.querySelector("#genre-form-title");
    els.genreId = document.querySelector("#genre-id");
    els.genreName = document.querySelector("#genre-name");
    els.genreDescription = document.querySelector("#genre-description");
    els.genreFeedback = document.querySelector("#genre-feedback");
    els.cancelGenreEdit = document.querySelector("#cancel-genre-edit");
    els.genreList = document.querySelector("#genre-list");
    els.selectedGenreTitle = document.querySelector("#selected-genre-title");
    els.selectedGenreSongs = document.querySelector("#selected-genre-songs");
    els.musicForm = document.querySelector("#music-form");
    els.musicFormTitle = document.querySelector("#music-form-title");
    els.musicId = document.querySelector("#music-id");
    els.musicTitle = document.querySelector("#music-title");
    els.musicDuration = document.querySelector("#music-duration");
    els.musicTrack = document.querySelector("#music-track");
    els.musicAlbum = document.querySelector("#music-album");
    els.musicGenre = document.querySelector("#music-genre");
    els.musicFeedback = document.querySelector("#music-feedback");
    els.cancelMusicEdit = document.querySelector("#cancel-music-edit");
    els.musicList = document.querySelector("#music-list");
    els.searchForm = document.querySelector("#search-form");
    els.filterTitle = document.querySelector("#filter-title");
    els.filterGenre = document.querySelector("#filter-genre");
    els.filterAlbum = document.querySelector("#filter-album");
    els.clearSearch = document.querySelector("#clear-search");
    els.searchResults = document.querySelector("#search-results");
    els.toast = document.querySelector("#toast");
}

function bindEvents() {
    els.navItems.forEach((item) => {
        item.addEventListener("click", () => setView(item.dataset.view));
    });

    els.apiSettings.addEventListener("submit", async (event) => {
        event.preventDefault();
        state.apiBase = normalizeApiBase(els.apiBase.value);
        els.apiBase.value = state.apiBase;
        localStorage.setItem("splitfyApiBase", state.apiBase);
        await loadCatalog();
        render();
    });

    els.genreForm.addEventListener("submit", saveGenre);
    els.cancelGenreEdit.addEventListener("click", resetGenreForm);
    els.musicForm.addEventListener("submit", saveMusic);
    els.cancelMusicEdit.addEventListener("click", resetMusicForm);
    els.searchForm.addEventListener("submit", submitSearch);
    els.clearSearch.addEventListener("click", clearSearch);

    document.addEventListener("click", handleAction);
    window.addEventListener("hashchange", () => setView(location.hash.replace("#", "") || "home", false));
}

async function loadCatalog() {
    setConnection("Carregando", "is-mock");
    try {
        const [genresPayload, songsPayload] = await Promise.all([
            apiRequest("/generos"),
            apiRequest("/musicas")
        ]);
        state.genres = unwrapList(genresPayload).map(normalizeGenre).filter(Boolean);
        state.songs = unwrapList(songsPayload).map(normalizeSong).filter(Boolean);
        state.useMock = false;
        if (!state.selectedGenreId && state.genres.length) {
            state.selectedGenreId = state.genres[0].id;
        }
        setConnection("API conectada", "is-api");
    } catch (error) {
        state.genres = demoGenres.map((genre) => ({ ...genre }));
        state.songs = demoSongs.map((song) => ({ ...song }));
        state.useMock = true;
        if (!state.selectedGenreId && state.genres.length) {
            state.selectedGenreId = state.genres[0].id;
        }
        setConnection("Previa local", "is-mock");
        showToast("API indisponivel agora. A interface esta usando dados locais de demonstracao.");
    }
}

async function apiRequest(path, options = {}) {
    const response = await fetch(`${state.apiBase}${path}`, {
        headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
            ...(options.headers || {})
        },
        ...options
    });

    if (!response.ok) {
        let message = `HTTP ${response.status}`;
        try {
            const payload = await response.json();
            message = payload.message || payload.detail || message;
        } catch (_error) {
            message = await response.text() || message;
        }
        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }
    return response.json();
}

async function saveGenre(event) {
    event.preventDefault();
    clearFeedback();

    const id = Number(els.genreId.value) || null;
    const payload = {
        nome: els.genreName.value.trim(),
        descricao: els.genreDescription.value.trim() || null
    };

    if (!payload.nome) {
        return setFeedback(els.genreFeedback, "Informe o nome do genero.");
    }

    try {
        if (state.useMock) {
            saveGenreLocally(id, payload);
        } else if (id) {
            await apiRequest(`/generos/${id}`, { method: "PUT", body: JSON.stringify(payload) });
            await loadCatalog();
        } else {
            await apiRequest("/generos", { method: "POST", body: JSON.stringify(payload) });
            await loadCatalog();
        }
        resetGenreForm();
        render();
        showToast("Genero salvo com sucesso.");
    } catch (error) {
        setFeedback(els.genreFeedback, error.message);
    }
}

async function saveMusic(event) {
    event.preventDefault();
    clearFeedback();

    const id = Number(els.musicId.value) || null;
    const payload = {
        titulo: els.musicTitle.value.trim(),
        duracaoSegundos: toOptionalNumber(els.musicDuration.value),
        numeroFaixa: toOptionalNumber(els.musicTrack.value),
        albumId: toOptionalNumber(els.musicAlbum.value),
        generoId: toOptionalNumber(els.musicGenre.value)
    };

    const validation = validateMusicPayload(payload);
    if (validation) {
        return setFeedback(els.musicFeedback, validation);
    }

    try {
        if (state.useMock) {
            saveMusicLocally(id, payload);
        } else if (id) {
            await apiRequest(`/musicas/${id}`, { method: "PUT", body: JSON.stringify(payload) });
            await loadCatalog();
        } else {
            await apiRequest("/musicas", { method: "POST", body: JSON.stringify(payload) });
            await loadCatalog();
        }
        resetMusicForm();
        render();
        showToast("Musica salva com sucesso.");
    } catch (error) {
        setFeedback(els.musicFeedback, error.message);
    }
}

async function submitSearch(event) {
    event.preventDefault();
    state.remoteSearchResults = null;

    if (state.useMock) {
        renderSearchResults();
        return;
    }

    const params = new URLSearchParams();
    const title = els.filterTitle.value.trim();
    const genreId = els.filterGenre.value;
    const albumId = els.filterAlbum.value.trim();

    if (title) params.set("titulo", title);
    if (genreId) params.set("generoId", genreId);
    if (albumId) params.set("albumId", albumId);

    try {
        const payload = await apiRequest(`/musicas?${params.toString()}`);
        state.remoteSearchResults = unwrapList(payload).map(normalizeSong).filter(Boolean);
        renderSearchResults();
        refreshIcons();
    } catch (error) {
        showToast(error.message);
    }
}

function clearSearch() {
    els.filterTitle.value = "";
    els.filterGenre.value = "";
    els.filterAlbum.value = "";
    state.remoteSearchResults = null;
    renderSearchResults();
}

function handleAction(event) {
    const button = event.target.closest("[data-action]");
    if (!button) return;

    const id = Number(button.dataset.id);
    const action = button.dataset.action;

    if (action === "edit-genre") editGenre(id);
    if (action === "delete-genre") deleteGenre(id);
    if (action === "view-genre") selectGenre(id);
    if (action === "edit-music") editMusic(id);
    if (action === "delete-music") deleteMusic(id);
    if (action === "play-music") playMusic(id);
}

function editGenre(id) {
    const genre = state.genres.find((item) => item.id === id);
    if (!genre) return;
    els.genreId.value = genre.id;
    els.genreName.value = genre.nome;
    els.genreDescription.value = genre.descricao || "";
    els.genreFormTitle.textContent = "Editar genero";
    setView("genres");
}

async function deleteGenre(id) {
    const genre = state.genres.find((item) => item.id === id);
    if (!genre || !window.confirm(`Excluir o genero "${genre.nome}"?`)) return;

    try {
        if (state.useMock) {
            if (state.songs.some((song) => song.generoId === id)) {
                throw new Error("Genero possui musicas vinculadas.");
            }
            state.genres = state.genres.filter((item) => item.id !== id);
        } else {
            await apiRequest(`/generos/${id}`, { method: "DELETE" });
            await loadCatalog();
        }
        if (state.selectedGenreId === id) {
            state.selectedGenreId = state.genres[0]?.id || null;
        }
        render();
        showToast("Genero excluido.");
    } catch (error) {
        showToast(error.message);
    }
}

function selectGenre(id) {
    state.selectedGenreId = id;
    setView("genres");
    renderSelectedGenreSongs();
    refreshIcons();
}

function editMusic(id) {
    const song = state.songs.find((item) => item.id === id);
    if (!song) return;
    els.musicId.value = song.id;
    els.musicTitle.value = song.titulo;
    els.musicDuration.value = song.duracaoSegundos;
    els.musicTrack.value = song.numeroFaixa || "";
    els.musicAlbum.value = song.albumId;
    els.musicGenre.value = song.generoId;
    els.musicFormTitle.textContent = "Editar musica";
    setView("songs");
}

async function deleteMusic(id) {
    const song = state.songs.find((item) => item.id === id);
    if (!song || !window.confirm(`Excluir a musica "${song.titulo}"?`)) return;

    try {
        if (state.useMock) {
            state.songs = state.songs.filter((item) => item.id !== id);
        } else {
            await apiRequest(`/musicas/${id}`, { method: "DELETE" });
            await loadCatalog();
        }
        render();
        showToast("Musica excluida.");
    } catch (error) {
        showToast(error.message);
    }
}

async function playMusic(id) {
    try {
        if (state.useMock) {
            const song = state.songs.find((item) => item.id === id);
            if (!song) return;
            song.reproducoes += 1;
            state.latestPlayId = id;
        } else {
            const entradaUsuario = window.prompt("Informe o ID do usuário que vai ouvir esta música:");
            if (entradaUsuario === null) return;
            const usuarioId = Number(entradaUsuario);
            if (!Number.isSafeInteger(usuarioId) || usuarioId <= 0) {
                throw new Error("Informe um ID de usuário positivo.");
            }
            const updated = normalizeSong(await apiRequest(`/musicas/${id}/play`, {
                method: "POST",
                body: JSON.stringify({ usuarioId })
            }));
            state.latestPlayId = updated.id;
            state.songs = state.songs.map((song) => song.id === updated.id ? updated : song);
        }
        render();
    } catch (error) {
        showToast(error.message);
    }
}

function saveGenreLocally(id, payload) {
    const duplicated = state.genres.some((genre) => {
        const sameName = genre.nome.toLowerCase() === payload.nome.toLowerCase();
        return sameName && genre.id !== id;
    });
    if (duplicated) {
        throw new Error("Ja existe um genero com esse nome.");
    }

    if (id) {
        state.genres = state.genres.map((genre) => (
            genre.id === id ? { ...genre, ...payload } : genre
        ));
    } else {
        state.genres.push({ id: nextId(state.genres), ...payload });
    }
}

function saveMusicLocally(id, payload) {
    const genre = state.genres.find((item) => item.id === payload.generoId);
    if (!genre) {
        throw new Error("Genero nao encontrado.");
    }

    if (id) {
        state.songs = state.songs.map((song) => (
            song.id === id
                ? { ...song, ...payload, generoNome: genre.nome }
                : song
        ));
    } else {
        state.songs.push({
            id: nextId(state.songs),
            ...payload,
            generoNome: genre.nome,
            reproducoes: 0,
            criadoEm: null
        });
    }
}

function render() {
    renderStats();
    renderSelects();
    renderFeatured();
    renderLatestPlay();
    renderGenreList();
    renderSelectedGenreSongs();
    renderMusicList();
    renderSearchResults();
    refreshIcons();
}

function renderStats() {
    const plays = state.songs.reduce((sum, song) => sum + (Number(song.reproducoes) || 0), 0);
    els.statsGenres.textContent = state.genres.length;
    els.statsSongs.textContent = state.songs.length;
    els.statsPlays.textContent = plays;
}

function renderSelects() {
    const genreOptions = [
        '<option value="">Selecione</option>',
        ...state.genres.map((genre) => `<option value="${genre.id}">${escapeHtml(genre.nome)}</option>`)
    ].join("");

    const filterOptions = [
        '<option value="">Todos</option>',
        ...state.genres.map((genre) => `<option value="${genre.id}">${escapeHtml(genre.nome)}</option>`)
    ].join("");

    const previousMusicGenre = els.musicGenre.value;
    const previousFilterGenre = els.filterGenre.value;
    els.musicGenre.innerHTML = genreOptions;
    els.filterGenre.innerHTML = filterOptions;

    if (previousMusicGenre) els.musicGenre.value = previousMusicGenre;
    if (previousFilterGenre) els.filterGenre.value = previousFilterGenre;
}

function renderFeatured() {
    const featured = state.songs.slice(0, 3);
    if (!featured.length) {
        els.featuredStrip.innerHTML = '<p class="empty-state">Cadastre a primeira musica para montar a vitrine do catalogo.</p>';
        return;
    }

    els.featuredStrip.innerHTML = featured.map((song) => `
        <article class="featured-card">
            ${renderCover(song)}
            <strong>${escapeHtml(song.titulo)}</strong>
            <span>${escapeHtml(song.generoNome || genreName(song.generoId))} - ${formatDuration(song.duracaoSegundos)}</span>
        </article>
    `).join("");
}

function renderLatestPlay() {
    const song = state.songs.find((item) => item.id === state.latestPlayId);
    if (!song) {
        els.latestPlay.innerHTML = "Nenhuma musica reproduzida nesta sessao.";
        return;
    }

    els.latestPlay.innerHTML = `
        <article class="song-card">
            ${renderCover(song)}
            <div class="song-main">
                <strong>${escapeHtml(song.titulo)}</strong>
                <div class="song-meta">
                    <span class="tag">${escapeHtml(song.generoNome || genreName(song.generoId))}</span>
                    <span class="tag">${song.reproducoes} plays</span>
                </div>
            </div>
        </article>
    `;
}

function renderGenreList() {
    if (!state.genres.length) {
        els.genreList.innerHTML = '<p class="empty-state">Nenhum genero cadastrado.</p>';
        return;
    }

    els.genreList.innerHTML = state.genres.map((genre) => {
        const count = state.songs.filter((song) => song.generoId === genre.id).length;
        return `
            <article class="genre-card">
                <div class="genre-content">
                    <strong>${escapeHtml(genre.nome)}</strong>
                    <p class="genre-description">${escapeHtml(genre.descricao || "Sem descricao.")}</p>
                    <span class="tag">${count} musica${count === 1 ? "" : "s"}</span>
                </div>
                <div class="card-actions">
                    <button class="mini-action" type="button" title="Ver musicas" data-action="view-genre" data-id="${genre.id}">
                        <i data-lucide="list-music"></i>
                    </button>
                    <button class="mini-action" type="button" title="Editar genero" data-action="edit-genre" data-id="${genre.id}">
                        <i data-lucide="pencil"></i>
                    </button>
                    <button class="mini-action is-danger" type="button" title="Excluir genero" data-action="delete-genre" data-id="${genre.id}">
                        <i data-lucide="trash-2"></i>
                    </button>
                </div>
            </article>
        `;
    }).join("");
}

function renderSelectedGenreSongs() {
    if (!state.selectedGenreId && state.genres.length) {
        state.selectedGenreId = state.genres[0].id;
    }

    const genre = state.genres.find((item) => item.id === state.selectedGenreId);
    if (!genre) {
        els.selectedGenreTitle.textContent = "Musicas por genero";
        els.selectedGenreSongs.innerHTML = '<p class="empty-state">Selecione um genero para ver as musicas vinculadas.</p>';
        return;
    }

    const songs = state.songs.filter((song) => song.generoId === genre.id);
    els.selectedGenreTitle.textContent = `Musicas de ${genre.nome}`;
    els.selectedGenreSongs.innerHTML = songs.length
        ? songs.map(renderSongCard).join("")
        : '<p class="empty-state">Nenhuma musica vinculada a este genero.</p>';
}

function renderMusicList() {
    els.musicList.innerHTML = state.songs.length
        ? state.songs.map(renderSongCard).join("")
        : '<p class="empty-state">Nenhuma musica cadastrada.</p>';
}

function renderSearchResults() {
    const songs = state.remoteSearchResults || getFilteredSongs();
    els.searchResults.innerHTML = songs.length
        ? songs.map(renderSongCard).join("")
        : '<p class="empty-state">Nenhuma musica encontrada com estes filtros.</p>';
}

function renderSongCard(song) {
    return `
        <article class="song-card">
            ${renderCover(song)}
            <div class="song-main">
                <strong>${escapeHtml(song.titulo)}</strong>
                <div class="song-meta">
                    <span class="tag">${escapeHtml(song.generoNome || genreName(song.generoId))}</span>
                    <span class="tag">Album ${song.albumId}</span>
                    <span class="tag">${formatDuration(song.duracaoSegundos)}</span>
                    <span class="tag">${song.reproducoes || 0} plays</span>
                </div>
                <div class="card-actions">
                    <button class="mini-action is-play" type="button" title="Registrar play" data-action="play-music" data-id="${song.id}">
                        <i data-lucide="play"></i>
                    </button>
                    <button class="mini-action" type="button" title="Editar musica" data-action="edit-music" data-id="${song.id}">
                        <i data-lucide="pencil"></i>
                    </button>
                    <button class="mini-action is-danger" type="button" title="Excluir musica" data-action="delete-music" data-id="${song.id}">
                        <i data-lucide="trash-2"></i>
                    </button>
                </div>
            </div>
        </article>
    `;
}

function renderCover(song) {
    const letter = (song.titulo || "S").trim().charAt(0).toUpperCase() || "S";
    const classes = ["cover-a", "cover-b", "cover-c"];
    const className = classes[(song.id || 0) % classes.length];
    return `<div class="cover ${className}" aria-hidden="true">${escapeHtml(letter)}</div>`;
}

function getFilteredSongs() {
    const title = els.filterTitle.value.trim().toLowerCase();
    const genreId = toOptionalNumber(els.filterGenre.value);
    const albumId = toOptionalNumber(els.filterAlbum.value);

    return state.songs.filter((song) => {
        const matchesTitle = !title || song.titulo.toLowerCase().includes(title);
        const matchesGenre = !genreId || song.generoId === genreId;
        const matchesAlbum = !albumId || song.albumId === albumId;
        return matchesTitle && matchesGenre && matchesAlbum;
    });
}

function resetGenreForm() {
    els.genreForm.reset();
    els.genreId.value = "";
    els.genreFormTitle.textContent = "Novo genero";
    clearFeedback();
}

function resetMusicForm() {
    els.musicForm.reset();
    els.musicId.value = "";
    els.musicFormTitle.textContent = "Nova musica";
    clearFeedback();
}

function validateMusicPayload(payload) {
    if (!payload.titulo) return "Informe o titulo da musica.";
    if (!payload.duracaoSegundos || payload.duracaoSegundos <= 0) return "Duracao deve ser maior que zero.";
    if (payload.numeroFaixa !== null && payload.numeroFaixa <= 0) return "Numero da faixa deve ser maior que zero.";
    if (!payload.albumId || payload.albumId <= 0) return "Informe o album ID.";
    if (!payload.generoId || payload.generoId <= 0) return "Selecione o genero.";
    return "";
}

function normalizeGenre(raw) {
    if (!raw) return null;
    return {
        id: Number(raw.id),
        nome: raw.nome || raw.name || "",
        descricao: raw.descricao || raw.description || ""
    };
}

function normalizeSong(raw) {
    if (!raw) return null;
    const generoId = Number(raw.generoId ?? raw.genero_id ?? raw.genreId ?? raw.genero?.id ?? raw.genre?.id);
    return {
        id: Number(raw.id),
        titulo: raw.titulo || raw.title || "",
        duracaoSegundos: Number(raw.duracaoSegundos ?? raw.duracao_segundos ?? raw.durationSeconds ?? 0),
        numeroFaixa: raw.numeroFaixa ?? raw.numero_faixa ?? raw.trackNumber ?? null,
        reproducoes: Number(raw.reproducoes ?? raw.playCount ?? 0),
        albumId: Number(raw.albumId ?? raw.album_id ?? raw.album?.id ?? 0),
        generoId,
        generoNome: raw.generoNome || raw.genero_nome || raw.genero?.nome || genreName(generoId),
        criadoEm: raw.criadoEm || raw.criado_em || null
    };
}

function unwrapList(payload) {
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.data)) return payload.data;
    return [];
}

function setView(view, updateHash = true) {
    const nextView = ["home", "genres", "songs", "search"].includes(view) ? view : "home";
    state.activeView = nextView;

    els.panels?.forEach((panel) => {
        panel.hidden = panel.dataset.viewPanel !== nextView;
    });

    els.navItems?.forEach((item) => {
        item.classList.toggle("is-active", item.dataset.view === nextView);
    });

    if (updateHash && location.hash !== `#${nextView}`) {
        history.replaceState(null, "", `#${nextView}`);
    }
}

function setConnection(text, className) {
    els.connectionStatus.textContent = text;
    els.connectionStatus.className = `status-pill ${className}`;
}

function setFeedback(element, message) {
    element.textContent = message;
}

function clearFeedback() {
    els.genreFeedback.textContent = "";
    els.musicFeedback.textContent = "";
}

function showToast(message) {
    els.toast.textContent = message;
    els.toast.classList.add("is-visible");
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => {
        els.toast.classList.remove("is-visible");
    }, 3200);
}

function refreshIcons() {
    if (window.lucide) {
        window.lucide.createIcons();
    }
}

function normalizeApiBase(value) {
    const trimmed = (value || "/api").trim();
    if (trimmed === "/") return "";
    return trimmed.replace(/\/$/, "");
}

function toOptionalNumber(value) {
    if (value === "" || value === null || value === undefined) return null;
    const number = Number(value);
    return Number.isFinite(number) ? number : null;
}

function nextId(collection) {
    return Math.max(0, ...collection.map((item) => Number(item.id) || 0)) + 1;
}

function genreName(id) {
    return state.genres.find((genre) => genre.id === id)?.nome || "Genero";
}

function formatDuration(seconds) {
    const total = Number(seconds) || 0;
    const minutes = Math.floor(total / 60);
    const rest = total % 60;
    return `${minutes}:${String(rest).padStart(2, "0")}`;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
