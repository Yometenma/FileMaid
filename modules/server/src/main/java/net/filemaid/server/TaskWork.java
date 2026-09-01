package net.filemaid.server;

/** A unit of background work that receives a context for progress and cancellation. */
@FunctionalInterface
public interface TaskWork {
    Object run(TaskContext context) throws Exception;
}
