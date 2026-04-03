package model;

import lombok.Builder;

@Builder
public record UserResponseDTO(
        Long id,
        String name,
        String email,
        Integer age,
        String createdAt
) {
}
