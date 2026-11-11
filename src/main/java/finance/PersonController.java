package finance;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/personen")
public class PersonController {

    private final PersonRepository repository;

    public PersonController(PersonRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Person> anlegen(@RequestBody Person person) {
        if (person.name() == null || person.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return repository.findByName(person.name())
                .<ResponseEntity<Person>>map(besteht -> ResponseEntity.ok(besteht.alsPerson()))
                .orElseGet(() -> {
                    PersonEntity gespeichert = repository.save(new PersonEntity(person.name()));
                    return ResponseEntity.status(HttpStatus.CREATED).body(gespeichert.alsPerson());
                });
    }

    @GetMapping
    public List<Person> alle() {
        return repository.findAll().stream().map(PersonEntity::alsPerson).toList();
    }
}
