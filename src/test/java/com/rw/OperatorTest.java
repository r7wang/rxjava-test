package com.rw;

import io.reactivex.Completable;
import io.reactivex.Observable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class OperatorTest {

    private Logger logger;
    private ObservableGenerator obsGen;
    private RxTester rxTester;

    @Before
    public void setup()
    {
        logger = new Logger();
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
    public void testMap()
    {
        rxTester.subscribe(
            obsGen.generate()
                .map(s -> String.format("%s-value", s)));
    }

    @Test
    public void testFlatMap()
    {
        rxTester.subscribe(
            obsGen.generate()
                .flatMap(s -> Observable.fromArray(s, s + 1, s + 2)));
    }

    @Test
    public void testSimpleGroupBy()
    {
        // We can do the same thing with a regular map.
        rxTester.subscribe(
            obsGen.generate()
                .groupBy(s -> s / 3, s -> s)  // Observable<GroupedObservable<Integer, Integer>>
                .flatMap(group -> group.map(s -> String.format("Group %s -> Value %s", group.getKey(), s))));
    }

    @Test
    public void testAggregationGroupBy()
    {
        // Aggregation shows a better use case of why we may want to use a groupBy.
        rxTester.subscribe(
            obsGen.generate()
                .groupBy(s -> s / 3, s -> s)  // Observable<GroupedObservable<Integer, Integer>>
                .flatMapSingle(grp -> grp.reduce(0, (accumulator, x) -> accumulator + x)));
    }

    @Test
    public void testConcatCompletableOrder()
    {
        // If we concatenate a lot of completables, in which order do they run?
        //  - When the entire subscription runs on a single thread, they are handled in order and an exception thrown
        //    in one of the stages prevents further observable emissions.
        List<Completable> toComplete = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final int completableIdx = i;
            toComplete.add(Completable.fromCallable(() -> {
                logger.log(String.format("Completable %s", completableIdx));
                if (completableIdx == 8) {
                    throw new RuntimeException();
                }
                return 0;
            }));
        }
        rxTester.subscribe(
            Completable.concat(toComplete)
                .onErrorComplete((e) -> {
                    logger.log("OnErrorComplete");
                    return true;
                }));
    }
}
