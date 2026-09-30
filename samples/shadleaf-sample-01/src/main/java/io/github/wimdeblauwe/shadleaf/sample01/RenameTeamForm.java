package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The form in the popover that renames the team. {@code teamName}, not {@code name}: the invite form on the same page
 * has a {@code name} field, and the ids must differ.
 */
public class RenameTeamForm {

  @NotBlank
  @Size(max = 40)
  private String teamName;

  public static RenameTeamForm of(String teamName) {
    RenameTeamForm form = new RenameTeamForm();
    form.setTeamName(teamName);
    return form;
  }

  public String getTeamName() {
    return teamName;
  }

  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }
}
