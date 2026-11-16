package serviceflow.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {
    @TempDir Path directory;
    private final ServiceManager manager = new ServiceManager();
    private Job add() { return manager.addJob("Ana", "Lawn", 2, 20, 5); }
    private String file() { return directory.resolve("session.tsv").toString(); }

    @Test void roundTripPreservesEveryStatusFieldsOrderTotalsAndNextId() throws IOException {
        add(); manager.startNextJob(); manager.completeActiveJob(); manager.markPaid(1);
        add(); manager.startNextJob(); manager.completeActiveJob();
        add(); manager.startNextJob();
        add(); add();
        manager.saveJobs(file());
        ServiceManager copy = new ServiceManager();
        copy.loadJobs(file());
        assertEquals(45, copy.getCollectedTotal());
        assertEquals(45, copy.getUnpaidTotal());
        assertEquals(3, copy.getActiveJob().getID());
        assertEquals(2, copy.getHistory().size());
        List<Integer> ids = new ArrayList<>();
        copy.forEach(job -> ids.add(job.getID()));
        assertEquals(List.of(4, 5), ids);
        for (int id = 1; id <= 5; id++) {
            Job old = manager.getJob(id), loaded = copy.getJob(id);
            assertEquals(old.toString(), loaded.toString());
            assertEquals(old.getHours(), loaded.getHours());
            assertEquals(old.getHourlyRate(), loaded.getHourlyRate());
            assertEquals(old.getMaterialsCost(), loaded.getMaterialsCost());
        }
        assertEquals(6, copy.addJob("Luis", "Hedges", 1, 0, 0).getID());
    }

    @Test void emptyAndClearedSessionsPreserveNextId() throws IOException {
        manager.saveJobs(file()); manager.loadJobs(file());
        assertTrue(manager.isEmpty());
        add(); manager.clearWaitingJobs(); manager.saveJobs(file());
        ServiceManager copy = new ServiceManager(); copy.loadJobs(file());
        assertEquals(2, copy.addJob("Ana", "Lawn", 1, 1, 0).getID());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "OTHER\t1\t2\n", "SERVICEFLOW\t2\t2\n", "SERVICEFLOW\t1\t0\n",
        "SERVICEFLOW\t1\t1\n1\tAna\tLawn\t1\t1\t0\tWAITING\t-\n",
        "SERVICEFLOW\t1\t3\n1\tAna\tLawn\t1\t1\t0\tWAITING\t-\n1\tAna\tLawn\t1\t1\t0\tWAITING\t-\n",
        "SERVICEFLOW\t1\t3\n1\tAna\tLawn\t1\t1\t0\tACTIVE\t-\n2\tAna\tLawn\t1\t1\t0\tACTIVE\t-\n",
        "SERVICEFLOW\t1\t2\n1\tAna\tLawn\t1\t1\t0\tUNKNOWN\t-\n",
        "SERVICEFLOW\t1\t2\n1\tAna\tLawn\t1\t1\t0\tPAID\t999\n",
        "SERVICEFLOW\t1\t2\n1\tAna\tLawn\t1\t1\t0\tCOMPLETED\tNaN\n",
        "SERVICEFLOW\t1\t2\n1\tAna\tLawn\t1\t1\t0\tWAITING\t1\n",
        "SERVICEFLOW\t1\t2\n1\tAna\tLawn\t-1\t1\t0\tWAITING\t-\n",
        "SERVICEFLOW\t1\t2\n1\t\tLawn\t1\t1\t0\tWAITING\t-\n",
        "SERVICEFLOW\t1\t2\ntruncated\n", "SERVICEFLOW\t1\tNaN\n"
    })
    void invalidFilesLeaveExistingSessionUntouched(String data) throws IOException {
        Job original = add();
        manager.startNextJob(); manager.completeActiveJob(); manager.markPaid(1);
        Job waiting = add();
        Files.writeString(Path.of(file()), data);
        assertThrows(IOException.class, () -> manager.loadJobs(file()));
        assertEquals(original.getID(), manager.getHistory().get(0).getID());
        assertEquals(45, manager.getCollectedTotal());
        assertSame(waiting, manager.peek());
        assertEquals(3, add().getID());
        assertEquals(data, Files.readString(Path.of(file())));
    }

    @Test void missingDirectoryAndInvalidPathsFailWithoutMutatingModel() {
        Job job = add();
        assertThrows(IOException.class, () -> manager.loadJobs(file()));
        assertThrows(IOException.class, () -> manager.loadJobs(directory.toString()));
        assertThrows(IOException.class, () -> manager.saveJobs(directory.resolve("missing/data").toString()));
        for (String bad : new String[] {null, "", " ", "\0"}) {
            assertThrows(IllegalArgumentException.class, () -> manager.saveJobs(bad));
            assertThrows(IllegalArgumentException.class, () -> manager.loadJobs(bad));
        }
        assertSame(job, manager.peek());
    }

    @Test void failedReplacementPreservesDestinationAndRemovesTemporaryFile() throws IOException {
        Path target = directory.resolve("occupied"); Files.createDirectory(target);
        Files.writeString(target.resolve("prior-save"), "keep me");
        add();
        assertThrows(IOException.class, () -> manager.saveJobs(target.toString()));
        assertEquals("keep me", Files.readString(target.resolve("prior-save")));
        try (var paths = Files.list(directory)) { assertEquals(1, paths.count()); }
        assertEquals(1, manager.jobCount());
    }

    @Test void savingAgainReplacesPreviousSnapshot() throws IOException {
        add(); manager.saveJobs(file());
        manager.startNextJob(); manager.saveJobs(file());
        ServiceManager copy = new ServiceManager(); copy.loadJobs(file());
        assertTrue(copy.isEmpty()); assertEquals(1, copy.getActiveJob().getID());
    }
}
