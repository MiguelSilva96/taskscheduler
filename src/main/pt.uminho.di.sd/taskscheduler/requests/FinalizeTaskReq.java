package taskscheduler.requests;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class FinalizeTaskReq implements CatalystSerializable {
    public String finalizedTask;

    public FinalizeTaskReq(String finalizedTask) {
        this.finalizedTask = finalizedTask;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(finalizedTask);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        this.finalizedTask = bufferInput.readString();
    }
}
