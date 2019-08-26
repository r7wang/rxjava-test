package com.rw;

public class Main
{

    public static void main(String[] args)
    {
        Logger logger = new Logger();
        TestRunner runner = new TestRunner(logger);

        // The test runner allows us to see how passing around object instance functions would look
        // like in an RxJava-style application (as opposed to static functions using Main::<funcName>.
        runner.run();

        /*
        // example: interval
        Observable<Long> clock = Observable.interval(0, 300, TimeUnit.MILLISECONDS);
        clock.blockingSubscribe(time -> {
            if (time % 2 == 0) {
                System.out.println("Tick");
            } else {
                System.out.println("Tock");
            }
        });

        // example: flatMap
        Observable<Integer> obsFlatMap = obsBase
            .flatMap(s -> Observable.fromArray(s, s + 1, s + 2));

        // example: map
        Observable<String> obsMap = obsBase
            .map(s -> s.toString() + "val");
         */
    }
}
