package taskscheduler.client;

import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import pt.haslab.ekit.Spread;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;
import taskscheduler.common.Scheduler;
import taskscheduler.common.Task;
import taskscheduler.requests.*;

import java.util.concurrent.CompletableFuture;

public class RemoteScheduler implements Scheduler {
    private SingleThreadContext tc;
    private Spread spread;
    private CompletableFuture<Object> comp;
    private String groupName;
    private SpreadGroup group;

    public RemoteScheduler(int clientId, String groupName) {
        this.tc = new SingleThreadContext("proto-%d", new Serializer());
        this.groupName = groupName;
        register();
        comp = null;
        try {
            spread = new Spread("cli"+clientId, false);
        } catch (SpreadException e) {
            e.printStackTrace();
        }
        handlers();
        spread.open().thenRun(() -> group = spread.join(groupName));
    }

    private void handlers() {
        spread.handler(NewTaskRep.class, (m, v) -> {
            if(comp != null)
                comp.complete(v);
        });
        spread.handler(NextTaskRep.class, (m, v) -> {
            if(v.task != null && comp != null)
                comp.complete(v);
        });
        spread.handler(FinalizeTaskRep.class, (m, v) -> {
           if(comp != null){
               comp.complete(v);
           }
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


    public boolean addsNewTask(String task) {
        NewTaskRep rep = null;
        comp = new CompletableFuture<>();
        SpreadMessage m = new SpreadMessage();
        m.addGroup(groupName);
        m.setAgreed();
        spread.multicast(m, new NewTaskReq(task));
        try {
            rep = (NewTaskRep) comp.get();
        } catch (Exception e) {
            System.out.println("Error adding task");
            return false;
        }
        return rep.success;
    }

    public Task getTask() {
        NextTaskRep rep = null;
        comp = new CompletableFuture<>();
        SpreadMessage m = new SpreadMessage();
        m.addGroup(groupName);
        m.setAgreed();
        spread.multicast(m, new NextTaskReq());
        try {
            rep = (NextTaskRep) comp.get();
        } catch (Exception e) {
            System.out.println("Error getting new task");
            return null;
        }
        return rep.task;
    }

    public boolean setFinalizedTask(String finalizedTask) {
        FinalizeTaskRep rep = null;
        comp = new CompletableFuture<>();
        SpreadMessage m = new SpreadMessage();
        m.addGroup(groupName);
        m.setAgreed();
        spread.multicast(m, new FinalizeTaskReq(finalizedTask));
        try {
            rep = (FinalizeTaskRep) comp.get();
        } catch (Exception e) {
            System.out.println("Error getting new task");
            return false;
        }
        return rep.success;
    }

    public void exit() {
        try {
            group.leave();
        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }


}
