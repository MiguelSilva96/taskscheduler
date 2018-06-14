package taskscheduler.server;

import spread.SpreadGroup;
import taskscheduler.common.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SchedulerImpl {

    private List<Task> nextTasks;
    private Map<String, Map<String, Task>> assignedTasks;
    private AtomicInteger taskIds;

    public SchedulerImpl() {
        taskIds = new AtomicInteger(0);
        nextTasks = new ArrayList<>();
        assignedTasks = new HashMap<>();
    }

    public void newTask(String name) {
        int id = taskIds.incrementAndGet();
        String url = "task" + id;
        Task newTask = new Task(name, url);
        nextTasks.add(newTask);
    }

    public Task nextTask(String client) {
        if (nextTasks.size() == 0)
            return null;

        Task task = nextTasks.get(0);
        Map<String, Task> clientTasks = assignedTasks.get(client);

        if (clientTasks == null) {
            clientTasks = new HashMap<>();
            assignedTasks.put(client, clientTasks);
        }

        clientTasks.put(task.getUrl(), task);
        nextTasks.remove(0);
        return task;
    }

    public boolean finalizeTask(String client, String url) {
	    Map<String, Task> clientTasks = assignedTasks.get(client);
	 	if (clientTasks == null)
	 	    return false;

	 	Task task = clientTasks.get(url);
	 	if (task == null)
	 	    return false;

	 	clientTasks.remove(url);
	 	return true;
    }
}
