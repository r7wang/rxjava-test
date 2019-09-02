package com.rw;

import io.grpc.Context;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestRunner
{
    private Logger logger;
    private ObservableGenerator obsGen;

    public TestRunner(
        Logger logger,
        ObservableGenerator obsGen)
    {
        this.logger = logger;
        this.obsGen = obsGen;
    }

    public void run()
    {
        testZip();
    }

    private void testZip()
    {
        runSubscribe(() ->
        {
            // Zip allows us to concurrently merge multiple observables together through an arbitrarily defined
            // function.
            //  - It will only do so if there are corresponding elements in both observables.
            Observable<Integer> obsA = obsGen.Generate(false, 1, 2, 5, 9)
               .subscribeOn(Schedulers.newThread());
            Observable<Integer> obsB = obsGen.Generate(false, 3, 6, 7)
               .subscribeOn(Schedulers.newThread());
            return Observable.zip(obsA, obsB, (a, b) -> a + b);
        }, true);
    }

    private void testConcatCompletableOrder()
    {
        // If we concatenate a lot of completables, in which order do they run?
        //  - When the entire subscription runs on a single thread, they are handled in order and an exception thrown
        //    in one of the stages prevent further stages from running.
        logger.log("Application Start");
        List<Completable> toComplete = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final int completableIdx = i;
            toComplete.add(Completable.fromCallable(() -> {
                logger.log(String.format("Completable %s", completableIdx));
                if (completableIdx % 5 == 0) {
                    sleep(1, 1000);
                }

                if (completableIdx == 8) {
                    throw new RuntimeException("Could not complete...");
                }
                return 0;
            }));
        }
        Completable comp = Completable.concat(toComplete)
            .onErrorComplete((e) -> {
                logger.log("OnErrorComplete");
                return true;
            });
        comp.subscribe();
        logger.log("Application End");
    }

    private void testRuntimeExceptionKeepEmitting()
    {
        // We want to avoid a situation where an exception causes the observable to shut down. To achieve this, the
        // following needs to happen:
        //  - The function that can potentially error must not throw the exception directly. It must return
        //    Observable.error(), or we must catch the exception somewhere and then call Observable.error().
        //  - We must follow the returned Observable directly with onErrorResumeNext() without returning back up to the
        //    parent observable.
        runSubscribe(() ->
        {
            Observable<Long> obs = Observable.interval(300, TimeUnit.MILLISECONDS)
                .flatMap((Long s) -> errorOnValue(s, 8L)
                    .onErrorResumeNext(Observable.just((long)Integer.MAX_VALUE)));
            return obs;
        }, true);
    }

    private void testRuntimeExceptionImpact()
    {
        // In the case below, it seems like any exceptions are swallowed and don't appear to impact the subscribing
        // thread.
        //  - If we have an onError handler, it will do what's in that handler.
        //  - If we don't have an onError handler, the exception will be printed to console, but nothing will happen.
        //  - In all cases, the last line is reachable.
        runSubscribe(() ->
        {
            Observable<Integer> obs = obsGen.Generate()
                .map((Integer s) -> {
                    if (true) {
                        throw new RuntimeException("Error 1");
                    }
                    return s + 1;
                })
                .map((Integer s) -> {
                    return s + 2;
                });
            return obs;
        }, true);
        logger.log("Is this code reachable?");
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
        subscribe(obs, 1, false);
        sleep(1, 5000);
        subscribe(obs, 2, false);
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
        runSubscribe(() ->
        {
            Observable<String> obs = obsGen.GenerateWithError()
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
        }, false);
    }

    private void testFireForget()
    {
        // Defines an approach to firing off a call to a microservice without needing to block on or wait for
        // the response. If the microservice call errors out, there's no impact on the main process.
        Context.current().fork().run(() -> {
            Observable obs = obsGen.GenerateWithError()
                .subscribeOn(Schedulers.newThread());
            subscribe(obs, true);
        });
        sleep(10, 500);
    }

    private void testScheduling()
    {
        // Uses a mix of subscribeOn and observeOn with print statements to better understand what threads are
        // running which parts of the system.
        //  - Try commenting out the subscribeOn or any of the observeOn.
        runSubscribe(() ->
        {
            Observable<String> obs = obsGen.Generate()
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .flatMap(this::expandNextValues)
                .observeOn(Schedulers.newThread())
                .map(this::intToString);
            return obs;
        }, false);
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
        subscribe(obs, true);
        try {
            Thread.sleep(1000);
        }
        catch (Exception ex) {
        }
        subscribe(obs, true);
    }

    private void testGroupBy()
    {
        // We can do the same thing with a regular map.
        Observable<String> obsGroupBy = obsGen.Generate()
            .groupBy(s -> s / 3, s -> s)  // Observable<GroupedObservable<Integer, Integer>>
            .flatMap(grp -> grp.map(s -> String.format("%s -> %s", grp.getKey(), s)));
        subscribe(obsGroupBy, true);

        // Aggregation shows a better use case of why we may want to use a groupBy.
        Observable<Integer> obsAgg = obsGen.Generate()
            .groupBy(s -> s / 3, s -> s)  // Observable<GroupedObservable<Integer, Integer>>
            .flatMap(grp -> grp.reduce(0, (accumulator, x) -> accumulator + x).toObservable());
        subscribe(obsAgg, true);
    }

    private void testMultipleSubscriber()
    {
        // Both subscribers see the same results.
        Observable<Integer> obs = Observable.fromArray(1, 2, 5, 9);
        subscribe(obs, 1, true);
        subscribe(obs, 2, true);
    }

    private void testEmission()
    {
        // Single emission.
        Single<String> singleSource = Single.just("single item");
        singleSource.subscribe(
            s -> logger.log("Item received: from singleSource " + s),
            Throwable::printStackTrace
        );

        // Maybe emits something.
        Maybe<String> maybeSource = Maybe.just("single item");
        maybeSource.subscribe(
            s -> logger.log("Item received: from maybeSource " + s),
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

    private void runSubscribe(AppInterface app, boolean isBlocking)
    {
        logger.log("Application Start");
        Observable obs = app.Run();
        subscribe(obs, isBlocking);
        if (!isBlocking) {
            sleep(10, 3);
        }
        logger.log("Application End");
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

    private <T> Observable<T> errorOnValue(T s, T comp) {
        if (s == comp) {
            return Observable.error(new RuntimeException("Unwanted value"));
        }
        return Observable.just(s);
    };

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

    private void subscribe(Observable obs, boolean isBlocking)
    {
        subscribe(obs, 1, isBlocking);
    }

    private <T> void subscribe(Observable<T> obs, int agentId, boolean isBlocking)
    {
        if (isBlocking) {
            obs.blockingSubscribe(
                s -> logger.log(String.format("Subscriber-%s: %s", agentId, s)),
                s -> logger.log(String.format("Subscriber-%s: Error", agentId)),
                () -> logger.log(String.format("Subscriber-%s: Complete", agentId)));
        }
        else {
            obs.subscribe(
                s -> logger.log(String.format("Subscriber-%s: %s", agentId, s)),
                s -> logger.log(String.format("Subscriber-%s: Error", agentId)),
                () -> logger.log(String.format("Subscriber-%s: Complete", agentId)));
        }
        logger.log("Subscribed");
    }
}
