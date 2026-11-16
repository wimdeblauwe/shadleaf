package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The rules of a room booking that look at more than one field, or at the bookings: each error goes on the field that
 * shows it, which on the page declares the fields it depends on ({@code depends-on}). See
 * {@link RoomBookingValidator}.
 */
@Documented
@Constraint(validatedBy = RoomBookingValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRoomBooking {

  String message() default "This booking is not valid.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
