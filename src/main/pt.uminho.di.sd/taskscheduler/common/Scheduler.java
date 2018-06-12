package taskscheduler.common;

public interface Scheduler {

    boolean addsNewTask(String task);
    String getTask();
    boolean setFinalizedTask(String finalizedTask);
}
