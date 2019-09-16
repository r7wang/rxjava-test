package com.rw;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ZipTest {

    private Logger logger;
    private ObservableGenerator obsGen;
    private SingleGenerator singleGen;
    private RxTester rxTester;

    @Before
    public void setup()
    {
        logger = new Logger();
        obsGen = new ObservableGenerator(logger);
        singleGen = new SingleGenerator(logger);
        rxTester = new RxTester(logger);
        logger.log("Application Start");
    }

    @After
    public void teardown()
    {
        logger.log("Application End");
    }

    // Zip allows us to concurrently merge multiple observables together through an arbitrarily defined function.
    //  - It will only do so if there are corresponding elements in both observables.
    @Test
    public void testObservableZipUnevenObservables()
    {
        rxTester.subscribe(
            Observable.zip(
                obsGen.generate(1, 2, 5, 9)
                    .subscribeOn(Schedulers.newThread()),
                obsGen.generate(3, 6, 7)
                    .subscribeOn(Schedulers.newThread()),
                (a, b) -> a + b)
        );
    }

    @Test
    public void testSingleZipConcurrent()
    {
        List<Single<TaskResult>> zipped = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (i == 4) {
                zipped.add(singleGen.generateTask(i, TaskResult.ERROR));
            } else {
                zipped.add(singleGen.generateTask(i, TaskResult.DONE));
            }
        }

        rxTester.subscribe(
            Single.zip(zipped, (Object[] objs) -> {
                boolean allDone = true;
                for (Object obj : objs) {
                    TaskResult result = (TaskResult) obj;
                    logger.log(String.format("Task Result: %s", result));
                    if (result == TaskResult.ERROR) {
                        allDone = false;
                    }
                }

                return allDone ? TaskResult.DONE : TaskResult.ERROR;
            }));
    }
}
