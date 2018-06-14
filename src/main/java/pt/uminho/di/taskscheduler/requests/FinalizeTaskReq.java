package pt.uminho.di.taskscheduler.requests;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class FinalizeTaskReq implements CatalystSerializable {
    public String finalizedTaskUrl;

    public FinalizeTaskReq(String finalizedTask) {
        this.finalizedTaskUrl = finalizedTask;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(finalizedTaskUrl);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        this.finalizedTaskUrl = bufferInput.readString();
    }
}
