package finance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EntryParts {

    private final EntryPartRepository partRepository;

    public EntryParts(EntryPartRepository partRepository) {
        this.partRepository = partRepository;
    }

    public List<LineItem> lineItems(Collection<EntryEntity> entries) {
        Map<Long, List<EntryPartEntity>> byEntry = partsOf(entries);
        List<LineItem> result = new ArrayList<>();
        for (EntryEntity entry : entries) {
            List<EntryPartEntity> parts = byEntry.getOrDefault(entry.getId(), List.of());
            if (parts.isEmpty()) {
                result.add(new LineItem(entry.getId(), null, entry.getDatum(), entry.getBetrag(),
                        entry.getKategorie(), entry.getPerson(),
                        Boolean.TRUE.equals(entry.getGemeinsam())));
            } else {
                for (EntryPartEntity part : parts) {
                    result.add(new LineItem(entry.getId(), part.getId(), entry.getDatum(),
                            part.getBetrag(), part.getKategorie(), part.getPerson(),
                            part.isGemeinsam()));
                }
            }
        }
        return result;
    }

    public Map<Long, List<EntryPartEntity>> partsOf(Collection<EntryEntity> entries) {
        Map<Long, List<EntryPartEntity>> byEntry = new LinkedHashMap<>();
        for (EntryPartEntity part : partRepository.findAllByOrderByEintragIdAscPositionAsc()) {
            byEntry.computeIfAbsent(part.getEintragId(), id -> new ArrayList<>()).add(part);
        }
        return byEntry;
    }

    public List<EntryPart> partsFor(Long entryId) {
        return partRepository.findByEintragIdOrderByPositionAsc(entryId).stream()
                .map(EntryPartEntity::asPart)
                .toList();
    }

    public Entry toApi(EntryEntity entry) {
        List<EntryPart> parts = partsFor(entry.getId());
        return new Entry(entry.getId(), entry.getDatum(), entry.getBetrag(), entry.getKategorie(),
                entry.getPerson(), entry.getGemeinsam(), entry.getZahler(), entry.getKonto(),
                entry.getVerwendungszweck(), entry.getQuelleId(),
                parts.isEmpty() ? null : parts);
    }

    public boolean isValid(List<EntryPart> parts, BigDecimal amount) {
        if (parts == null || parts.isEmpty() || amount == null || amount.signum() == 0) {
            return false;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (EntryPart part : parts) {
            if (part == null || part.betrag() == null || part.betrag().signum() == 0
                    || part.betrag().signum() != amount.signum()
                    || part.betrag().scale() > 2
                    || part.kategorie() == null || part.kategorie().isBlank()) {
                return false;
            }
            boolean personSet = part.person() != null && !part.person().isBlank();
            if (personSet == Boolean.TRUE.equals(part.gemeinsam())) {
                return false;
            }
            sum = sum.add(part.betrag());
        }
        return sum.compareTo(amount) == 0;
    }

    public boolean anyShared(List<EntryPart> parts) {
        return parts != null && parts.stream().anyMatch(part -> Boolean.TRUE.equals(part.gemeinsam()));
    }

    public EntryPart primary(List<EntryPart> parts) {
        EntryPart primary = null;
        for (EntryPart part : parts) {
            if (primary == null || part.betrag().abs().compareTo(primary.betrag().abs()) > 0) {
                primary = part;
            }
        }
        return primary;
    }

    @Transactional
    public void replaceParts(Long entryId, List<EntryPart> parts) {
        partRepository.deleteByEintragId(entryId);
        int position = 0;
        for (EntryPart part : parts) {
            partRepository.save(new EntryPartEntity(entryId, position++, part.betrag(),
                    part.kategorie(), part.person(), Boolean.TRUE.equals(part.gemeinsam())));
        }
    }

    @Transactional
    public void clearParts(Long entryId) {
        partRepository.deleteByEintragId(entryId);
    }
}
