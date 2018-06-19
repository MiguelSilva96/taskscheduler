package pt.uminho.di.taskscheduler.client;


import pt.uminho.di.taskscheduler.common.Task;

import java.util.HashMap;
import java.util.Map;

public class Client {

    /* THINGS THAT WE TESTED:
    * - Simple example represented here;
    * - Executing a lot of addNewTask with 1 server only, then add a Server and kill de first one
    * Then run another client to get the tasks;
    * - Have one Server running and then check that when we add another one
    * the leader election only happens after the state transfer.
    * - Check that killing a server triggers leader election.
    * - Executing a lot of addNewTask and then getting all of them. Kill the client without finalize.
    * Try to get the same tasks with another client (re-assigned).
     */

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
