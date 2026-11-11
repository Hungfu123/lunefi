package finance;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AusgleichsRepository extends JpaRepository<AusgleichsEntity, Long> {

    Optional<AusgleichsEntity> findByMonat(String monat);

    boolean existsByMonat(String monat);
}
