const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const root = path.resolve(__dirname, "../../main/resources/static");

function worker(fetch) {
    const handlers = {};
    const entries = new Map();
    const deleted = [];
    let claimed = false, activated = false;
    const cache = {
        async put(key, response) { entries.set(key, response); },
        async addAll(requests) { for (const request of requests) assert.ok(fs.existsSync(path.join(root, new URL(request.url).pathname === "/" ? "index.html" : new URL(request.url).pathname))); }
    };
    const context = {
        self: { location: { origin: "https://splitfy.test" }, addEventListener: (name, fn) => handlers[name] = fn,
            clients: { async claim() { claimed = true; } }, skipWaiting() { activated = true; } },
        caches: { open: async () => cache, match: async key => entries.get(key),
            keys: async () => ["splitfy-shell-v0", "splitfy-shell-v1", "other-app"], delete: async name => deleted.push(name) },
        URL, Response, Request: class extends Request { constructor(url, options) { super(new URL(url, "https://splitfy.test"), options); } }, fetch
    };
    vm.runInNewContext(fs.readFileSync(path.join(root, "sw.js"), "utf8"), context);
    return { handlers, entries, deleted, cache, get claimed() { return claimed; }, get activated() { return activated; },
        request(url, options) {
            let result;
            handlers.fetch({ request: new Request(new URL(url, "https://splitfy.test"), options), respondWith(value) { result = value; } });
            return result;
        }
    };
}

test("manifesto define app independente e icones PNG reais de 192 e 512", () => {
    const manifest = JSON.parse(fs.readFileSync(path.join(root, "manifest.webmanifest")));
    assert.equal(manifest.display, "standalone");
    assert.equal(manifest.scope, "/");
    assert.equal(manifest.start_url, "/");
    for (const icon of manifest.icons) {
        const png = fs.readFileSync(path.join(root, icon.src));
        const size = Number(icon.sizes.split("x")[0]);
        assert.equal(png.readUInt32BE(16), size);
        assert.equal(png.readUInt32BE(20), size);
    }
    assert.ok(manifest.icons.some(icon => icon.purpose === "maskable"));
});

test("instalacao so inclui arquivos existentes do shell publico", async () => {
    const w = worker();
    let installed;
    w.handlers.install({ waitUntil(value) { installed = value; } });
    await installed;
    assert.equal(w.activated, false);
});

test("service worker nao intercepta API, MP3, autorizacao ou servicos externos", () => {
    const w = worker(() => { throw new Error("Nao deve consultar a rede"); });
    for (const url of ["/api/musicas", "/api/musicas/4/audio", "/api/auth/me", "https://www.youtube.com/embed/video", "https://open.spotify.com/embed/track/test"]) {
        assert.equal(w.request(url), undefined);
    }
    assert.equal(w.request("/assets/app.js", { headers: { Authorization: "Bearer privado" } }), undefined);
    assert.equal(w.request("/", { method: "POST" }), undefined);
    assert.equal(w.entries.size, 0);
});

test("rede atualiza o shell e modo offline recupera a interface salva", async () => {
    let online = true;
    const w = worker(async () => { if (!online) throw new Error("offline"); return new Response("interface atual"); });
    assert.equal(await (await w.request("/index.html")).text(), "interface atual");
    online = false;
    assert.equal(await (await w.request("/index.html")).text(), "interface atual");
});

test("falha de armazenamento nao bloqueia a interface online", async () => {
    const w = worker(async () => new Response("online"));
    w.cache.put = async () => { throw new Error("quota"); };
    assert.equal(await (await w.request("/index.html")).text(), "online");
});

test("atualizacao remove apenas caches antigos do Splitfy e espera confirmacao", async () => {
    const w = worker();
    let activated;
    w.handlers.activate({ waitUntil(value) { activated = value; } });
    await activated;
    assert.deepEqual(w.deleted, ["splitfy-shell-v0"]);
    assert.equal(w.claimed, true);
    w.handlers.message({ data: { type: "OUTRA" } });
    assert.equal(w.activated, false);
    w.handlers.message({ data: { type: "ACTIVATE_UPDATE" } });
    assert.equal(w.activated, true);
});

function installUi({ standalone = false, apple = false, online = true } = {}) {
    const nodes = new Map(), events = {};
    const node = selector => {
        if (!nodes.has(selector)) nodes.set(selector, { hidden: false, textContent: "", open: false,
            listeners: {}, addEventListener(name, fn) { this.listeners[name] = fn; },
            showModal() { this.open = true; }, close() { this.open = false; } });
        return nodes.get(selector);
    };
    const navigator = { onLine: online, userAgent: apple ? "iPhone" : "Android", platform: "", maxTouchPoints: 0 };
    const context = { navigator, document: { querySelector: node }, window: {
        navigator, addEventListener: (name, fn) => events[name] = fn,
        matchMedia: () => ({ matches: standalone, addEventListener() {} })
    } };
    vm.runInNewContext(fs.readFileSync(path.join(root, "assets/pwa.js"), "utf8"), context);
    return { node, events, navigator };
}

test("instalacao iOS oferece instrucoes e app instalado oculta botao", async () => {
    const h = installUi({ apple: true, online: false });
    await h.node("#install-app").listeners.click();
    assert.match(h.node("#install-guidance").textContent, /Safari/);
    assert.equal(h.node("#install-dialog").open, true);
    assert.equal(h.node("#offline-status").hidden, false);
    const installed = installUi({ standalone: true });
    assert.equal(installed.node("#install-app").hidden, true);
});

test("prompt de instalacao so e acionado ao clicar e respeita instalacao concluida", async () => {
    const h = installUi();
    let prompted = 0;
    h.events.beforeinstallprompt({ preventDefault() {}, prompt: async () => prompted++, userChoice: Promise.resolve({ outcome: "accepted" }) });
    assert.equal(prompted, 0);
    await h.node("#install-app").listeners.click();
    assert.equal(prompted, 1);
    h.events.appinstalled();
    assert.equal(h.node("#install-app").hidden, true);
});
