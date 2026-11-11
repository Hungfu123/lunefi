package finance;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KontoRepository extends JpaRepository<KontoEntity, Long> {

    Optional<KontoEntity> findByName(String name);

    boolean existsByName(String name);
}
