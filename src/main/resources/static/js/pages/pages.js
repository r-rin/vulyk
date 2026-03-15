import { Utils } from "../core/utils.js";
import { AuthStore } from "../core/auth-store.js";
import { Ui } from "../core/ui.js";
import { Services } from "../core/services.js";
import { Render } from "../core/render.js";

async function downloadFile(fileId, fileNameHint) {
    const blob = await Services.files.download(fileId);
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = fileNameHint || ("file-" + fileId);
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
}

async function refreshReactionPanels(likedPostsEl, likedCommentsEl) {
    const likedPosts = await Services.reactions.likedPosts();
    likedPostsEl.innerHTML = Render.reactions(likedPosts.content || [], "No liked posts.");
    const likedComments = await Services.reactions.likedComments();
    likedCommentsEl.innerHTML = Render.reactions(likedComments.content || [], "No liked comments.");
}

const Pages = {
    async dashboard() {
        Ui.notify(
            AuthStore.getToken()
                ? "Session: AUTHENTICATED | Role: " + AuthStore.role()
                : "Session: ANONYMOUS"
        );
    },

    async login() {
        const form = Utils.qs("#login-form");
        if (!form) {
            return;
        }
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const fd = new FormData(form);
            try {
                const auth = await Services.auth.login({
                    identifier: String(fd.get("identifier") || "").trim(),
                    password: String(fd.get("password") || ""),
                });
                AuthStore.setToken(auth && auth.token);
                Ui.notify("Login successful.");
                Utils.go("/profile.html");
            } catch (err) {
                Ui.notify("Login failed: " + err.message);
            }
        });
    },

    async register() {
        const form = Utils.qs("#register-form");
        if (!form) {
            return;
        }
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const fd = new FormData(form);
            try {
                const auth = await Services.auth.register({
                    username: String(fd.get("username") || "").trim(),
                    email: String(fd.get("email") || "").trim(),
                    password: String(fd.get("password") || ""),
                    name: String(fd.get("name") || "").trim(),
                    bio: String(fd.get("bio") || "").trim(),
                    phoneNumber: String(fd.get("phoneNumber") || "").trim(),
                });
                AuthStore.setToken(auth && auth.token);
                Ui.notify("Registration successful.");
                Utils.go("/profile.html");
            } catch (err) {
                Ui.notify("Registration failed: " + err.message);
            }
        });
    },

    async logout() {
        try {
            await Services.auth.logout();
        } catch {
            // Stateless logout can ignore API errors.
        }
        AuthStore.setToken(null);
        Ui.notify("Logged out.");
    },

    async profile() {
        if (!AuthStore.requireAuth()) {
            return;
        }
        try {
            const me = await Services.users.me();
            Utils.qs("#profile-table").innerHTML = Render.profileTable(me);

            const postParams = new URLSearchParams();
            postParams.set("size", "100");
            ["PUBLISHED", "DRAFT", "HIDDEN", "REDACTED"].forEach((state) => {
                postParams.append("state", state);
            });
            const postsPage = await Services.posts.list(postParams);
            const ownPosts = (postsPage.content || []).filter(
                (post) => post.authorUsername === me.username
            );
            Utils.qs("#profile-posts").innerHTML = Render.posts(ownPosts, true);

            const ownItems = await Services.marketplace.list(
                new URLSearchParams("ownOnly=true&size=100")
            );
            Utils.qs("#profile-market").innerHTML = Render.marketItems(ownItems.content || [], true);

            const uploads = await Services.files.profileList();
            Utils.qs("#profile-uploads").innerHTML = Render.fileList(
                uploads.content || [],
                "No profile uploads found.",
                true,
                "profile"
            );

            const likedPostsEl = Utils.qs("#profile-liked-posts");
            const likedCommentsEl = Utils.qs("#profile-liked-comments");
            await refreshReactionPanels(likedPostsEl, likedCommentsEl);

            const favorites = await Services.marketplace.favorites();
            Utils.qs("#profile-favorites").innerHTML = Render.marketItems(favorites.content || [], true);

            Utils.delegated(document.body, "click", "[data-action='download-file']", async (button) => {
                try {
                    await downloadFile(
                        button.getAttribute("data-file-id"),
                        "profile-file-" + button.getAttribute("data-file-id")
                    );
                    Ui.notify("File downloaded.");
                } catch (err) {
                    Ui.notify("File download failed: " + err.message);
                }
            });

            Utils.delegated(document.body, "click", "[data-action='delete-file']", async (button) => {
                try {
                    await Services.files.delete(button.getAttribute("data-file-id"));
                    const refreshed = await Services.files.profileList();
                    Utils.qs("#profile-uploads").innerHTML = Render.fileList(
                        refreshed.content || [],
                        "No profile uploads found.",
                        true,
                        "profile"
                    );
                    Ui.notify("File deleted.");
                } catch (err) {
                    Ui.notify("File delete failed: " + err.message);
                }
            });

            Ui.notify("Profile loaded.");
        } catch (err) {
            Ui.notify("Failed to load profile: " + err.message);
        }
    },

    async profileEdit() {
        if (!AuthStore.requireAuth()) {
            return;
        }
        const editForm = Utils.qs("#profile-edit-form");
        const pictureForm = Utils.qs("#profile-picture-form");
        try {
            const me = await Services.users.me();
            editForm.username.value = me.username || "";
            editForm.email.value = me.email || "";
            editForm.name.value = me.name || "";
            editForm.phoneNumber.value = me.phoneNumber || "";
            editForm.bio.value = me.bio || "";
        } catch (err) {
            Ui.notify("Unable to load profile for editing: " + err.message);
        }

        editForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const fd = new FormData(editForm);
            try {
                await Services.users.updateMe({
                    username: String(fd.get("username") || "").trim(),
                    email: String(fd.get("email") || "").trim(),
                    name: String(fd.get("name") || "").trim(),
                    phoneNumber: String(fd.get("phoneNumber") || "").trim(),
                    bio: String(fd.get("bio") || "").trim(),
                });
                Ui.notify("Profile updated.");
            } catch (err) {
                Ui.notify("Profile update failed: " + err.message);
            }
        });

        pictureForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const fd = new FormData(pictureForm);
            const file = fd.get("file");
            if (!file || !file.size) {
                Ui.notify("Select a file first.");
                return;
            }
            try {
                await Services.files.uploadProfile(fd);
                pictureForm.reset();
                Ui.notify("Profile picture uploaded.");
            } catch (err) {
                Ui.notify("Picture upload failed: " + err.message);
            }
        });
    },

    async posts() {
        if (!AuthStore.requireAuth()) {
            return;
        }

        const me = await Services.users.me();
        const createForm = Utils.qs("#post-create-form");
        const filterForm = Utils.qs("#post-filter-form");
        const listEl = Utils.qs("#posts-list");
        const likedPostsEl = Utils.qs("#liked-posts");
        const likedCommentsEl = Utils.qs("#liked-comments");
        const ROOT_COMMENTS_BATCH = 10;
        const REPLIES_BATCH = 5;

        const refreshPostLikeStatus = async (postId) => {
            try {
                const status = await Services.reactions.postStatus(postId);
                listEl
                    .querySelectorAll("[data-action='toggle-like-post'][data-post-id='" + postId + "']")
                    .forEach((button) => {
                        button.dataset.liked = String(!!status.liked);
                        button.setAttribute("aria-pressed", status.liked ? "true" : "false");
                        button.classList.toggle("is-active", !!status.liked);
                    });
                const countEl = document.getElementById("post-like-count-" + postId);
                if (countEl) {
                    countEl.textContent = String(status.count || 0);
                }
            } catch {
                // Ignore status refresh errors.
            }
        };

        const refreshCommentLikeStatus = async (commentId) => {
            try {
                const status = await Services.reactions.commentStatus(commentId);
                listEl
                    .querySelectorAll(
                        "[data-action='toggle-like-comment'][data-comment-id='" + commentId + "']"
                    )
                    .forEach((button) => {
                        button.dataset.liked = String(!!status.liked);
                        button.setAttribute("aria-pressed", status.liked ? "true" : "false");
                        button.classList.toggle("is-active", !!status.liked);
                    });
                const countEl = document.getElementById("comment-like-count-" + commentId);
                if (countEl) {
                    countEl.textContent = String(status.count || 0);
                }
            } catch {
                // Ignore status refresh errors.
            }
        };

        const loadComments = async (postId, page) => {
            const commentsHost = listEl.querySelector("[data-post-comments='" + postId + "']");
            const control = listEl.querySelector(
                "[data-action='load-more-comments'][data-post-id='" + postId + "']"
            );
            if (!commentsHost) {
                return;
            }
            if (page === 0) {
                commentsHost.innerHTML = '<div class="loading">Loading comments...</div>';
            }
            const pageData = await Services.comments.list(postId, null, {
                size: ROOT_COMMENTS_BATCH,
                page,
            });
            const comments = pageData.content || [];
            if (page === 0) {
                commentsHost.innerHTML = Render.commentRows(comments, postId, me.username, false, 0);
            } else if (comments.length) {
                commentsHost.insertAdjacentHTML(
                    "beforeend",
                    Render.commentRows(comments, postId, me.username, false, 0)
                );
            }
            comments.forEach((comment) => {
                refreshCommentLikeStatus(comment.id);
            });
            if (control) {
                control.hidden = !comments.length || !!pageData.last;
                control.dataset.nextPage = String(page + 1);
            }
        };

        const loadReplies = async (postId, commentId, page, depth) => {
            const host = listEl.querySelector("[data-replies-for='" + commentId + "']");
            const control = listEl.querySelector(
                "[data-action='load-more-replies'][data-comment-id='" + commentId + "']"
            );
            if (!host) {
                return;
            }
            if (page === 0) {
                host.innerHTML = '<div class="loading">Loading replies...</div>';
            }
            const pageData = await Services.comments.list(postId, commentId, {
                size: REPLIES_BATCH,
                page,
            });
            const replies = pageData.content || [];
            if (page === 0) {
                host.innerHTML = replies.length
                    ? Render.commentRows(replies, postId, me.username, false, depth)
                    : "";
            } else if (replies.length) {
                host.insertAdjacentHTML(
                    "beforeend",
                    Render.commentRows(replies, postId, me.username, false, depth)
                );
            }
            replies.forEach((reply) => {
                refreshCommentLikeStatus(reply.id);
            });
            if (control) {
                control.hidden = !replies.length || !!pageData.last;
                control.dataset.nextPage = String(page + 1);
            }
        };

        const loadPosts = async () => {
            const fd = new FormData(filterForm);
            const params = new URLSearchParams();
            params.set("size", "30");
            const q = String(fd.get("q") || "").trim();
            const state = String(fd.get("state") || "").trim();
            if (q) {
                params.set("q", q);
            }
            if (state) {
                params.set("state", state);
            }
            const pageData = await Services.posts.list(params);
            const posts = pageData.content || [];
            listEl.innerHTML = Render.posts(posts, false);
            posts.forEach((post) => {
                refreshPostLikeStatus(post.id);
            });
        };

        createForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const fd = new FormData(createForm);
            try {
                await Services.posts.create({
                    title: String(fd.get("title") || "").trim(),
                    content: String(fd.get("content") || "").trim(),
                });
                createForm.reset();
                await loadPosts();
                Ui.notify("Post created.");
            } catch (err) {
                Ui.notify("Post creation failed: " + err.message);
            }
        });

        filterForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            try {
                await loadPosts();
                Ui.notify("Post list updated.");
            } catch (err) {
                Ui.notify("Post list loading failed: " + err.message);
            }
        });

        Utils.delegated(listEl, "click", "[data-action]", async (button) => {
            const action = button.getAttribute("data-action");
            const postId = button.getAttribute("data-post-id");
            const commentId = button.getAttribute("data-comment-id");
            try {
                if (action === "toggle-like-post" && postId) {
                    if (button.dataset.liked === "true") {
                        await Services.reactions.unlikePost(postId);
                    } else {
                        await Services.reactions.likePost(postId);
                    }
                    await refreshPostLikeStatus(postId);
                    await refreshReactionPanels(likedPostsEl, likedCommentsEl);
                    Ui.notify("Post reaction updated.");
                } else if (action === "toggle-comments" && postId) {
                    const panel = listEl.querySelector("[data-comment-panel='" + postId + "']");
                    panel.hidden = !panel.hidden;
                    if (!panel.hidden) {
                        await loadComments(postId, 0);
                    }
                } else if (action === "toggle-like-comment" && commentId) {
                    if (button.dataset.liked === "true") {
                        await Services.reactions.unlikeComment(commentId);
                    } else {
                        await Services.reactions.likeComment(commentId);
                    }
                    await refreshCommentLikeStatus(commentId);
                    await refreshReactionPanels(likedPostsEl, likedCommentsEl);
                    Ui.notify("Comment reaction updated.");
                } else if (action === "toggle-reply" && commentId) {
                    const replyForm = listEl.querySelector(
                        "[data-action='reply-comment'][data-comment-id='" + commentId + "']"
                    );
                    if (replyForm) {
                        replyForm.hidden = !replyForm.hidden;
                    }
                } else if (action === "load-more-comments" && postId) {
                    const nextPage = Number(button.dataset.nextPage || "0");
                    await loadComments(postId, nextPage);
                } else if (action === "load-more-replies" && postId && commentId) {
                    const nextPage = Number(button.dataset.nextPage || "0");
                    const depth = Number(button.dataset.depth || "1");
                    await loadReplies(postId, commentId, nextPage, depth);
                }
            } catch (err) {
                Ui.notify("Action failed: " + err.message);
            }
        });

        Utils.delegated(listEl, "submit", "form[data-action]", async (form, event) => {
            event.preventDefault();
            const action = form.getAttribute("data-action");
            try {
                if (action === "create-comment") {
                    const createPostId = form.getAttribute("data-post-id");
                    const createFd = new FormData(form);
                    await Services.comments.create(createPostId, {
                        content: String(createFd.get("content") || "").trim(),
                    });
                    form.reset();
                    const commentsControl = listEl.querySelector(
                        "[data-action='load-more-comments'][data-post-id='" + createPostId + "']"
                    );
                    if (commentsControl) {
                        commentsControl.dataset.nextPage = "0";
                    }
                    await loadComments(createPostId, 0);
                    Ui.notify("Comment created.");
                } else if (action === "reply-comment") {
                    const replyPostId = form.getAttribute("data-post-id");
                    const parentId = form.getAttribute("data-comment-id");
                    const replyFd = new FormData(form);
                    await Services.comments.create(replyPostId, {
                        content: String(replyFd.get("content") || "").trim(),
                        parentCommentId: Number(parentId),
                    });
                    form.reset();
                    form.hidden = true;
                    const control = listEl.querySelector(
                        "[data-action='load-more-replies'][data-comment-id='" + parentId + "']"
                    );
                    if (control) {
                        control.dataset.nextPage = "0";
                    }
                    const depth = Number((control && control.dataset.depth) || "1");
                    await loadReplies(replyPostId, parentId, 0, depth);
                    Ui.notify("Reply created.");
                }
            } catch (err) {
                Ui.notify("Form action failed: " + err.message);
            }
        });

        try {
            await loadPosts();
            await refreshReactionPanels(likedPostsEl, likedCommentsEl);
            Ui.notify("Posts loaded.");
        } catch (err) {
            Ui.notify("Failed to load posts: " + err.message);
        }
    },

    async marketplace() {
        if (!AuthStore.requireAuth()) {
            return;
        }
        const createForm = Utils.qs("#market-create-form");
        const filterForm = Utils.qs("#market-filter-form");
        const listEl = Utils.qs("#market-list");
        const favoritesEl = Utils.qs("#market-favorites");

        const refreshFavoriteStatus = async (itemId) => {
            try {
                const status = await Services.marketplace.favoriteStatus(itemId);
                const el = Utils.qs("#item-fav-status-" + itemId);
                if (el) {
                    el.textContent =
                        "favorite=" + (status.favorite ? "yes" : "no") + " total=" + status.count;
                }
            } catch {
                // Ignore status refresh errors.
            }
        };

        const loadFavorites = async () => {
            const pageData = await Services.marketplace.favorites();
            favoritesEl.innerHTML = Render.marketItems(pageData.content || [], true);
        };

        const loadItems = async () => {
            const fd = new FormData(filterForm);
            const params = new URLSearchParams();
            params.set("size", "50");
            ["q", "category", "status", "sortBy", "sortDir", "minPrice", "maxPrice"].forEach(
                (key) => {
                    const value = String(fd.get(key) || "").trim();
                    if (value) {
                        params.set(key, value);
                    }
                }
            );
            if (fd.get("ownOnly")) {
                params.set("ownOnly", "true");
            }
            const pageData = await Services.marketplace.list(params);
            const items = pageData.content || [];
            listEl.innerHTML = Render.marketItems(items, false);
            items.forEach((item) => {
                refreshFavoriteStatus(item.id);
            });
        };

        createForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const fd = new FormData(createForm);
            try {
                await Services.marketplace.create({
                    title: String(fd.get("title") || "").trim(),
                    description: String(fd.get("description") || "").trim(),
                    category: String(fd.get("category") || "").trim(),
                    price: Number(fd.get("price")),
                    status: "AVAILABLE",
                });
                createForm.reset();
                await loadItems();
                await loadFavorites();
                Ui.notify("Marketplace item created.");
            } catch (err) {
                Ui.notify("Item creation failed: " + err.message);
            }
        });

        filterForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            try {
                await loadItems();
                Ui.notify("Marketplace list updated.");
            } catch (err) {
                Ui.notify("Marketplace loading failed: " + err.message);
            }
        });

        Utils.delegated(listEl, "click", "[data-action]", async (button) => {
            const action = button.getAttribute("data-action");
            const itemId = button.getAttribute("data-item-id");
            try {
                if (action === "favorite-item" && itemId) {
                    await Services.marketplace.favorite(itemId);
                    await refreshFavoriteStatus(itemId);
                    await loadFavorites();
                    Ui.notify("Item favorited.");
                } else if (action === "unfavorite-item" && itemId) {
                    await Services.marketplace.unfavorite(itemId);
                    await refreshFavoriteStatus(itemId);
                    await loadFavorites();
                    Ui.notify("Item unfavorited.");
                } else if (action === "contact-seller" && itemId) {
                    const contact = await Services.marketplace.contact(itemId);
                    Ui.notify("Contact: " + contact.sellerUsername + " <" + contact.sellerEmail + ">");
                }
            } catch (err) {
                Ui.notify("Action failed: " + err.message);
            }
        });

        try {
            await loadItems();
            await loadFavorites();
            Ui.notify("Marketplace loaded.");
        } catch (err) {
            Ui.notify("Failed to load marketplace: " + err.message);
        }
    },

    async postDetail() {
        if (!AuthStore.requireAuth()) {
            return;
        }
        const postId = Utils.queryParam("id");
        if (!postId) {
            Ui.notify("Missing post id.");
            return;
        }

        const me = await Services.users.me();
        let post = await Services.posts.get(postId);
        const isOwner = me.username && post.authorUsername === me.username;
        let editMode = false;
        let draft = {
            title: post.title,
            content: post.content,
            state: post.state,
        };
        let attachments = [];

        const detailEl = Utils.qs("#post-detail");
        const fileSection = Utils.qs("#post-files-section");
        const fileForm = Utils.qs("#post-file-form");
        const filesEl = Utils.qs("#post-files");
        const commentsEl = Utils.qs("#post-comments");
        const commentForm = Utils.qs("#post-comment-form");
            const commentsLoadMoreBtn = Utils.qs("#post-comments-load-more");
            const ROOT_COMMENTS_BATCH = 10;
            const REPLIES_BATCH = 5;

        const loadPostLikeStatus = async () => {
            const status = await Services.reactions.postStatus(postId);
            const button = Utils.qs("#post-like-toggle");
            const countEl = Utils.qs("#post-detail-like-count");
            if (button) {
                button.dataset.liked = String(!!status.liked);
                button.setAttribute("aria-pressed", status.liked ? "true" : "false");
                button.classList.toggle("is-active", !!status.liked);
            }
            if (countEl) {
                countEl.textContent = String(status.count || 0);
            }
        };

        const renderPost = () => {
            detailEl.innerHTML = Render.postDetail(post, {
                isOwner,
                editMode,
                draft,
            });
        };

        const syncAttachments = () => {
            const hasFiles = attachments.length > 0;
            fileSection.hidden = !editMode && !hasFiles;
            fileForm.hidden = !(editMode && isOwner);
            filesEl.innerHTML = Render.fileList(
                attachments,
                editMode ? "No files attached yet." : "No files attached.",
                editMode && isOwner,
                "post"
            );
        };

        const loadFiles = async () => {
            const pageData = await Services.files.postAttachments(postId);
            attachments = pageData.content || [];
            syncAttachments();
        };

        const refreshCommentLikeStatus = async (commentId) => {
            try {
                const status = await Services.reactions.commentStatus(commentId);
                commentsEl
                    .querySelectorAll(
                        "[data-action='toggle-like-comment'][data-comment-id='" + commentId + "']"
                    )
                    .forEach((button) => {
                        button.dataset.liked = String(!!status.liked);
                        button.setAttribute("aria-pressed", status.liked ? "true" : "false");
                        button.classList.toggle("is-active", !!status.liked);
                    });
                const countEl = document.getElementById("comment-like-count-" + commentId);
                if (countEl) {
                    countEl.textContent = String(status.count || 0);
                }
            } catch {
                // Ignore status refresh errors.
            }
        };

        const loadComments = async (page) => {
            if (page === 0) {
                commentsEl.innerHTML = '<div class="loading">Loading comments...</div>';
            }
            const pageData = await Services.comments.list(postId, null, {
                size: ROOT_COMMENTS_BATCH,
                page,
            });
            const comments = pageData.content || [];
            if (page === 0) {
                commentsEl.innerHTML = Render.commentRows(comments, postId, me.username, true, 0);
            } else if (comments.length) {
                commentsEl.insertAdjacentHTML(
                    "beforeend",
                    Render.commentRows(comments, postId, me.username, true, 0)
                );
            }
            comments.forEach((comment) => {
                refreshCommentLikeStatus(comment.id);
            });
            if (commentsLoadMoreBtn) {
                commentsLoadMoreBtn.hidden = !comments.length || !!pageData.last;
                commentsLoadMoreBtn.dataset.nextPage = String(page + 1);
            }
        };

        const loadReplies = async (commentId, page, depth) => {
            const holder = commentsEl.querySelector("[data-replies-for='" + commentId + "']");
            const control = commentsEl.querySelector(
                "[data-action='load-more-replies'][data-comment-id='" + commentId + "']"
            );
            if (!holder) {
                return;
            }
            if (page === 0) {
                holder.innerHTML = '<div class="loading">Loading replies...</div>';
            }
            const pageData = await Services.comments.list(postId, commentId, {
                size: REPLIES_BATCH,
                page,
            });
            const replies = pageData.content || [];
            if (page === 0) {
                holder.innerHTML = replies.length
                    ? Render.commentRows(replies, postId, me.username, true, depth)
                    : "";
            } else if (replies.length) {
                holder.insertAdjacentHTML(
                    "beforeend",
                    Render.commentRows(replies, postId, me.username, true, depth)
                );
            }
            replies.forEach((reply) => {
                refreshCommentLikeStatus(reply.id);
            });
            if (control) {
                control.hidden = !replies.length || !!pageData.last;
                control.dataset.nextPage = String(page + 1);
            }
        };

        renderPost();

        const bindDetailActions = () => {
            const likeBtn = Utils.qs("#post-like-toggle");
            if (likeBtn) {
                likeBtn.addEventListener("click", async () => {
                    try {
                        if (likeBtn.dataset.liked === "true") {
                            await Services.reactions.unlikePost(postId);
                        } else {
                            await Services.reactions.likePost(postId);
                        }
                        await loadPostLikeStatus();
                        Ui.notify("Post reaction updated.");
                    } catch (err) {
                        Ui.notify("Post reaction failed: " + err.message);
                    }
                });
            }

            const editToggle = Utils.qs("#post-edit-toggle");
            if (editToggle) {
                editToggle.addEventListener("click", () => {
                    editMode = true;
                    draft = {
                        title: post.title,
                        content: post.content,
                        state: post.state,
                    };
                    renderPost();
                    syncAttachments();
                    bindDetailActions();
                });
            }

            const editCancel = Utils.qs("#post-edit-cancel");
            if (editCancel) {
                editCancel.addEventListener("click", () => {
                    editMode = false;
                    renderPost();
                    syncAttachments();
                    bindDetailActions();
                    loadPostLikeStatus();
                });
            }

            const inlineEditForm = Utils.qs("#post-inline-edit-form");
            if (inlineEditForm) {
                inlineEditForm.addEventListener("submit", async (event) => {
                    event.preventDefault();
                    const fd = new FormData(inlineEditForm);
                    const nextState = String(fd.get("state") || post.state || "PUBLISHED").trim();
                    try {
                        post = await Services.posts.update(postId, {
                            title: String(fd.get("title") || "").trim(),
                            content: String(fd.get("content") || "").trim(),
                        });
                        if (nextState && nextState !== post.state) {
                            post = await Services.posts.setState(postId, nextState);
                        }
                        editMode = false;
                        draft = {
                            title: post.title,
                            content: post.content,
                            state: post.state,
                        };
                        renderPost();
                        syncAttachments();
                        bindDetailActions();
                        await loadPostLikeStatus();
                        Ui.notify("Post updated.");
                    } catch (err) {
                        Ui.notify("Post update failed: " + err.message);
                    }
                });
            }

            const deleteBtn = Utils.qs("#post-delete-btn");
            if (deleteBtn) {
                deleteBtn.addEventListener("click", async () => {
                    try {
                        await Services.posts.delete(postId);
                        Ui.notify("Post deleted.");
                        Utils.go("/posts.html");
                    } catch (err) {
                        Ui.notify("Delete failed: " + err.message);
                    }
                });
            }
        };

        bindDetailActions();

        if (isOwner) {
            fileForm.addEventListener("submit", async (event) => {
                event.preventDefault();
                if (!editMode) {
                    Ui.notify("Enable edit mode to change attachments.");
                    return;
                }
                const fd = new FormData(fileForm);
                const file = fd.get("file");
                if (!file || !file.size) {
                    Ui.notify("Select file first.");
                    return;
                }
                try {
                    await Services.files.uploadPostAttachment(postId, fd);
                    fileForm.reset();
                    await loadFiles();
                    Ui.notify("Attachment uploaded.");
                } catch (err) {
                    Ui.notify("Upload failed: " + err.message);
                }
            });
        } else {
            fileForm.hidden = true;
        }

        commentForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const fd = new FormData(commentForm);
            try {
                await Services.comments.create(postId, {
                    content: String(fd.get("content") || "").trim(),
                });
                commentForm.reset();
                if (commentsLoadMoreBtn) {
                    commentsLoadMoreBtn.dataset.nextPage = "0";
                }
                await loadComments(0);
                Ui.notify("Comment created.");
            } catch (err) {
                Ui.notify("Comment creation failed: " + err.message);
            }
        });

        if (commentsLoadMoreBtn) {
            commentsLoadMoreBtn.addEventListener("click", async () => {
                try {
                    const nextPage = Number(commentsLoadMoreBtn.dataset.nextPage || "0");
                    await loadComments(nextPage);
                } catch (err) {
                    Ui.notify("Loading more comments failed: " + err.message);
                }
            });
        }

        Utils.delegated(commentsEl, "click", "[data-action]", async (button) => {
            const action = button.getAttribute("data-action");
            const commentId = button.getAttribute("data-comment-id");
            try {
                if (action === "toggle-like-comment" && commentId) {
                    if (button.dataset.liked === "true") {
                        await Services.reactions.unlikeComment(commentId);
                    } else {
                        await Services.reactions.likeComment(commentId);
                    }
                    await refreshCommentLikeStatus(commentId);
                    Ui.notify("Comment reaction updated.");
                } else if (action === "toggle-reply" && commentId) {
                    const replyForm = commentsEl.querySelector(
                        "[data-action='reply-comment'][data-comment-id='" + commentId + "']"
                    );
                    if (replyForm) {
                        replyForm.hidden = !replyForm.hidden;
                    }
                } else if (action === "load-more-replies" && commentId) {
                    const nextPage = Number(button.dataset.nextPage || "0");
                    const depth = Number(button.dataset.depth || "1");
                    await loadReplies(commentId, nextPage, depth);
                } else if (action === "toggle-edit-comment" && commentId) {
                    const row = button.closest(".comment-row");
                    if (row) {
                        const editCommentForm = row.querySelector(
                            "[data-action='edit-comment'][data-comment-id='" + commentId + "']"
                        );
                        const body = row.querySelector("[data-comment-body='" + commentId + "']");
                        if (editCommentForm && body) {
                            const shouldOpen = editCommentForm.hidden;
                            editCommentForm.hidden = !shouldOpen;
                            body.hidden = shouldOpen;
                            button.textContent = shouldOpen ? "CLOSE" : "EDIT";
                        }
                    }
                } else if (action === "delete-comment" && commentId) {
                    await Services.comments.delete(commentId);
                    await loadComments(0);
                    Ui.notify("Comment deleted.");
                }
            } catch (err) {
                Ui.notify("Comment action failed: " + err.message);
            }
        });

        Utils.delegated(commentsEl, "submit", "form[data-action]", async (form, event) => {
            event.preventDefault();
            const action = form.getAttribute("data-action");
            try {
                if (action === "reply-comment") {
                    const parentId = form.getAttribute("data-comment-id");
                    const replyFd = new FormData(form);
                    await Services.comments.create(postId, {
                        content: String(replyFd.get("content") || "").trim(),
                        parentCommentId: Number(parentId),
                    });
                    form.reset();
                    form.hidden = true;
                    const control = commentsEl.querySelector(
                        "[data-action='load-more-replies'][data-comment-id='" + parentId + "']"
                    );
                    if (control) {
                        control.dataset.nextPage = "0";
                    }
                    const depth = Number((control && control.dataset.depth) || "1");
                    await loadReplies(parentId, 0, depth);
                    Ui.notify("Reply created.");
                } else if (action === "edit-comment") {
                    const editId = form.getAttribute("data-comment-id");
                    const editFd = new FormData(form);
                    await Services.comments.update(editId, {
                        content: String(editFd.get("content") || "").trim(),
                    });
                    await loadComments(0);
                    Ui.notify("Comment updated.");
                }
            } catch (err) {
                Ui.notify("Comment form failed: " + err.message);
            }
        });

        Utils.delegated(filesEl, "click", "[data-action]", async (button) => {
            const action = button.getAttribute("data-action");
            const fileId = button.getAttribute("data-file-id");
            try {
                if (action === "download-file") {
                    await downloadFile(fileId, "post-file-" + fileId);
                    Ui.notify("File downloaded.");
                } else if (action === "delete-file") {
                    if (!editMode) {
                        Ui.notify("Enable edit mode to change attachments.");
                        return;
                    }
                    await Services.files.delete(fileId);
                    await loadFiles();
                    Ui.notify("File deleted.");
                }
            } catch (err) {
                Ui.notify("File action failed: " + err.message);
            }
        });

        await loadPostLikeStatus();
        await loadComments(0);
        await loadFiles();
        Ui.notify("Post page loaded.");
    },

    async marketItemDetail() {
        if (!AuthStore.requireAuth()) {
            return;
        }

        const itemId = Utils.queryParam("id");
        if (!itemId) {
            Ui.notify("Missing item id.");
            return;
        }

        const me = await Services.users.me();
        let item = await Services.marketplace.get(itemId);
        const isOwner = me.username && item.sellerUsername === me.username;

        const detailEl = Utils.qs("#item-detail");
        const ownerSection = Utils.qs("#item-owner-section");
        const visitorSection = Utils.qs("#item-visitor-section");
        const editForm = Utils.qs("#item-edit-form");

        const bindToForm = () => {
            if (!isOwner) {
                return;
            }
            editForm.title.value = item.title || "";
            editForm.description.value = item.description || "";
            editForm.category.value = item.category || "";
            editForm.price.value = item.price || "";
            editForm.status.value = item.status || "AVAILABLE";
        };

        const renderItem = () => {
            detailEl.innerHTML = Render.marketItemDetail(item);
        };

        renderItem();
        ownerSection.hidden = !isOwner;
        visitorSection.hidden = isOwner;
        bindToForm();

        if (!isOwner) {
            Utils.qs("#item-contact-btn").addEventListener("click", async () => {
                try {
                    const contact = await Services.marketplace.contact(itemId);
                    Ui.notify("Contact: " + contact.sellerUsername + " <" + contact.sellerEmail + ">");
                } catch (err) {
                    Ui.notify("Contact failed: " + err.message);
                }
            });

            Utils.qs("#item-favorite-btn").addEventListener("click", async () => {
                try {
                    const status = await Services.marketplace.favorite(itemId);
                    Ui.notify("Favorited. Total favorites: " + status.count);
                } catch (err) {
                    Ui.notify("Favorite failed: " + err.message);
                }
            });

            Utils.qs("#item-unfavorite-btn").addEventListener("click", async () => {
                try {
                    const status = await Services.marketplace.unfavorite(itemId);
                    Ui.notify("Unfavorited. Total favorites: " + status.count);
                } catch (err) {
                    Ui.notify("Unfavorite failed: " + err.message);
                }
            });

            Utils.qs("#item-favorite-status-btn").addEventListener("click", async () => {
                try {
                    const status = await Services.marketplace.favoriteStatus(itemId);
                    Ui.notify(
                        "favorite=" + (status.favorite ? "yes" : "no") + " total=" + status.count
                    );
                } catch (err) {
                    Ui.notify("Favorite status failed: " + err.message);
                }
            });
        } else {
            editForm.addEventListener("submit", async (event) => {
                event.preventDefault();
                const fd = new FormData(editForm);
                try {
                    item = await Services.marketplace.update(itemId, {
                        title: String(fd.get("title") || "").trim(),
                        description: String(fd.get("description") || "").trim(),
                        category: String(fd.get("category") || "").trim(),
                        price: Number(fd.get("price")),
                        status: String(fd.get("status") || "AVAILABLE").trim(),
                    });
                    renderItem();
                    bindToForm();
                    Ui.notify("Item updated.");
                } catch (err) {
                    Ui.notify("Update failed: " + err.message);
                }
            });

            Utils.qs("#item-status-btn").addEventListener("click", async () => {
                try {
                    item = await Services.marketplace.setStatus(itemId, editForm.status.value);
                    renderItem();
                    bindToForm();
                    Ui.notify("Item status updated.");
                } catch (err) {
                    Ui.notify("Status update failed: " + err.message);
                }
            });

            Utils.qs("#item-delete-btn").addEventListener("click", async () => {
                try {
                    await Services.marketplace.delete(itemId);
                    Ui.notify("Item deleted.");
                    Utils.go("/marketplace.html");
                } catch (err) {
                    Ui.notify("Delete failed: " + err.message);
                }
            });
        }

        Ui.notify("Item page loaded.");
    },

    async personDetail() {
        if (!AuthStore.requireAuth()) {
            return;
        }
        const username = Utils.queryParam("username");
        if (!username) {
            Ui.notify("Missing username.");
            return;
        }

        const me = await Services.users.me();
        Utils.qs("#person-title").textContent = username.toUpperCase();
        Utils.qs("#person-self-actions").hidden = me.username !== username;

        const postParams = new URLSearchParams();
        postParams.set("size", "100");
        postParams.append("state", "PUBLISHED");
        const postsPage = await Services.posts.list(postParams);
        const posts = (postsPage.content || []).filter((post) => post.authorUsername === username);
        Utils.qs("#person-posts").innerHTML = Render.posts(posts, true);

        const itemParams = new URLSearchParams();
        itemParams.set("size", "100");
        const itemsPage = await Services.marketplace.list(itemParams);
        const items = (itemsPage.content || []).filter((item) => item.sellerUsername === username);
        Utils.qs("#person-items").innerHTML = Render.marketItems(items, true);

        Ui.notify("Person page loaded.");
    },
};

const pageControllers = {
    dashboard: Pages.dashboard,
    login: Pages.login,
    register: Pages.register,
    logout: Pages.logout,
    profile: Pages.profile,
    "profile-edit": Pages.profileEdit,
    posts: Pages.posts,
    marketplace: Pages.marketplace,
    "post-detail": Pages.postDetail,
    "market-item-detail": Pages.marketItemDetail,
    "person-detail": Pages.personDetail,
};

export async function boot() {
    const controller = pageControllers[Utils.page()];
    if (!controller) {
        return;
    }
    try {
        await controller();
    } catch (err) {
        Ui.notify("Page initialization failed: " + err.message);
    }
}
