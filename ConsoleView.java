package serviceflow;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;

/**
 * Displays ServiceFlow information and reads console input.
 * The controller handles menu actions and input parsing.
 *
 * @author Noah Bierman
 * @version 2026.11.08
 */
public class ConsoleView
{
    private final Scanner input;
    private final PrintStream output;

    /**
     * Creates a console view.
     *
     * @param input the input source
     * @param output the output destination
     * @throws IllegalArgumentException if either argument is null
     */
    public ConsoleView(Scanner input, PrintStream output)
    {
        if (input == null || output == null)
        {
            throw new IllegalArgumentException(
                "Input and output cannot be null.");
        }

        this.input = input;
        this.output = output;
    }

    /**
     * Displays the main menu.
     */
    public void showMenu()
    {
        output.println();
        output.println("ServiceFlow");
        output.println("1. Add a job");
        output.println("2. View waiting jobs");
        output.println("3. View next job");
        output.println("4. Start next job");
        output.println("5. Complete active job");
        output.println("6. Mark a bill as paid");
        output.println("7. View job history");
        output.println("8. View payment totals");
        output.println("9. Save jobs");
        output.println("10. Load jobs");
        output.println("11. Clear waiting jobs");
        output.println("0. Exit");
    }

    /**
     * Displays a prompt and reads a line of text.
     *
     * @param prompt the prompt to display
     * @return the unchanged input text, or null if input has ended
     * @throws IllegalArgumentException if the prompt is null
     */
    public String readText(String prompt)
    {
        if (prompt == null)
        {
            throw new IllegalArgumentException("Prompt cannot be null.");
        }

        output.print(prompt);
        output.flush();

        if (input.hasNextLine())
        {
            return input.nextLine();
        }

        return null;
    }

    /**
     * Displays one job.
     *
     * @param job the job to display
     * @throws IllegalArgumentException if the job is null
     */
    public void showJob(Job job)
    {
        output.print(formatJob(job));
    }

    /**
     * Displays the supplied jobs.
     *
     * @param jobs the jobs to display
     * @throws IllegalArgumentException if the array or any entry is null
     */
    public void showJobs(Job[] jobs)
    {
        output.print(formatJobs(jobs));
    }

    /**
     * Formats the supplied jobs without printing them.
     *
     * @param jobs the jobs to format
     * @return the formatted list, or an empty-list message
     * @throws IllegalArgumentException if the array or any entry is null
     */
    public String formatJobs(Job[] jobs)
    {
        if (jobs == null)
        {
            throw new IllegalArgumentException("Job list cannot be null.");
        }

        if (jobs.length == 0)
        {
            return "No jobs to display." + System.lineSeparator();
        }

        StringBuilder text = new StringBuilder();

        for (int i = 0; i < jobs.length; i++)
        {
            if (i > 0)
            {
                text.append(System.lineSeparator());
            }

            text.append(formatJob(jobs[i]));
        }

        return text.toString();
    }

    /**
     * Displays the payment totals supplied by the model.
     *
     * @param paid the collected total
     * @param unpaid the unpaid total
     * @throws IllegalArgumentException if either amount is negative,
     *         NaN, or infinite
     */
    public void showTotal(double paid, double unpaid)
    {
        if (!Double.isFinite(paid) || !Double.isFinite(unpaid)
            || paid < 0 || unpaid < 0)
        {
            throw new IllegalArgumentException(
                "Totals must be finite and cannot be negative.");
        }

        output.println("Collected: " + formatMoney(paid));
        output.println("Unpaid: " + formatMoney(unpaid));
    }

    /**
     * Displays a message.
     *
     * @param message the message to display
     * @throws IllegalArgumentException if the message is null
     */
    public void showMessage(String message)
    {
        if (message == null)
        {
            throw new IllegalArgumentException("Message cannot be null.");
        }

        output.println(message);
    }

    /**
     * Builds the display text for one job.
     *
     * @param job the job to format
     * @return the formatted job information
     * @throws IllegalArgumentException if the job is null
     */
    private String formatJob(Job job)
    {
        if (job == null)
        {
            throw new IllegalArgumentException("Job cannot be null.");
        }

        StringBuilder text = new StringBuilder();
        String newLine = System.lineSeparator();

        text.append("Job ID: ").append(job.getID()).append(newLine);
        text.append("Customer: ").append(job.getName()).append(newLine);
        text.append("Service: ").append(job.getServiceDescription())
            .append(newLine);
        text.append("Estimated hours: ").append(job.getHours())
            .append(newLine);
        text.append("Hourly rate: ")
            .append(formatMoney(job.getHourlyRate())).append(newLine);
        text.append("Materials: ")
            .append(formatMoney(job.getMaterialsCost())).append(newLine);
        text.append("Status: ").append(job.getStatus()).append(newLine);
        text.append("Estimate: ").append(formatMoney(job.getCost()))
            .append(newLine);

        Double bill = job.getBill();

        if (bill == null)
        {
            text.append("Bill: not billed").append(newLine);
        }
        else
        {
            text.append("Bill: ").append(formatMoney(bill))
                .append(newLine);
        }

        return text.toString();
    }

    /**
     * Formats an amount as dollars with two decimal places.
     *
     * @param amount the amount to display
     * @return the formatted amount
     */
    private String formatMoney(double amount)
    {
        return String.format(Locale.US, "$%.2f", amount);
    }
}