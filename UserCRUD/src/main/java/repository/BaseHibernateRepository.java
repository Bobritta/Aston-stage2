package repository;

import exception.handler.GlobalExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HibernateUtil;

import java.util.List;
import java.util.Optional;

public abstract class BaseHibernateRepository<T, ID> implements DataRepository<T, ID> {
    private final Class<T> entityClass;
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    private static final Logger logger = LoggerFactory.getLogger(BaseHibernateRepository.class);

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
    public T deleteById(ID id) {
        T entity = findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись с id " + id + " не найдена для удаления"));
        getSession().remove(entity);

        logger.debug("Сущность {} с id {} успешно помечена на удаление", entityClass.getSimpleName(), id);
        return entity;
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

