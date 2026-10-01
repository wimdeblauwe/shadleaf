package io.github.wimdeblauwe.shadleaf.sample01;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
}
