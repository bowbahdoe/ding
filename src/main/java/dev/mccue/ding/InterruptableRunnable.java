package dev.mccue.ding;

@FunctionalInterface
interface InterruptableRunnable {
    void run() throws InterruptedException;
}
