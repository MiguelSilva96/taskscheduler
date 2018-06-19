package pt.uminho.di.taskscheduler.server;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import pt.uminho.di.taskscheduler.common.Constants;
import pt.uminho.di.taskscheduler.common.Scheduler;
import pt.uminho.di.taskscheduler.common.Task;
import pt.uminho.di.taskscheduler.server.requests.StateFragment;

import javax.annotation.processing.SupportedSourceVersion;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/* This is temporary, state transfer has to be fragmented */
public class SchedulerImpl implements /*CatalystSerializable,*/ Scheduler {

    private TreeSet<Task> nextTasks;
    private Map<String, Map<String, Task>> assignedTasks;
    private int taskIds;

    public SchedulerImpl() {
        taskIds = 0;
        nextTasks = new TreeSet<>();
        assignedTasks = new HashMap<>();
    }

    public boolean addNewTask(String name) {
        int id = taskIds++;
        String url = "task" + id;
        String nme = name;
        if(name.length() > 100)
            nme = name.substring(0, Constants.TASK_SIZE);
        Task newTask = new Task(nme, url);
        nextTasks.add(newTask);
        return true;
    }

    public Task getTask(String client) {
        if (nextTasks.size() == 0)
            return null;

        Task task = nextTasks.pollFirst();
        Map<String, Task> clientTasks = assignedTasks.get(client);

        if (clientTasks == null) {
            clientTasks = new HashMap<>();
            assignedTasks.put(client, clientTasks);
        }

        clientTasks.put(task.getUrl(), task);
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

    public void freeClientAssignedTasks(String client) {
        Map<String, Task> tasks = assignedTasks.get(client);
        if(tasks != null) {
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, Task> entry : tasks.entrySet()) {
                nextTasks.add(entry.getValue());
                toRemove.add(entry.getKey());
            }
            for (String s : toRemove) {
                assignedTasks.remove(s);
            }
        }
    }

    public void addFragment(StateFragment fragment) {
        for(Task task : fragment.nextTasks)
            nextTasks.add(task);
        for(String client : fragment.assignedTasks.keySet()) {
            Map<String, Task> tasks = assignedTasks.get(client);
            if (tasks == null)
                tasks = new HashMap<>();
            for(Task t : fragment.assignedTasks.get(client))
                tasks.put(t.getUrl(), t);
            assignedTasks.put(client, tasks);
        }
        this.taskIds = fragment.tasksId;
    }

    private StateFragment baseFragment(int msgNum) {
        StateFragment r = new StateFragment();
        r.tasksId = taskIds;
        r.msgNum = msgNum;
        return r;
    }

    public List<StateFragment> getFragments() {
        List<StateFragment> fragments = new ArrayList<>();
        int currentFragmentSize = Constants.MIN_META_SIZE;
        int msgNum = 0;

        StateFragment currentFragment = baseFragment(msgNum);
        msgNum++;

        for(Task task : nextTasks) {
            int taskSize = Constants.TASK_SIZE + task.getUrl().length();
            currentFragmentSize += taskSize;
            if(currentFragmentSize <= Constants.FRAGMENT_SIZE)
                currentFragment.nextTasks.add(task);
            else {
                fragments.add(currentFragment);
                currentFragment = baseFragment(msgNum);
                msgNum++;
                currentFragmentSize = Constants.MIN_META_SIZE + taskSize;
                currentFragment.nextTasks.add(task);
            }
        }

        for(String key : assignedTasks.keySet()) {
            Map<String, Task> value = assignedTasks.get(key);
            currentFragmentSize += key.length();
            if(currentFragmentSize <= Constants.FRAGMENT_SIZE)
                currentFragment.assignedTasks.put(key, new ArrayList<>());
            else {
                fragments.add(currentFragment);
                currentFragment = baseFragment(msgNum);
                msgNum++;
                currentFragment.assignedTasks.put(key, new ArrayList<>());
                currentFragmentSize = Constants.MIN_META_SIZE + key.length();
            }
            for(String url : value.keySet()) {
                Task t = value.get(url);
                int taskSize = url.length() + Constants.TASK_SIZE;
                currentFragmentSize += taskSize;

                if(currentFragmentSize <= Constants.FRAGMENT_SIZE)
                    currentFragment.assignedTasks.get(key).add(t);
                else {
                    fragments.add(currentFragment);
                    currentFragment = baseFragment(msgNum);
                    msgNum++;
                    currentFragmentSize = Constants.MIN_META_SIZE + taskSize;
                    currentFragmentSize += key.length();
                    currentFragment.assignedTasks.put(key, new ArrayList<>());
                    currentFragment.assignedTasks.get(key).add(t);
                }
            }
        }
        fragments.add(currentFragment);

        if(fragments.size() > 0)
            fragments.get(fragments.size() - 1).isLast = true;

        return fragments;
    }


/*
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
        nextTasks = new TreeSet<>();
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
*/
}
