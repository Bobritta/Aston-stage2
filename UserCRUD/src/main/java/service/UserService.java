package service;

import model.UserCreateDTO;
import model.UserResponseDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {
    void createUser(UserCreateDTO dto);

    Optional<UserResponseDTO> findById(long id);

    Optional<UserResponseDTO> findByEmail(String email);

    Optional<UserResponseDTO> findByUsername(String username);

    List<UserResponseDTO> findAll();

    boolean updateUser(long id, UserCreateDTO dto);

    boolean deleteById(long id);
}
