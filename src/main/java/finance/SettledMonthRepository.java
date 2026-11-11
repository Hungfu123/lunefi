package finance;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SettledMonthRepository extends JpaRepository<SettledMonthEntity, Long> {

    Optional<SettledMonthEntity> findByMonat(String monat);

    boolean existsByMonat(String monat);
}
