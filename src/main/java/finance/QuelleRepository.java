package finance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuelleRepository extends JpaRepository<QuelleEntity, Long> {
    boolean existsByName(String name);
}
