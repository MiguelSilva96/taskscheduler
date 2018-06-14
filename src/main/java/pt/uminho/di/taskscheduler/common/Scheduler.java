package pt.uminho.di.taskscheduler.common;

public interface Scheduler {

    boolean addsNewTask(String task);
    Task getTask();
    boolean setFinalizedTask(String finalizedTask);
}
