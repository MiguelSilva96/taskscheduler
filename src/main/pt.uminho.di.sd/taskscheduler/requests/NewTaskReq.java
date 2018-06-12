package taskscheduler.requests;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class NewTaskReq implements CatalystSerializable {
    public String task;
    public NewTaskReq(String task){
        this.task = task;
    }
    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(task);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        this.task = bufferInput.readString();
    }
}
