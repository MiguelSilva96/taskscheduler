package pt.uminho.di.taskscheduler.server.requests;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import pt.uminho.di.taskscheduler.common.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StateFragment implements CatalystSerializable {
    // considering errors in our rough approximation
    // we will count only until 50kb just to be sure because we are
    public List<Task> nextTasks; // size tasks + meta info about size 4bytes
    public Map<String, List<Task>> assignedTasks; // size key + size tasks + meta about size 4bytes
    public int tasksId; // 4 bytes
    public boolean isLast; // 1 byte
    public int msgNum; // 4 bytes

    // used only on fragment 0
    // considering that the server list won't be very large, we assume
    // that there will be space for it (because we only count size until 50kb roughly)
    public List<String> members;

    public StateFragment() {
        nextTasks = new ArrayList<>();
        assignedTasks = new HashMap<>();
        members = new ArrayList<>();
        isLast = false;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        // sum all and multiply by task * size()
        bufferOutput.writeInt(assignedTasks.keySet().size()); // 4 bytes
        for(String key : assignedTasks.keySet()) {
            bufferOutput.writeString(key); // size(key)
            bufferOutput.writeInt(assignedTasks.get(key).size()); // 4bytes
            for(Task task : assignedTasks.get(key))
                serializer.writeObject(task, bufferOutput); // 100 bytes + size(url)
            // sum all and multiply by task * size()
        }
        // sum all from last for cicle
        bufferOutput.writeBoolean(isLast); // 1 byte
        bufferOutput.writeInt(msgNum);
        bufferOutput.writeInt(tasksId); // 4 bytes

        bufferOutput.writeInt(members.size());
        for(String m : members)
            bufferOutput.writeString(m);

        bufferOutput.writeInt(nextTasks.size()); // 4bytes
        for(Task task : nextTasks)
            serializer.writeObject(task, bufferOutput); // 100 bytes + size(url)
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {

        int size = bufferInput.readInt();
        for(int i = 0; i < size; i++) {
            List<Task> clientTasks = new ArrayList<>();
            String key = bufferInput.readString();
            int task_size = bufferInput.readInt();
            for(int j = 0; j < task_size; j++) {
                Task t = serializer.readObject(bufferInput);
                clientTasks.add(t);
            }
            assignedTasks.put(key, clientTasks);
        }

        isLast = bufferInput.readBoolean();
        msgNum = bufferInput.readInt();
        tasksId = bufferInput.readInt();

        size = bufferInput.readInt();
        for(int i = 0; i < size; i++) {
            members.add(bufferInput.readString());
        }

        size = bufferInput.readInt();
        for(int i = 0; i < size; i++) {
            nextTasks.add(serializer.readObject(bufferInput));
        }

    }
}
