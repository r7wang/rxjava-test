package com.rw;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

public class RxTester {
    private Logger logger;

    public RxTester(Logger logger)
    {
        this.logger = logger;
    }

    public void subscribe(Completable source)
    {
        subscribe(source, 1);
    }

    public void subscribe(Completable source, int subscriberId)
    {
        source.subscribe(
            () -> logger.log(String.format("Subscriber-%s: Complete", subscriberId)),
            s -> logger.log(String.format("Subscriber-%s: Error", subscriberId)));
    }

    public <T> void subscribe(Single<T> source)
    {
        subscribe(source, 1);
    }

    public <T> void subscribe(Single<T> source, int subscriberId)
    {
        T result = source.blockingGet();
        logger.log(String.format("Subscriber-%s: %s", subscriberId, result));
    }

    public <T> void subscribe(Observable<T> source)
    {
        subscribe(source, 1);
    }

    public <T> void subscribe(Observable<T> source, int subscriberId)
    {
        source.blockingSubscribe(
            s -> logger.log(String.format("Subscriber-%s: %s", subscriberId, s)),
            s -> logger.log(String.format("Subscriber-%s: Error", subscriberId)),
            () -> logger.log(String.format("Subscriber-%s: Complete", subscriberId)));
    }

    public <T> void subscribeAndWait(Observable<T> source, long waitMillis)
    {
        subscribeAndWait(source, 1, waitMillis);
    }

    public <T> void subscribeAndWait(Observable<T> source, int subscriberId, long waitMillis)
    {
        Disposable disposable = source.subscribe(
            s -> logger.log(String.format("Subscriber-%s: %s", subscriberId, s)),
            s -> logger.log(String.format("Subscriber-%s: Error", subscriberId)),
            () -> logger.log(String.format("Subscriber-%s: Complete", subscriberId)));
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException ex) {}
        disposable.dispose();
    }
}
