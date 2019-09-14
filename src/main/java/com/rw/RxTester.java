package com.rw;

import io.reactivex.Observable;
import io.reactivex.Single;

public class RxTester {
    private Logger logger;

    public RxTester(Logger logger)
    {
        this.logger = logger;
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
}
