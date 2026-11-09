package serviceflow.model;

import java.math.BigDecimal;
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
