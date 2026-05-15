package com.github.rrin.vulyk.lab.module.auth.auth01;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemEntity;
import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.post.PostState;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.domain.entity.user.UserRole;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import com.github.rrin.vulyk.repository.MarketplaceItemRepository;
import com.github.rrin.vulyk.repository.PostRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnLabEnabled(AuthProfileIdorLab.LAB_ID)
public class AuthProfileIdorLabSeeder implements ApplicationRunner {

    public static final String TARGET_USERNAME = "ops-manager";
    private static final String SEEDED_POST_TITLE = "Ops Change Window Memo";
    private static final String SEEDED_MARKETPLACE_TITLE = "Operations Access Badge Holder";

    private final AuthProfileIdorLab labDefinition;
    private final LabProgressService labProgressService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        labProgressService.ensureProgressRows(labDefinition);

        UserEntity target = ensureUser(
            TARGET_USERNAME,
            "ops-manager@vulyk.lab",
            "Operations Manager",
            "Owns sensitive profile metadata for access control exercises.",
            "+1000000001"
        );

        ensurePublishedPost(target);
        ensureMarketplaceItem(target);
    }

    private UserEntity ensureUser(String username, String email, String name, String bio, String phoneNumber) {
        UserEntity user = userRepository.findByEmail(email)
            .orElseGet(() -> UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("auth01-seeded-password"))
                .role(UserRole.USER)
                .build());

        user.setUsername(username);
        user.setName(name);
        user.setBio(bio);
        user.setPhoneNumber(phoneNumber);
        return userRepository.save(user);
    }

    private void ensurePublishedPost(UserEntity author) {
        PostEntity post = postRepository.findByAuthorIdAndTitleIgnoreCase(author.getId(), SEEDED_POST_TITLE)
            .orElseGet(() -> PostEntity.builder()
                .author(author)
                .title(SEEDED_POST_TITLE)
                .build());

        post.setContent("Operations update from " + TARGET_USERNAME + ". Contact this account for coordination details.");
        post.setState(PostState.PUBLISHED);
        postRepository.save(post);
    }

    private void ensureMarketplaceItem(UserEntity seller) {
        MarketplaceItemEntity item = marketplaceItemRepository
            .findBySellerIdAndTitleIgnoreCase(seller.getId(), SEEDED_MARKETPLACE_TITLE)
            .orElseGet(() -> MarketplaceItemEntity.builder()
                .seller(seller)
                .title(SEEDED_MARKETPLACE_TITLE)
                .build());

        item.setDescription("Field operations accessory listed by " + TARGET_USERNAME + ".");
        item.setCategory("operations");
        item.setPrice(new BigDecimal("14.99"));
        item.setStatus(MarketplaceItemStatus.AVAILABLE);
        marketplaceItemRepository.save(item);
    }
}
