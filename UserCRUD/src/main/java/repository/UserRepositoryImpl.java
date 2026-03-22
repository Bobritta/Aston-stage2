package repository;

import model.User;

import java.util.Optional;

public class UserRepositoryImpl extends BaseHibernateRepository<User, Long> implements UserRepository {

    public UserRepositoryImpl() {
        super(User.class);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return getSession().createQuery("from User where email = :email", User.class)
                .setParameter("email", email)
                .uniqueResultOptional();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return getSession().createQuery("from User where username = :username", User.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }
}