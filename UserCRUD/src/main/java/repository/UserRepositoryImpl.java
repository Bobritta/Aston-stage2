package repository;

import model.UserEntity;

import java.util.Optional;

public class UserRepositoryImpl extends BaseHibernateRepository<UserEntity, Long> implements UserRepository {

    public UserRepositoryImpl() {
        super(UserEntity.class);
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return getSession().createQuery("from User where email = :email", UserEntity.class)
                .setParameter("email", email)
                .uniqueResultOptional();
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return getSession().createQuery("from User where username = :username", UserEntity.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }
}