import { Utils } from "./utils.js";

const TOKEN_KEY = "vulyk_jwt";

export const AuthStore = {
    getToken() {
        return window.localStorage.getItem(TOKEN_KEY);
    },
    setToken(token) {
        if (token) {
            window.localStorage.setItem(TOKEN_KEY, token);
            return;
        }
        window.localStorage.removeItem(TOKEN_KEY);
    },
    payload() {
        const token = this.getToken();
        if (!token) {
            return null;
        }
        try {
            const parts = token.split(".");
            if (parts.length < 2) {
                return null;
            }
            const payload = parts[1].replaceAll("-", "+").replaceAll("_", "/");
            return JSON.parse(window.atob(payload));
        } catch {
            return null;
        }
    },
    role() {
        const payload = this.payload();
        return payload && payload.role ? payload.role : "UNKNOWN";
    },
    requireAuth() {
        if (!this.getToken()) {
            Utils.go("/login.html");
            return false;
        }
        return true;
    },
};
