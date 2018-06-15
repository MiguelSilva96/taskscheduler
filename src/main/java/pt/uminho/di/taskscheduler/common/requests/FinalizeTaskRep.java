package pt.uminho.di.taskscheduler.common.requests;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class FinalizeTaskRep implements CatalystSerializable {

    public boolean success;

    public FinalizeTaskRep() { }

    public FinalizeTaskRep(boolean success) {
        this.success = success;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeBoolean(success);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        this.success = bufferInput.readBoolean();
    }
}