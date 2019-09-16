package com.rw;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

public class ObservableGenerator
{
    private Logger logger;

    public ObservableGenerator(Logger logger)
    {
        this.logger = logger;
    }

    public Observable<Integer> generate()
    {
        return generate(1, 2, 5, 9);
    }

    public <T> Observable<T> generate(T... values)
    {
        return Observable.create((ObservableEmitter<T> obs) -> {
            logger.log("Observable Before Emit");
            for (T i : values) {
                obs.onNext(i);
                logger.log(String.format("Observable Emitted %s", i));
            }

            obs.onComplete();
            logger.log("Observable After Emit");
        });
    }
}
