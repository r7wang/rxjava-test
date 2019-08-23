package com.rw;

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;

public class Main {

    public static void main(String[] args) {
        System.out.println(String.format("(Thread-%s) Application Start", Thread.currentThread().getId()));
        Observable<String> obsBase = Observable
            .create((ObservableEmitter<Integer> obs) -> {
                System.out.println(String.format("(Thread-%s) Observable Before Emit", Thread.currentThread().getId()));
                int[] values = {1, 2, 5, 9};
                for (int i : values) {
                    obs.onNext(i);
                    System.out.println(String.format("(Thread-%s) Observable Emit", Thread.currentThread().getId()));
                }
                obs.onComplete();
                System.out.println(String.format("(Thread-%s) Observable After Emit", Thread.currentThread().getId()));
            })
            .subscribeOn(Schedulers.newThread())
            .observeOn(Schedulers.newThread())
            .flatMap(s -> {
                System.out.println(String.format("(Thread-%s) FlatMap: %s", Thread.currentThread().getId(), s));
                return Observable.fromArray(s, s + 1, s + 2, s + 3, s + 4, s + 5);
            })
            .observeOn(Schedulers.newThread())
            .map(s -> {
                System.out.println(String.format("(Thread-%s) Map: %s", Thread.currentThread().getId(), s));
                return s.toString() + "val";
            });
        testOutput(obsBase);
        System.out.println(String.format("(Thread-%s) Application End", Thread.currentThread().getId()));

        /*
        // example: observable.fromArray
        Observable<Integer> obsBase = Observable.fromArray(1, 2, 5, 9);

        // example: interval
        Observable<Long> clock = Observable.interval(0, 300, TimeUnit.MILLISECONDS);
        clock.blockingSubscribe(time -> {
            if (time % 2 == 0) {
                System.out.println("Tick");
            } else {
                System.out.println("Tock");
            }
        });

        // example: defer
        Observable<Long> obsBase = Observable.defer(() -> {
            long time = System.currentTimeMillis();
            return Observable.just(time);
        });
        obsBase.subscribe(time -> System.out.println(time));
        try {
            Thread.sleep(1000);
        } catch (Exception ex) {}
        obsBase.subscribe(time -> System.out.println(time));

        // example: groupBy (simple)
        //  We can do the same thing with a regular map.
        Observable<String> obsGroupBy = obsBase
            .groupBy(s -> s/3, s -> s)  // Observable<GroupedObservable<Integer, Integer>>
            .flatMap(grp -> grp.map(s -> String.format("%s -> %s", grp.getKey(), s)));

        // example: groupBy (aggregation)
        //  This shows a better use case of why we may want to use a groupBy.
        Observable<Integer> obsAgg = obsBase
            .groupBy(s -> s/3, s -> s)  // Observable<GroupedObservable<Integer, Integer>>
            .flatMap(grp -> grp.reduce(0, (accumulator, x) -> accumulator + x).toObservable());

        // example: flatMap
        Observable<Integer> obsFlatMap = obsBase
            .flatMap(s -> Observable.fromArray(s, s + 1, s + 2));

        // example: map
        Observable<String> obsMap = obsBase
            .map(s -> s.toString() + "val");

        // example: multiple subscriptions to the same observable
        testMultiOutput(obsFlatMap);

        // example: single emission
        Single<String> singleSource = Single.just("single item");
        singleSource.subscribe(
                s -> System.out.println("Item received: from singleSource " +  s),
                Throwable::printStackTrace
        );

        // example: some maybe emission
        Maybe<String> maybeSource = Maybe.just("single item");
        maybeSource.subscribe(
                s -> System.out.println("Item received: from maybeSource " +  s),
                Throwable::printStackTrace,
                () -> System.out.println("Done from MaybeSource")
        );

        // example: none maybe emission
        Maybe<Integer> emptySource = Maybe.empty();
        emptySource.subscribe(
                s -> System.out.println("Item received: from emptySource" + s),
                Throwable::printStackTrace,
                () -> System.out.println("Done from EmptySource")
        );
         */
    }

    public static void testOutput(Observable obs) {
        testOutput(obs, 1);
    }

    public static void testOutput(Observable obs, int agentId) {
        obs.blockingSubscribe(
                s -> System.out.println(String.format("(Thread-%s) Subscriber-%s: %s", Thread.currentThread().getId(), agentId, s)),
                s -> System.out.println(String.format("(Thread-%s) Subscriber-%s: Error", Thread.currentThread().getId(), agentId)),
                () -> System.out.println(String.format("(Thread-%s) Subscriber-%s: Complete", Thread.currentThread().getId(), agentId)));
    }

    public static void testMultiOutput(Observable obs) {
        testOutput(obs, 1);
        testOutput(obs, 2);
    }
}
