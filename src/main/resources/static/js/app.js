import { Utils } from "./core/utils.js";
import { Ui } from "./core/ui.js";
import { Services } from "./core/services.js";
import { Render } from "./core/render.js";

function bindUiDelegates() {
    document.addEventListener("click", (event) => {
        const toggleBtn = event.target.closest("[data-toggle-target]");
        if (toggleBtn) {
            const targetId = toggleBtn.getAttribute("data-toggle-target");
            const target = document.getElementById(targetId);
            if (target) {
                target.hidden = !target.hidden;
            }
            return;
        }

        const confirmBtn = event.target.closest("[data-confirm]");
        if (confirmBtn) {
            const message = confirmBtn.getAttribute("data-confirm") || "Are you sure?";
            if (!window.confirm(message)) {
                event.preventDefault();
                event.stopPropagation();
            }
        }
    });
}

function escapeText(value) {
    return Utils.escapeHtml(value == null ? "" : String(value));
}

function buildPostCard(post) {
    const postId = escapeText(post.id);
    const title = escapeText(post.title);
    const author = escapeText(post.authorUsername);
    const state = escapeText(post.state);
    const content = escapeText(post.content);
    return (
        '<article class="post-card">' +
        '<h3><a href="/web/posts/' + postId + '">' + title + "</a></h3>" +
        '<p class="meta"><a href="/web/person/' + encodeURIComponent(post.authorUsername || "") + '">' +
        author +
        "</a><span>" +
        state +
        "</span></p>" +
        "<p>" +
        content +
        "</p>" +
        '<a class="btn" href="/web/posts/' + postId + '">OPEN PAGE</a>' +
        "</article>"
    );
}

function buildMarketCard(item) {
    const itemId = escapeText(item.id);
    const title = escapeText(item.title);
    const seller = escapeText(item.sellerUsername);
    const status = escapeText(item.status);
    const category = escapeText(item.category);
    const description = escapeText(item.description);
    const price = escapeText(item.price);
    return (
        '<article class="market-card">' +
        '<h3><a href="/web/marketplace/' + itemId + '">' + title + "</a></h3>" +
        '<p class="meta"><a href="/web/person/' + encodeURIComponent(item.sellerUsername || "") + '">' +
        seller +
        "</a><span>" +
        status +
        "</span><span>" +
        category +
        "</span></p>" +
        "<p>" +
        description +
        "</p>" +
        '<p class="price">Price: ' + price + "</p>" +
        '<a class="btn" href="/web/marketplace/' + itemId + '">OPEN PAGE</a>' +
        "</article>"
    );
}

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

