package serviceflow.model;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Owns the waiting line and the status, billing, and payment rules. */
public final class ServiceManager implements Iterable<Job> {
    private final LinkedQueue<Job> jobQueue = new LinkedQueue<>();
    private final List<Job> allJobs = new ArrayList<>();
    private Job activeJob;
    private int nextJobId = 1;

    public void addJob(Job job) {
        if (job == null || !Job.WAITING.equals(job.getStatus())) {
            throw new IllegalArgumentException("A new waiting job is required.");
        }

        if (job.getID() < nextJobId) {
            throw new IllegalArgumentException("Job ID has already been used or is below the next ID.");
        }

        jobQueue.enqueue(job);
        allJobs.add(job);
        nextJobId = job.getID() + 1;
    }

    public Job addJob(String name, String description, double hours,
        double rate, double materials) {
        if (nextJobId == Integer.MAX_VALUE) {
            throw new IllegalStateException("No more job IDs are available.");
        }

        Job job = new Job(nextJobId, name, description, hours, rate, materials);
        addJob(job);
        return job;
    }

    public Job peek() {
        return jobQueue.peek();
    }

    public int jobCount() {
        return jobQueue.size();
    }

    public boolean isEmpty() {
        return jobQueue.isEmpty();
    }

    @Override
    public Iterator<Job> iterator() {
        return jobQueue.iterator();
    }

    public Job removeJob() {
        return startNextJob();
    }

    public Job startNextJob() {
        if (activeJob != null) {
            throw new IllegalStateException("Complete the active job before starting another.");
        }

        Job next = jobQueue.dequeue().withStatus(Job.ACTIVE, null);
        allJobs.set(findJobIndex(next.getID()), next);
        activeJob = next;
        return next;
    }

    public Job completeActiveJob() {
        if (activeJob == null) {
            throw new IllegalStateException("Start a job before trying to complete it.");
        }

        Job completed = activeJob.withStatus(Job.COMPLETED, activeJob.getCost());
        allJobs.set(findJobIndex(completed.getID()), completed);
        activeJob = null;
        return completed;
    }

    public Job markPaid(int jobId) {
        int index = findJobIndex(jobId);
        Job job = allJobs.get(index);

        if (Job.PAID.equals(job.getStatus())) {
            throw new IllegalStateException("Job #" + jobId + " is already paid.");
        }

        if (!Job.COMPLETED.equals(job.getStatus())) {
            throw new IllegalStateException("Complete job #" + jobId + " before recording payment.");
        }

        Job paid = job.withStatus(Job.PAID, job.getBill());
        allJobs.set(index, paid);
        return paid;
    }

    public Job getJob(int jobId) {
        return allJobs.get(findJobIndex(jobId));
    }

    public Job getActiveJob() {
        return activeJob;
    }

    public List<Job> getHistory() {
        List<Job> history = new ArrayList<>();

        for (Job job : allJobs) {
            if (Job.COMPLETED.equals(job.getStatus()) || Job.PAID.equals(job.getStatus())) {
                history.add(job);
            }
        }

        return List.copyOf(history);
    }

    public double getCollectedTotal() {
        return totalForStatus(Job.PAID);
    }

    public double getUnpaidTotal() {
        return totalForStatus(Job.COMPLETED);
    }

    public void clearWaitingJobs() {
        allJobs.removeIf(job -> Job.WAITING.equals(job.getStatus()));
        jobQueue.clear();
    }

