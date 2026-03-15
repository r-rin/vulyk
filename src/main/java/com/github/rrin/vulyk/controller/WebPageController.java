package com.github.rrin.vulyk.controller;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import com.github.rrin.vulyk.domain.entity.post.PostState;
import com.github.rrin.vulyk.dto.auth.LoginRequest;
import com.github.rrin.vulyk.dto.auth.RegisterRequest;
import com.github.rrin.vulyk.dto.comment.CommentRequest;
import com.github.rrin.vulyk.dto.file.FileAttachmentResponse;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceContactResponse;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceItemRequest;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceItemResponse;
import com.github.rrin.vulyk.dto.post.PostRequest;
import com.github.rrin.vulyk.dto.post.PostResponse;
import com.github.rrin.vulyk.dto.user.UpdateProfileRequest;
import com.github.rrin.vulyk.dto.user.UserProfileResponse;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.service.CommentService;
import com.github.rrin.vulyk.service.FileService;
import com.github.rrin.vulyk.service.MarketplaceService;
import com.github.rrin.vulyk.service.PostService;
import com.github.rrin.vulyk.service.ReactionService;
import com.github.rrin.vulyk.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class WebPageController {

    private static final String JWT_COOKIE_NAME = "VULYK_TOKEN";

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final ReactionService reactionService;
    private final MarketplaceService marketplaceService;
    private final FileService fileService;

    @GetMapping({"/", "/app", "/dashboard", "/index", "/home"})
    public String rootAlias() {
        return "redirect:/web/dashboard";
    }

    @GetMapping("/web/dashboard")
    public String dashboard(
        @AuthenticationPrincipal String principalEmail,
        Model model
    ) {
        addAuthModel(principalEmail, model);

        Page<PostResponse> recentPosts = postService.list(
            PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "createdAt")),
            List.of(PostState.PUBLISHED),
            null
        );
        model.addAttribute("recentPosts", recentPosts.getContent());

        Page<MarketplaceItemResponse> recentItems = marketplaceService.list(
            PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "createdAt")),
            null,
            MarketplaceItemStatus.AVAILABLE,
            null,
            null,
            null,
            "createdAt",
            "desc",
            false,
            principalEmail
        );
        model.addAttribute("recentItems", recentItems.getContent());
        return "web/dashboard";
    }

    @GetMapping("/web/login")
    public String loginPage(
        @AuthenticationPrincipal String principalEmail,
        Model model
    ) {
        if (isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/dashboard";
        }
        addAuthModel(principalEmail, model);
        return "web/login";
    }

    @PostMapping("/web/login")
    public String login(
        @RequestParam String identifier,
        @RequestParam String password,
        HttpServletResponse response,
        RedirectAttributes redirectAttributes
    ) {
        try {
            String token = userService.login(new LoginRequest(identifier, password)).getToken();
            addJwtCookie(response, token);
            redirectAttributes.addFlashAttribute("notice", "Logged in successfully.");
            return "redirect:/web/dashboard";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/web/login";
        }
    }

    @GetMapping("/web/register")
    public String registerPage(
        @AuthenticationPrincipal String principalEmail,
        Model model
    ) {
        if (isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/dashboard";
        }
        addAuthModel(principalEmail, model);
        return "web/register";
    }

    @PostMapping("/web/register")
    public String register(
        @RequestParam String username,
        @RequestParam String email,
        @RequestParam String password,
        @RequestParam(name = "name", required = false) String name,
        @RequestParam(name = "phoneNumber", required = false) String phoneNumber,
        @RequestParam(name = "bio", required = false) String bio,
        HttpServletResponse response,
        RedirectAttributes redirectAttributes
    ) {
        try {
            String token = userService.register(new RegisterRequest(
                username,
                email,
                password,
                name,
                phoneNumber,
                bio
            )).getToken();
            addJwtCookie(response, token);
            redirectAttributes.addFlashAttribute("notice", "Account created and authenticated.");
            return "redirect:/web/dashboard";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/web/register";
        }
    }

    @GetMapping("/web/logout")
    public String logoutPage(
        @AuthenticationPrincipal String principalEmail,
        Model model
    ) {
        addAuthModel(principalEmail, model);
        return "web/logout";
    }

    @PostMapping("/web/logout")
    public String logout(HttpServletResponse response) {
        clearJwtCookie(response);
        return "redirect:/web/login";
    }

    @GetMapping("/web/profile")
    public String profile(
        @AuthenticationPrincipal String principalEmail,
        @RequestParam(name = "tab", defaultValue = "posts") String tab,
        Model model
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        addAuthModel(principalEmail, model);
        UserProfileResponse profile = userService.getProfile(principalEmail);
        model.addAttribute("viewProfile", profile);

        Page<PostResponse> profilePosts = postService.list(
            PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt")),
            List.of(PostState.PUBLISHED, PostState.DRAFT, PostState.HIDDEN, PostState.REDACTED),
            null
        );
        List<PostResponse> ownPosts = profilePosts.getContent().stream()
            .filter(post -> profile.getUsername().equals(post.getAuthorUsername()))
            .toList();
        model.addAttribute("ownPosts", ownPosts);

        Page<MarketplaceItemResponse> ownItems = marketplaceService.list(
            PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt")),
            null,
            null,
            null,
            null,
            null,
            "createdAt",
            "desc",
            true,
            principalEmail
        );
        model.addAttribute("ownItems", ownItems.getContent());
        model.addAttribute("activeProfileTab", "marketplace".equalsIgnoreCase(tab) ? "marketplace" : "posts");

        return "web/profile";
    }

    @GetMapping("/web/profile/files")
    public String profileFiles(
        @AuthenticationPrincipal String principalEmail,
        @RequestParam(name = "page", defaultValue = "0") int page,
        Model model
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        addAuthModel(principalEmail, model);
        Page<FileAttachmentResponse> uploads = fileService.listProfileUploads(
            principalEmail,
            PageRequest.of(Math.max(page, 0), 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        model.addAttribute("uploadsPage", uploads);
        return "web/profile-files";
    }

    @GetMapping("/web/profile/edit")
    public String profileEdit(
        @AuthenticationPrincipal String principalEmail,
        Model model
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        addAuthModel(principalEmail, model);
        model.addAttribute("viewProfile", userService.getProfile(principalEmail));
        return "web/profile-edit";
    }

    @PostMapping("/web/profile/edit")
    public String profileEditSave(
        @AuthenticationPrincipal String principalEmail,
        @RequestParam String username,
        @RequestParam String email,
        @RequestParam(name = "name", required = false) String name,
        @RequestParam(name = "phoneNumber", required = false) String phoneNumber,
        @RequestParam(name = "bio", required = false) String bio,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            userService.updateProfile(principalEmail, new UpdateProfileRequest(username, email, name, bio, phoneNumber));
            redirectAttributes.addFlashAttribute("notice", "Profile updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/profile/edit";
    }

    @PostMapping("/web/profile/picture")
    public String uploadProfilePicture(
        @AuthenticationPrincipal String principalEmail,
        @RequestParam("file") MultipartFile file,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            fileService.uploadProfile(principalEmail, file);
            redirectAttributes.addFlashAttribute("notice", "Profile picture uploaded.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/profile/edit";
    }

    @GetMapping("/web/posts")
    public String posts(
        @AuthenticationPrincipal String principalEmail,
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "state", required = false) String state,
        @RequestParam(name = "page", defaultValue = "0") int page,
        Model model
    ) {
        List<PostState> states = parseStateFilter(state);
        Page<PostResponse> posts = postService.list(
            PageRequest.of(Math.max(page, 0), 10, Sort.by(Sort.Direction.DESC, "createdAt")),
            states,
            query
        );

        addAuthModel(principalEmail, model);
        model.addAttribute("postsPage", posts);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("state", state == null ? "" : state);
        return "web/posts";
    }

    @PostMapping("/web/posts")
    public String createPost(
        @AuthenticationPrincipal String principalEmail,
        @RequestParam String title,
        @RequestParam String content,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            postService.create(principalEmail, new PostRequest(title, content));
            redirectAttributes.addFlashAttribute("notice", "Post published.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/posts";
    }

    @GetMapping("/web/posts/{postId}")
    public String postDetail(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long postId,
        Model model
    ) {
        PostResponse post = postService.get(postId);
        UserProfileResponse authorProfile = userService.getProfileByUsername(post.getAuthorUsername());
        List<FileAttachmentResponse> attachments = List.of();
        if (isAuthenticatedPrincipal(principalEmail)) {
            attachments = fileService.listPostAttachments(
                postId,
                principalEmail,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();
        }

        addAuthModel(principalEmail, model);
        model.addAttribute("post", post);
        model.addAttribute("authorProfile", authorProfile);
        model.addAttribute("authorDisplayName", displayName(authorProfile));
        model.addAttribute("authorImageUrl", profileImageUrl(authorProfile));
        model.addAttribute("attachments", attachments);
        model.addAttribute("canEditPost", canEditPost(principalEmail, post));
        return "web/post-detail";
    }

    @PostMapping("/web/posts/{postId}/update")
    public String updatePost(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long postId,
        @RequestParam String title,
        @RequestParam String content,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            postService.update(postId, principalEmail, new PostRequest(title, content));
            redirectAttributes.addFlashAttribute("notice", "Post updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/posts/" + postId;
    }

    @PostMapping("/web/posts/{postId}/state")
    public String updatePostState(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long postId,
        @RequestParam String state,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            PostState postState = PostState.valueOf(state.toUpperCase(Locale.ROOT));
            postService.updateState(postId, principalEmail, postState);
            redirectAttributes.addFlashAttribute("notice", "Post state updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/posts/" + postId;
    }

    @PostMapping("/web/posts/{postId}/delete")
    public String deletePost(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long postId,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            postService.delete(postId, principalEmail);
            redirectAttributes.addFlashAttribute("notice", "Post deleted.");
            return "redirect:/web/posts";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/web/posts/" + postId;
        }
    }

    @PostMapping("/web/posts/{postId}/comments")
    public String createComment(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long postId,
        @RequestParam String content,
        @RequestParam(name = "parentCommentId", required = false) Long parentCommentId,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            commentService.create(postId, principalEmail, new CommentRequest(content, parentCommentId));
            redirectAttributes.addFlashAttribute("notice", "Comment added.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/posts/" + postId;
    }

    @PostMapping("/web/comments/{commentId}/update")
    public String updateComment(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long commentId,
        @RequestParam Long postId,
        @RequestParam String content,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            commentService.update(commentId, principalEmail, new CommentRequest(content, null));
            redirectAttributes.addFlashAttribute("notice", "Comment updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/posts/" + postId;
    }

    @PostMapping("/web/comments/{commentId}/delete")
    public String deleteComment(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long commentId,
        @RequestParam Long postId,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            commentService.delete(commentId, principalEmail);
            redirectAttributes.addFlashAttribute("notice", "Comment deleted.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/posts/" + postId;
    }

    @PostMapping("/web/posts/{postId}/like")
    public String likePost(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long postId,
        RedirectAttributes redirectAttributes
    ) {
        return reactToPost(principalEmail, postId, true, redirectAttributes);
    }

    @PostMapping("/web/posts/{postId}/unlike")
    public String unlikePost(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long postId,
        RedirectAttributes redirectAttributes
    ) {
        return reactToPost(principalEmail, postId, false, redirectAttributes);
    }

    @PostMapping("/web/comments/{commentId}/like")
    public String likeComment(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long commentId,
        @RequestParam Long postId,
        RedirectAttributes redirectAttributes
    ) {
        return reactToComment(principalEmail, commentId, postId, true, redirectAttributes);
    }

    @PostMapping("/web/comments/{commentId}/unlike")
    public String unlikeComment(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long commentId,
        @RequestParam Long postId,
        RedirectAttributes redirectAttributes
    ) {
        return reactToComment(principalEmail, commentId, postId, false, redirectAttributes);
    }

    @GetMapping("/web/marketplace")
    public String marketplace(
        @AuthenticationPrincipal String principalEmail,
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
        @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
        @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
        @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
        @RequestParam(name = "ownOnly", defaultValue = "false") boolean ownOnly,
        @RequestParam(name = "page", defaultValue = "0") int page,
        Model model
    ) {
        MarketplaceItemStatus parsedStatus = parseMarketplaceStatus(status);

        Page<MarketplaceItemResponse> items = marketplaceService.list(
            PageRequest.of(Math.max(page, 0), 12, Sort.by(Sort.Direction.DESC, "createdAt")),
            query,
            parsedStatus,
            category,
            minPrice,
            maxPrice,
            sortBy,
            sortDir,
            ownOnly,
            principalEmail
        );

        addAuthModel(principalEmail, model);
        model.addAttribute("itemsPage", items);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("category", category == null ? "" : category);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("ownOnly", ownOnly);
        model.addAttribute("minPrice", minPrice == null ? "" : minPrice.toPlainString());
        model.addAttribute("maxPrice", maxPrice == null ? "" : maxPrice.toPlainString());
        return "web/marketplace";
    }

    @PostMapping("/web/marketplace")
    public String createMarketplaceItem(
        @AuthenticationPrincipal String principalEmail,
        @RequestParam String title,
        @RequestParam String description,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam BigDecimal price,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            marketplaceService.create(
                principalEmail,
                new MarketplaceItemRequest(title, description, category, price, MarketplaceItemStatus.AVAILABLE)
            );
            redirectAttributes.addFlashAttribute("notice", "Marketplace item created.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/marketplace";
    }

    @GetMapping("/web/marketplace/{itemId}")
    public String marketplaceItem(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long itemId,
        Model model
    ) {
        MarketplaceItemResponse item = marketplaceService.get(itemId);
        addAuthModel(principalEmail, model);

        model.addAttribute("item", item);
        model.addAttribute("isOwner", isCurrentUser(item.getSellerUsername(), principalEmail));

        if (isAuthenticatedPrincipal(principalEmail)) {
            model.addAttribute("favoriteStatus", marketplaceService.favoriteStatus(itemId, principalEmail));
        }

        return "web/market-item";
    }

    @PostMapping("/web/marketplace/{itemId}/update")
    public String updateMarketplaceItem(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long itemId,
        @RequestParam String title,
        @RequestParam String description,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam BigDecimal price,
        @RequestParam String status,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            MarketplaceItemStatus parsedStatus = MarketplaceItemStatus.valueOf(status.toUpperCase(Locale.ROOT));
            marketplaceService.update(
                itemId,
                principalEmail,
                new MarketplaceItemRequest(title, description, category, price, parsedStatus)
            );
            redirectAttributes.addFlashAttribute("notice", "Marketplace item updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/marketplace/" + itemId;
    }

    @PostMapping("/web/marketplace/{itemId}/status")
    public String updateMarketplaceStatus(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long itemId,
        @RequestParam String status,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            MarketplaceItemStatus parsedStatus = MarketplaceItemStatus.valueOf(status.toUpperCase(Locale.ROOT));
            marketplaceService.updateStatus(itemId, principalEmail, parsedStatus);
            redirectAttributes.addFlashAttribute("notice", "Marketplace item status updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/marketplace/" + itemId;
    }

    @PostMapping("/web/marketplace/{itemId}/delete")
    public String deleteMarketplaceItem(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long itemId,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            marketplaceService.delete(itemId, principalEmail);
            redirectAttributes.addFlashAttribute("notice", "Marketplace item deleted.");
            return "redirect:/web/marketplace";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/web/marketplace/" + itemId;
        }
    }

    @PostMapping("/web/marketplace/{itemId}/favorite")
    public String favoriteMarketplaceItem(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long itemId,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            marketplaceService.favorite(itemId, principalEmail);
            redirectAttributes.addFlashAttribute("notice", "Item favorited.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/marketplace/" + itemId;
    }

    @PostMapping("/web/marketplace/{itemId}/unfavorite")
    public String unfavoriteMarketplaceItem(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long itemId,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            marketplaceService.unfavorite(itemId, principalEmail);
            redirectAttributes.addFlashAttribute("notice", "Item unfavorited.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/marketplace/" + itemId;
    }

    @PostMapping("/web/marketplace/{itemId}/contact")
    public String contactMarketplaceSeller(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable Long itemId,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            MarketplaceContactResponse contact = marketplaceService.contactSeller(itemId, principalEmail);
            redirectAttributes.addFlashAttribute(
                "notice",
                "Seller: " + contact.getSellerUsername() + " <" + contact.getSellerEmail() + ">"
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/marketplace/" + itemId;
    }

    @GetMapping("/web/person/{username}")
    public String personPage(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable String username,
        Model model
    ) {
        UserProfileResponse person = userService.getProfileByUsername(username);
        addAuthModel(principalEmail, model);

        Page<PostResponse> posts = postService.list(
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
            List.of(PostState.PUBLISHED),
            null
        );
        List<PostResponse> personPosts = posts.getContent().stream()
            .filter(post -> username.equals(post.getAuthorUsername()))
            .toList();

        Page<MarketplaceItemResponse> items = marketplaceService.list(
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
            null,
            null,
            null,
            null,
            null,
            "createdAt",
            "desc",
            false,
            principalEmail
        );
        List<MarketplaceItemResponse> personItems = items.getContent().stream()
            .filter(item -> username.equals(item.getSellerUsername()))
            .toList();

        model.addAttribute("person", person);
        model.addAttribute("personPosts", personPosts);
        model.addAttribute("personItems", personItems);
        return "web/person";
    }

    @GetMapping("/posts-page")
    public String postsPageAlias() {
        return "redirect:/web/posts";
    }

    @GetMapping("/post-page")
    public String postPageAlias(@RequestParam(name = "id", required = false) Long postId) {
        if (postId == null) {
            return "redirect:/web/posts";
        }
        return "redirect:/web/posts/" + postId;
    }

    @GetMapping("/login-page")
    public String loginPageAlias() {
        return "redirect:/web/login";
    }

    @GetMapping("/register-page")
    public String registerPageAlias() {
        return "redirect:/web/register";
    }

    @GetMapping("/logout-page")
    public String logoutPageAlias() {
        return "redirect:/web/logout";
    }

    @GetMapping("/profile-page")
    public String profilePageAlias() {
        return "redirect:/web/profile";
    }

    @GetMapping("/profile-edit-page")
    public String profileEditPageAlias() {
        return "redirect:/web/profile/edit";
    }

    @GetMapping("/marketplace-page")
    public String marketplacePageAlias() {
        return "redirect:/web/marketplace";
    }

    @GetMapping("/market-item-page")
    public String marketItemPageAlias(@RequestParam(name = "id", required = false) Long itemId) {
        if (itemId == null) {
            return "redirect:/web/marketplace";
        }
        return "redirect:/web/marketplace/" + itemId;
    }

    @GetMapping("/person-page")
    public String personPageAlias(@RequestParam(name = "username", required = false) String username) {
        if (username == null || username.isBlank()) {
            return "redirect:/web/dashboard";
        }
        return "redirect:/web/person/" + username;
    }

    private void addAuthModel(String principalEmail, Model model) {
        boolean authenticated = isAuthenticatedPrincipal(principalEmail);
        model.addAttribute("principalEmail", principalEmail);
        model.addAttribute("isAuthenticated", authenticated);

        if (authenticated) {
            UserProfileResponse profile = userService.getProfile(principalEmail);
            model.addAttribute("profile", profile);
            model.addAttribute("principalUsername", profile.getUsername());
            model.addAttribute("profileDisplayName", displayName(profile));
            model.addAttribute("profileImageUrl", profileImageUrl(profile));
        } else {
            model.addAttribute("principalUsername", null);
            model.addAttribute("profileDisplayName", "Guest");
            model.addAttribute("profileImageUrl", placeholderImageUrl("Guest"));
        }
    }

    private String profileImageUrl(UserProfileResponse profile) {
        if (profile.getProfilePictureId() != null) {
            return "/files/" + profile.getProfilePictureId();
        }
        return placeholderImageUrl(displayName(profile));
    }

    private String displayName(UserProfileResponse profile) {
        if (profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName();
        }
        if (profile.getUsername() != null && !profile.getUsername().isBlank()) {
            return profile.getUsername();
        }
        return "User";
    }

    private String placeholderImageUrl(String label) {
        String value = (label == null || label.isBlank()) ? "User" : label.trim();
        return "https://placehold.co/250x250?text=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private List<PostState> parseStateFilter(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }

        try {
            return List.of(PostState.valueOf(state.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid post state: " + state);
        }
    }

    private MarketplaceItemStatus parseMarketplaceStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return MarketplaceItemStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid marketplace status: " + status);
        }
    }

    private String reactToPost(
        String principalEmail,
        Long postId,
        boolean like,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            if (like) {
                reactionService.likePost(postId, principalEmail);
            } else {
                reactionService.unlikePost(postId, principalEmail);
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/web/posts/" + postId;
    }

    private String reactToComment(
        String principalEmail,
        Long commentId,
        Long postId,
        boolean like,
        RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return "redirect:/web/login";
        }

        try {
            if (like) {
                reactionService.likeComment(commentId, principalEmail);
            } else {
                reactionService.unlikeComment(commentId, principalEmail);
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/web/posts/" + postId;
    }

    private boolean canEditPost(String principalEmail, PostResponse post) {
        return isAuthenticatedPrincipal(principalEmail) && isCurrentUser(post.getAuthorUsername(), principalEmail);
    }

    private boolean isCurrentUser(String username, String principalEmail) {
        if (!isAuthenticatedPrincipal(principalEmail)) {
            return false;
        }
        UserProfileResponse profile = userService.getProfile(principalEmail);
        return profile.getUsername() != null && profile.getUsername().equals(username);
    }

    private boolean isAuthenticatedPrincipal(String principalEmail) {
        return principalEmail != null
            && !principalEmail.isBlank()
            && !"anonymousUser".equalsIgnoreCase(principalEmail);
    }

    private void addJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 12);
        response.addCookie(cookie);
    }

    private void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

}
