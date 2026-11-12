package finance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryPartRepository extends JpaRepository<EntryPartEntity, Long> {

    List<EntryPartEntity> findByEintragIdOrderByPositionAsc(Long eintragId);

    List<EntryPartEntity> findAllByOrderByEintragIdAscPositionAsc();

    void deleteByEintragId(Long eintragId);
}
