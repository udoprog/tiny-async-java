# TinyAsync

A small asynchronous library for Java.

* Only depend on Java 7.
* Do one thing, and do it well.
* Throw checked exceptions in transformers, and collectors.
* Give the user control over how to handle internal functionality
  through AsyncCaller.
* Separation of implementation and interface through an api package suitable
  for inclusion in your public APIs.

All components of the public API aims to be fully thread safe.

For an overview of the library, check out the
[API](tiny-async-api/src/main/java/eu/toolchain/async) and the [Usage](#usage)
section below.

Props to Guava and the ListenableFuture, which has acted as inspiration
for many improvements to ConcurrentFuture.

# Setup

Add tiny-async-core as a dependency to your project, and tiny-async-api as
a dependency to your public API.

```
<dependency>
  <groupId>eu.toolchain.async</groupId>
  <artifactId>tiny-async</artifactId>
  <version>1.0.0</version>
</dependency>
```

After that, the first step is to instantiate the framework.

See [AsyncSetup.java](tiny-async-core/src/example/java/eu/toolchain/examples/AsyncSetup.java)
for an example of how to do this.

# Usage

The following section contains documentation on how to use TinyAsync.

## Subscribing to events

The following methods allow you to subscribe to interesting events on the
futures.

* ```Future<T> AsyncFuture#on(FutureDone<T>)```
* ```Future<T> AsyncFuture#on(FutureFinished)```
* ```Future<T> AsyncFuture#on(FutureCancelled)```
* ```Future<T> AsyncFuture#onAny(FutureDone<Object>)```

If the event handlers throw an exception, this is intepreted as an 'internal'
error, and will be reported as such in the provided ```AsyncCaller```.

There is no other reasonable way to handle this circumstance, and you are
expected to avoid throwing exceptions here (or implement a sane AsyncCaller
handle).

See examples:

* [Subscribe example](tiny-async-core/src/example/java/eu/toolchain/examples/AsyncSubscribeExample.java)

## Blocking until a result is available

This is the implemented behaviour of ```java.util.concurrent.Future#get()```.

Blocking isn't a terribly interesting async behaviour, and frankly it is beyond
me why ```java.util.concurrent.Future``` is so poorly designed.

You should mostly rely on [Subscribing to events](#subscribing-to-events),
transformers and collectors.

Note: most of these examples make use of ```#get()```, mainly because it is
convenient in this contrived context.

See examples:

* [Blocking example](tiny-async-core/src/example/java/eu/toolchain/examples/AsyncBlockingExample.java)

## Using a java.util.concurrent.Callable

* ```AsyncFuture<T> TinyAsync#call(Callable<T>)```
* ```AsyncFuture<T> TinyAsync#call(Callable<T>, ExecutorService)```
* ```AsyncFuture<T> TinyAsync#call(Callable<T>, ExecutorService, ResolvableFuture)```

See examples:

* [blocking example](tiny-async-core/src/example/java/eu/toolchain/examples/AsyncBlockingExample.java)

This only works if you've configured a default ExecutorService, otherwise
you will have to provide it as a second argument to
```async.call(..., ExecutorService)```.

## Providing static errors or results

These methods will return a future that already has a resolved, failed
state, or cancelled state.

* ```AsyncFuture<T> TinyAsync#resolved(T result)```
* ```AsyncFuture<T> TinyAsync#failed(Throwable cause)```
* ```AsyncFuture<T> TinyAsync#cancelled()```

This is useful when implementing methods that return futures, but you are
unable to provide the target future, like when catching and handling an
exception.

See examples:

* [Static results example](tiny-async-core/src/example/java/eu/toolchain/examples/AsyncStaticResultsExample.java)

## Transforming results

Transformations is a lightweight way of forward potential results or failures.

They allow you to take a value A, and convert it to a value B.

They also allows to take a falied future, and convert it into a value B.

* ```AsyncFuture<C> AsyncFuture#transform(Transform<T, C>)```
* ```AsyncFuture<C> AsyncFuture#transform(LazyTransform<T, C>)```
* ```AsyncFuture<C> AsyncFuture#error(Transform<Throwable, C>)```
* ```AsyncFuture<C> AsyncFuture#error(LazyTransform<Throwable, C>)```

See examples:

* [Transform example](tiny-async-core/src/example/java/eu/toolchain/examples/AsyncTransformExample.java)

## Collecting Many Results

When you have a collection of asynchronous operations that needs to be
'collected', the process is called collecting.

* ```AsyncFuture<Collection<T>> TinyAsync#collect(Collection<AsyncFuture<C>>)```
* ```AsyncFuture<Void> TinyAsync#collectAndIgnore(Collection<AsyncFuture<C>>)```
* ```AsyncFuture<T> TinyAsync#collect(Collection<AsyncFuture<C>>, Collector<C, T>)```
* ```AsyncFuture<T> TinyAsync#collect(Collection<AsyncFuture<C>>, StreamCollector<C, T>)```

The first type ```Collector``` collects the result from all the futures and
executes the reduction.
This has the benefit of not requiring to synchronize.

The second type ```StreamCollector``` is called with the results as they
resolve or fail.
This has the benefit of using less memory overall, since the framework does not
have to maintain all the seen reults so far, but requires synchronization from
the user.

See examples:

* [Collector example](tiny-async-core/src/example/java/eu/toolchain/examples/AsyncCollectorExample.java)
* [Stream collector example](tiny-async-core/src/example/java/eu/toolchain/examples/AsyncStreamCollectorExample.java)

## Manually resolving a Future

Sometimes it is necessary to manually resolve a future at specific points.

For this purpose, most the futures which allows this implements the
```ResolvableFuture#resolve(T result)``` method.

It is recommended that a ResolvableFuture is casted down to an AsyncFuture in
all public APIs.

See examples:

* [Manually resolving example](tiny-async-core/src/example/java/eu/toolchain/examples/AsyncManuallyResolvingExample.java)
