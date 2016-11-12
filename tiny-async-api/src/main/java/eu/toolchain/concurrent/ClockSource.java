package eu.toolchain.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * A clock source that gives the current time in milliseconds.
 */
public interface ClockSource {
  TimeUnit UNIT = TimeUnit.MILLISECONDS;

  /**
   * Get the current time in milliseconds.
   *
   * @return a timestamp
   */
  long now();

  ClockSource SYSTEM = System::currentTimeMillis;

  static ClockSource system() {
    return SYSTEM;
  }
}
