const { test } = require("node:test");
const assert = require("node:assert/strict");
const vm = require("node:vm");
const fs = require("node:fs");
const path = require("node:path");
const assets = path.resolve(__dirname, "../../main/resources/static/assets");
const token = (role = "USER", exp = Date.now() / 1000 + 3600) =>
    "header." + Buffer.from(JSON.stringify({ sub: "7", role, exp })).toString("base64url") + ".signature";

function harness(saved, response) {
    const nodes = new Map();
    const node = (selector) => {
        if (!nodes.has(selector)) nodes.set(selector, {
            open: false, value: "", textContent: "", disabled: false, listeners: {},
            addEventListener(name, fn) { this.listeners[name] = fn; },
            showModal() { this.open = true; }, close() { this.open = false; }, reset() {}
        });
        return nodes.get(selector);
    };
    const storage = new Map(saved ? [["splitfy.session", JSON.stringify(saved)]] : []);
    const calls = [];
    const form = node("#login-form");
    form.elements = { email: { value: "teste@example.test" }, senha: { value: "senha-de-teste" },
        nome: { value: "Teste" }, dataNascimento: { value: "2000-01-01" } };
    const context = {
        document: { querySelector: node },
        window: { dispatchEvent() {} },
        Event: class {},
        sessionStorage: { getItem: k => storage.get(k), setItem: (k, v) => storage.set(k, v), removeItem: k => storage.delete(k) },
        atob: v => Buffer.from(v, "base64").toString(),
        setTimeout(fn) { context.expiry = fn; return 1; }, clearTimeout() {},
        fetch: async (...args) => { calls.push(args); return response; }
    };
    vm.createContext(context);
    vm.runInContext(fs.readFileSync(path.join(assets, "auth.js"), "utf8"), context);
    return { context, node, storage, calls, auth: context.window.splitfyAuth,
        submit: () => form.listeners.submit({ preventDefault() {} }) };
}

test("login envia credenciais, mantém somente token e limpa a senha", async () => {
    const h = harness(null, { ok: true, json: async () => ({ tokenAcesso: token() }) });
    await h.submit();
    assert.equal(h.calls[0][0], "/api/auth/login");
    assert.equal(JSON.parse(h.calls[0][1].body).senha, "senha-de-teste");
    assert.equal(h.auth.getSession().userId, 7);
    assert.equal(h.node("#login-form").elements.senha.value, "");
    assert.equal(h.storage.get("splitfy.session").includes("senha-de-teste"), false);
    assert.equal(h.node("#session-button").textContent, "Sair");
    h.node("#session-button").listeners.click();
    assert.equal(h.auth.getSession(), null);
    assert.equal(h.storage.size, 0);
});

test("login inválido e token expirado não criam sessão", async () => {
    const h = harness(null, { ok: false, status: 401 });
    await h.submit();
    assert.equal(h.auth.getSession(), null);
    assert.match(h.node("#login-feedback").textContent, /inválidos/);
    const expired = harness({ token: token("USER", 1), email: "teste@example.test" });
    assert.equal(expired.auth.getSession(), null);
    assert.equal(expired.storage.size, 0);
});

test("restaura sessão e retorna ao login ao expirar", () => {
    const h = harness({ token: token("ADMIN"), email: "teste@example.test" });
    assert.equal(h.auth.getSession().role, "ADMIN");
    h.context.expiry();
    assert.equal(h.auth.getSession(), null);
    assert.equal(h.node("#login-dialog").open, true);
});

test("cadastro público cria conta sem solicitar permissão ADMIN e depois faz login", async () => {
    const h = harness(null, { ok: true, json: async () => ({ tokenAcesso: token() }) });
    h.node("#signup-toggle").listeners.click();
    assert.equal(h.node("#signup-name-field").hidden, false);
    await h.submit();
    assert.equal(h.calls[0][0], "/api/usuarios");
    assert.equal(h.calls[1][0], "/api/auth/login");
    assert.equal(Object.hasOwn(JSON.parse(h.calls[0][1].body), "role"), false);
    assert.equal(h.auth.getSession().role, "USER");
});

function apiHarness(session, status = 200) {
    let calls = 0;
    let headers;
    let expired = false;
    const source = fs.readFileSync(path.join(assets, "app.js"), "utf8");
    const request = source.slice(source.indexOf("    async function request("), source.indexOf("    function setConnection("));
    const context = {
        API_BASE_URL: "", window: { splitfyAuth: { getSession: () => session, expire: () => { expired = true; } } },
        extractError: () => "Erro",
        fetch: async (url, options) => {
            calls++; headers = options.headers;
            return { status, ok: status === 200, text: async () => "[]" };
        }
    };
    vm.createContext(context);
    vm.runInContext(request, context);
    return { request: context.request, get calls() { return calls; }, get headers() { return headers; }, get expired() { return expired; } };
}

test("API não faz requisição sem sessão e envia Bearer quando autenticada", async () => {
    const guest = apiHarness(null);
    await assert.rejects(guest.request("/api/generos"), /Entre/);
    assert.equal(guest.calls, 0);
    const logged = apiHarness({ token: "token-de-teste" });
    await logged.request("/api/generos");
    assert.equal(logged.headers.Authorization, "Bearer token-de-teste");
});

test("401 solicita novo login; 403 informa falta de permissão", async () => {
    const unauthorized = apiHarness({ token: "token-de-teste" }, 401);
    await assert.rejects(unauthorized.request("/api/generos"), /expirou/);
    assert.equal(unauthorized.expired, true);
    const forbidden = apiHarness({ token: "token-de-teste" }, 403);
    await assert.rejects(forbidden.request("/api/usuarios"), /permissão/);
    assert.equal(forbidden.expired, false);
});
