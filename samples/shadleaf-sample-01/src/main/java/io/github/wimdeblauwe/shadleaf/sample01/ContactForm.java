package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** The form backing object of the form page. JavaBeans accessors, as Spring's data binding and th:field expect. */
public class ContactForm {

  @NotBlank
  private String name;

  @NotBlank
  @Email
  private String email;

  @NotBlank
  private String topic;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate callbackDate;

  @Size(min = 10, max = 500)
  private String message;

  @NotBlank
  private String replyBy;

  private boolean newsletter;

  @AssertTrue(message = "must be accepted")
  private boolean terms;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public LocalDate getCallbackDate() {
    return callbackDate;
  }

  public void setCallbackDate(LocalDate callbackDate) {
    this.callbackDate = callbackDate;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getReplyBy() {
    return replyBy;
  }

  public void setReplyBy(String replyBy) {
    this.replyBy = replyBy;
  }

  public boolean isNewsletter() {
    return newsletter;
  }

  public void setNewsletter(boolean newsletter) {
    this.newsletter = newsletter;
  }

  public boolean isTerms() {
    return terms;
  }

  public void setTerms(boolean terms) {
    this.terms = terms;
  }
}
