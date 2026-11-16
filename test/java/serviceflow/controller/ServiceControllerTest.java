package serviceflow.controller;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import serviceflow.model.Job;
import serviceflow.model.ServiceManager;
import serviceflow.view.ConsoleView;

import static org.junit.jupiter.api.Assertions.*;

class ServiceControllerTest {
    private final ServiceManager manager = new ServiceManager();

    @Test
    void consoleCompletesTheMvpAndShowsTheActiveJob() {
        String screen = runSession(
            "1", "Ana", "Mow front lawn", "2.50", "40", "15",
            "1", "Luis", "Trim hedges", "1", "35", "0",
            "2", "3", "4", "2", "5", "8", "6", "1", "7", "8", "0");

        assertEquals(Job.PAID, manager.getJob(1).getStatus());
        assertEquals(115, manager.getCollectedTotal());
        assertEquals(0, manager.getUnpaidTotal());
        assertEquals(2, manager.peek().getID());
        assertTrue(screen.contains("Waiting jobs (2)"));
        assertTrue(screen.contains("Active job"));
        assertTrue(screen.contains("#1 Ana | Mow front lawn | ACTIVE"));
        assertTrue(screen.contains("Collected: $0.00 | Unpaid: $115.00"));
        assertTrue(screen.contains("Collected: $115.00 | Unpaid: $0.00"));
        assertTrue(screen.contains("Completed and paid jobs"));
    }

    @Test
    void badInputReportsErrorsAndTheSessionContinues() {
        String screen = runSession(
            "oops", "3", "4", "5",
            "1", " ", "Ana", " ", "Lawn", "abc", "NaN", "1e-400", "2.5", "-40", "15",
            "1", "Ana", "Lawn", "2.5", "40", "15",
            "1", "Luis", "Hedges", "1", "25", "0",
            "6", "abc", "999", "1", "4", "4", "6", "1", "5", "6", "1", "6", "1", "0");

        assertEquals(Job.PAID, manager.getJob(1).getStatus());
        assertEquals(2, manager.peek().getID());
        assertEquals(115, manager.getCollectedTotal());
        assertTrue(screen.contains("Choose an option"));
        assertTrue(screen.contains("No jobs are waiting"));
        assertTrue(screen.contains("Start a job before"));
        assertTrue(screen.contains("This field is required"));
        assertTrue(screen.contains("Enter a finite decimal number"));
        assertTrue(screen.contains("within the supported range and precision"));
        assertTrue(screen.contains("Hourly rate must be between"));
        assertTrue(screen.contains("whole-number job ID"));
        assertTrue(screen.contains("Job #999 was not found"));
        assertTrue(screen.contains("Complete job #1 before recording payment"));
        assertTrue(screen.contains("Complete the active job before starting another"));
        assertTrue(screen.contains("Job #1 is already paid"));
    }

    @Test
    void endOfInputDuringARequestLeavesNoPartialJob() {
        runSession("1", "Ana", "Lawn", "2.5");

        assertTrue(manager.isEmpty());
        assertEquals(1, manager.addJob("Luis", "Hedges", 1, 25, 0).getID());
    }

    @Test
    void clearRequiresConfirmationAndLeavesActiveJobAlone() {
        Job ana = manager.addJob("Ana", "Lawn", 1, 40, 0);
        Job luis = manager.addJob("Luis", "Hedges", 1, 25, 0);

        runSession("4", "11", "n", "11");
        assertEquals(luis.getID(), manager.peek().getID());
        assertEquals(ana.getID(), manager.getActiveJob().getID());

        runSession("11", "y", "0");
        assertTrue(manager.isEmpty());
        assertEquals(ana.getID(), manager.getActiveJob().getID());
    }

    private String runSession(String... commands) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (Scanner input = new Scanner(String.join("\n", commands));
            PrintStream print = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            new ServiceController(manager, new ConsoleView(input, print)).run();
        }

        return output.toString(StandardCharsets.UTF_8);
    }
}
