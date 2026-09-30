package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** The form in the dialog that edits a {@link Member}. */
public class MemberForm {

  @NotBlank
  private String name;

  @NotBlank
  @Email
  private String email;

  public static MemberForm of(Member member) {
    MemberForm form = new MemberForm();
    form.setName(member.name());
    form.setEmail(member.email());
    return form;
  }

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
}
