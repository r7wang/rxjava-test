package com.rw;

public class Main
{

    public static void main(String[] args)
    {
        Logger logger = new Logger();
        ObservableGenerator obsGen = new ObservableGenerator(logger);
        TestRunner runner = new TestRunner(logger, obsGen);

        // The test runner allows us to see how passing around object instance functions would look
        // like in an RxJava-style application (as opposed to static functions using Main::<funcName>.
        runner.run();

        /*
        // example: flatMap
        Observable<Integer> obsFlatMap = obsBase
            .flatMap(s -> Observable.fromArray(s, s + 1, s + 2));

        // example: map
        Observable<String> obsMap = obsBase
            .map(s -> s.toString() + "val");
         */
    }
}
