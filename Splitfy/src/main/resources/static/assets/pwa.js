(() => {
    "use strict";
    const button = document.querySelector("#install-app");
    const dialog = document.querySelector("#install-dialog");
    const guidance = document.querySelector("#install-guidance");
    const offline = document.querySelector("#offline-status");
    const update = document.querySelector("#app-update");
    let installPrompt = null;
    let waitingWorker = null;
    let applyingUpdate = false;
    const standalone = () => window.matchMedia("(display-mode: standalone)").matches || window.navigator.standalone === true;

    function refreshConnection() { offline.hidden = navigator.onLine; }
    function refreshInstall() { button.hidden = standalone(); }

    window.addEventListener("beforeinstallprompt", event => {
        event.preventDefault();
        installPrompt = event;
        refreshInstall();
    });
    window.addEventListener("appinstalled", () => { installPrompt = null; button.hidden = true; dialog.close(); });
    window.matchMedia("(display-mode: standalone)").addEventListener?.("change", refreshInstall);

    button.addEventListener("click", async () => {
        if (installPrompt) {
            const prompt = installPrompt;
            installPrompt = null;
            try { await prompt.prompt(); await prompt.userChoice; }
            catch { showInstructions(); }
        } else showInstructions();
    });

    function showInstructions() {
        const appleMobile = /iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
        guidance.textContent = appleMobile
            ? "No iPhone ou iPad, abra o Splitfy no Safari. Toque em Compartilhar, depois em Adicionar à Tela de Início e confirme Adicionar. Se aparecer Abrir como App da Web, deixe essa opção ativada."
            : "No Android, use o menu do Chrome, Edge ou outro navegador compatível e escolha Instalar aplicativo ou Adicionar à tela inicial. No computador, procure o ícone de instalação na barra de endereço ou a opção Instalar este site como aplicativo no menu. No Safari do Mac, use Arquivo → Adicionar ao Dock. A opção exata depende do navegador.";
        dialog.showModal();
    }

    document.querySelector("#install-close").addEventListener("click", () => dialog.close());
    window.addEventListener("online", refreshConnection);
    window.addEventListener("offline", refreshConnection);
    refreshConnection();
    refreshInstall();

    function offerUpdate(registration) {
        if (!registration.waiting || !navigator.serviceWorker.controller) return;
        waitingWorker = registration.waiting;
        update.hidden = false;
    }
    update.addEventListener("click", () => {
        if (!waitingWorker) return;
        applyingUpdate = true;
        waitingWorker.postMessage({ type: "ACTIVATE_UPDATE" });
    });

    if ("serviceWorker" in navigator && window.isSecureContext) {
        window.addEventListener("load", async () => {
            try {
                const registration = await navigator.serviceWorker.register("/sw.js", { updateViaCache: "none" });
                offerUpdate(registration);
                registration.addEventListener("updatefound", () => {
                    const worker = registration.installing;
                    worker?.addEventListener("statechange", () => { if (worker.state === "installed") offerUpdate(registration); });
                });
                navigator.serviceWorker.addEventListener("controllerchange", () => {
                    if (applyingUpdate) window.location.reload();
                });
                document.addEventListener("visibilitychange", () => { if (!document.hidden) registration.update().catch(() => {}); });
            } catch { /* Online playback and installation guidance still work without offline caching. */ }
        });
    }
})();
