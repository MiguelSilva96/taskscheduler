package pt.uminho.di.taskscheduler.server;

import spread.SpreadMessage;

public class RequestInfo {
    protected SpreadMessage messageInfo;
    protected Object request;

    public RequestInfo(SpreadMessage messageInfo, Object request) {
        this.messageInfo = messageInfo;
        this.request = request;
    }
}
