package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.shadleaf.sample01.Person.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores a role as its rank rather than its name, so the database sorts roles by rank. An explicit rank, unlike
 * {@code EnumType.ORDINAL}, survives reordering the enum's constants.
 */
@Converter
class RoleConverter implements AttributeConverter<Role, Integer> {

  @Override
  public Integer convertToDatabaseColumn(Role role) {
    return role == null ? null : role.getRank();
  }

  @Override
  public Role convertToEntityAttribute(Integer rank) {
    return rank == null ? null : Role.ofRank(rank);
  }
}
