package com.rw;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

public class ObservableGenerator {

    private Logger logger;

    public ObservableGenerator(Logger logger) {
        this.logger = logger;
    }

    public Observable<Integer> Generate()
    {
        return Generate(false, 1, 2, 5, 9);
    }

    public Observable<Integer> GenerateWithError()
    {
        return Generate(true, 1, 2, 5, 9);
    }

    public <T> Observable<T> Generate(boolean shouldError, T... values)
    {
        return Observable.create((ObservableEmitter<T> obs) -> {
            logger.log("Observable Before Emit");
            for (T i : values) {
                obs.onNext(i);
                logger.log(String.format("Observable Emitted %s", i));
            }

            if (shouldError) {
                logger.log("Observable Throwing Exception");
                throw new RuntimeException("Something bad happened...");
            }

            obs.onComplete();
            logger.log("Observable After Emit");
        });
    }
}
