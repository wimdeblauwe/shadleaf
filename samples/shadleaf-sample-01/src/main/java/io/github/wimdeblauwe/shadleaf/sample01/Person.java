package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.util.Locale;

/** A row of the people page. */
@Entity
public class Person {

  /**
   * A person's role, ranked from the most rights to the fewest. The column holds the rank ({@link RoleConverter}), so
   * sorting by role sorts by rank (Owner, Admin, Member, Guest), not alphabetically as a string column would.
   */
  public enum Role {
    OWNER(1), ADMIN(2), MEMBER(3), GUEST(4);

    private final int rank;

    Role(int rank) {
      this.rank = rank;
    }

    public int getRank() {
      return rank;
    }

    /** The role as the page shows it: Owner, Admin, Member, Guest. */
    public String getLabel() {
      return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }

    static Role ofRank(int rank) {
      for (Role role : values()) {
        if (role.rank == rank) {
          return role;
        }
      }
      throw new IllegalArgumentException("No role has rank " + rank);
    }
  }

  @Id
  @GeneratedValue
  private Long id;
  private String name;
  private String email;
  @Convert(converter = RoleConverter.class)
  private Role role;
  private int orders;
  private LocalDate joined;

  protected Person() {
  }

  public Person(String name, String email, Role role, int orders, LocalDate joined) {
    this.name = name;
    this.email = email;
    this.role = role;
    this.orders = orders;
    this.joined = joined;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public Role getRole() {
    return role;
  }

  public int getOrders() {
    return orders;
  }

  public LocalDate getJoined() {
    return joined;
  }

  public void changeRole(Role role) {
    this.role = role;
  }
}
