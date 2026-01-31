package com.github.rrin.vulyk.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 60)
    private String username;

    @Email
    @Size(max = 120)
    private String email;

    @Size(max = 60)
    private String name;

    @Size(max = 2000)
    private String bio;

    @Size(max = 15)
    private String phoneNumber;
}
