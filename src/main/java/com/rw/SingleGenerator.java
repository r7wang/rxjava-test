package com.rw;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class SingleGenerator
{
    static int TASK_ID = 1;

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
        // Not atomic, but it's not super critical here.
        int taskId = TASK_ID++;

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
