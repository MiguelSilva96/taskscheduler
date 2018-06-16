package pt.uminho.di.taskscheduler.common.requests;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import pt.uminho.di.taskscheduler.common.Task;

public class NextTaskRep implements CatalystSerializable {

    public Task task;
    public int request;

    public NextTaskRep() { }

    public NextTaskRep(Task task, int request) {
        this.task = task;
        this.request = request;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        serializer.writeObject(task, bufferOutput);
        bufferOutput.writeInt(request);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        task = serializer.readObject(bufferInput);
        request = bufferInput.readInt();
    }
}
