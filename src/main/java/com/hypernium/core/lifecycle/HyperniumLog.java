package com.hypernium.core.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central logger. Plan section 65: only log initialization, configuration, subsystem state,
 * warnings and errors. Never call this from the render, tick, entity or chunk loops.
 */
public final class HyperniumLog {

  private static final Logger LOGGER = LoggerFactory.getLogger("Hypernium");

  private HyperniumLog() {}

  public static void info(String message, Object... args) {
    LOGGER.info(message, args);
  }

  public static void debug(String message, Object... args) {
    LOGGER.debug(message, args);
  }

  public static void warn(String message, Object... args) {
    LOGGER.warn(message, args);
  }

  public static void error(String message, Object... args) {
    LOGGER.error(message, args);
  }
}
