# Why Asynchronous?

This section tries to answer the question; _why consider asynchronous
programming_?

_Note:_ If you spot anything you disagree with, please open an Issue and discuss.

## APIs and Contracts Matter

A component defines its contract through which public methods it expose.
Breaking this contract typically means large re-factoring of other code, or to
put it, any consumer of the API.

Asynchronous programming provides a given pattern that forces the consumer to
take into account that operations might block (more on why this matter in later
sections).

The decision on whether to be synchronous or not is then left to the
implementation of the API. The point is that the API does not have to change.

### A synchronous API

Consider the following two, completely made up interfaces.

```java
public interface MusicServiceAPI {
    public List<Album> getAlbumsForArtist(Artist artist, int limit);
    public List<Song> getSongsForAlbum(Album album);
}
```

```java
public class SyncConsumerOfAPI {
    private MusicServiceAPI api;

    public List<Song> findSongForTop5Albums(Artist artist) throws Exception {
        final List<Song> songs = new ArrayList<>();

        for (final Album album : api.getAlbumsForArtist(artist, 5))
            songs.addAll(api.getSongsForAlbum(album));

        return songs.
    }
}
```

At some point you will realize that the your application is using up _a lot_ of
threads waiting for albums, or for songs.
In effect, every consumer of _the synchronous API_ has to have a properly
(or overly) sized thread pool to cope with the fact that requests will block.

At some point you start realizing that `MusicServiceAPI#getSongsForAlbum()`
request is taking 500ms to complete, so in order to serve 1000 concurrent
requests/s you have to increase the size of your thread pools to be (or grow to)
at least 500. The size of your thread pools _have to grow_ with the number of
requests that you intend to handle.

You might also be slightly irked that the requests for fetching the songs for
the top 5 albums is happening in sequence. So lets add another thread pool to
handle _that_ as well. And since we are moving the computations to another
thread, we need to deal with concurrency.

After a few iterations of trying to size the number of threads you could decide
to just increase the number of threads to a ridiculous value so that you _never_
have to deal with this pain again.

All of a sudden your applications becomes slower, you're starting to encounter
the symptoms of thread starvation - some of your threads rarely seem to run,
or the inherent cost of context switching - your overall throughput suffers.

The exact scenario will differ, your OS will most likely do its best to try
and slice up the available time to maximize throughput.

### An Asynchronous API

```java
public interface MusicServiceAPI {
    public AsyncFuture<List<Album>> getAlbumsForArtist(Artist artist, int limit);
    public AsyncFuture<List<Song>> getSongsForAlbum(Album album);
}
```

```java
public class AsyncConsumerOfAPI {
    private MusicServiceAPI api;

    public AsyncFuture<List<Song>> findSongForTop5Albums(Artist artist) {
        return api.getAlbumsForArtist(artist, 5).lazyTransform(albums -> {
            final List<AsyncFuture<List<Song>>> songs = new ArrayList<>();

            for (final Album album : albums)
                songs.add(api.findSongsForAlbum(album));

            return async.collect(songs).transform(albumSongs -> {
                final List<Song> allSongs = new ArrayList<>();

                for (final List<Song> songs : albumSongs)
                    allSongs.addAll(songs);

                return allSongs;
            })
        })
    }
}
```

So apart from looking strange, what is the difference?

We react to the result of `getAlbumsForArtist(Artist, int)` as it becomes
available, there is no thread waiting for the result.

We react to the result of a group of requests, and better yet they could
potentially be parallelized, that is up to the `MusicServiceAPI` to decide.
Again, we react when all the results become available by concatenating them.

In the above paragraphs, _Reacting_ typically means handing a task to an already
active thread on a pool. If the async pattern is employed they rarely block, and
that makes the job of sizing the pool easier. For one you can for most cases
match the number of available cpus.

## Tricky Thread Scheduling

Reference: [WhyAsync.java, TrickyThreadScheduling Example](../tiny-async-examples/src/main/java/eu/toolchain/examples/WhyAsync.java)

This example is intended to showcase how two fairly innocuous mistakes; **a)**
a too small thread pool, and **b)** blocking some of your live threads, can be
detrimental to throughput. Under the correct circumstances they could cause
deadlocks. E.g. during slow, or numerous requests.

The example is intended to showcase this isolated scenario, for which similar
ones could pop up in a non-trivial application that tries to deal with
concurrency.

The following image showcases how async futures utilizes all available threads,
even though the programming pattern between the two solutions are not
significantly different.

![Tricky Thread Scheduling](images/whyasync-tricky-thread-scheduling.png)
