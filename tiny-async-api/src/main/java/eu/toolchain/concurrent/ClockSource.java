package eu.toolchain.concurrent;

public interface ClockSource {
  long now();

  ClockSource SYSTEM = System::currentTimeMillis;

  static ClockSource system() {
    return SYSTEM;
  }
}
