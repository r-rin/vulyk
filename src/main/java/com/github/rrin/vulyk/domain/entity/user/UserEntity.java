package com.github.rrin.vulyk.domain.entity.user;

import com.github.rrin.vulyk.domain.Identifiable;
import com.github.rrin.vulyk.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity implements Identifiable<Long> {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "name", length = 60)
    private String name;

    @Column(name = "bio", length = 2000)
    private String bio;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email", unique = true, length = 120)
    private String email;

    @Column(name = "phone_number", unique = true, length = 15)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, insertable = false)
    UserRole role;
}
