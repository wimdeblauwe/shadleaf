package io.github.wimdeblauwe.shadleaf.sample01;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PersonRepository extends JpaRepository<Person, Long> {

  /** The people whose name or email contains the text, ignoring case, a page at a time. */
  Page<Person> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);

  /** Everyone, a slice at a time: no count query, only whether there is a next slice (the load more page). */
  Slice<Person> findAllBy(Pageable pageable);

  /** How many people of each role joined in each month, for the charts page. */
  @Query("""
      select new io.github.wimdeblauwe.shadleaf.sample01.ChartsController$JoinCount(
        extract(year from p.joined), extract(month from p.joined), p.role, count(p))
      from Person p
      group by extract(year from p.joined), extract(month from p.joined), p.role""")
  List<ChartsController.JoinCount> countJoinedPerMonthAndRole();

  /** The orders of each role: the total and the average per person, for the charts page. */
  @Query("""
      select new io.github.wimdeblauwe.shadleaf.sample01.ChartsController$RoleOrders(p.role, sum(p.orders),
        avg(p.orders))
      from Person p
      group by p.role""")
  List<ChartsController.RoleOrders> ordersPerRole();
}
