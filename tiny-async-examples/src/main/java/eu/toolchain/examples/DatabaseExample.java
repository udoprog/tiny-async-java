package eu.toolchain.examples;

import eu.toolchain.concurrent.ApplyHandle;
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
      return tx.write("a", 42).composeHandle(tx);
    });

    final Stage<Void> w2 = database.beginTransaction().thenCompose(tx -> {
      final List<Stage<Void>> writes = new ArrayList<>();

      try {
        writes.add(tx.write("a", 14));
        writes.add(tx.write("b", 13));
      } catch (final Exception e) {
        return tx.rollback().thenFail(e);
      }

      return async.collectAndDiscard(writes).composeHandle(tx);
    });

    w1.join();

    try {
      w2.join();
    } catch(final Exception e) {
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

    public class Transaction implements ApplyHandle<Void, Stage<Void>> {
      final List<Runnable> writes = new ArrayList<>();

      public Stage<Void> write(final String id, final int value) {
        return async.call(() -> {
          if (value == 13) {
            throw new RuntimeException("unlucky number...");
          }

          writes.add(() -> {
            store.put(id, value);
          });

          return null;
        });
      }

      public Stage<Void> commit() {
        commits.incrementAndGet();

        return async.call(() -> {
          synchronized (store) {
            writes.forEach(Runnable::run);
          }

          return null;
        });
      }

      public Stage<Void> rollback() {
        rollbacks.incrementAndGet();

        return async.call(() -> {
          writes.clear();
          return null;
        });
      }

      @Override
      public Stage<Void> completed(final Void result) {
        return commit();
      }

      @Override
      public Stage<Void> failed(final Throwable cause) {
        // rollback on failure, but still propagate the error
        return rollback().thenFail(cause);
      }

      @Override
      public Stage<Void> cancelled() {
        // rollback on cancelled, but still propagate the cancelled
        return rollback().thenCancel();
      }
    }
  }
}
