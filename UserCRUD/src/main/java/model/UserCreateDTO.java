package model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UserCreateDTO(
        @NotBlank String name,
        @Email String email,
        @Min(0) Integer age
) {
}
