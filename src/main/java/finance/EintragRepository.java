package finance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EintragRepository extends JpaRepository<EintragEntity, Long> {
    boolean existsByQuelleIdAndExterneKennung(Long quelleId, String externeKennung);
}
