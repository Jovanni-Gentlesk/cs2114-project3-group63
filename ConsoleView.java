package serviceflow.view;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;
import java.util.StringJoiner;

import serviceflow.model.Job;

/** Reads raw lines and displays the information supplied by the controller. */
public final class ConsoleView {
    private final Scanner input;
    private final PrintStream output;

    public ConsoleView(Scanner input, PrintStream output) {
        if (input == null || output == null) {
            throw new IllegalArgumentException("Console input and output are required.");
        }

        this.input = input;
        this.output = output;
    }

    public void showMenu() {
        output.println();
        output.println("ServiceFlow");
        output.println("1 Add request        2 Waiting and active jobs   3 Peek at next job");
        output.println("4 Start next job     5 Finish active job         6 Record payment");
        output.println("7 History            8 Payment totals           11 Clear waiting jobs");
        output.println("0 Exit");
    }

    public String readText(String prompt) {
        if (prompt == null) {
            throw new IllegalArgumentException("A prompt is required.");
        }

        output.print(prompt);
        output.flush();
        return input.hasNextLine() ? input.nextLine() : null;
    }

    public void showJob(Job job) {
        output.println(formatJob(job));
    }

    public void showJobs(Job[] jobs) {
        output.println(formatJobs(jobs));
    }

    public String formatJobs(Job[] jobs) {
        if (jobs == null) {
            throw new IllegalArgumentException("A job list is required.");
        }

        if (jobs.length == 0) {
            return "No jobs to display.";
        }

        StringJoiner lines = new StringJoiner(System.lineSeparator());

        for (Job job : jobs) {
            lines.add(formatJob(job));
        }

        return lines.toString();
    }

    public void showTotal(double paid, double unpaid) {
        if (!Double.isFinite(paid) || !Double.isFinite(unpaid) || paid < 0 || unpaid < 0) {
            throw new IllegalArgumentException("Totals must be finite and nonnegative.");
        }

        output.printf(Locale.ROOT, "Collected: $%.2f | Unpaid: $%.2f%n", paid, unpaid);
    }

    public void showMessage(String message) {
        if (message == null) {
            throw new IllegalArgumentException("A message is required.");
        }

        output.println(message);
    }

    private String formatJob(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("A job is required.");
        }

        String billText = job.getBill() == null ? "not billed"
            : String.format(Locale.ROOT, "bill $%.2f", job.getBill());

        return String.format(Locale.ROOT, "#%d %s | %s | %s | quote $%.2f | %s",
            job.getID(), job.getName(), job.getServiceDescription(), job.getStatus(),
            job.getCost(), billText);
    }
}
