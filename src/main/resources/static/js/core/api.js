import { AuthStore } from "./auth-store.js";

export const Api = {
    headers(json) {
        const headers = {};
        const token = AuthStore.getToken();
        if (json) {
            headers["Content-Type"] = "application/json";
        }
        if (token) {
            headers.Authorization = "Bearer " + token;
        }
        return headers;
    },
    async parseError(response) {
        let text = "";
        try {
            text = await response.text();
        } catch {
            text = "Request failed.";
        }
        try {
            const parsed = JSON.parse(text);
            return parsed.message || parsed.error || text || "Request failed.";
        } catch {
            return text || "Request failed.";
        }
    },
    async request(url, options) {
        const response = await fetch(url, options);
        if (!response.ok) {
            throw new Error(await this.parseError(response));
        }
        return response;
    },
    async json(url, method, payload) {
        const response = await this.request(url, {
            method,
            headers: this.headers(true),
            body: payload == null ? undefined : JSON.stringify(payload),
        });
        if (response.status === 204) {
            return null;
        }
        const contentType = response.headers.get("content-type") || "";
        return contentType.indexOf("application/json") >= 0 ? response.json() : null;
    },
    async form(url, method, formData) {
        const response = await this.request(url, {
            method,
            headers: this.headers(false),
            body: formData,
        });
        if (response.status === 204) {
            return null;
        }
        const contentType = response.headers.get("content-type") || "";
        return contentType.indexOf("application/json") >= 0 ? response.json() : null;
    },
    async blob(url) {
        const response = await this.request(url, {
            method: "GET",
            headers: this.headers(false),
        });
        return response.blob();
    },
};
