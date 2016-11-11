package eu.toolchain.examples;

import eu.toolchain.concurrent.CompletionHandle;
import eu.toolchain.concurrent.CompletionStage;
import eu.toolchain.concurrent.TinyFuture;
import java.util.concurrent.ExecutionException;

/**
 * An example application that showcases subscription of events on an {@code CompletionStage}.
 */
public class AsyncSubscribeExample {
  public static void main(String argv[]) throws InterruptedException, ExecutionException {
    TinyFuture async = FutureSetup.setup();

    final CompletionStage<Integer> f = async.call(() -> {
      Thread.sleep(1000);
      return 10;
    });

    f.handle(new CompletionHandle<Integer>() {
      @Override
      public void resolved(Integer result) throws Exception {
        System.out.println("result: " + result);
      }

      // uh-oh. Something went wrong.
      @Override
      public void failed(Throwable e) throws Exception {
        System.out.println("error: " + e);
      }

      @Override
      public void cancelled() throws Exception {
        System.out.println("cancelled");
      }
    });

    System.out.println("result: " + f.join());
    System.out.println("ok, bye!");
  }
}
