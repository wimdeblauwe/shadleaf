package io.github.wimdeblauwe.shadleaf.sample01;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

  /** The people whose name or email contains the text, ignoring case, a page at a time. */
  Page<Person> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);

  /** Everyone, a slice at a time: no count query, only whether there is a next slice (the load more page). */
  Slice<Person> findAllBy(Pageable pageable);
}
