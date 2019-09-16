package com.rw;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.atomic.AtomicInteger;

public class SingleGenerator
{
     private Logger logger;

    public SingleGenerator(Logger logger)
    {
        this.logger = logger;
    }

    public Single<TaskResult> generateTask(int taskId, TaskResult result)
    {
        return Single.fromCallable(() -> task(taskId, result))
            .subscribeOn(Schedulers.newThread());
    }

    private TaskResult task(int taskId, TaskResult result)
    {
        logger.log(String.format("Task-%s: Start", taskId));
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException ex) {}
        logger.log(String.format("Task-%s: Finish", taskId));

        return result;
    }
}
