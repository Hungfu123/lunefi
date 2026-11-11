package finance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kategorien")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final EntryRepository entryRepository;
    private final PersonRepository personRepository;

    public CategoryController(CategoryRepository categoryRepository, EntryRepository entryRepository,
            PersonRepository personRepository) {
        this.categoryRepository = categoryRepository;
        this.entryRepository = entryRepository;
        this.personRepository = personRepository;
    }

    @GetMapping
    public List<CategoryInfo> list() {
        TreeSet<String> names = new TreeSet<>();
        entryRepository.findAll().forEach(entry -> names.add(entry.getKategorie()));
        categoryRepository.findAll().forEach(category -> names.add(category.getName()));

        List<CategoryInfo> categories = new ArrayList<>();
        for (String name : names) {
            KategorieEntity configured = categoryRepository.findById(name).orElse(null);
            SplitConfiguration split = configured == null
                    ? new SplitConfiguration("HALBHALB", null)
                    : configured.asSplitConfiguration();
            String attitude = configured == null ? Attitude.FLEXIBEL : configured.asAttitude();
            categories.add(new CategoryInfo(name, split, attitude));
        }
        return categories;
    }

    @PutMapping("/{kategorie}/haltung")
    public ResponseEntity<Attitude> setAttitude(@PathVariable String kategorie,
            @RequestBody Attitude attitude) {
        if (attitude == null || !attitude.isValid()) {
            return ResponseEntity.badRequest().build();
        }
        KategorieEntity saved = categoryRepository.findById(kategorie)
                .map(existing -> {
                    existing.setAttitude(attitude.haltung());
                    return existing;
                })
                .orElseGet(() -> new KategorieEntity(kategorie, "HALBHALB", Map.of(), attitude.haltung()));
        return ResponseEntity.ok(new Attitude(categoryRepository.save(saved).asAttitude()));
    }

    @PutMapping("/{kategorie}/split")
    public ResponseEntity<SplitConfiguration> setSplit(@PathVariable String kategorie,
            @RequestBody SplitConfiguration split) {
        if (!isValid(split)) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, Integer> shares = "ANTEILE".equals(split.art()) ? split.anteile() : Map.of();
        KategorieEntity saved = categoryRepository.findById(kategorie)
                .map(existing -> {
                    existing.setArt(split.art());
                    existing.getAnteile().clear();
                    existing.getAnteile().putAll(shares);
                    return existing;
                })
                .orElseGet(() -> new KategorieEntity(kategorie, split.art(), shares));
        return ResponseEntity.ok(categoryRepository.save(saved).asSplitConfiguration());
    }

    private boolean isValid(SplitConfiguration split) {
        if (split == null || !("HALBHALB".equals(split.art()) || "ANTEILE".equals(split.art()))) {
            return false;
        }
        if (!"ANTEILE".equals(split.art())) {
            return true;
        }
        Map<String, Integer> shares = split.anteile();
        if (shares == null || shares.isEmpty()) {
            return false;
        }
        int total = 0;
        for (Map.Entry<String, Integer> share : shares.entrySet()) {
            if (share.getKey() == null || share.getKey().isBlank()
                    || share.getValue() == null || share.getValue() <= 0
                    || !personRepository.existsByName(share.getKey())) {
                return false;
            }
            total += share.getValue();
        }
        return total == 100;
    }
}
