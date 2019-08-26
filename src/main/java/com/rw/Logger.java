package com.rw;

public class Logger {
    public void log(String s)
    {
        System.out.println(String.format("(Thread-%s) %s", threadId(), s));
    }

    private long threadId()
    {
        return Thread.currentThread().getId();
    }
}
