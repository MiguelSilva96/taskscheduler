package pt.uminho.di.taskscheduler.common.requests;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class FinalizeTaskReq implements CatalystSerializable {
    public String finalizedTaskUrl;
    public int request;

    public FinalizeTaskReq() { }

    public FinalizeTaskReq(String finalizedTask, int request) {
        this.finalizedTaskUrl = finalizedTask;
        this.request = request;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(finalizedTaskUrl);
        bufferOutput.writeInt(request);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        this.finalizedTaskUrl = bufferInput.readString();
        this.request = bufferInput.readInt();
    }
}
