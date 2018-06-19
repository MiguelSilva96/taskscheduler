package pt.uminho.di.taskscheduler.server.requests;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import pt.uminho.di.taskscheduler.common.Task;
import pt.uminho.di.taskscheduler.server.SchedulerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/* This class was used for state transfer without fragmentation
* Not in use at the current version
* */
public class StateRep implements CatalystSerializable {
    public List<String> members;
    public SchedulerImpl scheduler;

    public StateRep() {}
    public StateRep(SchedulerImpl scheduler, List<String> members) {
        this.scheduler = scheduler;
        this.members = members;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        serializer.writeObject(scheduler, bufferOutput);
        bufferOutput.writeInt(members.size());
        for(String m : members)
            bufferOutput.writeString(m);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        int size;
        members = new ArrayList<>();
        scheduler = serializer.readObject(bufferInput);
        size = bufferInput.readInt();
        for(int i = 0; i < size; i++)
            members.add(bufferInput.readString());
    }
}
