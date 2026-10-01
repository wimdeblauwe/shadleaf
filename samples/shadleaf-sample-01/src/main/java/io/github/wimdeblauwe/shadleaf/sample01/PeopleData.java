package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.shadleaf.sample01.Person.Role;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Fills the in-memory database with people at startup: every combination of a first and a last name, with a role,
 * an order count and a join date from a seeded random, so every start (and every test) sees the same rows.
 */
@Component
class PeopleData implements ApplicationRunner {

  private static final List<String> FIRST_NAMES = List.of("Ada", "Alan", "Barbara", "Claude", "Dennis", "Edsger",
      "Frances", "Grace", "Hedy", "Ivan", "Joan", "Ken", "Linus", "Margaret", "Niklaus", "Radia", "Sophie", "Tim");
  private static final List<String> LAST_NAMES = List.of("Allen", "Backus", "Cerf", "Dijkstra", "Engelbart",
      "Floyd", "Goldberg", "Hamilton", "Iverson", "Kay", "Liskov", "Moore", "Perlman", "Ritchie", "Wirth");
  private static final LocalDate FIRST_JOINED = LocalDate.of(2020, 1, 6);

  private final PersonRepository repository;

  PeopleData(PersonRepository repository) {
    this.repository = repository;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (repository.count() > 0) {
      return;
    }
    Random random = new Random(42);
    Role[] roles = Role.values();
    List<Person> people = new ArrayList<>();
    for (String lastName : LAST_NAMES) {
      for (String firstName : FIRST_NAMES) {
        String email = (firstName + "." + lastName + "@example.com").toLowerCase();
        // Mostly members: one owner in 40, a few admins and guests.
        int pick = random.nextInt(40);
        Role role = pick == 0 ? Role.OWNER : pick < 5 ? Role.ADMIN : pick < 9 ? Role.GUEST : Role.MEMBER;
        people.add(new Person(firstName + " " + lastName, email, role, random.nextInt(60),
            FIRST_JOINED.plusDays(random.nextInt(2000))));
      }
    }
    repository.saveAll(people);
  }
}
