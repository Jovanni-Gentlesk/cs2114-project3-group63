package serviceflow.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/** A singly linked FIFO queue with constant-time enqueue, dequeue, and peek. */
public final class LinkedQueue<T> implements Iterable<T> {
    private Node<T> firstNode;
    private Node<T> lastNode;
    private int numberOfEntries;

    private static final class Node<T> {
        private final T entry;
        private Node<T> next;

        private Node(T entry) {
            this.entry = entry;
        }
    }

    public boolean enqueue(T newEntry) {
        if (newEntry == null) {
            throw new IllegalArgumentException("A queue entry is required.");
        }

        Node<T> node = new Node<>(newEntry);
        if (lastNode == null) {
            firstNode = node;
        }
        else {
            lastNode.next = node;
        }

        lastNode = node;
        numberOfEntries++;
        return true;
    }

    public T dequeue() {
        T entry = peek();
        firstNode = firstNode.next;
        numberOfEntries--;

        if (firstNode == null) {
            lastNode = null;
        }

        return entry;
    }

    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("No jobs are waiting.");
        }
        return firstNode.entry;
    }

    public void clear() {
        firstNode = null;
        lastNode = null;
        numberOfEntries = 0;
    }

    public int size() {
        return numberOfEntries;
    }

    public boolean isEmpty() {
        return numberOfEntries == 0;
    }

    public Object[] toArray() {
        return snapshot().toArray();
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(snapshot()).iterator();
    }

    @Override
    public String toString() {
        return snapshot().toString();
    }

    private List<T> snapshot() {
        List<T> entries = new ArrayList<>(numberOfEntries);

        for (Node<T> node = firstNode; node != null; node = node.next) {
            entries.add(node.entry);
        }

        return entries;
    }
}
