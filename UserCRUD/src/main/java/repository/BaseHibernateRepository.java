package repository;

import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HibernateUtil;

import java.util.List;
import java.util.Optional;

/**
 * Базовая реализация репозитория на основе Hibernate.
 *
 * @param <T>  тип сущности
 * @param <ID> тип идентификатора сущности
 */
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

    /**
     * Сохраняет или обновляет одиночную сущность.
     * Использует метод {@link Session#merge(Object)}, что делает предварительный SELECT
     * для проверки существования записи. Подходит для работы с detached объектами.
     *
     * @param entity сущность для сохранения
     * @return сохраненная копия сущности, привязанная к текущей сессии
     */
    @Override
    public T save(T entity) {
        return getSession().merge(entity);
    }

    /**
     * Осуществляет пакетную вставку (Batch Insert) списка новых сущностей.
     * <p>
     * Использует {@link Session#persist(Object)} для максимальной производительности.
     * Каждые 50 записей выполняется flush и clear сессии для оптимизации памяти.
     * </p>
     * <b>Внимание:</b> метод предназначен только для новых записей. Если сущность
     * уже существует в базе, будет выброшено исключение. После выполнения метода
     * объекты становятся detached (отсоединенными от сессии).
     *
     * @param entities список новых сущностей
     * @return список переданных сущностей с заполненными ID
     */
    @Override
    public List<T> createAll(List<T> entities) {
        Session session = getSession();
        int batchSize = 50;

        for (int i = 0; i < entities.size(); i++) {
            session.persist(entities.get(i));

            if (i > 0 && i % batchSize == 0) {
                session.flush();
                session.clear();
            }
        }
        return entities;
    }

    /**
     * Удаляет сущность по её идентификатору.
     *
     * @param id идентификатор сущности
     * @return удаленная сущность
     * @throws EntityNotFoundException если запись с таким id не найдена
     */
    @Override
    public T deleteById(ID id) {
        T entity = findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись с id " + id + " не найдена для удаления"));
        getSession().remove(entity);

        logger.debug("Сущность {} с id {} успешно помечена на удаление", entityClass.getSimpleName(), id);
        return entity;
    }

    /**
     * Поиск сущности по идентификатору.
     *
     * @param id идентификатор
     * @return Optional с найденной сущностью или пустой, если запись отсутствует
     */
    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(getSession().get(entityClass, id));
    }

    /**
     * Получение всех записей данной сущности.
     *
     * @return список всех сущностей в таблице
     */
    @Override
    public List<T> findAll() {
        return getSession().createQuery("from " + entityClass.getName(), entityClass).list();
    }
}

