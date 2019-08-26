package com.rw;

import io.grpc.Context;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

public class TestRunner {
    private Logger logger;

    public TestRunner(Logger logger)
    {
        this.logger = logger;
    }

    public void run() {
        testHotObservable();
    }

    private void testHotObservable()
    {
        // One of the reasons to use this is when there are multiple subscribers and we want to avoid re-computing the
        // observable multiple times.
        //  - When we subscribe too late, the new subscribers don't see all of the previously emitted items.
        //  - If we subscribe after the observable has already completed, then the new subscriber's completion callback
        //    won't trigger.
        logger.log("Application Start");
        ConnectableObservable<Long> obs = Observable.interval(0, 300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .doOnNext(count -> {
                    logger.log(String.format("Emit %s", count));
                })
                .publish();
        logger.log("Connecting");
        obs.connect();
        logger.log("Connected");
        sleep(1, 1000);
        subscribe(obs, 1);
        sleep(1, 5000);
        subscribe(obs, 2);
        sleep(5, 3000);
        logger.log("Application End");
    }

    private void testDoOnError()
    {
        // This probably makes a lot more sense to use when we want to clean up after an error. The subscriber onError
        // may not have any knowledge on how to clean up.
        //
        // Not all flatMap and map operations will complete after the error occurs, but there doesn't seem to be a
        // specific guarantee on when execution will stop (best effort).
        //
        //  - We don't want to make any assumption on how many emits have been consumed, especially if there have been
        //    multiple threads involved. Any final state should ideally be handled by the subscriber unless we have a
        //    good way to store partial state.
        //  - The doOnError function will be called as long as main thread is still active.
        //  - It seems that ordering is followed for doOnError sequences.
        //  - It seems that the duration of doOnError execution impacts how much work is done by flatMap and map. If
        //    doOnError takes too long, then execution will occur for a longer period of time after the error occurs.
        runNonBlockingSubscribe(() ->
        {
            Observable<String> obs = baseObservable(true)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(Schedulers.newThread())
                    .doOnError(error -> {
                        logger.log(String.format("doOnError: %s", error.getMessage()));
                    })
                    .flatMap(this::expandNextValues)
                    .observeOn(Schedulers.newThread())
                    .doOnError(error -> {
                        // This should also get triggered if there is an error, but it should
                        // happen after the first one, and on a different thread.
                        logger.log(String.format("doOnError: %s", error.getMessage()));
                    })
                    .map(this::intToString);
            return obs;
        });
    }

    private void testFireForget(boolean shouldError)
    {
        // Defines an approach to firing off a call to a microservice without needing to block on or wait for
        // the response. If the microservice call errors out, there's no impact on the main process.
        Context.current().fork().run(() -> {
            Observable obs = baseObservable(shouldError)
                    .subscribeOn(Schedulers.newThread());
            subscribe(obs);
        });
        sleep(10, 500);
    }

    private void testScheduling()
    {
        // Uses a mix of subscribeOn and observeOn with print statements to better understand what threads are
        // running which parts of the system.
        //  - Try commenting out the subscribeOn or any of the observeOn.
        runNonBlockingSubscribe(() ->
        {
            Observable<String> obs = baseObservable(false)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(Schedulers.newThread())
                    .flatMap(this::expandNextValues)
                    .observeOn(Schedulers.newThread())
                    .map(this::intToString);
            return obs;
        });
    }

    private void testDefer()
    {
        // Compared to Observable.just, defer runs the function when a new subscription is received, which means that
        // both subscribers will see different times. Observable.fromCallable is similar but only emits a single
        // object.
        Observable<Long> obs = Observable.defer(() -> {
            long time = System.currentTimeMillis();
            return Observable.just(time);
        });
        // Observable<Long> obs = Observable.fromCallable(() -> {
        //    long time = System.currentTimeMillis();
        //    return time;
        // });
        // Observable<Long> obs = Observable.just(System.currentTimeMillis());
        subscribe(obs);
        try {
            Thread.sleep(1000);
        } catch (Exception ex) {}
        subscribe(obs);
    }

    private void testGroupBy()
    {
        // We can do the same thing with a regular map.
        Observable<String> obsGroupBy = baseObservable(false)
                .groupBy(s -> s/3, s -> s)  // Observable<GroupedObservable<Integer, Integer>>
                .flatMap(grp -> grp.map(s -> String.format("%s -> %s", grp.getKey(), s)));
        subscribe(obsGroupBy);

        // Aggregation shows a better use case of why we may want to use a groupBy.
        Observable<Integer> obsAgg = baseObservable(false)
                .groupBy(s -> s/3, s -> s)  // Observable<GroupedObservable<Integer, Integer>>
                .flatMap(grp -> grp.reduce(0, (accumulator, x) -> accumulator + x).toObservable());
        subscribe(obsAgg);
    }

    private void testMultipleSubscriber()
    {
        // Both subscribers see the same results.
        Observable<Integer> obs = Observable.fromArray(1, 2, 5, 9);
        subscribe(obs, 1);
        subscribe(obs, 2);
    }

    private void testEmission()
    {
        // Single emission.
        Single<String> singleSource = Single.just("single item");
        singleSource.subscribe(
                s -> logger.log("Item received: from singleSource " +  s),
                Throwable::printStackTrace
        );

        // Maybe emits something.
        Maybe<String> maybeSource = Maybe.just("single item");
        maybeSource.subscribe(
                s -> logger.log("Item received: from maybeSource " +  s),
                Throwable::printStackTrace,
                () -> logger.log("Done from MaybeSource")
        );

        // Maybe emits nothing.
        Maybe<Integer> emptySource = Maybe.empty();
        emptySource.subscribe(
                s -> logger.log("Item received: from emptySource" + s),
                Throwable::printStackTrace,
                () -> logger.log("Done from EmptySource")
        );
    }

    private void runNonBlockingSubscribe(AppInterface app)
    {
        logger.log("Application Start");
        Observable obs = app.Run();
        subscribe(obs);
        sleep(10, 3);
        logger.log("Application End");
    }

    private Observable<Integer> baseObservable(boolean shouldError)
    {
        return Observable
                .create((ObservableEmitter<Integer> obs) -> {
                    logger.log("Observable Before Emit");
                    int[] values = {1, 2, 5, 9};
                    for (int i : values) {
                        obs.onNext(i);
                        logger.log("Observable Emitted");
                    }

                    if (shouldError) {
                        logger.log("Observable Throwing Exception");
                        throw new RuntimeException("Something bad happened...");
                    }

                    obs.onComplete();
                    logger.log("Observable After Emit");
                });
    }

    private Observable<Integer> expandNextValues(Integer s)
    {
        logger.log(String.format("FlatMap: %s", s));
        return Observable.fromArray(s, s + 1, s + 2, s + 3, s + 4, s + 5);
    }

    private String intToString(Integer s)
    {
        logger.log(String.format("Map: %s", s));
        return s.toString() + "val";
    }

    private void sleep(int numCycles, int intervalMs)
    {
        for (int i = 0; i < numCycles; i++) {
            logger.log("Sleeping...");
            try {
                // Need a very short interval to see thread interleaving.
                Thread.sleep(intervalMs);
            }
            catch (InterruptedException ex) {
                logger.log("Sleep Interrupted");
                break;
            }
        }
    }

    private void subscribe(Observable obs)
    {
        subscribe(obs, 1);
    }

    private void subscribe(Observable obs, int agentId)
    {
        obs.subscribe(
                s -> logger.log(String.format("Subscriber-%s: %s", agentId, s)),
                s -> logger.log(String.format("Subscriber-%s: Error", agentId)),
                () -> logger.log(String.format("Subscriber-%s: Complete", agentId)));
        logger.log("Subscribed");
    }
}
