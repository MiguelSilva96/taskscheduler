package pt.uminho.di.taskscheduler.server;

import java.util.ArrayList;
import java.util.List;

public class StateTransferManager {
    private List<RequestInfo> requests;
    private List<Integer> retransmissions;

    public StateTransferManager() {
        requests = new ArrayList<>();
        retransmissions = new ArrayList<>();
    }

    public void addRequest(RequestInfo ri) {
        requests.add(ri);
    }

    public List<RequestInfo> getRequests() {
        return requests;
    }

    public void updateRetransmissions() {
        retransmissions.add(requests.size());
    }




}
