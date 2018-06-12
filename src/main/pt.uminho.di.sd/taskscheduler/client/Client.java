package taskscheduler.client;

import taskscheduler.common.Scheduler;

import java.rmi.Remote;

public class Client {

    public static void main (String[] args) {
        int clientId = Integer.parseInt(args[0]);
        String groupName = args[1];
        RemoteScheduler scheduler = new RemoteScheduler(clientId, groupName);
        scheduler.addsNewTask("dar banho ao baião");
        String taskToDo = scheduler.getTask();
        System.out.println(taskToDo);
        scheduler.setFinalizedTask(taskToDo);
        scheduler.exit();
    }


}