async function initPostDetailPanel() {
    if (Utils.page() !== "post-detail") {
        return;
    }

    const postId = document.body.getAttribute("data-post-id");
    const detailEl = Utils.qs("#post-detail");
    const pageTitle = Utils.qs("#post-page-title");
    const detailView = Utils.qs("#post-detail-view", detailEl);
    const titleEl = Utils.qs("#post-detail-title", detailEl);
    const contentEl = Utils.qs("#post-detail-content", detailEl);
    const stateEl = Utils.qs("#post-detail-state");
    const authorLinkEl = Utils.qs("#post-detail-author-link");
    const createdEl = Utils.qs("#post-detail-created");
    const updatedDetailWrapEl = Utils.qs("#post-detail-updated-extra");
    const updatedDetailEl = Utils.qs("#post-detail-updated-detail");
    const likeBtn = Utils.qs("#post-like-toggle");
    const likeCountEl = Utils.qs("#post-detail-like-count");
    const editToggle = Utils.qs("#post-edit-toggle", detailEl);
    const editPanel = Utils.qs("#post-edit-panel", detailEl);
    const editCancel = Utils.qs("#post-edit-cancel", detailEl);
    const inlineEditForm = Utils.qs("#post-inline-edit-form", detailEl);
    const deleteBtn = Utils.qs("#post-delete-btn", detailEl);
    const fileSection = Utils.qs("#post-files-section");
    const fileForm = Utils.qs("#post-file-form");
    const filesEl = Utils.qs("#post-files");
    if (!postId || !detailEl || !detailView || !fileSection || !fileForm || !filesEl) {
        return;
    }

    let editMode = false;
    let attachments = [];
    let attachmentsLoaded = false;

    const currentPost = () => ({
        id: postId,
        title: detailView.dataset.postTitle || "",
        content: detailView.dataset.postContent || "",
        state: detailView.dataset.postState || "PUBLISHED",
        authorUsername: detailView.dataset.authorUsername || "",
        createdAt: detailView.dataset.postCreatedAt || "",
        updatedAt: detailView.dataset.postUpdatedAt || detailView.dataset.postCreatedAt || "",
    });

    const setEditValues = (post) => {
        if (!inlineEditForm) {
            return;
        }
        const titleField = Utils.qs("#post-edit-title", inlineEditForm);
        const contentField = Utils.qs("#post-edit-content", inlineEditForm);
        const stateField = Utils.qs("#post-edit-state", inlineEditForm);
        if (titleField) {
            titleField.value = post.title || "";
        }
        if (contentField) {
            contentField.value = post.content || "";
        }
        if (stateField) {
            stateField.value = post.state || "PUBLISHED";
        }
    };

    const syncReadView = (post) => {
        detailView.dataset.postTitle = post.title || "";
        detailView.dataset.postContent = post.content || "";
        detailView.dataset.postState = post.state || "PUBLISHED";
        detailView.dataset.postCreatedAt = post.createdAt || detailView.dataset.postCreatedAt || "";
        detailView.dataset.postUpdatedAt = post.updatedAt || "";
        detailView.dataset.authorUsername = post.authorUsername || "";

        if (pageTitle) {
            pageTitle.textContent = post.title || "";
        }
        if (titleEl) {
            titleEl.textContent = post.title || "";
        }
        if (contentEl) {
            contentEl.textContent = post.content || "";
        }
        if (stateEl) {
            stateEl.textContent = post.state || "";
        }
        if (authorLinkEl) {
            authorLinkEl.textContent = post.authorUsername || "";
            authorLinkEl.href = "/web/person/" + encodeURIComponent(post.authorUsername || "");
        }
        if (createdEl) {
            createdEl.textContent = Utils.time(post.createdAt);
        }
        if (updatedDetailEl) {
            updatedDetailEl.textContent = post.updatedAt ? Utils.time(post.updatedAt) : "";
        }
        if (updatedDetailWrapEl) {
            updatedDetailWrapEl.hidden = !post.updatedAt;
        }

        setEditValues(post);
    };

    const hasRenderedFiles = () => !!filesEl.querySelector("[data-file-id]");

    const syncLayout = () => {
        const hasFiles = attachmentsLoaded ? attachments.length > 0 : hasRenderedFiles();
        detailView.hidden = editMode;
        if (editPanel) {
            editPanel.hidden = !editMode;
        }
        fileForm.hidden = !editMode;
        fileSection.hidden = !editMode && !hasFiles;
    };

    const loadPostLikeStatus = async () => {
        try {
            const status = await Services.reactions.postStatus(postId);
            if (likeBtn) {
                likeBtn.dataset.liked = String(!!status.liked);
                likeBtn.setAttribute("aria-pressed", status.liked ? "true" : "false");
                likeBtn.classList.toggle("is-active", !!status.liked);
            }
            if (likeCountEl) {
                likeCountEl.textContent = String(status.count || 0);
            }
        } catch {
            // Ignore like status failures.
        }
    };

    const syncAttachments = () => {
        attachmentsLoaded = true;
        filesEl.innerHTML = Render.fileList(
            attachments,
            editMode ? "No files attached yet." : "No files attached.",
            editMode,
            "post"
        );
        syncLayout();
    };

    const loadFiles = async () => {
        try {
            const pageData = await Services.files.postAttachments(postId);
            attachments = pageData.content || [];
        } catch {
            attachments = [];
        }
        syncAttachments();
    };

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

    if (editToggle) {
        editToggle.addEventListener("click", async () => {
            editMode = true;
            setEditValues(currentPost());
            syncLayout();
            await loadFiles();
        });
    }

    if (editCancel) {
        editCancel.addEventListener("click", () => {
            editMode = false;
            setEditValues(currentPost());
            syncAttachments();
        });
    }

    if (inlineEditForm) {
        inlineEditForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const fd = new FormData(inlineEditForm);
            let post = currentPost();
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
                syncReadView(post);
                syncAttachments();
                await loadPostLikeStatus();
                Ui.notify("Post updated.");
            } catch (err) {
                Ui.notify("Post update failed: " + err.message);
            }
        });
    }

    if (deleteBtn) {
        deleteBtn.addEventListener("click", async () => {
            if (!window.confirm("Delete this post?")) {
                return;
            }
            try {
                await Services.posts.delete(postId);
                Ui.notify("Post deleted.");
                Utils.go("/web/posts");
            } catch (err) {
                Ui.notify("Delete failed: " + err.message);
            }
        });
    }

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

    setEditValues(currentPost());
    syncLayout();
    await loadPostLikeStatus();
}

