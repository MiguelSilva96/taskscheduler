package pt.uminho.di.taskscheduler.server;

import pt.uminho.di.taskscheduler.common.Constants;
import pt.uminho.di.taskscheduler.common.Scheduler;
import pt.uminho.di.taskscheduler.common.Task;
import pt.uminho.di.taskscheduler.common.requests.*;
import pt.uminho.di.taskscheduler.server.requests.*;

import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import pt.haslab.ekit.Spread;
import spread.MembershipInfo;
import spread.SpreadException;
import spread.SpreadMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReplicatedServer {

    private Scheduler scheduler;
    private Spread spread;
    private SingleThreadContext tc;
    private CompletableFuture<StateFragment> stateTransfer;
    private int myId, orderForState;
    private ServerLeadershipManager membershipManager;

    public ReplicatedServer(int serverId) {
        tc = new SingleThreadContext("proc-%d", new Serializer());
        try {
            spread = new Spread("proc" + serverId, true);
        } catch (SpreadException se) {
            System.out.println("Exception on spread:");
            System.out.println(se.getMessage());
        }
        this.myId = serverId;
        this.orderForState = 0;
        this.membershipManager = new ServerLeadershipManager(spread);
        this.scheduler = new SchedulerImpl();
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
        tc.serializer().register(StateFragment.class);
        //tc.serializer().register(StateRep.class);
        tc.serializer().register(UpToDate.class);
        tc.serializer().register(ClientLeft.class);
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

    private void handleUpToDate(SpreadMessage m, UpToDate v) {
        membershipManager.addMember(m.getSender().toString());
        membershipManager.electLeader();
    }

    private void handleClientLeft(SpreadMessage m, ClientLeft v) {
        ((SchedulerImpl)scheduler).freeClientAssignedTasks(v.client);
        membershipManager.clientInfoReceived(v.client);
    }

    private void runningHandlers() {
        spread.handler(NewTaskReq.class, this::handleNewTask);
        spread.handler(NextTaskReq.class, this::handleNextTask);
        spread.handler(FinalizeTaskReq.class, this::handleFinalizeTask);
        spread.handler(UpToDate.class, this::handleUpToDate);
        spread.handler(ClientLeft.class, this::handleClientLeft);
        spread.handler(MembershipInfo.class, this::handleMembership);
        spread.handler(StateReq.class, (m, v) -> {
            List<String> members = membershipManager.getMembers();
            List<StateFragment> fs = ((SchedulerImpl)scheduler).getFragments();
            fs.get(0).members = members;
            for(StateFragment f : fs) {
                SpreadMessage m2 = new SpreadMessage();
                m2.addGroup(m.getSender());
                m2.setAgreed();
                spread.multicast(m2, f);
            }
        });
        // overwrite the handler for state transfer
        spread.handler(StateFragment.class, (m, v) -> {});
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
                spread.handler(UpToDate.class, (m2, v2) ->
                        requests.add(new RequestInfo(m2, v2))
                );
                spread.handler(ClientLeft.class, (m2, v2) ->
                        requests.add(new RequestInfo(m2, v2))
                );
                spread.handler(MembershipInfo.class, (m2, v2) ->
                        requests.add(new RequestInfo(m2, v2))
                );
            }
        });
        spread.handler(StateFragment.class, (m, v) -> {
            if (orderForState++ == v.msgNum) {
                if(v.msgNum == 0) {
                    membershipManager.setMembers(v.members);
                    membershipManager.electLeader();
                }
                ((SchedulerImpl)scheduler).addFragment(v);
                if(v.isLast)
                    stateTransfer.complete(v);
            }
        });
        /*
        spread.handler(StateRep.class, (m, v) -> {
            This is temporary, implement fragmented state transfer
            this.scheduler = v.scheduler;
            membershipManager.setMembers(v.members);
            membershipManager.electLeader();
            stateTransfer.complete(v);
        });
        */
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
        else if (reqInfo.request instanceof ClientLeft) {
            ClientLeft req = (ClientLeft) reqInfo.request;
            handleClientLeft(reqInfo.messageInfo, req);
        }
        else if(reqInfo.request instanceof UpToDate) {
            UpToDate req = (UpToDate) reqInfo.request;
            handleUpToDate(reqInfo.messageInfo, req);
        }
        else if (reqInfo.request instanceof MembershipInfo) {
            MembershipInfo req = (MembershipInfo) reqInfo.request;
            handleMembership(reqInfo.messageInfo, req);
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
        m.addGroup(Constants.SERVER_GROUP);
        m.setAgreed();
        spread.multicast(m, new ClientLeft(client));
    }

    private void handleServerMembership(MembershipInfo mInfo) {
        if(mInfo.isCausedByJoin());
        else if(mInfo.isCausedByLeave()) {
            // remove from server list
            membershipManager.removeMember(mInfo.getLeft().toString());
            membershipManager.electLeader();
        }
        else if(mInfo.isCausedByDisconnect()) {
            membershipManager.removeMember(mInfo.getDisconnected().toString());
            membershipManager.electLeader();
        }
    }

    private void handleMembership(SpreadMessage m, MembershipInfo v) {
        if(v.isRegularMembership()) {
            String group;
            group = v.getGroup().toString();
            if (group.equals(Constants.CLIENT_GROUP))
                if(membershipManager.leader()) {
                    membershipManager.addMembershipInfo(v);
                    handleClientMembership(v);
                }
                else
                    membershipManager.addMembershipInfo(v);
            else
                handleServerMembership(v);
        }
    }

    private void imUpToDate() {
        SpreadMessage m = new SpreadMessage();
        m.addGroup(Constants.SERVER_GROUP);
        m.setAgreed();
        spread.multicast(m, new UpToDate());
    }

    public void start() {
        tc.execute(() -> {
            try {
                if (myId == 0) {
                    runningHandlers();
                }
                else {
                    List<RequestInfo> requests = new ArrayList<>();
                    stateTransfer = new CompletableFuture<>();
                    stateHandlers(requests);
                    stateTransfer.thenRun(() -> {
                        runningHandlers();
                        for (RequestInfo r : requests)
                            handleRequest(r);
                        imUpToDate();
                        System.out.println("State transfer done");
                    });
                }

                spread.open().thenRun(() -> System.out.println("starting")).get();
                spread.join(Constants.SERVER_GROUP);
                spread.join(Constants.CLIENT_GROUP);

                if (myId != 0) {
                    StateReq sr = new StateReq();
                    SpreadMessage m = new SpreadMessage();
                    m.addGroup(Constants.SERVER_GROUP);
                    m.setAgreed();
                    spread.multicast(m, sr);
                } else {
                    String me = spread.getPrivateGroup().toString();
                    membershipManager.addMember(me);
                    membershipManager.electLeader();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
