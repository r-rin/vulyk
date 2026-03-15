import { Utils } from "./utils.js";

export const Ui = {
    notify(message) {
        const el = Utils.qs("#global-status");
        if (el) {
            el.textContent = message;
        }
    },
};