async function initPostsInfiniteScroll() {
    if (Utils.page() !== "posts") {
        return;
    }

    const listEl = Utils.qs("#posts-list");
    const emptyEl = Utils.qs("#posts-empty");
    const sentinel = Utils.qs("#posts-scroll-sentinel");
    if (!listEl || !sentinel) {
        return;
    }

    let nextPage = Number(listEl.dataset.nextPage || "1");
    let isLast = String(listEl.dataset.last || "false") === "true";
    let isLoading = false;

    const loadNext = async () => {
        if (isLoading || isLast) {
            return;
        }
        isLoading = true;
        sentinel.textContent = "Loading more posts...";
        try {
            const params = new URLSearchParams();
            params.set("size", "10");
            params.set("page", String(nextPage));
            const q = listEl.dataset.query || "";
            const state = listEl.dataset.state || "";
            if (q.trim()) {
                params.set("q", q.trim());
            }
            if (state.trim()) {
                params.set("state", state.trim());
            }

            const pageData = await Services.posts.list(params);
            const posts = pageData.content || [];
            if (posts.length) {
                listEl.insertAdjacentHTML("beforeend", posts.map(buildPostCard).join(""));
                if (emptyEl) {
                    emptyEl.hidden = true;
                }
            }
            isLast = !!pageData.last;
            nextPage = Number(pageData.number || nextPage) + 1;
            sentinel.hidden = isLast;
        } catch (err) {
            Ui.notify("Post auto-load failed: " + err.message);
            sentinel.textContent = "Scroll to retry loading posts";
        } finally {
            isLoading = false;
        }
    };

    sentinel.hidden = isLast;
    if (isLast) {
        return;
    }

    const observer = new IntersectionObserver(
        async (entries) => {
            const first = entries[0];
            if (first && first.isIntersecting) {
                await loadNext();
            }
        },
        { rootMargin: "200px 0px" }
    );
    observer.observe(sentinel);
}

async function initMarketplaceInfiniteScroll() {
    if (Utils.page() !== "marketplace") {
        return;
    }

    const listEl = Utils.qs("#market-list");
    const emptyEl = Utils.qs("#market-empty");
    const sentinel = Utils.qs("#market-scroll-sentinel");
    if (!listEl || !sentinel) {
        return;
    }

    let nextPage = Number(listEl.dataset.nextPage || "1");
    let isLast = String(listEl.dataset.last || "false") === "true";
    let isLoading = false;

    const loadNext = async () => {
        if (isLoading || isLast) {
            return;
        }
        isLoading = true;
        sentinel.textContent = "Loading more items...";
        try {
            const params = new URLSearchParams();
            params.set("size", "12");
            params.set("page", String(nextPage));

            const mappings = [
                ["q", listEl.dataset.query],
                ["status", listEl.dataset.status],
                ["category", listEl.dataset.category],
                ["sortBy", listEl.dataset.sortBy],
                ["sortDir", listEl.dataset.sortDir],
                ["minPrice", listEl.dataset.minPrice],
                ["maxPrice", listEl.dataset.maxPrice],
            ];
            mappings.forEach(([key, value]) => {
                if (value != null && String(value).trim() !== "") {
                    params.set(key, String(value).trim());
                }
            });
            if (String(listEl.dataset.ownOnly || "false") === "true") {
                params.set("ownOnly", "true");
            }

            const pageData = await Services.marketplace.list(params);
            const items = pageData.content || [];
            if (items.length) {
                listEl.insertAdjacentHTML("beforeend", items.map(buildMarketCard).join(""));
                if (emptyEl) {
                    emptyEl.hidden = true;
                }
            }
            isLast = !!pageData.last;
            nextPage = Number(pageData.number || nextPage) + 1;
            sentinel.hidden = isLast;
        } catch (err) {
            Ui.notify("Marketplace auto-load failed: " + err.message);
            sentinel.textContent = "Scroll to retry loading items";
        } finally {
            isLoading = false;
        }
    };

    sentinel.hidden = isLast;
    if (isLast) {
        return;
    }

    const observer = new IntersectionObserver(
        async (entries) => {
            const first = entries[0];
            if (first && first.isIntersecting) {
                await loadNext();
            }
        },
        { rootMargin: "220px 0px" }
    );
    observer.observe(sentinel);
}

