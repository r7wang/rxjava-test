package com.rw;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// Summary
//  - onErrorReturn catches both errors and exceptions, irrespective of whether they come from map or flatMap.
//  - There are two ways to use onErrorReturn to catch exception, either through map or flatMap.
//  - Once an error or exception is detected, nothing else is emitted.
public class ErrorTest {

    private Logger logger;
    private ObservableGenerator obsGen;
    private RxTester rxTester;
    private int subscriberId = 1;

    // Keeps track of the current emission index from the source observable.
    private int curInstance;

    @Before
    public void setup()
    {
        logger = new Logger();
        obsGen = new ObservableGenerator(logger);
        rxTester = new RxTester(logger);
        curInstance = 0;
        logger.log("Application Start");
    }

    @After
    public void teardown()
    {
        logger.log("Application End");
    }

    @Test
    public void testHandleMapExceptionFromSingle()
    {
        rxTester.subscribe(
            Single.just(1)
                .map(s -> exceptionFunc(s, 0))
                .onErrorReturn(throwable -> -1));
    }

    @Test
    public void testHandleFlatMapErrorFromSingle()
    {
        rxTester.subscribe(
            Single.just(1)
                .flatMap(s -> singleErrorFunc(s, 0))
                .onErrorReturn(throwable -> -1));
    }

    @Test
    public void testHandleFlatMapExceptionFromSingle()
    {
        rxTester.subscribe(
            Single.just(1)
                .flatMap(s -> singleExceptionFunc(s, 0))
                .onErrorReturn(throwable -> -1));
    }

    @Test
    public void testHandleMapExceptionFromObservable()
    {
        rxTester.subscribe(
            obsGen.generate()
                .map(s -> exceptionFunc(s, 2))
                .onErrorReturn(throwable -> -1));
    }

    @Test
    public void testHandleFlatMapErrorFromObservable()
    {
        rxTester.subscribe(
            obsGen.generate()
                .flatMap(s -> observableErrorFunc(s, 2))
                .onErrorReturn(throwable -> -1));
    }

    @Test
    public void testHandleFlatMapExceptionFromObservable()
    {
        rxTester.subscribe(
            obsGen.generate()
                .flatMap(s -> observableExceptionFunc(s, 2))
                .onErrorReturn(throwable -> -1));
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

    private <T> Single<T> singleExceptionFunc(T s, int failInstance)
    {
        if (curInstance++ == failInstance) {
            throw new RuntimeException();
        }
        return Single.just(s);
    }

    private <T> Observable<T> observableErrorFunc(T s, int failInstance)
    {
        if (curInstance++ == failInstance) {
            return Observable.error(new RuntimeException());
        }
        return Observable.just(s);
    }

    private <T> Observable<T> observableExceptionFunc(T s, int failInstance)
    {
        if (curInstance++ == failInstance) {
            throw new RuntimeException();
        }
        return Observable.just(s);
    }
}
