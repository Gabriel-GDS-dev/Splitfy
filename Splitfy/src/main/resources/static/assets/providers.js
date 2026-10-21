(() => {
    "use strict";
    let spotifyApi, youtubeApi, controller, timer;
    let generation = 0, playing = false, provider;

    function loadApi(source) {
        if (source === "SPOTIFY" && spotifyApi) return spotifyApi;
        if (source === "YOUTUBE" && youtubeApi) return youtubeApi;
        const promise = new Promise((resolve, reject) => {
            const script = document.createElement("script");
            const timeout = setTimeout(() => reject(new Error("O player demorou para carregar.")), 12000);
            const ready = (value) => { clearTimeout(timeout); resolve(value); };
            script.onerror = () => { clearTimeout(timeout); reject(new Error("Não foi possível acessar o serviço.")); };
            if (source === "SPOTIFY") {
                window.onSpotifyIframeApiReady = ready;
                script.src = "https://open.spotify.com/embed/iframe-api/v1";
            } else {
                window.onYouTubeIframeAPIReady = () => ready(window.YT);
                script.src = "https://www.youtube.com/iframe_api";
            }
            document.head.appendChild(script);
        });
        if (source === "SPOTIFY") spotifyApi = promise.catch((error) => { spotifyApi = null; throw error; });
        else youtubeApi = promise.catch((error) => { youtubeApi = null; throw error; });
        return source === "SPOTIFY" ? spotifyApi : youtubeApi;
    }

    function stop() {
        generation += 1;
        clearInterval(timer);
        try { controller?.destroy(); } catch {}
        controller = null;
        playing = false;
        provider = null;
        document.querySelector("#provider-embed").replaceChildren();
    }

    async function play(source, url, callbacks) {
        stop();
        const version = generation;
        provider = source;
        const api = await loadApi(source);
        if (version !== generation) return;
        const host = document.createElement("div");
        document.querySelector("#provider-embed").appendChild(host);
        const valid = () => version === generation;
        if (source === "SPOTIFY") {
            const id = new URL(url).pathname.split("/").pop();
            await new Promise((resolve, reject) => {
                const timeout = setTimeout(() => reject(new Error("Spotify não respondeu.")), 12000);
                api.createController(host, { uri: `spotify:track:${id}`, width: "100%", height: 152 }, (embed) => {
                    if (!valid()) { clearTimeout(timeout); embed.destroy(); resolve(); return; }
                    controller = embed;
                    embed.addListener("ready", () => { clearTimeout(timeout); if (valid()) { embed.play(); resolve(); } });
                    embed.addListener("playback_started", () => { if (valid()) callbacks.onStarted(); });
                    embed.addListener("playback_update", (event) => {
                        if (!valid()) return;
                        playing = !event.data.isPaused && (event.data.duration <= 0 || event.data.position < event.data.duration);
                        callbacks.onState(playing);
                        callbacks.onProgress(event.data.position / 1000, event.data.duration / 1000);
                    });
                });
            });
        } else {
            const id = new URL(url).searchParams.get("v");
            await new Promise((resolve, reject) => {
                const timeout = setTimeout(() => reject(new Error("YouTube não respondeu.")), 12000);
                controller = new api.Player(host, {
                    width: "100%", height: 300, videoId: id,
                    playerVars: { playsinline: 1, origin: location.origin },
                    events: {
                        onReady: (event) => {
                            clearTimeout(timeout);
                            if (!valid()) { event.target.destroy(); resolve(); return; }
                            event.target.setVolume(Number(document.querySelector('.player-volume input').value));
                            event.target.playVideo();
                            resolve();
                        },
                        onStateChange: (event) => {
                            if (!valid()) return;
                            playing = event.data === 1;
                            callbacks.onState(playing);
                            if (playing) callbacks.onStarted();
                        },
                        onError: () => {
                            clearTimeout(timeout);
                            if (valid()) callbacks.onError(new Error("O vídeo não está disponível para reprodução neste site."));
                            resolve();
                        },
                        onAutoplayBlocked: () => { if (valid()) callbacks.onState(false); }
                    }
                });
                timer = setInterval(() => {
                    if (valid() && controller?.getCurrentTime) callbacks.onProgress(controller.getCurrentTime(), controller.getDuration());
                }, 500);
            });
        }
        if (valid()) {
            const frame = document.querySelector("#provider-embed iframe");
            if (frame) { frame.title = source === "SPOTIFY" ? "Player oficial Spotify" : "Player oficial YouTube"; frame.setAttribute("referrerpolicy", "strict-origin-when-cross-origin"); }
        }
    }

    window.splitfyProviders = {
        play, stop,
        toggle() {
            if (!controller) return;
            if (provider === "SPOTIFY") controller.togglePlay();
            else if (playing) controller.pauseVideo();
            else controller.playVideo();
        },
        volume(value) { if (provider === "YOUTUBE") controller?.setVolume?.(value); }
    };
})();
