package eu.toolchain.concurrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TinyStackUtils {
  private static final String STACK_LINE_FORMAT = "%s.%s (%s:%d)";

  public static String formatStack(StackTraceElement[] stack) {
    if (stack == null || stack.length == 0) {
      return "unknown";
    }

    final List<String> entries = new ArrayList<>(stack.length);

    for (final StackTraceElement e : stack) {
      entries.add(
          String.format(STACK_LINE_FORMAT, e.getClassName(), e.getMethodName(), e.getFileName(),
              e.getLineNumber()));
    }

    final Iterator<String> it = entries.iterator();

    final StringBuilder builder = new StringBuilder();

    while (it.hasNext()) {
      builder.append(it.next());

      if (it.hasNext()) {
        builder.append("\n  ");
      }
    }

    return builder.toString();
  }
}
