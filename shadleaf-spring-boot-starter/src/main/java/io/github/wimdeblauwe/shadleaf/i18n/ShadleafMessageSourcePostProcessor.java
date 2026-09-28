package io.github.wimdeblauwe.shadleaf.i18n;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * Puts the Shadleaf strings behind the application's own {@code messageSource} bean, whether Spring Boot configured it
 * or the application declared it, without replacing it.
 * <p>
 * An application without a {@code messageSource} bean gets a {@link ShadleafMessageSource} as its message source
 * instead, from the auto-configuration.
 */
public class ShadleafMessageSourcePostProcessor implements BeanPostProcessor {

  private static final Log logger = LogFactory.getLog(ShadleafMessageSourcePostProcessor.class);

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME.equals(beanName) && bean instanceof MessageSource source
        && !ShadleafMessageSource.attachTo(source)) {
      logger.warn("The messageSource bean (" + bean.getClass().getName() + ") is not a HierarchicalMessageSource, "
          + "so the Shadleaf strings (sl.* keys) cannot be added behind it. Define them in the application's own "
          + "messages, or add " + ShadleafMessageSource.BASENAME + " to its basenames.");
    }
    return bean;
  }
}
