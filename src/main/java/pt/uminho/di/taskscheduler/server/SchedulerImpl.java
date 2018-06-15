package pt.uminho.di.taskscheduler.server;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import pt.uminho.di.taskscheduler.common.Scheduler;
import pt.uminho.di.taskscheduler.common.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/* This is temporary, state transfer has to be fragmented */
public class SchedulerImpl implements CatalystSerializable, Scheduler {

    private List<Task> nextTasks;
    private Map<String, Map<String, Task>> assignedTasks;
    private AtomicInteger taskIds;

    public SchedulerImpl() {
        taskIds = new AtomicInteger(0);
        nextTasks = new ArrayList<>();
        assignedTasks = new HashMap<>();
    }

    public boolean addNewTask(String name) {
        int id = taskIds.incrementAndGet();
        String url = "task" + id;
        Task newTask = new Task(name, url);
        nextTasks.add(newTask);
        return true;
    }

    public Task getTask(String client) {
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

    public boolean setFinalizedTask(String client, String url) {
        Map<String, Task> clientTasks = assignedTasks.get(client);
        if (clientTasks == null)
            return false;

        Task task = clientTasks.get(url);
        if (task == null)
            return false;

        clientTasks.remove(url);
        return true;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeInt(nextTasks.size());
        for(Task task : nextTasks)
            serializer.writeObject(task, bufferOutput);

        bufferOutput.writeInt(assignedTasks.keySet().size());
        for(String key : assignedTasks.keySet()) {
            bufferOutput.writeString(key);
            bufferOutput.writeInt(assignedTasks.get(key).values().size());
            for(Task task : assignedTasks.get(key).values())
                serializer.writeObject(task, bufferOutput);
        }

        bufferOutput.writeInt(taskIds.get());
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        nextTasks = new ArrayList<>();
        assignedTasks = new HashMap<>();

        int size = bufferInput.readInt();
        for(int i = 0; i < size; i++)
            nextTasks.add(serializer.readObject(bufferInput));

        size = bufferInput.readInt();
        for(int i = 0; i < size; i++) {
            Map<String, Task> clientTasks = new HashMap<>();
            String key = bufferInput.readString();
            int task_size = bufferInput.readInt();
            for(int j = 0; j < task_size; j++) {
                Task t = serializer.readObject(bufferInput);
                clientTasks.put(t.getUrl(), t);
            }
            assignedTasks.put(key, clientTasks);
        }

        taskIds = new AtomicInteger(bufferInput.readInt());
    }
}
