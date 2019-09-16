package com.rw;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.atomic.AtomicInteger;

public class SingleGenerator
{
    static AtomicInteger TASK_ID = new AtomicInteger(0);

    private Logger logger;

    public SingleGenerator(Logger logger)
    {
        this.logger = logger;
    }

    public Single<TaskResult> generateTask()
    {
        return Single.fromCallable(this::task)
            .subscribeOn(Schedulers.newThread());
    }

    public TaskResult task()
    {
        int taskId = TASK_ID.incrementAndGet();

        logger.log(String.format("Task-%s: Start", taskId));
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException ex) {
        }
        logger.log(String.format("Task-%s: Finish", taskId));

        if (taskId == 4) {
            return TaskResult.ERROR;
        }
        return TaskResult.DONE;
    }
}
