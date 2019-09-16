package com.rw;

import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

public class TestRunner
{
    private Logger logger;
    private Sleeper sleeper;

    public TestRunner(
        Logger logger,
        Sleeper sleeper)
    {
        this.logger = logger;
        this.sleeper = sleeper;
    }

    public void run()
    {
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
        sleeper.sleep(1, 1000);
        subscribe(obs, 1, false);
        sleeper.sleep(1, 5000);
        subscribe(obs, 2, false);
        sleeper.sleep(5, 3000);
        logger.log("Application End");
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
