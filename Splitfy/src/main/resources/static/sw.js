"use strict";
const CACHE = "splitfy-shell-v2";
const SHELL = ["/", "/index.html", "/manifest.webmanifest", "/assets/styles.css",
    "/assets/lucide.min.js", "/assets/auth.js", "/assets/providers.js", "/assets/app.js",
    "/assets/pwa.js", "/assets/album-covers.png", "/assets/icons/icon.svg",
    "/assets/icons/icon-192.png", "/assets/icons/icon-512.png",
    "/assets/icons/icon-maskable-512.png", "/assets/icons/apple-touch-icon.png"];
const PUBLIC_FILES = new Set(SHELL);

self.addEventListener("install", event => {
    event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(SHELL.map(url => new Request(url, { cache: "reload" })))));
});

self.addEventListener("activate", event => {
    event.waitUntil((async () => {
        const names = await caches.keys();
        await Promise.all(names.filter(name => name.startsWith("splitfy-shell-") && name !== CACHE).map(name => caches.delete(name)));
        await self.clients.claim();
    })());
});

self.addEventListener("message", event => {
    if (event.data?.type === "ACTIVATE_UPDATE") self.skipWaiting();
});

// Cache only the explicit public app shell. Accounts, API responses, tokens,
// uploaded MP3s, remote images and Spotify/YouTube requests never enter Cache Storage.
self.addEventListener("fetch", event => {
    const request = event.request;
    const url = new URL(request.url);
    if (request.method !== "GET" || url.origin !== self.location.origin
            || request.headers.has("Authorization") || !PUBLIC_FILES.has(url.pathname)) return;
    event.respondWith((async () => {
        try {
            const response = await fetch(request, { cache: "no-cache" });
            if (!response.ok) throw new Error("App shell unavailable");
            const cache = await caches.open(CACHE);
            try { await cache.put(url.pathname, response.clone()); } catch { /* Storage pressure must not block a valid online response. */ }
            return response;
        } catch {
            const cached = await caches.match(url.pathname);
            if (cached) return cached;
            return Response.error();
        }
    })());
});
