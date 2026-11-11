package finance;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<PersonEntity, Long> {

    boolean existsByName(String name);

    Optional<PersonEntity> findByName(String name);
}