    /** Writes a complete versioned snapshot, replacing the old file atomically.
     * If atomic replacement is unsupported, the save fails and the old file stays.
     */
    public void saveJobs(String filename) throws IOException {
        Path target = sessionPath(filename);
        StringBuilder data = new StringBuilder("SERVICEFLOW\t1\t" + nextJobId + "\n");
        for (Job job : allJobs) {
            data.append(job.getID()).append('\t').append(job.getName()).append('\t')
                .append(job.getServiceDescription()).append('\t').append(job.getHours()).append('\t')
                .append(job.getHourlyRate()).append('\t').append(job.getMaterialsCost()).append('\t')
                .append(job.getStatus()).append('\t')
                .append(job.getBill() == null ? "-" : job.getBill()).append('\n');
        }
        Path temporary = Files.createTempFile(target.getParent(), ".serviceflow-", ".tmp");
        try {
            Files.writeString(temporary, data, StandardCharsets.UTF_8);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        }
        finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Validates into a temporary model; a rejected file never changes this model. */
    public void loadJobs(String filename) throws IOException {
        List<String> lines = Files.readAllLines(sessionPath(filename), StandardCharsets.UTF_8);
        ServiceManager restored = new ServiceManager();
        try {
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("Empty session file.");
            }
            String[] header = lines.get(0).split("\t", -1);
            if (header.length != 3 || !header[0].equals("SERVICEFLOW") || !header[1].equals("1")) {
                throw new IllegalArgumentException("Unsupported session header.");
            }
            int restoredNextId = Integer.parseInt(header[2]);
            int previousId = 0;
            for (int line = 1; line < lines.size(); line++) {
                String[] fields = lines.get(line).split("\t", -1);
                if (fields.length != 8) {
                    throw new IllegalArgumentException("Invalid record on line " + (line + 1));
                }
                Job job = new Job(Integer.parseInt(fields[0]), fields[1], fields[2],
                    Double.parseDouble(fields[3]), Double.parseDouble(fields[4]),
                    Double.parseDouble(fields[5]));
                if (job.getID() <= previousId) {
                    throw new IllegalArgumentException("IDs must be unique and increasing.");
                }
                previousId = job.getID();
                String status = fields[6];
                Double bill = null;
                switch (status) {
                    case Job.WAITING, Job.ACTIVE -> {
                        if (!fields[7].equals("-")) {
                            throw new IllegalArgumentException("Unfinished job has a bill.");
                        }
                    }
                    case Job.COMPLETED, Job.PAID -> {
                        bill = Double.valueOf(fields[7]);
                        if (!Double.isFinite(bill) || Double.compare(bill, job.getCost()) != 0) {
                            throw new IllegalArgumentException("Bill does not match the quote.");
                        }
                    }
                    default -> throw new IllegalArgumentException("Unknown job status.");
                }
                job = job.withStatus(status, bill);
                restored.allJobs.add(job);
                if (status.equals(Job.WAITING)) {
                    restored.jobQueue.enqueue(job);
                }
                else if (status.equals(Job.ACTIVE)) {
                    if (restored.activeJob != null) {
                        throw new IllegalArgumentException("More than one active job.");
                    }
                    restored.activeJob = job;
                }
            }
            if (restoredNextId <= previousId || restoredNextId <= 0) {
                throw new IllegalArgumentException("Invalid next job ID.");
            }
            restored.nextJobId = restoredNextId;
        }
        catch (IllegalArgumentException error) {
            throw new IOException("Invalid session: " + error.getMessage(), error);
        }
        allJobs.clear();
        allJobs.addAll(restored.allJobs);
        jobQueue.clear();
        for (Job job : restored.jobQueue) {
            jobQueue.enqueue(job);
        }
        activeJob = restored.activeJob;
        nextJobId = restored.nextJobId;
    }

    private static Path sessionPath(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("A session filename is required.");
        }
        return Path.of(filename).toAbsolutePath();
    }

    private int findJobIndex(int jobId) {
        if (jobId <= 0) {
            throw new IllegalArgumentException("Enter a positive job ID.");
        }

        for (int index = 0; index < allJobs.size(); index++) {
            if (allJobs.get(index).getID() == jobId) {
                return index;
            }
        }

        throw new IllegalArgumentException("Job #" + jobId + " was not found.");
    }

    private double totalForStatus(String status) {
        BigDecimal total = BigDecimal.ZERO;

        for (Job job : allJobs) {
            if (status.equals(job.getStatus())) {
                total = total.add(BigDecimal.valueOf(job.getBill()));
            }
        }

        return total.doubleValue();
    }
}
