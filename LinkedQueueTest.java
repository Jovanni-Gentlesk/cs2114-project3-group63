package serviceflow.model;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinkedQueueTest {
    private final LinkedQueue<String> queue = new LinkedQueue<>();

    @Test
    void enqueuePeekAndDequeuePreserveFifo() {
        assertTrue(queue.enqueue("Ana"));
        assertTrue(queue.enqueue("Luis"));
        assertEquals("Ana", queue.peek());
        assertEquals(2, queue.size());
        assertEquals("Ana", queue.dequeue());
        assertEquals("Luis", queue.dequeue());
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    void queueCanBeRefilledAfterRemovingTheOnlyEntry() {
        queue.enqueue("Ana");
        assertEquals("Ana", queue.dequeue());
        queue.enqueue("Luis");
        assertEquals("Luis", queue.peek());
        assertEquals("Luis", queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void clearResetsBothEndsAndCount() {
        queue.enqueue("Ana");
        queue.enqueue("Luis");
        queue.clear();
        queue.clear();

        assertEquals("[]", queue.toString());
        assertArrayEquals(new Object[0], queue.toArray());

        queue.enqueue("Mia");
        assertEquals("Mia", queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void emptyOperationsAndNullEntryAreRejected() {
        assertThrows(NoSuchElementException.class, queue::peek);
        assertThrows(NoSuchElementException.class, queue::dequeue);
        assertThrows(NoSuchElementException.class, () -> queue.iterator().next());
        assertThrows(IllegalArgumentException.class, () -> queue.enqueue(null));
        assertTrue(queue.isEmpty());

        queue.enqueue("Ana");
        assertThrows(IllegalArgumentException.class, () -> queue.enqueue(null));
        assertArrayEquals(new Object[] {"Ana"}, queue.toArray());
    }

    @Test
    void arrayAndIteratorAreIndependentSnapshots() {
        queue.enqueue("Ana");
        queue.enqueue("Luis");
        assertEquals("[Ana, Luis]", queue.toString());

        Object[] array = queue.toArray();
        Iterator<String> iterator = queue.iterator();
        array[0] = "Mia";

        assertEquals("Ana", queue.dequeue());
        assertEquals("Ana", iterator.next());
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        assertEquals("Luis", iterator.next());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
        assertEquals("Luis", queue.peek());
    }
}
