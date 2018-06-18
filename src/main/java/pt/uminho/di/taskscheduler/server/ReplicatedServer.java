package pt.uminho.di.taskscheduler.server;

import pt.uminho.di.taskscheduler.common.Scheduler;
import pt.uminho.di.taskscheduler.common.Task;
import pt.uminho.di.taskscheduler.common.requests.*;
import pt.uminho.di.taskscheduler.server.requests.*;

import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import pt.haslab.ekit.Spread;
import spread.MembershipInfo;
import spread.SpreadException;
import spread.SpreadGroup;
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
    private boolean imLeader;

    public ReplicatedServer(int serverId) {
        tc = new SingleThreadContext("proc-%d", new Serializer());
        try {
            spread = new Spread("proc" + serverId, true);
        } catch (SpreadException se) {
            System.out.println("Exception on spread:");
            System.out.println(se.getMessage());
        }
        this.myId = serverId;
        this.imLeader = false;
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

    private void handleNewTask(SpreadMessage m, NewTaskReq v) {
        boolean res = scheduler.addNewTask(v.task);
        NewTaskRep rep = new NewTaskRep(res, v.request);
        SpreadMessage m2 = new SpreadMessage();
        m2.addGroup(m.getSender());
        m2.setReliable();
        spread.multicast(m2, rep);
    }

    private void handleNextTask(SpreadMessage m, NextTaskReq v) {
        Task task = scheduler.getTask(m.getSender().toString());
        NextTaskRep rep = new NextTaskRep(task, v.request);
        SpreadMessage m2 = new SpreadMessage();
        m2.addGroup(m.getSender());
        m2.setReliable();
        spread.multicast(m2, rep);
    }

    private void handleFinalizeTask(SpreadMessage m, FinalizeTaskReq v) {
        String client = m.getSender().toString();
        boolean res = scheduler.setFinalizedTask(client, v.finalizedTaskUrl);
        FinalizeTaskRep rep = new FinalizeTaskRep(res, v.request);
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
            StateRep state = new StateRep((SchedulerImpl) scheduler);
            SpreadMessage m2 = new SpreadMessage();
            m2.addGroup(m.getSender());
            m2.setAgreed();
            spread.multicast(m2, state);
        });
    }

    private void stateHandlers(List<RequestInfo> requests) {
        spread.handler(StateReq.class, (m, v) -> {
            /*
               Lets check if this is our request,
               if it is, that means we need to start saving
               all the requests to execute after state transfer
            */
            if(m.getSender().equals(spread.getPrivateGroup())) {
                spread.handler(NewTaskReq.class, (m2, v2) ->
                        requests.add(new RequestInfo(m2, v2))
                );
                spread.handler(NextTaskReq.class, (m2, v2) ->
                        requests.add(new RequestInfo(m2, v2))
                );
                spread.handler(FinalizeTaskReq.class, (m2, v2) ->
                        requests.add(new RequestInfo(m2, v2))
                );
            }
        });
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
        else if(reqInfo.request instanceof MembershipInfo) {

        }
    }

    private void handleClientMembership(MembershipInfo mInfo) {
        /*
           Not checking if it was a server because:
           If it is a server that left we can do the same process
           because there won't be tasks for the server, so nothing
           bad will happen. It also doesn't impact the performance
           that much because it just checks if that "client" has
           tasks in a map (~=O(1)) and a server will never have.
        */
        String client = "";

        if(mInfo.isCausedByDisconnect())
            client = mInfo.getDisconnected().toString();
        else if(mInfo.isCausedByLeave())
            client = mInfo.getLeft().toString();
        // Still needs to handle network partition
        else return;

        SpreadMessage m = new SpreadMessage();
        m.addGroup(SERVER_GROUP);
        m.setAgreed();
        spread.multicast(m, new ClientLeft(client));
    }

    private void leaderElection() {

    }

    private void handleServerMembership(MembershipInfo mInfo) {
        if(mInfo.isCausedByJoin());
        else if(mInfo.isCausedByLeave()) {
            // remove from server list
            leaderElection();
        }
    }

    private void membershipHandler() {
        spread.handler(MembershipInfo.class, (m, v) -> {
            if(v.isRegularMembership()) {
                String group;
                group = v.getGroup().toString();
                if (group.equals(CLIENT_GROUP) && imLeader)
                    handleClientMembership(v);
                else
                    handleServerMembership(v);
            }
        });
    }

    public void start() {
        tc.execute(() -> {
            try {
                membershipHandler();
                if (myId == 0) {
                    runningHandlers();
                    this.imLeader = true;
                }
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
                spread.join(CLIENT_GROUP);

                if (myId != 0) {
                    StateReq sr = new StateReq();
                    SpreadMessage m = new SpreadMessage();
                    m.addGroup(SERVER_GROUP);
                    m.setAgreed();
                    spread.multicast(m, sr);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
