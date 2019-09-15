package com.rw;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// Summary
//  - Observable.defer() and Observable.fromCallable() both defer execution until subscription occurs, which means that
//    we don't know what will be emitted at creation time.
//  - Observable.defer() additionally provides the opportunity to emit multiple objects.
//  - When there are multiple subscribers on a cold observable, the emissions fire once per subscriber. You don't lose
//    items that other subscribers previously saw, but at the same time, deferral of emissions might cause another
//    subscriber to see different values.
public class EmissionTest {

    private Logger logger;
    private Sleeper sleeper;
    private RxTester rxTester;

    @Before
    public void setup()
    {
        logger = new Logger();
        sleeper = new Sleeper(logger);
        rxTester = new RxTester(logger);
        logger.log("Application Start");
    }

    @After
    public void teardown()
    {
        logger.log("Application End");
    }

    @Test
    public void testSingle() {
        Single<String> singleSource = Single.just("single item");
        singleSource.subscribe(
                s -> logger.log(String.format("Item: %s", s)),
                Throwable::printStackTrace
        );
    }

    @Test
    public void testMaybeWithItem() {
        Maybe<String> maybeSource = Maybe.just("single item");
        maybeSource.subscribe(
                s -> logger.log(String.format("Item: %s", s)),
                Throwable::printStackTrace,
                () -> logger.log("Done from MaybeSource")
        );
    }

    @Test
    public void testMaybeWithNothing() {
        Maybe<Integer> emptySource = Maybe.empty();
        emptySource.subscribe(
                s -> logger.log(String.format("Item: %s", s)),
                Throwable::printStackTrace,
                () -> logger.log("Done from EmptySource")
        );
    }

    @Test
    public void testDefer()
    {
        Observable<Long> obs = Observable.defer(() -> Observable.just(getTime()));
        rxTester.subscribe(obs, 1);
        sleeper.sleep(1, 1000);
        rxTester.subscribe(obs, 2);
    }

    @Test
    public void testFromCallable()
    {
        Observable<Long> obs = Observable.fromCallable(this::getTime);
        rxTester.subscribe(obs, 1);
        sleeper.sleep(1, 1000);
        rxTester.subscribe(obs, 2);
    }

    private Long getTime()
    {
        return System.currentTimeMillis();
    }
}
