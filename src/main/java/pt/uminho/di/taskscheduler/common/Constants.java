package pt.uminho.di.taskscheduler.common;

public class Constants {
    /* The server group where client requests are received */
    public static final String SERVER_GROUP = "srv_group";

    /* This group will be useful to know when clients fail */
    public static final String CLIENT_GROUP = "cli_group";

    /* The limit for the task description */
    public static final short TASK_SIZE = 100;

    /* Limit size for StateFragment */
    public static final int FRAGMENT_SIZE = 50000; // ~=50kb

    /* Minimum METAINFO in a fragment */
    public static final short MIN_META_SIZE = 21;
}
