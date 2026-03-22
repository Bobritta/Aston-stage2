package repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import util.HibernateUtil;

import java.util.List;
import java.util.Optional;

public abstract class BaseHibernateRepository<T, ID> implements DataRepository<T, ID> {
    private final Class<T> entityClass;
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    protected BaseHibernateRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public T save(T entity) {
        return getSession().merge(entity);
    }

    @Override
    public void deleteById(ID id) {
        getSession().createMutationQuery("delete from " + entityClass.getName() + " where id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(getSession().get(entityClass, id));
    }

    @Override
    public List<T> findAll() {
        return getSession().createQuery("from " + entityClass.getName(), entityClass).list();
    }
}

