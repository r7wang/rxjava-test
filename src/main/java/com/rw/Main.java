package com.rw;

public class Main
{

    public static void main(String[] args)
    {
        Logger logger = new Logger();
        Sleeper sleeper = new Sleeper(logger);
        ObservableGenerator obsGen = new ObservableGenerator(logger);
        TestRunner runner = new TestRunner(
            logger,
            sleeper,
            obsGen);

        // The test runner allows us to see how passing around object instance functions would look
        // like in an RxJava-style application (as opposed to static functions using Main::<funcName>.
        runner.run();
    }
}
