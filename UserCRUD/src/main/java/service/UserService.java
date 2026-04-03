package service;

import model.UserCreateDTO;
import model.UserResponseDTO;
import model.UserUpdateDTO;

import java.util.List;

public interface UserService {
    UserResponseDTO createUser(UserCreateDTO dto);

    UserResponseDTO findById(long id);
    UserResponseDTO findByEmail(String email);
    UserResponseDTO findByUsername(String username);

    List<UserResponseDTO> findAll();

    UserResponseDTO updateUser(long id, UserUpdateDTO dto);

    UserResponseDTO deleteById(long id);
}
