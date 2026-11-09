package serviceflow.model;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceManagerTest {
    private final ServiceManager manager = new ServiceManager();

    @Test
    void twoJobsStartInOrderAndPaymentMovesTheBillToCollected() {
        Job ana = addAna();
        Job luis = manager.addJob("Luis", "Hedges", 1, 25, 0);
        Iterator<Job> waiting = manager.iterator();

        assertSame(ana, manager.peek());
        assertEquals(2, manager.jobCount());
        assertEquals(0, manager.getUnpaidTotal());

        Job active = manager.startNextJob();
        assertEquals(Job.ACTIVE, active.getStatus());
        assertSame(active, manager.getActiveJob());
        assertSame(luis, manager.peek());
        assertNull(active.getBill());
        assertEquals(0, manager.getUnpaidTotal());
        assertSame(ana, waiting.next());
        assertThrows(UnsupportedOperationException.class, waiting::remove);
        assertSame(luis, waiting.next());

        Job completed = manager.completeActiveJob();
        assertNull(manager.getActiveJob());
        assertEquals(Job.COMPLETED, completed.getStatus());
        assertEquals(115.0, completed.getBill().doubleValue());
        assertEquals(115, manager.getUnpaidTotal());
        assertEquals(0, manager.getCollectedTotal());

        Job paid = manager.markPaid(ana.getID());
        assertEquals(Job.PAID, paid.getStatus());
        assertEquals(completed.getBill(), paid.getBill());
        assertEquals(0, manager.getUnpaidTotal());
        assertEquals(115, manager.getCollectedTotal());
        assertEquals(Job.WAITING, ana.getStatus());
        assertEquals(Job.COMPLETED, completed.getStatus());

        assertEquals(luis.getID(), manager.removeJob().getID());
        Job luisCompleted = manager.completeActiveJob();
        assertEquals(List.of(paid, luisCompleted), manager.getHistory());
        assertEquals(25, manager.getUnpaidTotal());
        assertTrue(manager.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> manager.getHistory().clear());
    }

    @Test
    void rejectedActionsPreserveJobsOrderAndTotals() {
        assertThrows(NoSuchElementException.class, manager::peek);
        assertThrows(NoSuchElementException.class, manager::startNextJob);
        assertThrows(IllegalStateException.class, manager::completeActiveJob);

        Job ana = addAna();
        Job luis = manager.addJob("Luis", "Hedges", 1, 25, 0);
        assertThrows(IllegalStateException.class, () -> manager.markPaid(ana.getID()));

        Job active = manager.startNextJob();
        assertThrows(IllegalStateException.class, manager::startNextJob);
        assertThrows(IllegalStateException.class, () -> manager.markPaid(ana.getID()));
        assertThrows(IllegalArgumentException.class, () -> manager.markPaid(999));
        assertThrows(IllegalArgumentException.class, () -> manager.getJob(0));
        assertSame(active, manager.getActiveJob());
        assertSame(luis, manager.peek());
        assertEquals(1, manager.jobCount());
        assertEquals(0, manager.getUnpaidTotal());

        manager.completeActiveJob();
        assertThrows(IllegalStateException.class, manager::completeActiveJob);

        manager.markPaid(ana.getID());
        assertThrows(IllegalStateException.class, () -> manager.markPaid(ana.getID()));
        assertEquals(115, manager.getCollectedTotal());
        assertEquals(0, manager.getUnpaidTotal());
    }

    @Test
    void invalidAddsDoNotUseAnIdOrChangeTheWaitingLine() {
        assertThrows(IllegalArgumentException.class,
            () -> manager.addJob(" ", "Lawn", 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> manager.addJob(null));

        Job ana = addAna();
        assertEquals(1, ana.getID());
        assertThrows(IllegalArgumentException.class, () -> manager.addJob(ana));
        assertSame(ana, manager.peek());
        assertEquals(1, manager.jobCount());
        assertEquals(2, manager.addJob("Luis", "Hedges", 1, 25, 0).getID());
    }

    @Test
    void explicitIdAtTheLimitExhaustsTheIdCounterCleanly() {
        Job last = new Job(Integer.MAX_VALUE - 1, "Ana", "Lawn", 1, 25, 0);
        manager.addJob(last);

        assertThrows(IllegalStateException.class, this::addAna);
        assertEquals(1, manager.jobCount());
        assertSame(last, manager.peek());
    }

    @Test
    void clearingWaitingJobsPreservesActiveWorkHistoryAndNextId() {
        Job ana = addAna();
        manager.startNextJob();
        Job completed = manager.completeActiveJob();

        Job activeRequest = manager.addJob("Luis", "Hedges", 1, 25, 0);
        Job active = manager.startNextJob();
        Job waiting = manager.addJob("Mia", "Leaves", 1, 30, 0);

        manager.clearWaitingJobs();
        manager.clearWaitingJobs();

        assertTrue(manager.isEmpty());
        assertSame(active, manager.getActiveJob());
        assertEquals(activeRequest.getID(), active.getID());
        assertEquals(List.of(completed), manager.getHistory());
        assertEquals(115, manager.getUnpaidTotal());
        assertThrows(IllegalArgumentException.class, () -> manager.getJob(waiting.getID()));
        assertThrows(IllegalArgumentException.class, () -> manager.addJob(waiting));
        assertEquals(waiting.getID() + 1, manager.addJob("Sam", "Edging", 1, 40, 0).getID());
        assertEquals(1, ana.getID());
    }

    @Test
    void totalsAddSeveralBillsWithoutBinaryRoundingDrift() {
        for (double bill : new double[] {0.1, 0.2}) {
            manager.addJob("Ana", "Lawn", 1, bill, 0);
            manager.startNextJob();
            manager.completeActiveJob();
        }

        assertEquals(0.30, manager.getUnpaidTotal());
        manager.markPaid(1);
        manager.markPaid(2);
        assertEquals(0.30, manager.getCollectedTotal());
        assertEquals(0, manager.getUnpaidTotal());
    }

    private Job addAna() {
        return manager.addJob("Ana", "Mow lawn", 2.5, 40, 15);
    }
}
