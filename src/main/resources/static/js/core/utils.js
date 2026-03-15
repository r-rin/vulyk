export const Utils = {
    qs(selector, root) {
        return (root || document).querySelector(selector);
    },
    qsa(selector, root) {
        return Array.from((root || document).querySelectorAll(selector));
    },
    page() {
        return document.body.getAttribute("data-page") || "";
    },
    queryParam(key) {
        return new URLSearchParams(window.location.search).get(key);
    },
    escapeHtml(text) {
        return String(text == null ? "" : text)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    },
    time(value) {
        if (!value) {
            return "-";
        }
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? "-" : date.toLocaleString();
    },
    go(url) {
        window.location.href = url;
    },
    delegated(root, eventType, selector, handler) {
        root.addEventListener(eventType, (event) => {
            const target = event.target.closest(selector);
            if (!target || !root.contains(target)) {
                return;
            }
            handler(target, event);
        });
    },
};
