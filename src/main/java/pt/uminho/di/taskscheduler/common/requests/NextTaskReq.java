package pt.uminho.di.taskscheduler.common.requests;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class NextTaskReq implements CatalystSerializable {

    public int request;

    public NextTaskReq() { }

    public NextTaskReq(int request) {
        this.request = request;
    }


    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeInt(request);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        this.request = bufferInput.readInt();
    }
}
