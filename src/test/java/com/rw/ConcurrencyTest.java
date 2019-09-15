package com.rw;

import io.grpc.Context;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConcurrencyTest {

    private Logger logger;
    private Sleeper sleeper;
    private ObservableGenerator obsGen;
    private RxTester rxTester;

    @Before
    public void setup()
    {
        logger = new Logger();
        sleeper = new Sleeper(logger);
        obsGen = new ObservableGenerator(logger);
        rxTester = new RxTester(logger);
        logger.log("Application Start");
    }

    @After
    public void teardown()
    {
        logger.log("Application End");
    }

    @Test
    public void testFireForget()
    {
        // Defines an approach to firing off a call to a microservice without needing to block on or wait for
        // the response. If the microservice call errors out, there's no impact on the main process.
        Context.current().fork().run(() -> {
            rxTester.subscribe(
                obsGen.generateWithError()
                    .subscribeOn(Schedulers.newThread()));
        });
        sleeper.sleep(10, 500);
    }

    @Test
    public void testScheduling()
    {
        // Uses a mix of subscribeOn and observeOn with print statements to better understand what threads are
        // running which parts of the system.
        //  - Try commenting out the subscribeOn or any of the observeOn.
        rxTester.subscribe(
            obsGen.generate()
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .flatMap(s -> {
                    logger.log(String.format("FlatMap: %s", s));
                    return Observable.fromArray(s, s + 1, s + 2, s + 3, s + 4, s + 5);
                })
                .observeOn(Schedulers.newThread())
                .map(s -> {
                    logger.log(String.format("Map: %s", s));
                    return String.format("%s-value", s);
                }));
    }
}
