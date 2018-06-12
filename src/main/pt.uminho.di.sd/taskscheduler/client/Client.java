package taskscheduler.client;

import taskscheduler.common.Task;

import java.util.HashMap;
import java.util.Map;

public class Client {

    public static void main (String[] args) {
        Map<String, Task> tasks = new HashMap<>();
        int clientId = Integer.parseInt(args[0]);
        String groupName = args[1];
        RemoteScheduler scheduler = new RemoteScheduler(clientId, groupName);
        scheduler.addsNewTask("dar banho ao baião");
        Task taskToDo = scheduler.getTask();
        tasks.put(taskToDo.getName(), taskToDo);
        System.out.println(taskToDo.getName());
        scheduler.setFinalizedTask(taskToDo.getUrl());
        scheduler.exit();
    }


}
