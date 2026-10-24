package io.github.wimdeblauwe.shadleaf.sample01;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * The upload form on a person's page: the files of its {@code sl:file-upload multiple}. A browser sends one empty part
 * for a file input without files, so the controller looks at the files that are not empty.
 */
public class UploadForm {

  private List<MultipartFile> files = new ArrayList<>();

  public List<MultipartFile> getFiles() {
    return files;
  }

  public void setFiles(List<MultipartFile> files) {
    this.files = files;
  }
}
