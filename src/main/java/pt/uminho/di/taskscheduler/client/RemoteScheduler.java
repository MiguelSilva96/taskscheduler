package pt.uminho.di.taskscheduler.client;

import pt.uminho.di.taskscheduler.common.Scheduler;
import pt.uminho.di.taskscheduler.common.Task;
import pt.uminho.di.taskscheduler.common.requests.*;

import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import pt.haslab.ekit.Spread;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

// not thread safe - it needs to have one instance per thread
public class RemoteScheduler implements Scheduler {
    private SingleThreadContext tc;
    private Spread spread;
    private CompletableFuture<Object> comp;
    private String groupName;
    private SpreadGroup group;
    private int request;

    public RemoteScheduler(int clientId, String groupName) {
        this.tc = new SingleThreadContext("proc-%d", new Serializer());
        this.groupName = groupName;
        register();
        comp = null;
        request = 0;
        try {
            spread = new Spread("cli"+clientId, false);
        } catch (SpreadException e) {
            e.printStackTrace();
        }
        handlers();
        tc.execute(() ->
            spread.open().thenRun(() -> group = spread.join(groupName))
        );
    }

    private void handlers() {
        spread.handler(NewTaskRep.class, (m, v) -> {
            if (request == v.request && comp != null)
                comp.complete(v);
        });
        spread.handler(NextTaskRep.class, (m, v) -> {
            if (request == v.request && comp != null)
                comp.complete(v);
        });
        spread.handler(FinalizeTaskRep.class, (m, v) -> {
           if (request == v.request && comp != null)
               comp.complete(v);
        });
    }

    private void register() {
        tc.serializer().register(NewTaskRep.class);
        tc.serializer().register(NewTaskReq.class);
        tc.serializer().register(NextTaskRep.class);
        tc.serializer().register(NextTaskReq.class);
        tc.serializer().register(FinalizeTaskReq.class);
        tc.serializer().register(FinalizeTaskRep.class);
    }


    public boolean addNewTask(String task) {
        NewTaskRep rep = null;
        comp = new CompletableFuture<>();
        SpreadMessage m = new SpreadMessage();
        m.addGroup(groupName);
        m.setAgreed();
        spread.multicast(m, new NewTaskReq(task, ++request));
        try {
            rep = (NewTaskRep) comp.get();
        } catch (Exception e) {
            return false;
        }
        return rep.success;
    }

    public Task getTask(String client) {
        NextTaskRep rep = null;
        comp = new CompletableFuture<>();
        SpreadMessage m = new SpreadMessage();
        m.addGroup(groupName);
        m.setAgreed();
        spread.multicast(m, new NextTaskReq(++request));
        try {
            rep = (NextTaskRep) comp.get();
        } catch (Exception e) {
            return null;
        }
        return rep.task;
    }

    public boolean setFinalizedTask(String client, String finalizedTask) {
        FinalizeTaskRep rep = null;
        comp = new CompletableFuture<>();
        SpreadMessage m = new SpreadMessage();
        m.addGroup(groupName);
        m.setAgreed();
        spread.multicast(m, new FinalizeTaskReq(finalizedTask, ++request));
        try {
            rep = (FinalizeTaskRep) comp.get();
        } catch (Exception e) {
            return false;
        }
        return rep.success;
    }

    public void close() {
        spread.leave(group);
        try {
            spread.close().get();
        } catch (ExecutionException|InterruptedException e) {
            e.printStackTrace();
        }
    }
}
