package io.github.wimdeblauwe.shadleaf.component;

import org.thymeleaf.exceptions.TemplateProcessingException;

/**
 * Thrown when a component is used or declared incorrectly: an unknown component, an illegal prop value, a missing
 * accessible name, or an invalid {@code <sl:props>} block.
 * <p>
 * It extends {@link TemplateProcessingException} so that, when thrown while rendering, Thymeleaf adds the template
 * name and line to the message.
 */
public class ShadleafComponentException extends TemplateProcessingException {

  public ShadleafComponentException(String message) {
    super(message);
  }

  public ShadleafComponentException(String message, Throwable cause) {
    super(message, cause);
  }
}
