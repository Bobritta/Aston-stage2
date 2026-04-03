package model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Данные для обновления пользователя.
 *
 * @param id id, не может быть пустым или отрицательным
 * @param name  полное имя пользователя, не может быть пустым
 * @param email адрес электронной почты, должен быть валидным форматом
 * @param age   возраст пользователя, не может быть отрицательным
 */
@Builder
public record UserUpdateDTO(
        @NotBlank @Min(0) long id,
        @NotBlank String name,
        @Email String email,
        @Min(0) Integer age) {

}
