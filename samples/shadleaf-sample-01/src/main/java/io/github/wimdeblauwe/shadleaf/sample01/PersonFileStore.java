package io.github.wimdeblauwe.shadleaf.sample01;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * The files of each person: the ones made up from their details ({@link PersonFile#of(Person)}) and the ones uploaded
 * on their page, kept in memory, as the people are. An upload with the name of a file already there replaces it.
 */
@Component
public class PersonFileStore {

  private final Map<Long, List<PersonFile>> uploads = new ConcurrentHashMap<>();

  /** The person's files, the made-up ones first, then the uploads in the order they came. */
  public List<PersonFile> of(Person person) {
    List<PersonFile> files = new ArrayList<>(PersonFile.of(person));
    for (PersonFile upload : uploads.getOrDefault(person.getId(), List.of())) {
      files.removeIf(file -> file.name().equals(upload.name()));
      files.add(upload);
    }
    return files;
  }

  public void add(Person person, List<PersonFile> files) {
    uploads.compute(person.getId(), (id, current) -> {
      List<PersonFile> updated = new ArrayList<>(current == null ? List.of() : current);
      for (PersonFile file : files) {
        updated.removeIf(other -> other.name().equals(file.name()));
        updated.add(file);
      }
      return List.copyOf(updated);
    });
  }
}
