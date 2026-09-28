package io.github.wimdeblauwe.shadleaf.component;

/**
 * The type of a declared prop, which decides how its attribute value is coerced.
 */
public enum PropType {
  /** Any text. The default when a prop declares neither {@code type} nor {@code values}. */
  STRING,
  /** {@code true} or {@code false}; a bare attribute ({@code <sl:button disabled>}) means {@code true}. */
  BOOLEAN,
  /** A decimal number, coerced to {@link java.math.BigDecimal}. */
  NUMBER,
  /** One of a fixed set of values, declared with {@code values="a b c"}. */
  ENUM
}
