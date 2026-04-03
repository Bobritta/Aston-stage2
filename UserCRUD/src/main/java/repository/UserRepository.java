package repository;

import model.UserEntity;

import java.util.Optional;

public interface UserRepository extends DataRepository<UserEntity, Long>{
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);
}
