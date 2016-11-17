package eu.toolchain.examples;

import eu.toolchain.concurrent.Async;
import eu.toolchain.concurrent.Stage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;

public class DatabaseExample {
  public static final AtomicInteger commits = new AtomicInteger();
  public static final AtomicInteger rollbacks = new AtomicInteger();

  public static void main(String[] argv) throws Exception {
    final Async async = Helpers.setup();
    final Database database = new Database(async);

    final Stage<Void> w1 = database.beginTransaction().thenCompose(tx -> {
      return tx.write("a", 42).withCloser(tx::commit, tx::rollback);
    });

    final Stage<Void> w2 = database.beginTransaction().thenCompose(tx -> {
      final List<Stage<Void>> writes = new ArrayList<>();
      writes.add(tx.write("a", 14));
      writes.add(tx.write("b", -1));
      writes.add(tx.write("c", -2));
      writes.add(tx.write("d", -3));
      return async.collectAndDiscard(writes).withCloser(tx::commit, tx::rollback);
    });

    w1.join();

    try {
      w2.join();
    } catch (final Exception e) {
      System.out.println("Error: " + e.getMessage());
      e.printStackTrace(System.out);
    }

    System.out.println("Commits: " + commits.get());
    System.out.println("Rollbacks: " + rollbacks.get());
  }

  @RequiredArgsConstructor
  public static class Database {
    private final Async async;

    private final Map<String, Integer> store = new HashMap<>();

    public Stage<Transaction> beginTransaction() {
      return async.completed(new Transaction());
    }

    public class Transaction {
      final List<Runnable> writes = new ArrayList<>();

      public Stage<Void> write(final String id, final int value) {
        return async.call(() -> {
          if (value <= 0) {
            throw new RuntimeException("unlucky number " + value + "...");
          }

          writes.add(() -> {
            store.put(id, value);
          });

          return null;
        });
      }

      public Stage<Void> commit() {
        return async.<Void>call(() -> {
          synchronized (store) {
            writes.forEach(Runnable::run);
          }

          return null;
        }).whenComplete(v -> commits.incrementAndGet());
      }

      public Stage<Void> rollback() {
        return async.<Void>call(() -> {
          writes.clear();
          return null;
        }).whenComplete(v -> rollbacks.incrementAndGet());
      }
    }
  }
}