async function initPostDetailComments() {
    if (Utils.page() !== "post-detail") {
        return;
    }

    const postId = document.body.getAttribute("data-post-id");
    const commentsEl = Utils.qs("#post-comments");
    const commentForm = Utils.qs("#post-comment-form");
    const loadMoreBtn = Utils.qs("#post-comments-load-more");

    if (!postId || !commentsEl || !commentForm || !loadMoreBtn) {
        return;
    }

    let meUsername = null;
    try {
        const me = await Services.users.me();
        meUsername = me && me.username ? me.username : null;
    } catch {
        // Anonymous users can still browse visible comments.
    }

    const ROOT_COMMENTS_BATCH = 10;
    const REPLIES_BATCH = 5;

    const refreshCommentLikeStatus = async (commentId) => {
        try {
            const status = await Services.reactions.commentStatus(commentId);
            commentsEl
                .querySelectorAll("[data-action='toggle-like-comment'][data-comment-id='" + commentId + "']")
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
            // Ignore individual like status failures.
        }
    };

    const refreshVisibleLikeStatuses = async () => {
        const ids = new Set(
            Utils.qsa("[data-comment-id]", commentsEl)
                .map((el) => el.getAttribute("data-comment-id"))
                .filter((id) => !!id)
        );
        await Promise.all(Array.from(ids).map((id) => refreshCommentLikeStatus(id)));
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
            commentsEl.innerHTML = Render.commentRows(comments, postId, meUsername, true, 0);
        } else if (comments.length) {
            commentsEl.insertAdjacentHTML(
                "beforeend",
                Render.commentRows(comments, postId, meUsername, true, 0)
            );
        }

        await refreshVisibleLikeStatuses();
        loadMoreBtn.hidden = !comments.length || !!pageData.last;
        loadMoreBtn.dataset.nextPage = String(page + 1);
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
                ? Render.commentRows(replies, postId, meUsername, true, depth)
                : "";
        } else if (replies.length) {
            holder.insertAdjacentHTML(
                "beforeend",
                Render.commentRows(replies, postId, meUsername, true, depth)
            );
        }

        await refreshVisibleLikeStatuses();
        if (control) {
            control.hidden = !replies.length || !!pageData.last;
            control.dataset.nextPage = String(page + 1);
        }
    };

    commentForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const fd = new FormData(commentForm);
        try {
            await Services.comments.create(postId, {
                content: String(fd.get("content") || "").trim(),
            });
            commentForm.reset();
            loadMoreBtn.dataset.nextPage = "0";
            await loadComments(0);
            Ui.notify("Comment created.");
        } catch (err) {
            Ui.notify("Comment creation failed: " + err.message);
        }
    });

    loadMoreBtn.addEventListener("click", async () => {
        try {
            const nextPage = Number(loadMoreBtn.dataset.nextPage || "0");
            await loadComments(nextPage);
        } catch (err) {
            Ui.notify("Loading more comments failed: " + err.message);
        }
    });

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
                    const editForm = row.querySelector(
                        "[data-action='edit-comment'][data-comment-id='" + commentId + "']"
                    );
                    const body = row.querySelector("[data-comment-body='" + commentId + "']");
                    if (editForm && body) {
                        const shouldOpen = editForm.hidden;
                        editForm.hidden = !shouldOpen;
                        body.hidden = shouldOpen;
                        button.textContent = shouldOpen ? "CLOSE" : "EDIT";
                    }
                }
            } else if (action === "delete-comment" && commentId) {
                await Services.comments.delete(commentId);
                loadMoreBtn.dataset.nextPage = "0";
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
                loadMoreBtn.dataset.nextPage = "0";
                await loadComments(0);
                Ui.notify("Comment updated.");
            }
        } catch (err) {
            Ui.notify("Comment form failed: " + err.message);
        }
    });

    await loadComments(0);
}

document.addEventListener("DOMContentLoaded", async () => {
    bindUiDelegates();
    try {
        await initPostDetailPanel();
        await initPostDetailComments();
        await initPostsInfiniteScroll();
        await initMarketplaceInfiniteScroll();
    } catch (err) {
        Ui.notify("Frontend initialization failed: " + err.message);
    }
});
