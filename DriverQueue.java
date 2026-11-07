import java.util.*;

public class DriverQueue {
    private final Map<String, Deque<Van>> queuesByDestination = new LinkedHashMap<>();

    public void enqueueIfAbsent(String destination, Van van) {
        Deque<Van> queue = queuesByDestination.computeIfAbsent(destination, k -> new ArrayDeque<>());
        if (!queue.contains(van)) {
            queue.addLast(van);
        }
    }

    public void enqueue(String destination, Van van) {
        Deque<Van> queue = queuesByDestination.computeIfAbsent(destination, k -> new ArrayDeque<>());
        queue.addLast(van);
    }

    public Van dequeue(String destination) {
        Deque<Van> queue = queuesByDestination.get(destination);
        if (queue == null || queue.isEmpty()) return null;
        return queue.removeFirst();
    }

    public Van peek(String destination) {
        Deque<Van> queue = queuesByDestination.get(destination);
        if (queue == null || queue.isEmpty()) return null;
        return queue.peekFirst();
    }

    public List<Van> getQueue(String destination) {
        Deque<Van> queue = queuesByDestination.get(destination);
        if (queue == null) return Collections.emptyList();
        return new ArrayList<>(queue);
    }

    public int positionOf(String destination, Van van) {
        List<Van> list = getQueue(destination);
        int index = list.indexOf(van);
        return (index == -1) ? -1 : index + 1; // 1-indexed queue position
    }

    public int size(String destination) {
        Deque<Van> queue = queuesByDestination.get(destination);
        return (queue == null) ? 0 : queue.size();
    }

    public Map<String, Deque<Van>> getAllQueues() {
        return queuesByDestination;
    }
}