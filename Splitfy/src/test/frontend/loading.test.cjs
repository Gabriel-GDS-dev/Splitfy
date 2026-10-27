const { test } = require("node:test");
const assert = require("node:assert/strict");
const vm = require("node:vm");
const fs = require("node:fs");
const path = require("node:path");
const app = fs.readFileSync(path.resolve(__dirname, "../../main/resources/static/assets/app.js"), "utf8");

function harness(request) {
    const context = { state: { activeUserId: 7, songs: Array.from({ length: 200 }, (_, id) => ({ id })) }, request };
    vm.createContext(context);
    vm.runInContext(app.slice(app.indexOf("    function unwrapList("), app.indexOf("    function extractError("))
        + app.slice(app.indexOf("    async function loadUserData("), app.indexOf("    async function loadProfiles(")), context);
    return context;
}

test("200 musicas carregam avaliacoes e historico com apenas duas requisicoes", async () => {
    const paths = [];
    const h = harness(async path => {
        paths.push(path);
        return path.includes("avaliacoes") ? [{ musicaId: 3, curtido: true }] : [{ musicaId: 4 }];
    });
    await h.loadUserData();
    assert.deepEqual(paths, ["/api/avaliacoes/usuarios/7", "/api/usuarios/7/historico?limite=100"]);
    assert.equal(h.state.reviews[0].musicaId, 3);
    assert.equal(h.state.history[0].musicaId, 4);
});

test("atualizar historico nao consulta avaliacoes e descarta resposta de outro usuario", async () => {
    const paths = [];
    let resolve;
    const h = harness(path => { paths.push(path); return new Promise(done => { resolve = done; }); });
    h.state.history = [];
    const pending = h.loadHistory();
    h.state.activeUserId = 8;
    resolve([{ musicaId: 4 }]);
    await pending;
    assert.deepEqual(paths, ["/api/usuarios/7/historico?limite=100"]);
    assert.equal(h.state.history.length, 0);
});

test("sem usuario nao consulta o servidor", async () => {
    const h = harness(() => { throw new Error("Requisicao inesperada"); });
    h.state.activeUserId = null;
    await h.loadUserData();
    await h.loadHistory();
    assert.equal(h.state.reviews.length, 0);
    assert.equal(h.state.history.length, 0);
});
