package pt.uminho.di.taskscheduler.client;


import pt.uminho.di.taskscheduler.common.Task;

import java.util.HashMap;
import java.util.Map;

public class Client {

    public static void main (String[] args) {
        Map<String, Task> tasks = new HashMap<>();
        int clientId = Integer.parseInt(args[0]);
        // This is not actually used, just to match the Scheduler API
        String client = "c" + clientId;

        // Add new task
        RemoteScheduler scheduler = new RemoteScheduler(clientId);
        scheduler.addNewTask("Buy groceries");

        // Get a task to do
        Task taskToDo = scheduler.getTask(client);
        tasks.put(taskToDo.getName(), taskToDo);
        System.out.println(taskToDo.getName());

        // Finalize the task
        scheduler.setFinalizedTask(client, taskToDo.getUrl());
        scheduler.close();
    }
}
