package com.rw;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

// Summary
//  - onErrorReturn catches both errors and exceptions, irrespective of whether they come from map or flatMap.
//  - There are two ways to use onErrorReturn to catch exception, either through map or flatMap.
//  - Once an error or exception is detected, nothing else is emitted.
//  - Unhandled exceptions in the observable chain have no impact on the subscribing thread.
//  - doOnError might be something to use when we want to clean up after an error. The subscriber onError() may not
//    have enough knowledge to do this effectively.
public class ErrorTest {

    private Logger logger;
    private ObservableGenerator obsGen;
    private RxTester rxTester;

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

    @Test
    public void testRuntimeExceptionImpact()
    {
        // Exceptions are swallowed and don't appear to impact the subscribing thread.
        //  - If we have an onError handler, it will do what's in that handler.
        //  - If we don't have an onError handler, the exception will be printed to console, but nothing will happen.
        //  - In all cases, the last line is reachable.
        rxTester.subscribe(
            obsGen.generate()
                .map((Integer s) -> {
                    throw new RuntimeException();
                }));
        logger.log("Is this code reachable?");
    }

    @Test
    public void testRuntimeExceptionKeepEmitting()
    {
        // We want to avoid a situation where an exception causes the observable to shut down. To achieve this, the
        // following needs to happen:
        //  - The function that can potentially error must not throw the exception directly. It must return
        //    Observable.error(), or we must catch the exception somewhere and then call Observable.error().
        //  - We must follow the returned Observable directly with onErrorResumeNext() without returning back up to the
        //    parent observable. If we even add something like an identity map between the error and
        //    onErrorResumeNext(), the observable chain will stop after the emission that caused the error.
        rxTester.subscribeAndWait(
            Observable.interval(20, TimeUnit.MILLISECONDS)
                .flatMap((Long s) -> observableErrorFunc(s, 8))
                //.map(s -> s)
                .onErrorResumeNext(Observable.just((long) Integer.MAX_VALUE)),
            500);
    }

    @Test
    public void testDoOnErrorOrder()
    {
        // Order of doOnError() will determine whether or not it executes. If doOnError() is specified before an error
        // occurs in the observable chain, then it won't be invoked.
        rxTester.subscribe(
            obsGen.generate()
                .doOnError(error -> logger.log(String.format("doOnError (Before): %s", error)))
                .map(s -> exceptionFunc(s, 2))
                .doOnError(error -> logger.log(String.format("doOnError (After): %s", error)))
        );
    }

    @Test
    public void testMultipleDoOnError()
    {
        // Both doOnError() callbacks will be invoked, in the order specified.
        rxTester.subscribe(
            obsGen.generate()
                .map(s -> exceptionFunc(s, 2))
                .doOnError(error -> logger.log(String.format("doOnError (Before): %s", error)))
                .doOnError(error -> logger.log(String.format("doOnError (After): %s", error)))
        );
    }

    @Test
    public void testEmissionHaltingOnError()
    {
        // Not all operations will complete after the error occurs, but there doesn't seem to be a specific guarantee
        // on when execution will stop (best effort).
        //  - We don't want to make any assumption on how many emits have been consumed, especially if there have been
        //    multiple threads involved. Any final state should ideally be handled by the subscriber unless we have a
        //    good way to store partial state.
        //  - If a doError exists in the chain, it seems that the duration of doOnError execution impacts how much work
        //    is done by the chained operators. If doOnError takes a long time, then execution will occur for a longer
        //    period of time after the error occurs.
        rxTester.subscribe(
            obsGen.generate(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .map(s -> {
                    if (s == 8) {
                        logger.log("Map (Stage 1): Error");
                        throw new RuntimeException();
                    }
                    logger.log(String.format("Map (Stage 1): %s", s));
                    return s + 10;
                })
                .observeOn(Schedulers.newThread())
                .map(s -> {
                    logger.log(String.format("Map (Stage 2): %s", s));
                    return s + 100;
                })
                .observeOn(Schedulers.newThread())
                .map(s -> {
                    logger.log(String.format("Map (Stage 3): %s", s));
                    return s + 1000;
                })
        );
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
