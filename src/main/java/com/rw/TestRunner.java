package com.rw;

import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

public class TestRunner
{
    private Logger logger;
    private Sleeper sleeper;
    private ObservableGenerator obsGen;

    public TestRunner(
        Logger logger,
        Sleeper sleeper,
        ObservableGenerator obsGen)
    {
        this.logger = logger;
        this.sleeper = sleeper;
        this.obsGen = obsGen;
    }

    public void run()
    {
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
        sleeper.sleep(1, 1000);
        subscribe(obs, 1, false);
        sleeper.sleep(1, 5000);
        subscribe(obs, 2, false);
        sleeper.sleep(5, 3000);
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
            Observable<String> obs = obsGen.generateWithError()
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

    private void runSubscribe(AppInterface app, boolean isBlocking)
    {
        logger.log("Application Start");
        Observable obs = app.Run();
        subscribe(obs, isBlocking);
        if (!isBlocking) {
            sleeper.sleep(10, 3);
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
