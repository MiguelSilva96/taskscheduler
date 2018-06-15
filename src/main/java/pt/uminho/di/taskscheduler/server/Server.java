package pt.uminho.di.taskscheduler.server;

public class Server {
    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        ReplicatedServer rs = new ReplicatedServer(id);
        rs.start();
    }
}
