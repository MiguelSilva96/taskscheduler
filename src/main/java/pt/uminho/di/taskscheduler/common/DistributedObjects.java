package pt.uminho.di.taskscheduler.common;

import pt.uminho.di.taskscheduler.common.requests.*;
import pt.uminho.di.taskscheduler.server.SchedulerImpl;
import pt.uminho.di.taskscheduler.server.requests.StateRep;

import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import pt.haslab.ekit.Spread;
import spread.SpreadException;
import spread.SpreadMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class DistributedObjects {
/* Will just leave this here if needed
    private static final String SERVER_GROUP = "srv_group";
    private static final String CLIENT_GROUP = "cli_group";

    private SingleThreadContext tc;
    private AtomicInteger id;
    private Spread spread;
    private Map<Integer, Object> objs;
    private CompletableFuture<StateRep> stateTransfer;

    public DistributedObjects(int myId) {
        objs = new HashMap<>();
        id = new AtomicInteger(0);
        tc = new SingleThreadContext("proc-%d", new Serializer());
        try {
            Spread spread = new Spread("proc" + myId, true);
        } catch (SpreadException se) {
            System.out.println("Exception on spread:");
            System.out.println(se.getMessage());
        }
        register();
    }

    private void register() {
        tc.serializer().register(FinalizeTaskRep.class);
        tc.serializer().register(FinalizeTaskReq.class);
        tc.serializer().register(NewTaskReq.class);
        tc.serializer().register(NewTaskRep.class);
        tc.serializer().register(NextTaskReq.class);
        tc.serializer().register(NextTaskRep.class);
    }

    private void runningHandlers() {
        SchedulerImpl scheduler = (SchedulerImpl) objs.get(1);
        spread.handler(NewTaskReq.class, (m, v) -> {
            ...
            int balance = bank.balance();
            BalanceRep rep = new BalanceRep(balance, v.req);
            SpreadMessage m2 = new SpreadMessage();
            m2.addGroup(m.getSender());
            m2.setAgreed();
            s.multicast(m2, rep);
        });
        s.handler(MovementReq.class, (m, v) -> {
            boolean res = bank.movement(v.value);
            MovementRep rep = new MovementRep(res, v.req);
            SpreadMessage m2 = new SpreadMessage();
            m2.addGroup(m.getSender());
            m2.setAgreed();
            s.multicast(m2, rep);
        });
        s.handler(StateReq.class, (m, v) -> {
            StateRep state = new StateRep();
            SpreadMessage m2 = new SpreadMessage();
            m2.addGroup(m.getSender());
            m2.setAgreed();
            s.multicast(m2, state);
        });
    }

    public void initialize() {
        tc.execute(() -> {
            try {
                if(id == 0) runningHandlers();
                else {
                    stateTransfer = new CompletableFuture<>();
                    stateHandlers();
                    stateTransfer.thenRun(() -> {
                        runningHandlers();
                        for (RequestInfo r : requests)
                            handleRequest(r);
                    });
                }
                s.open().thenRun(() -> System.out.println("starting")).get();
                s.join("grupo");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }



    public ObjRef exportObj(Object o){

        objs.put(id.incrementAndGet(), o);

        if(o instanceof Scheduler)
            return new ObjRef(SERVER_GROUP, id.get(), "scheduler");
        else if(o instanceof Task)
            return new ObjRef(SERVER_GROUP, id.get(), "task");
        return null;
    }

    public Object importObj(ObjRef o){

        if(o.cls.equals("scheduler"))
            return new RemoteScheduler(tc, spread, SERVER_GROUP);

        if(o.cls.equals("task")){
            return new RemoteTask(tc, spread, SERVER_GROUP);
        }

        return null;

    }
*/
}
