package tasks;

/**
 * A generic interface for queued tasks. All implementations
 * should supply a zero-arg constructor.
 * 
 * @author syyang
 */
public interface Task {
 
    /**
     * Executes a task.
     */
    void execute(TaskContext context) throws Exception;

}
