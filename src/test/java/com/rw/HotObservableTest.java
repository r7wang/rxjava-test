package com.rw;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class HotObservableTest {

    private Logger logger;
    private RxTester rxTester;

    @Before
    public void setup()
    {
        logger = new Logger();
        rxTester = new RxTester(logger);
        logger.log("Application Start");
    }

    @After
    public void teardown()
    {
        logger.log("Application End");
    }

    @Test
    public void testHotObservable()
    {
        // One of the reasons to use this is when there are multiple subscribers and we want to avoid re-computing the
        // observable multiple times.
        //  - When we subscribe too late, the new subscribers don't see all of the previously emitted items.
        //  - If we subscribe after the observable has already completed, then the new subscriber's completion callback
        //    won't trigger.
        ConnectableObservable<Long> obs = Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .doOnNext(count -> {
                    logger.log(String.format("Emit %s", count));
                })
                .publish();
        logger.log("Connecting");
        Disposable disposable = obs.connect();
        logger.log("Connected");
        rxTester.subscribeAndWait(obs, 1, 1000);
        rxTester.subscribeAndWait(obs, 2, 2000);
        disposable.dispose();
    }
}
