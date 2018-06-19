package pt.uminho.di.taskscheduler.server;

import pt.uminho.di.taskscheduler.common.Task;

import java.util.List;

public class StateFragment {
    public boolean isForNextTasks; // 1 byte
    public List<Task> tasks; // (100 bytes + size(all urls)) * size()
    public int tasksId; // 4 bytes
}
