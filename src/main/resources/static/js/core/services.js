import { Api } from "./api.js";

export const Services = {
    users: {
        me() {
            return Api.json("/users/me", "GET");
        },
        updateMe(payload) {
            return Api.json("/users/me", "PUT", payload);
        },
    },
    auth: {
        login(payload) {
            return Api.json("/login", "POST", payload);
        },
        register(payload) {
            return Api.json("/register", "POST", payload);
        },
        logout() {
            return Api.json("/logout", "POST");
        },
    },
    posts: {
        list(params) {
            return Api.json("/posts?" + params.toString(), "GET");
        },
        get(id) {
            return Api.json("/posts/" + id, "GET");
        },
        create(payload) {
            return Api.json("/posts", "POST", payload);
        },
        update(id, payload) {
            return Api.json("/posts/" + id, "PUT", payload);
        },
        setState(id, state) {
            return Api.json("/posts/" + id + "/state", "PUT", { state });
        },
        delete(id) {
            return Api.json("/posts/" + id, "DELETE");
        },
    },
    comments: {
        list(postId, parentId, options) {
            const opts = options || {};
            const size = opts.size == null ? 20 : opts.size;
            const page = opts.page == null ? 0 : opts.page;
            let url = "/posts/" + postId + "/comments?size=" + encodeURIComponent(size) + "&page=" + encodeURIComponent(page);
            if (parentId != null) {
                url += "&parentCommentId=" + parentId;
            }
            return Api.json(url, "GET");
        },
        create(postId, payload) {
            return Api.json("/posts/" + postId + "/comments", "POST", payload);
        },
        update(commentId, payload) {
            return Api.json("/comments/" + commentId, "PUT", payload);
        },
        delete(commentId) {
            return Api.json("/comments/" + commentId, "DELETE");
        },
    },
    reactions: {
        likePost(postId) {
            return Api.json("/posts/" + postId + "/likes", "POST");
        },
        unlikePost(postId) {
            return Api.json("/posts/" + postId + "/likes", "DELETE");
        },
        postStatus(postId) {
            return Api.json("/posts/" + postId + "/likes", "GET");
        },
        likeComment(commentId) {
            return Api.json("/comments/" + commentId + "/likes", "POST");
        },
        unlikeComment(commentId) {
            return Api.json("/comments/" + commentId + "/likes", "DELETE");
        },
        commentStatus(commentId) {
            return Api.json("/comments/" + commentId + "/likes", "GET");
        },
        likedPosts() {
            return Api.json("/likes/posts?size=20", "GET");
        },
        likedComments() {
            return Api.json("/likes/comments?size=20", "GET");
        },
    },
    marketplace: {
        list(params) {
            return Api.json("/marketplace/items?" + params.toString(), "GET");
        },
        get(id) {
            return Api.json("/marketplace/items/" + id, "GET");
        },
        create(payload) {
            return Api.json("/marketplace/items", "POST", payload);
        },
        update(id, payload) {
            return Api.json("/marketplace/items/" + id, "PUT", payload);
        },
        setStatus(id, status) {
            return Api.json("/marketplace/items/" + id + "/status", "PUT", { status });
        },
        delete(id) {
            return Api.json("/marketplace/items/" + id, "DELETE");
        },
        seller(id) {
            return Api.json("/marketplace/items/" + id + "/seller", "GET");
        },
        contact(id) {
            return Api.json("/marketplace/items/" + id + "/contact", "GET");
        },
        favorite(id) {
            return Api.json("/marketplace/items/" + id + "/favorite", "POST");
        },
        unfavorite(id) {
            return Api.json("/marketplace/items/" + id + "/favorite", "DELETE");
        },
        favoriteStatus(id) {
            return Api.json("/marketplace/items/" + id + "/favorite", "GET");
        },
        favorites() {
            return Api.json("/marketplace/items/favorites?size=50", "GET");
        },
    },
    files: {
        uploadProfile(fd) {
            return Api.form("/files/profile", "POST", fd);
        },
        profileList() {
            return Api.json("/files/profile?size=100", "GET");
        },
        uploadPostAttachment(postId, fd) {
            return Api.form("/files/posts/" + postId, "POST", fd);
        },
        postAttachments(postId) {
            return Api.json("/files/posts/" + postId + "?size=100", "GET");
        },
        delete(id) {
            return Api.json("/files/" + id, "DELETE");
        },
        download(id) {
            return Api.blob("/files/" + id);
        },
    },
};
