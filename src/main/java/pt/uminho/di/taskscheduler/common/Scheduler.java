package pt.uminho.di.taskscheduler.common;

public interface Scheduler {

    boolean addNewTask(String task);
    Task getTask(String client);
    boolean setFinalizedTask(String client, String finalizedTask);
}
