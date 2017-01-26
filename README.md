# TinyAsync

[![Build Status][build-badge]][build]
[![Coverage Status][coveralls-badge]][coveralls]
[![Maven Central][maven-central-badge]][maven-central]

A tiny asynchronous library for Java.

Writing multithreaded code is hard, TinyAsync tries to make it easier by providing simple
abstractions for executing and manipulating computations through a clean API abstraction.

* [Why Asynchronous?](docs/why-async.md)
* [API Docs](https://udoprog.github.io/tiny-async-java/apidocs/latest/)

[build]: https://travis-ci.org/udoprog/tiny-async-java
[build-badge]: https://travis-ci.org/udoprog/tiny-async-java.svg?branch=master
[coveralls]: https://coveralls.io/r/udoprog/tiny-async-java?branch=master
[coveralls-badge]: https://coveralls.io/repos/udoprog/tiny-async-java/badge.svg?branch=master
[maven-central]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22eu.toolchain.async2%22
[maven-central-badge]: https://maven-badges.herokuapp.com/maven-central/eu.toolchain.async2/tiny-async-api/badge.svg

# Why TinyAsync?

Everything is tucked behind [a set of API interfaces][api], and some functionality has been moved
into the stage itself to allow for a clean, chained programming style (especially with java 8).

Since all interaction with the framework happens behind plain old java interfaces, the using
component rarely needs to maintain a direct dependency to **tiny-async-core**. The less components
that specify a direct dependency to a potentially messy and changing implementation - the better.
See [API Separation](#api-separation) for more details on how this will be maintained long term.

This has the added benefit of making TinyAsync superb for [testing](#testing-with-tinyasync).
Your components don't even have to know about concurrency, all you need to do is mock the expected
framework behavior.

For an overview of the library, check out the [API][api] and the [Usage](#usage) section below.

[api]: /tiny-async-api/src/main/java/eu/toolchain/concurrent

# Setup

TinyAsync is available [through maven][maven].

```xml
<dependency>
  <groupId>eu.toolchain.async2</groupId>
  <artifactId>tiny-async-api</artifactId>
  <version>${tiny-async.version}</version>
</dependency>
```

If you have an API project, you can add a dependency to **tiny-async-api**, which only contains the
interfaces used by TinyAsync. This provides a basic level of indirection and is all you need to
interact with the library.

If you have an application or framework that intends to provide an implementation of TinyAsync, you
should depend on **tiny-async-core**.
This contains the implementation of TinyAsync.

See [API Separation](#api-separation) for why the API is distributed in a separate package.

After that, the first step is to create a new instance of the framework.

```java
final Async async = CoreAsync.builder().build();
```

The builder [has a few options][builder] that you can use to customize behavior.

For a more detailed example, see [Helpers.java][helpers].

[maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22eu.toolchain.async%2
[builder]: https://udoprog.github.io/tiny-async-java/apidocs/latest/eu/toolchain/concurrent/CoreAsync.Builder.html
[helpers]: /tiny-async-examples/src/main/java/eu/toolchain/examples/helpers/Helpers.java

# API Separation

The separation between the API and core is done to reduce issues with drifts in dependencies.

A specific version of the API is always intended to be signature compatible with future versions.
If you build a project against version `1.1.0` of the API, it will be working with all _future_
`1.x.x` versions of core.

Deprecated components will be be marked with `@Deprecated` and removed in the next major version,
packages will also be renamed to avoid future classpath conflicts.

# Testing with TinyAsync

TinyAsync heavily favor isolated unit tests that mock as much as possible,
see some of the [core test suite][core-tests] if you want some
inspiration.

[core-tests]: /tiny-async-core/src/test/java/eu/toolchain/concurrent/

# Usage

You can see how the library is used by looking at the provided examples:

* [Building stages from scratch][stages-from-scratch]
* [Immediate results][immediate-results]
* [Blocking][blocking]
* [Completing in different thread)][something-reckless]
* [Listen for changes][listen]
* [Transform the result][transform]
* [Collection][collect], and [stream collections][stream-collect], and
  [eventual collection][eventual-collect] of results.
* [Retry Operations][retry-it] ([API][retry-apidocs])
* [Managed Resources][managed-it] ([API][managed-apidocs])

[stages-from-scratch]: /tiny-async-examples/src/main/java/eu/toolchain/examples/FromScratch.java
[blocking]: /tiny-async-examples/src/main/java/eu/toolchain/examples/Blocking.java
[immediate-results]: /tiny-async-examples/src/main/java/eu/toolchain/examples/ImmediateResults.java
[something-reckless]: /tiny-async-examples/src/main/java/eu/toolchain/examples/SomethingReckless.java
[listen]: /tiny-async-examples/src/main/java/eu/toolchain/examples/Listen.java
[transform]: /tiny-async-examples/src/main/java/eu/toolchain/examples/Transform.java
[collect]: /tiny-async-examples/src/main/java/eu/toolchain/examples/Collect.java
[stream-collect]: /tiny-async-examples/src/main/java/eu/toolchain/examples/StreamCollect.java
[eventually-collect]: /tiny-async-examples/src/main/java/eu/toolchain/examples/EventuallyCollect.java
[retry-it]: /tiny-async-core/src/test/java/eu/toolchain/concurrent/RetryUntilResolvedIT.java
[retry-apidocs]: https://udoprog.github.io/tiny-async-java/apidocs/latest/eu/toolchain/concurrent/Async.html#retryUntilCompleted-java.util.concurrent.Callable-eu.toolchain.concurrent.RetryPolicy-
[managed-it]: /tiny-async-core/src/test/java/eu/toolchain/concurrent/ManagedIT.java
[managed-apidocs]: https://udoprog.github.io/tiny-async-java/apidocs/latest/eu/toolchain/concurrent/Managed.html

## Other Async Libraries

* [Google Guava (`com.google.common.util.concurrent.ListenableFuture`)](https://github.com/google/guava)
* [RxJava](https://github.com/ReactiveX/RxJava)
* [JDeferred](http://jdeferred.org/)
