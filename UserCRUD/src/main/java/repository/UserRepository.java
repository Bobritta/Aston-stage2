package repository;

import model.User;

import java.util.Optional;

public interface UserRepository extends DataRepository<User, Long>{
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);
}
