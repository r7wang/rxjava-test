package com.rw;

import io.grpc.Context;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.schedulers.Schedulers;

public class Main
{

    public static void main(String[] args)
    {
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

    public static void testBlocking()
    {
        // TODO: Implement.
    }

    public static void testFireForget(boolean shouldError)
    {
        Context.current().fork().run(() -> {
            Observable obs = baseObservable(shouldError)
                .subscribeOn(Schedulers.newThread());
            subscribe(obs);
        });
        sleep(10, 500);
    }

    public static void testScheduling()
    {
        runApp(() ->
        {
            Observable<String> obsBase = baseObservable(false)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .flatMap(s -> {
                    print(String.format("FlatMap: %s", s));
                    return Observable.fromArray(s, s + 1, s + 2, s + 3, s + 4, s + 5);
                })
                .observeOn(Schedulers.newThread())
                .map(s -> {
                    print(String.format("Map: %s", s));
                    return s.toString() + "val";
                });
            return obsBase;
        });
    }

    public static void runApp(AppInterface runner)
    {
        print("Application Start");
        Observable obs = runner.Run();
        subscribe(obs);
        sleep(10, 3);
        print("Application End");
    }

    public static Observable<Integer> baseObservable(boolean shouldError)
    {
        return Observable
            .create((ObservableEmitter<Integer> obs) -> {
                print("Observable Before Emit");
                int[] values = {1, 2, 5, 9};
                for (int i : values) {
                    obs.onNext(i);
                    print("Observable Emit");
                }

                if (shouldError) {
                    throw new RuntimeException("Something bad happened...");
                }

                obs.onComplete();
                print("Observable After Emit");
            });
    }

    public static void subscribe(Observable obs)
    {
        subscribe(obs, 1);
    }

    public static void subscribe(Observable obs, int agentId)
    {
        obs.subscribe(
            s -> print(String.format("Subscriber-%s: %s", agentId, s)),
            s -> print(String.format("Subscriber-%s: Error", agentId)),
            () -> print(String.format("Subscriber-%s: Complete", agentId)));
        print("Subscribed");
    }

    public static void subscribeMulti(Observable obs)
    {
        subscribe(obs, 1);
        subscribe(obs, 2);
    }

    public static void sleep(int numCycles, int intervalMs)
    {
        for (int i = 0; i < numCycles; i++) {
            print("Sleeping...");
            try {
                // Need a very short interval to see thread interleaving.
                Thread.sleep(intervalMs);
            }
            catch (InterruptedException ex) {
                print("Sleep Interrupted");
                break;
            }
        }
    }

    public static void print(String s)
    {
        System.out.println(String.format("(Thread-%s) %s", threadId(), s));
    }

    public static long threadId()
    {
        return Thread.currentThread().getId();
    }
}
