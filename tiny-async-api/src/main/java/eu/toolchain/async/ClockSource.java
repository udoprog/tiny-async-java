package eu.toolchain.async;

public interface ClockSource {
    long now();

    ClockSource SYSTEM = System::currentTimeMillis;

    static ClockSource system() {
        return SYSTEM;
    }
}
