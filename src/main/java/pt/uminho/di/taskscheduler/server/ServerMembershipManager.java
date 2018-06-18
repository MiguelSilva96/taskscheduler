package pt.uminho.di.taskscheduler.server;

import spread.MembershipInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ServerMembershipManager {
    private HashSet<String> members;
    private boolean imLeader;
    private List<MembershipInfo> clientInfo;

    public ServerMembershipManager() {
        members = new HashSet<>();
        clientInfo = new ArrayList<>();
        imLeader = false;
    }

    public void setImLeader(boolean imLeader) {
        this.imLeader = imLeader;
    }

    public void addMember(String member) {
        members.add(member);
    }

    public void removeMember(String member) {
        members.remove(member);
    }



}
