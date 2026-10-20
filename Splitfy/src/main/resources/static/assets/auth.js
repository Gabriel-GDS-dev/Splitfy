(() => {
    "use strict";
    const storageKey = "splitfy.session";
    const dialog = document.querySelector("#login-dialog");
    const form = document.querySelector("#login-form");
    const feedback = document.querySelector("#login-feedback");
    const button = document.querySelector("#session-button");
    let session = null;
    let expiryTimer;

    function decode(token) {
        const part = token.split(".")[1].replaceAll("-", "+").replaceAll("_", "/");
        return JSON.parse(atob(part));
    }

    function updateButton() {
        button.textContent = session ? "Sair" : "Entrar";
        button.title = session ? "Encerrar sessão" : "Entrar com sua conta";
    }

    function clear(message = "") {
        session = null;
        clearTimeout(expiryTimer);
        sessionStorage.removeItem(storageKey);
        updateButton();
        feedback.textContent = message;
        window.dispatchEvent(new Event("splitfy:session"));
    }

    function save(value, notify = true) {
        const claims = decode(value.token);
        if (!claims.sub || !Number.isFinite(claims.exp) || claims.exp * 1000 <= Date.now()) {
            throw new Error("Sessão expirada. Entre novamente.");
        }
        session = { token: value.token, email: value.email, userId: Number(claims.sub),
            role: claims.role, expiresAt: claims.exp * 1000 };
        sessionStorage.setItem(storageKey, JSON.stringify({ token: session.token, email: session.email }));
        clearTimeout(expiryTimer);
        expiryTimer = setTimeout(() => {
            clear("Sua sessão expirou. Entre novamente.");
            open();
        }, Math.min(session.expiresAt - Date.now(), 2147483647));
        updateButton();
        if (notify) window.dispatchEvent(new Event("splitfy:session"));
    }

    function open() {
        if (!dialog.open) dialog.showModal();
    }

    window.splitfyAuth = {
        getSession: () => session,
        open,
        expire: () => {
            if (session) clear("Sua sessão expirou. Entre novamente.");
            open();
        }
    };
    try {
        const saved = JSON.parse(sessionStorage.getItem(storageKey) || "null");
        if (saved) save(saved, false);
    } catch {
        sessionStorage.removeItem(storageKey);
    }
    updateButton();
    button.addEventListener("click", () => {
        if (session) {
            clear();
            form.reset();
        }
        open();
    });
    document.querySelector("#login-close").addEventListener("click", () => dialog.close());
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const submit = document.querySelector("#login-submit");
        submit.disabled = true;
        feedback.textContent = "Entrando…";
        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json", Accept: "application/json" },
                body: JSON.stringify({ email: form.elements.email.value.trim(), senha: form.elements.senha.value })
            });
            if (!response.ok) {
                throw new Error(response.status === 401 ? "E-mail ou senha inválidos." : "Não foi possível entrar. Tente novamente.");
            }
            const data = await response.json();
            save({ token: data.tokenAcesso, email: form.elements.email.value.trim() });
            form.elements.senha.value = "";
            feedback.textContent = "";
            dialog.close();
        } catch (error) {
            feedback.textContent = error.message === "Failed to fetch"
                ? "Não foi possível conectar ao servidor." : error.message;
        } finally {
            submit.disabled = false;
        }
    });
})();
