package io.github.wimdeblauwe.shadleaf.dev;

import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.metadata.ComponentMetadata;
import io.github.wimdeblauwe.shadleaf.metadata.WebTypes;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.context.SmartLifecycle;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes the web-types file for IntelliJ IDEA and WebStorm ({@code shadleaf.dev.web-types-file}) from the running
 * application's {@link ComponentRegistry}, so completion knows exactly the components this application renders: an
 * overridden template with changed props, and the application's own {@code <sl:*>} components, included.
 * <p>
 * The file is written when the application starts. With {@code shadleaf.dev.templates-path} set, templates change
 * without a restart, so it is checked again every {@code recheckInterval}. An unchanged file is never touched.
 */
public class WebTypesFileWriter implements SmartLifecycle {

  private static final Log logger = LogFactory.getLog(WebTypesFileWriter.class);

  private final ComponentRegistry registry;
  private final JsonMapper jsonMapper;
  private final Path file;
  private final @Nullable Duration recheckInterval;
  private @Nullable ScheduledExecutorService executor;
  private volatile boolean running;

  /**
   * @param recheckInterval how often to regenerate the file while running; {@code null} to write it once
   */
  public WebTypesFileWriter(ComponentRegistry registry, JsonMapper jsonMapper, Path file,
      @Nullable Duration recheckInterval) {
    this.registry = registry;
    this.jsonMapper = jsonMapper;
    this.file = file;
    this.recheckInterval = recheckInterval;
  }

  @Override
  public void start() {
    write();
    if (recheckInterval != null) {
      ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "shadleaf-web-types");
        thread.setDaemon(true);
        return thread;
      });
      long millis = recheckInterval.toMillis();
      scheduler.scheduleWithFixedDelay(this::writeQuietly, millis, millis, TimeUnit.MILLISECONDS);
      executor = scheduler;
    }
    running = true;
  }

  @Override
  public void stop() {
    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }
    running = false;
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  /** Writes the file now; {@code true} when its content changed. */
  public boolean write() {
    boolean written = WebTypes.write(ComponentMetadata.of(registry, ComponentMetadata.libraryVersion()), jsonMapper,
        file);
    if (written) {
      logger.info("Wrote web-types for " + registry.names().size() + " Shadleaf components to "
          + file.toAbsolutePath());
    }
    return written;
  }

  private void writeQuietly() {
    try {
      write();
    } catch (RuntimeException e) {
      // A template being edited can be briefly invalid; the next check picks up the fixed version.
      logger.debug("Could not regenerate " + file, e);
    }
  }
}
