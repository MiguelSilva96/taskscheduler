package pt.uminho.di.taskscheduler.server.requests;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class ClientLeft implements CatalystSerializable {
    public String client;

    public ClientLeft() { }

    public ClientLeft(String client) {
        this.client = client;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(client);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        this.client = bufferInput.readString();
    }
}
