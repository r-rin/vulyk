import { Utils } from "./utils.js";
import { AuthStore } from "./auth-store.js";

export const Render = {
    profileTable(profile) {
        const rows = [
            ["Username", profile.username],
            ["Name", profile.name],
            ["Email", profile.email],
            ["Phone", profile.phoneNumber],
            ["Bio", profile.bio],
            ["Profile Picture ID", profile.profilePictureId],
            ["JWT Role", AuthStore.role()],
        ];
        return rows
            .map(
                (row) =>
                    '<div class="row"><span class="cell label">' +
                    Utils.escapeHtml(row[0]) +
                    '</span><span class="cell">' +
                    Utils.escapeHtml(row[1] == null ? "-" : row[1]) +
                    "</span></div>"
            )
            .join("");
    },

    reactions(items, emptyText) {
        if (!items || !items.length) {
            return '<div class="empty">' + Utils.escapeHtml(emptyText) + "</div>";
        }
        return items
            .map(
                (item) =>
                    '<div class="comment-row"><p class="meta"><span>' +
                    Utils.escapeHtml(item.targetType || "") +
                    " #" +
                    Utils.escapeHtml(item.targetId || "") +
                    "</span><span>" +
                    Utils.escapeHtml(Utils.time(item.likedAt)) +
                    "</span></p><p><strong>" +
                    Utils.escapeHtml(item.title || "") +
                    '</strong></p><p class="snippet">' +
                    Utils.escapeHtml(item.snippet || "") +
                    "</p></div>"
            )
            .join("");
    },

    fileList(files, emptyText, canDelete, context) {
        if (!files || !files.length) {
            return '<div class="empty">' + Utils.escapeHtml(emptyText) + "</div>";
        }
        return files
            .map(
                (file) =>
                    '<div class="file-row" data-context="' +
                    Utils.escapeHtml(context || "") +
                    '"><div><strong>' +
                    Utils.escapeHtml(file.originalFilename || "(unnamed)") +
                    '</strong></div><div class="file-meta">ID ' +
                    Utils.escapeHtml(file.id) +
                    " | " +
                    Utils.escapeHtml(file.contentType || "unknown") +
                    " | " +
                    Utils.escapeHtml(file.fileSize || 0) +
                    " bytes | " +
                    Utils.escapeHtml(Utils.time(file.createdAt)) +
                    '</div><div class="mini-actions"><button class="btn" type="button" data-action="download-file" data-file-id="' +
                    Utils.escapeHtml(file.id) +
                    '">DOWNLOAD</button>' +
                    (canDelete
                        ? '<button class="btn btn-danger" type="button" data-action="delete-file" data-file-id="' +
                          Utils.escapeHtml(file.id) +
                          '">DELETE</button>'
                        : "") +
                    "</div></div>"
            )
            .join("");
    },

    postCard(post, compact) {
        const quickActions = compact
            ? ""
            : '<div class="mini-actions"><button class="btn btn-icon state-toggle" type="button" data-action="toggle-like-post" data-post-id="' +
              Utils.escapeHtml(post.id) +
              '" data-liked="false" aria-pressed="false" title="Toggle like"><span class="heart-icon">&#9829;</span><span class="like-count" id="post-like-count-' +
              Utils.escapeHtml(post.id) +
              '">0</span></button><button class="btn" type="button" data-action="toggle-comments" data-post-id="' +
              Utils.escapeHtml(post.id) +
              '">COMMENTS</button><a class="btn" href="/post.html?id=' +
              encodeURIComponent(post.id) +
              '">OPEN PAGE</a></div>' +
              '<div class="inline-form" data-comment-panel="' +
              Utils.escapeHtml(post.id) +
              '" hidden><form class="inline-form" data-action="create-comment" data-post-id="' +
              Utils.escapeHtml(post.id) +
              '"><textarea name="content" maxlength="2000" required placeholder="Write comment"></textarea><button class="btn" type="submit">ADD COMMENT</button></form><div class="comments-list" data-post-comments="' +
              Utils.escapeHtml(post.id) +
              '"><div class="loading">Comments not loaded.</div></div><button type="button" class="linkish-btn" data-action="load-more-comments" data-post-id="' +
              Utils.escapeHtml(post.id) +
              '" data-next-page="0" hidden>Load more comments</button></div>';

        return (
            '<article class="post-card" data-post-id="' +
            Utils.escapeHtml(post.id) +
            '"><h3><a href="/post.html?id=' +
            encodeURIComponent(post.id) +
            '">' +
            Utils.escapeHtml(post.title) +
            '</a></h3><p class="meta"><a href="/person.html?username=' +
            encodeURIComponent(post.authorUsername || "") +
            '">' +
            Utils.escapeHtml(post.authorUsername || "") +
            "</a><span>" +
            Utils.escapeHtml(post.state || "") +
            "</span><span>" +
            Utils.escapeHtml(Utils.time(post.updatedAt || post.createdAt)) +
            "</span></p><p>" +
            Utils.escapeHtml(post.content || "") +
            "</p>" +
            quickActions +
            "</article>"
        );
    },

    posts(posts, compact) {
        if (!posts || !posts.length) {
            return '<div class="empty">No posts found.</div>';
        }
        return posts.map((post) => this.postCard(post, compact)).join("");
    },

    commentRows(comments, postId, meUsername, allowManage, depth) {
        const currentDepth = depth == null ? 0 : depth;
        if (!comments || !comments.length) {
            return '<div class="empty">No comments yet.</div>';
        }

        return comments
            .map((comment) => {
                const own = !!(allowManage && meUsername && comment.authorUsername === meUsername);
                return (
                    '<div class="comment-row ' +
                    (currentDepth === 0 ? "root-comment" : "reply-comment") +
                    '" data-depth="' +
                    Utils.escapeHtml(currentDepth) +
                    '" data-post-id="' +
                    Utils.escapeHtml(postId) +
                    '" data-comment-id="' +
                    Utils.escapeHtml(comment.id) +
                    '">' +
                    (own
                        ? '<button class="btn subtle-corner" type="button" data-action="toggle-edit-comment" data-comment-id="' +
                          Utils.escapeHtml(comment.id) +
                          '" title="Edit comment">EDIT</button>'
                        : "") +
                    '<p class="meta"><a href="/web/person/' +
                    encodeURIComponent(comment.authorUsername || "") +
                    '">' +
                    Utils.escapeHtml(comment.authorUsername || "") +
                    "</a><span>" +
                    Utils.escapeHtml(Utils.time(comment.updatedAt || comment.createdAt)) +
                    '</span></p><p class="comment-body" data-comment-body="' +
                    Utils.escapeHtml(comment.id) +
                    '">' +
                    Utils.escapeHtml(comment.content || "") +
                    '</p><div class="mini-actions"><button class="btn btn-icon state-toggle" type="button" data-action="toggle-like-comment" data-comment-id="' +
                    Utils.escapeHtml(comment.id) +
                    '" data-liked="false" aria-pressed="false" title="Toggle like"><span class="heart-icon">&#9829;</span><span class="like-count" id="comment-like-count-' +
                    Utils.escapeHtml(comment.id) +
                    '">0</span></button><button class="btn" type="button" data-action="toggle-reply" data-comment-id="' +
                    Utils.escapeHtml(comment.id) +
                    '">REPLY</button>' +
                    (own
                        ? '<button class="btn btn-danger" type="button" data-action="delete-comment" data-comment-id="' +
                          Utils.escapeHtml(comment.id) +
                          '">DELETE</button>'
                        : "") +
                    "</div>" +
                    (own
                                                ? '<form class="inline-form" data-action="edit-comment" data-comment-id="' +
                          Utils.escapeHtml(comment.id) +
                          '" hidden><textarea name="content" maxlength="2000" required>' +
                          Utils.escapeHtml(comment.content || "") +
                          '</textarea><button class="btn" type="submit">SAVE COMMENT</button></form>'
                        : "") +
                    '<form class="inline-form" data-action="reply-comment" data-post-id="' +
                    Utils.escapeHtml(postId) +
                    '" data-comment-id="' +
                    Utils.escapeHtml(comment.id) +
                    '" hidden><textarea name="content" maxlength="2000" required placeholder="Reply text"></textarea><button class="btn" type="submit">SEND REPLY</button></form><div class="comment-replies" data-replies-for="' +
                    Utils.escapeHtml(comment.id) +
                    '"></div><button type="button" class="linkish-btn" data-action="load-more-replies" data-post-id="' +
                    Utils.escapeHtml(postId) +
                    '" data-comment-id="' +
                    Utils.escapeHtml(comment.id) +
                    '" data-depth="' +
                    Utils.escapeHtml(currentDepth + 1) +
                    '" data-next-page="0">Load replies</button></div>'
                );
            })
            .join("");
    },

    postDetail(post, options) {
        const opts = options || {};
        const draft = opts.draft || {};
        const editMode = !!opts.editMode;
        const isOwner = !!opts.isOwner;

        if (!editMode) {
            return (
                '<article class="post-card post-detail-card"><div class="post-head"><h2>' +
                Utils.escapeHtml(post.title) +
                '</h2>' +
                (isOwner
                    ? '<button class="btn subtle-corner top-right" type="button" id="post-edit-toggle" title="Edit post">EDIT</button>'
                    : "") +
                '</div><p class="meta"><a href="/web/person/' +
                encodeURIComponent(post.authorUsername || "") +
                '">' +
                Utils.escapeHtml(post.authorUsername || "") +
                "</a><span>" +
                Utils.escapeHtml(post.state || "") +
                "</span><span>" +
                Utils.escapeHtml(Utils.time(post.updatedAt || post.createdAt)) +
                '</span></p><p class="post-content">' +
                Utils.escapeHtml(post.content || "") +
                '</p><div class="author-box"><strong>Author:</strong> <a href="/web/person/' +
                encodeURIComponent(post.authorUsername || "") +
                '">' +
                Utils.escapeHtml(post.authorUsername || "") +
                "</a> | <strong>Created:</strong> " +
                Utils.escapeHtml(Utils.time(post.createdAt)) +
                (post.updatedAt && post.updatedAt !== post.createdAt
                    ? " | <strong>Updated:</strong> " + Utils.escapeHtml(Utils.time(post.updatedAt))
                    : "") +
                '</div><div class="mini-actions"><button class="btn btn-icon state-toggle" type="button" id="post-like-toggle" data-liked="false" aria-pressed="false" title="Toggle like"><span class="heart-icon">&#9829;</span><span class="like-count" id="post-detail-like-count">0</span></button></div></article>'
            );
        }

        return (
            '<article class="post-card post-detail-card"><div class="post-head"><h2>EDIT POST</h2><button class="btn subtle-corner top-right" type="button" id="post-edit-cancel" title="Cancel editing">CANCEL</button></div><form class="retro-form" id="post-inline-edit-form"><label for="post-edit-title">Title</label><input id="post-edit-title" name="title" maxlength="100" required value="' +
            Utils.escapeHtml(draft.title || post.title || "") +
            '"><label for="post-edit-content">Content</label><textarea id="post-edit-content" name="content" maxlength="1000" required>' +
            Utils.escapeHtml(draft.content || post.content || "") +
            '</textarea><label for="post-edit-state">State</label><select id="post-edit-state" name="state"><option value="DRAFT"' +
            ((draft.state || post.state) === "DRAFT" ? " selected" : "") +
            '>DRAFT</option><option value="PUBLISHED"' +
            ((draft.state || post.state) === "PUBLISHED" ? " selected" : "") +
            '>PUBLISHED</option><option value="HIDDEN"' +
            ((draft.state || post.state) === "HIDDEN" ? " selected" : "") +
            '>HIDDEN</option><option value="REDACTED"' +
            ((draft.state || post.state) === "REDACTED" ? " selected" : "") +
            '>REDACTED</option></select><div class="mini-actions"><button type="submit" class="btn">SAVE</button><button type="button" class="btn btn-danger" id="post-delete-btn">DELETE</button></div></form></article>'
        );
    },

    marketCard(item, compact) {
        const quickButtons = compact
            ? ""
            : '<div class="mini-actions"><button type="button" class="btn btn-success" data-action="favorite-item" data-item-id="' +
              Utils.escapeHtml(item.id) +
              '">FAVORITE</button><button type="button" class="btn btn-danger" data-action="unfavorite-item" data-item-id="' +
              Utils.escapeHtml(item.id) +
              '">UNFAVORITE</button><button type="button" class="btn" data-action="contact-seller" data-item-id="' +
              Utils.escapeHtml(item.id) +
              '">CONTACT</button><span class="snippet" id="item-fav-status-' +
              Utils.escapeHtml(item.id) +
              '"></span></div>';

        return (
            '<article class="market-card" data-item-id="' +
            Utils.escapeHtml(item.id) +
            '"><h3><a href="/web/marketplace/' +
            Utils.escapeHtml(item.id) +
            '">' +
            Utils.escapeHtml(item.title) +
            '</a></h3><p class="meta"><a href="/web/person/' +
            encodeURIComponent(item.sellerUsername || "") +
            '">' +
            Utils.escapeHtml(item.sellerUsername || "") +
            "</a><span>" +
            Utils.escapeHtml(item.status || "") +
            "</span><span>" +
            Utils.escapeHtml(item.category || "") +
            "</span><span>" +
            Utils.escapeHtml(Utils.time(item.updatedAt || item.createdAt)) +
            "</span></p><p>" +
            Utils.escapeHtml(item.description || "") +
            '</p><p class="price">Price: ' +
            Utils.escapeHtml(item.price) +
            '</p><div class="mini-actions"><a class="btn" href="/web/marketplace/' +
            Utils.escapeHtml(item.id) +
            '">OPEN PAGE</a></div>' +
            quickButtons +
            "</article>"
        );
    },

    marketItems(items, compact) {
        if (!items || !items.length) {
            return '<div class="empty">No marketplace items found.</div>';
        }
        return items.map((item) => this.marketCard(item, compact)).join("");
    },

    marketItemDetail(item) {
        return (
            "<h2>" +
            Utils.escapeHtml(item.title) +
            '</h2><p class="meta"><a href="/web/person/' +
            encodeURIComponent(item.sellerUsername || "") +
            '">' +
            Utils.escapeHtml(item.sellerUsername || "") +
            "</a><span>" +
            Utils.escapeHtml(item.status || "") +
            "</span><span>" +
            Utils.escapeHtml(item.category || "") +
            "</span><span>" +
            Utils.escapeHtml(Utils.time(item.updatedAt || item.createdAt)) +
            '</span></p><p>' +
            Utils.escapeHtml(item.description || "") +
            '</p><p class="price">Price: ' +
            Utils.escapeHtml(item.price) +
            "</p>"
        );
    },
};