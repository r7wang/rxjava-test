package com.rw;

public class Sleeper
{
    private Logger logger;

    public Sleeper(Logger logger)
    {
        this.logger = logger;
    }

    public void sleep(int numCycles, int intervalMs)
    {
        for (int i = 0; i < numCycles; i++) {
            logger.log("Sleeping...");
            try {
                // Need a very short interval to see thread interleaving.
                Thread.sleep(intervalMs);
            }
            catch (InterruptedException ex) {
                logger.log("Sleep Interrupted");
                break;
            }
        }
    }
}
