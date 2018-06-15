package pt.uminho.di.taskscheduler.server.requests;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import pt.uminho.di.taskscheduler.common.Task;
import pt.uminho.di.taskscheduler.server.SchedulerImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StateRep implements CatalystSerializable {
    public SchedulerImpl scheduler;

    public StateRep() {}
    public StateRep(SchedulerImpl scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        serializer.writeObject(scheduler, bufferOutput);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        scheduler = serializer.readObject(bufferInput);
    }
}
