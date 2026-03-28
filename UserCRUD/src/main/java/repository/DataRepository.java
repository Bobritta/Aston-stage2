package repository;

import java.util.List;
import java.util.Optional;

public interface DataRepository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    T deleteById(ID id);
}

