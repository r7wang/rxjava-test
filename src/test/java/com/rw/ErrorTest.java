package com.rw;

import io.reactivex.Single;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ErrorTest {

    private Logger logger;
    private ObservableGenerator obsGen;
    private int agentId = 1;

    // Keeps track of the current emission index from the source observable.
    private int curInstance;

    @Before
    public void setup()
    {
        logger = new Logger();
        obsGen = new ObservableGenerator(logger);
        curInstance = 0;
        logger.log("Application Start");
    }

    @After
    public void teardown()
    {
        logger.log("Application End");
    }

    @Test
    public void testHandleMapExceptionrFromSingle()
    {
        Integer result = Single.just(1)
            .map(s -> exceptionFunc(s, 0))
            .onErrorReturn(throwable -> -1)
            .blockingGet();
        logger.log(String.format("Result: %s", result));
    }

    @Test
    public void testHandleMapExceptionFromObservable()
    {
        // We expect the exception to terminate all future emissions.
        obsGen.generate()
            .map(s -> exceptionFunc(s, 2))
            .onErrorReturn(throwable -> -1)
            .blockingSubscribe(
                s -> logger.log(String.format("Subscriber-%s: %s", agentId, s)),
                s -> logger.log(String.format("Subscriber-%s: Error", agentId)),
                () -> logger.log(String.format("Subscriber-%s: Complete", agentId)));
    }

    @Test
    public void testHandleMapErrorFromSingle()
    {
        Integer result = Single.just(1)
            .flatMap(s -> singleErrorFunc(s, 0))
            .onErrorReturn(throwable -> -1)
            .blockingGet();
        logger.log(String.format("Result: %s", result));
    }

    private <T> T exceptionFunc(T s, int failInstance)
    {
        if (curInstance++ == failInstance) {
            throw new RuntimeException();
        }

        return s;
    }

    private <T> Single<T> singleErrorFunc(T s, int failInstance)
    {
        if (curInstance++ == failInstance) {
            return Single.error(new RuntimeException());
        }

        return Single.just(s);
    }
}
