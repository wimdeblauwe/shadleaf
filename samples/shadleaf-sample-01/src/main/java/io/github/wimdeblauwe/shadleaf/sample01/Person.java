package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDate;

/** A row of the people page. */
@Entity
public class Person {

  public enum Role {
    OWNER, ADMIN, MEMBER, GUEST
  }

  @Id
  @GeneratedValue
  private Long id;
  private String name;
  private String email;
  @Enumerated(EnumType.STRING)
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
}
