package serviceflow.controller;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import serviceflow.model.ServiceManager;
import serviceflow.model.Job;
import serviceflow.view.ConsoleView;
import static org.junit.jupiter.api.Assertions.*;

/** Extra integration checks; the core model suite never constructs a view. */
class AutosaveTest {
    @TempDir Path directory;
    private Path file() { return directory.resolve("session.tsv"); }
    private String run(ServiceManager manager, Path path, String... lines) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Scanner input = new Scanner(String.join("\n", lines));
             PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new ServiceController(manager, new ConsoleView(input, output), path.toString()).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
    private ServiceManager load() throws Exception {
        ServiceManager manager = new ServiceManager(); manager.loadJobs(file().toString()); return manager;
    }
    @Test void eachSuccessfulMutationIsSavedAndRestored() throws Exception {
        assertTrue(run(new ServiceManager(), file(), "1", "Ana", "Lawn", "2", "20", "5", "0")
            .contains("No saved session yet"));
        assertEquals(1, load().jobCount());
        run(new ServiceManager(), file(), "4", "0");
        assertEquals(Job.ACTIVE, load().getActiveJob().getStatus());
        run(new ServiceManager(), file(), "5", "0");
        assertEquals(45, load().getUnpaidTotal());
        run(new ServiceManager(), file(), "6", "1", "0");
        assertEquals(45, load().getCollectedTotal());
        run(new ServiceManager(), file(), "1", "Luis", "Hedges", "1", "10", "0", "0");
        run(new ServiceManager(), file(), "11", "y", "0");
        assertTrue(load().isEmpty()); assertEquals(45, load().getCollectedTotal());
        assertEquals(3, load().addJob("Next", "Work", 1, 1, 0).getID());
    }
    @Test void rejectedAndReadOnlyActionsDoNotWriteFile() throws Exception {
        run(new ServiceManager(), file(), "1", "Ana", "Lawn", "2", "20", "5", "0");
        byte[] before = Files.readAllBytes(file());
        Files.setLastModifiedTime(file(), FileTime.fromMillis(100000));
        FileTime time = Files.getLastModifiedTime(file());
        run(new ServiceManager(), file(), "2", "3", "7", "8", "oops", "5", "6", "1",
            "11", "n", "1", "Ana", "Lawn", "-1", "20", "5", "0");
        assertArrayEquals(before, Files.readAllBytes(file()));
        assertEquals(time, Files.getLastModifiedTime(file()));
    }
    @Test void corruptStartupFileIsNeverOverwritten() throws Exception {
        Files.writeString(file(), "broken data");
        ServiceManager manager = new ServiceManager();
        String screen = run(manager, file(), "1", "Ana", "Lawn", "2", "20", "5", "0");
        assertEquals(1, manager.jobCount());
        assertTrue(screen.contains("Autosave disabled"));
        assertTrue(screen.contains("in memory only"));
        assertEquals("broken data", Files.readString(file()));
    }
    @Test void failedSaveWarnsAndKeepsChangeInMemory() {
        ServiceManager manager = new ServiceManager();
        String screen = run(manager, directory.resolve("missing/session.tsv"),
            "1", "Ana", "Lawn", "2", "20", "5", "2", "0");
        assertEquals(1, manager.jobCount());
        assertTrue(screen.contains("Could not save session"));
        assertTrue(screen.contains("Waiting jobs (1)"));
    }
    @Test void additionalBadFieldsAndEndOfInputDoNotCrash() {
        for (String[] fields : new String[][] {
            {"Ana", "Lawn", "0", "1", "0"},
            {"Ana", "Lawn", "1", "1", "-1"},
            {"Ana", "Lawn", "1.001", "1", "0"},
            {"Ana|Luis", "Lawn", "1", "1", "0"},
            {"Ana", "Lawn", "1", "10001", "0"}
        }) {
            ServiceManager manager = new ServiceManager();
            run(manager, file(), "1", fields[0], fields[1], fields[2], fields[3], fields[4], "0");
            assertTrue(manager.isEmpty());
        }
        for (String[] input : new String[][] {{"1"}, {"1", "Ana"}, {"1", "Ana", "Lawn"},
            {"1", "Ana", "Lawn", "1"}, {"1", "Ana", "Lawn", "1", "1"}, {"6"}, {"11"}}) {
            assertDoesNotThrow(() -> run(new ServiceManager(), file(), input));
        }
    }
}
