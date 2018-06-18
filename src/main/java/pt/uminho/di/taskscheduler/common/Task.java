package pt.uminho.di.taskscheduler.common;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class Task implements CatalystSerializable, Comparable<Task> {

    private String name;
    private String url;

    public Task (String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(name);
        bufferOutput.writeString(url);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        name = bufferInput.readString();
        url = bufferInput.readString();
    }

    @Override
    public int compareTo(Task t) {
        String s1 = t.getUrl();
        int id1 = Integer.parseInt((String) this.url.subSequence(4, this.url.length()));
        int id2 = Integer.parseInt((String) s1.subSequence(4, s1.length()));
        return id1>id2?1:id2==id1?0:-1;
    }
}
