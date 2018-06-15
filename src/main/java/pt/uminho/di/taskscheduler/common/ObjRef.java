package pt.uminho.di.taskscheduler.common;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

// Just leaving this here if needed
public class ObjRef implements CatalystSerializable {
    public String spreadGroup;
    public int id;
    public String cls;

    public ObjRef() { }

    public ObjRef(String spreadGroup, int id, String cls) {
        this.spreadGroup = spreadGroup;
        this.id = id;
        this.cls = cls;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(cls);
        bufferOutput.writeInt(id);
        bufferOutput.writeString(cls);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        spreadGroup = bufferInput.readString();
        id = bufferInput.readInt();
        cls = bufferInput.readString();
    }
}