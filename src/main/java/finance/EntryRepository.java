package finance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRepository extends JpaRepository<EntryEntity, Long> {
    boolean existsByQuelleIdAndExterneKennung(Long quelleId, String externeKennung);
}
