package bd.ac.kuet.swms.repository;

import java.util.List;
import java.util.Optional;

public interface Repository<T> {
    T save(T entity);

    Optional<T> findById(int id);

    List<T> findAll();

    boolean delete(int id);
}
