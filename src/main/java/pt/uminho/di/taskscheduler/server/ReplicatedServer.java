package pt.uminho.di.taskscheduler.server;

import pt.uminho.di.taskscheduler.common.Scheduler;
import pt.uminho.di.taskscheduler.common.Task;
import pt.uminho.di.taskscheduler.common.requests.*;
import pt.uminho.di.taskscheduler.server.requests.*;

import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import pt.haslab.ekit.Spread;
import spread.SpreadException;
import spread.SpreadMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReplicatedServer {

    /* The server group where client messages are received */
    private static final String SERVER_GROUP = "srv_group";

    /* This group will be useful to know when clients fail */
    private static final String CLIENT_GROUP = "cli_group";

    private Scheduler scheduler;
    private Spread spread;
    private SingleThreadContext tc;
    private CompletableFuture<StateRep> stateTransfer;
    private int myId;

    public ReplicatedServer(int serverId) {
        tc = new SingleThreadContext("proc-%d", new Serializer());
        try {
            spread = new Spread("proc" + serverId, true);
        } catch (SpreadException se) {
            System.out.println("Exception on spread:");
            System.out.println(se.getMessage());
        }
        this.myId = serverId;
        register();
    }

    private void register() {
        tc.serializer().register(FinalizeTaskRep.class);
        tc.serializer().register(FinalizeTaskReq.class);
        tc.serializer().register(NewTaskReq.class);
        tc.serializer().register(NewTaskRep.class);
        tc.serializer().register(NextTaskReq.class);
        tc.serializer().register(NextTaskRep.class);
        tc.serializer().register(StateReq.class);
        tc.serializer().register(StateRep.class);
    }

    public void handleNewTask(SpreadMessage m, NewTaskReq v) {
        boolean res = scheduler.addNewTask(v.task);
        NewTaskRep rep = new NewTaskRep(res);
        SpreadMessage m2 = new SpreadMessage();
        m2.addGroup(m.getSender());
        m2.setReliable();
        spread.multicast(m2, rep);
    }

    public void handleNextTask(SpreadMessage m, NextTaskReq v) {
        Task task = scheduler.getTask(m.getSender().toString());
        NextTaskRep rep = new NextTaskRep(task);
        SpreadMessage m2 = new SpreadMessage();
        m2.addGroup(m.getSender());
        m2.setReliable();
        spread.multicast(m2, rep);
    }

    public void handleFinalizeTask(SpreadMessage m, FinalizeTaskReq v) {
        String client = m.getSender().toString();
        boolean res = scheduler.setFinalizedTask(client, v.finalizedTaskUrl);
        FinalizeTaskRep rep = new FinalizeTaskRep(res);
        SpreadMessage m2 = new SpreadMessage();
        m2.addGroup(m.getSender());
        m2.setReliable();
        spread.multicast(m2, rep);
    }

    private void runningHandlers() {
        spread.handler(NewTaskReq.class, this::handleNewTask);
        spread.handler(NextTaskReq.class, this::handleNextTask);
        spread.handler(FinalizeTaskReq.class, this::handleFinalizeTask);
        spread.handler(StateReq.class, (m, v) -> {
            StateRep state = new StateRep();
            SpreadMessage m2 = new SpreadMessage();
            m2.addGroup(m.getSender());
            m2.setAgreed();
            spread.multicast(m2, state);
        });
    }

    private void stateHandlers(List<RequestInfo> requests) {
        spread.handler(NewTaskReq.class, (m, v) ->
            requests.add(new RequestInfo(m, v))
        );
        spread.handler(NextTaskReq.class, (m, v) ->
            requests.add(new RequestInfo(m, v))
        );
        spread.handler(FinalizeTaskReq.class, (m, v) ->
            requests.add(new RequestInfo(m, v))
        );
        spread.handler(StateRep.class, (m, v) -> {
            /* This is temporary, implement fragmented state transfer */
            this.scheduler = v.scheduler;
            stateTransfer.complete(v);
        });
    }

    private void handleRequest(RequestInfo reqInfo) {
        if (reqInfo.request instanceof NewTaskReq) {
            NewTaskReq req = (NewTaskReq) reqInfo.request;
            handleNewTask(reqInfo.messageInfo, req);
        }
        else if (reqInfo.request instanceof NextTaskReq) {
            NextTaskReq req = (NextTaskReq) reqInfo.request;
            handleNextTask(reqInfo.messageInfo, req);
        }
        else if (reqInfo.request instanceof FinalizeTaskReq) {
            FinalizeTaskReq req = (FinalizeTaskReq) reqInfo.request;
            handleFinalizeTask(reqInfo.messageInfo, req);
        }
    }

    public void start() {
        tc.execute(() -> {
            try {
                if(myId == 0) runningHandlers();
                else {
                    List<RequestInfo> requests = new ArrayList<>();
                    stateTransfer = new CompletableFuture<>();
                    stateHandlers(requests);
                    stateTransfer.thenRun(() -> {
                        runningHandlers();
                        for (RequestInfo r : requests)
                            handleRequest(r);
                    });
                }
                spread.open().thenRun(() -> System.out.println("starting")).get();
                spread.join(SERVER_GROUP);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
