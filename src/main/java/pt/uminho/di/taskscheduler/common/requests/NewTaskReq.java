package pt.uminho.di.taskscheduler.common.requests;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class NewTaskReq implements CatalystSerializable {
    public String task;
    public int request;

    public NewTaskReq() { }
    public NewTaskReq(String task, int request){
        this.task = task;
        this.request = request;
    }
    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(task);
        bufferOutput.writeInt(request);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        this.task = bufferInput.readString();
        this.request = bufferInput.readInt();
    }
}
