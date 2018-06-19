package pt.uminho.di.taskscheduler.server;


import pt.haslab.ekit.Spread;
import pt.uminho.di.taskscheduler.common.Constants;
import pt.uminho.di.taskscheduler.server.requests.ClientLeft;
import spread.MembershipInfo;
import spread.SpreadMessage;

import java.util.*;

public class ServerLeadershipManager {
    private TreeSet<String> members;
    private boolean imLeader;
    private List<String> clientsLeftNotOrdered;
    private Spread spread;

    public ServerLeadershipManager(Spread spread) {
        members = new TreeSet<>();
        clientsLeftNotOrdered = new ArrayList<>();
        this.spread = spread;
    }

    public void electLeader() {
        String leader = members.first();
        String me = spread.getPrivateGroup().toString();
        if (leader.equals(me))
            imLeader();
    }

    private void imLeader() {
        this.imLeader = true;
        System.out.println("im the leader");
        // send all messages about clients leaving
        // that still haven't been resent to the server group
        for (String client : clientsLeftNotOrdered) {
            SpreadMessage m = new SpreadMessage();
            m.addGroup(Constants.SERVER_GROUP);
            m.setAgreed();
            spread.multicast(m, new ClientLeft(client));
        }
    }

    public void addMember(String member) {
        members.add(member);
    }

    public void removeMember(String member) {
        members.remove(member);
    }

    public boolean leader() {
        return imLeader;
    }

    public void addMembershipInfo(MembershipInfo mInfo) {
        String client;
        if (mInfo.isCausedByDisconnect()) {
            client = mInfo.getDisconnected().toString();
        }
        else if (mInfo.isCausedByLeave()) {
            client = mInfo.getLeft().toString();
        }
        else return;
        clientsLeftNotOrdered.add(client);
    }

    public List<String> getMembers() {
        List<String> r = new ArrayList<>();
        for(String m : members)
            r.add(m);
        return r;
    }

    public void setMembers(List<String> members) {
        for (String m : members)
            this.members.add(m);
    }

    public void clientInfoReceived(String client) {
        for (String c : clientsLeftNotOrdered) {
            if (c.equals(client)) {
                clientsLeftNotOrdered.remove(c);
                return;
            }

        }
    }
}
