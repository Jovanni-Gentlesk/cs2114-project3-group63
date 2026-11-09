package serviceflow.demo;

import java.util.Locale;

import serviceflow.model.Job;
import serviceflow.model.ServiceManager;

/** Exercises the model with fixed inputs and no console view or controller. */
public final class ModelDriver {
    private ModelDriver() {
    }

    public static void main(String[] args) {
        ServiceManager manager = new ServiceManager();
        Job first = manager.addJob("Ana", "Mow front lawn", 2.5, 40, 15);
        Job second = manager.addJob("Luis", "Trim hedges", 1, 35, 0);

        System.out.println("Waiting count: " + manager.jobCount());
        System.out.println("Next: " + manager.peek());

        Job active = manager.startNextJob();
        require(active.getID() == first.getID() && manager.peek().getID() == second.getID(),
            "Jobs must start in FIFO order.");
        System.out.println("Started: " + active);

        Job completed = manager.completeActiveJob();
        require(completed.getBill() == 115 && manager.getUnpaidTotal() == 115,
            "The $115 bill must appear in the unpaid total.");
        System.out.println("Completed: " + completed);

        Job paid = manager.markPaid(first.getID());
        require(manager.getCollectedTotal() == 115 && manager.getUnpaidTotal() == 0,
            "Payment must move the bill from unpaid to collected.");
        require(manager.getHistory().size() == 1 && first.getStatus().equals(Job.WAITING),
            "History must include the paid job and the original snapshot must stay unchanged.");

        System.out.println("Paid: " + paid);
        System.out.printf(Locale.ROOT, "Collected: $%.2f | Unpaid: $%.2f%n",
            manager.getCollectedTotal(), manager.getUnpaidTotal());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
