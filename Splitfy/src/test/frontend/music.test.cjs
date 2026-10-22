const { test } = require("node:test");
const assert = require("node:assert/strict");
const vm = require("node:vm");
const fs = require("node:fs");
const path = require("node:path");
const assets = path.resolve(__dirname, "../../main/resources/static/assets");
const app = fs.readFileSync(path.join(assets, "app.js"), "utf8");

function nodes() {
    const map = new Map();
    const node = selector => {
        if (!map.has(selector)) map.set(selector, { value: "", textContent: "", hidden: false, style: {}, scrollIntoView() {} });
        return map.get(selector);
    };
    return { node, document: { querySelector: node } };
}

test("trocar o resultado substitui metadados e invalida o album da selecao anterior", () => {
    const h = nodes();
    const context = { document: h.document, state: { editor: {} } };
    vm.createContext(context);
    vm.runInContext(app.slice(app.indexOf("    function applyMusicResult("), app.indexOf("    function bindEvents(")), context);
    const first = { fonte: "YOUTUBE", titulo: "Primeira", artista: "Cantor A", album: "Album A", url: "primeira", duracaoSegundos: 200 };
    context.applyMusicResult(first);
    context.state.editor.importAlbumId = 5;
    h.node('[name="spotifyUrl"]').value = "fonte-da-faixa-antiga";
    context.applyMusicResult({ ...first, titulo: "Segunda", artista: "Cantor B", album: "Album B", url: "segunda", duracaoSegundos: null });
    assert.equal(h.node('[name="titulo"]').value, "Segunda");
    assert.equal(h.node('[name="duracaoSegundos"]').value, "");
    assert.equal(h.node("#import-artist").value, "Cantor B");
    assert.equal(h.node("#import-album").value, "Album B");
    assert.equal(h.node('[name="spotifyUrl"]').value, "");
    assert.equal(context.state.editor.importAlbumId, null);
});

test("adicionar outra fonte preserva titulo, duracao e album da musica escolhida", () => {
    const h = nodes();
    const context = { document: h.document, state: { editor: {} } };
    vm.createContext(context);
    vm.runInContext(app.slice(app.indexOf("    function applyMusicResult("), app.indexOf("    function bindEvents(")), context);
    context.applyMusicResult({ fonte: "SPOTIFY", titulo: "Faixa", artista: "Cantor", album: "Album", url: "spotify", duracaoSegundos: 210 });
    context.state.editor.importAlbumId = 7;
    context.applyMusicResult({ fonte: "YOUTUBE", titulo: "Faixa (video oficial)", artista: "Canal", url: "youtube", duracaoSegundos: 220 }, true);
    assert.equal(h.node('[name="titulo"]').value, "Faixa");
    assert.equal(h.node('[name="duracaoSegundos"]').value, 210);
    assert.equal(context.state.editor.importAlbumId, 7);
    assert.equal(h.node('[name="spotifyUrl"]').value, "spotify");
    assert.equal(h.node('[name="youtubeUrl"]').value, "youtube");
});

for (const rejects of [false, true]) test(`resposta atrasada do MP3 nao altera a nova faixa (${rejects ? "bloqueio" : "sucesso"})`, async () => {
    const h = nodes();
    let resolveOld, rejectOld;
    const pending = new Promise((resolve, reject) => { resolveOld = resolve; rejectOld = reject; });
    let plays = 0;
    const registered = [];
    const songs = [{ id: 1, titulo: "Antiga" }, { id: 2, titulo: "Atual" }];
    const context = {
        document: h.document, state: { activeUserId: 1, songs }, failedSources: new Set(), playbackRequest: 0,
        window: { splitfyAuth: { getSession: () => ({ token: "teste" }) } },
        songById: id => songs.find(song => song.id === id), userById: () => ({ ativo: true }),
        availableSources: () => ["MP3"], renderPlayer() {}, showToast() {},
        clearAudio() { context.playbackRequest++; },
        registerPlayback: song => registered.push(song.id), URL: { createObjectURL: () => "blob:teste" },
        fetch: async () => ({ ok: true, status: 200, blob: async () => ({}) }),
        audio: { play: () => ++plays === 1 ? pending : Promise.resolve() }
    };
    vm.createContext(context);
    vm.runInContext(app.slice(app.indexOf("    async function playSong("), app.indexOf("    function adjacentSong(")), context);
    const first = context.playSong(1);
    while (plays === 0) await new Promise(resolve => setImmediate(resolve));
    await context.playSong(2);
    if (rejects) { const error = new Error("blocked"); error.name = "NotAllowedError"; rejectOld(error); }
    else resolveOld();
    await first;
    assert.deepEqual(registered, [2]);
    assert.equal(h.node("#provider-status").textContent, "Reproduzindo o MP3 salvo no Splitfy.");
});

function providers() {
    const callbacks = [];
    const embeds = [];
    const host = { replaceChildren() {}, appendChild() {} };
    const context = {
        window: {}, URL, location: { origin: "https://splitfy.test" },
        document: {
            querySelector: selector => selector === "#provider-embed" ? host : selector.includes("input") ? { value: "70" } : null,
            createElement: () => ({}), head: { appendChild() {} }
        },
        setTimeout: () => 1, clearTimeout() {}, setInterval: () => 2, clearInterval() {}
    };
    vm.createContext(context);
    vm.runInContext(fs.readFileSync(path.join(assets, "providers.js"), "utf8"), context);
    const api = { createController(host, options, callback) {
        const listeners = {};
        const embed = { listeners, plays: 0, destroyed: 0, addListener(name, fn) { listeners[name] = fn; },
            play() { this.plays++; }, destroy() { this.destroyed++; } };
        embeds.push(embed); callbacks.push(() => callback(embed));
    } };
    return { context, callbacks, embeds, api, player: context.window.splitfyProviders };
}

test("Spotify pronto depois da troca nao inicia a faixa antiga", async () => {
    const h = providers();
    const events = { onStarted() {}, onState() {}, onProgress() {} };
    const old = h.player.play("SPOTIFY", "https://open.spotify.com/track/primeira", events);
    h.context.window.onSpotifyIframeApiReady(h.api);
    await new Promise(resolve => setImmediate(resolve));
    h.callbacks[0]();
    const current = h.player.play("SPOTIFY", "https://open.spotify.com/track/segunda", events);
    await new Promise(resolve => setImmediate(resolve));
    h.callbacks[1]();
    h.embeds[0].listeners.ready();
    h.embeds[1].listeners.ready();
    await Promise.all([old, current]);
    assert.equal(h.embeds[0].plays, 0);
    assert.equal(h.embeds[1].plays, 1);
    assert.ok(h.embeds[0].destroyed > 0);
});
