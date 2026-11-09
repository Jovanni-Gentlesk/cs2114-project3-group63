package serviceflow.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import serviceflow.model.Job;
import serviceflow.model.ServiceManager;
import serviceflow.view.ConsoleView;

/** Maps console commands to model operations and presents the results. */
public final class ServiceController {
    private final ServiceManager manager;
    private final ConsoleView view;
    private boolean running;

    public ServiceController(ServiceManager manager, ConsoleView view) {
        if (manager == null || view == null) {
            throw new IllegalArgumentException("A service manager and a console view are required.");
        }

        this.manager = manager;
        this.view = view;
    }

    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in)) {
            ConsoleView view = new ConsoleView(input, System.out);
            new ServiceController(new ServiceManager(), view).run();
        }
    }

    public void run() {
        running = true;

        while (running) {
            view.showMenu();
            String choice = readText("Choice: ");

            if (choice == null) {
                return;
            }

            switch (choice.strip()) {
                case "0" -> running = false;
                case "1" -> addJob();
                case "2" -> {
                    view.showMessage("Waiting jobs (" + jobCount() + ")");
                    jobList();

                    Job activeJob = manager.getActiveJob();
                    if (activeJob == null) {
                        view.showMessage("No active job.");
                    }
                    else {
                        view.showMessage("Active job");
                        view.showJob(activeJob);
                    }
                }
                case "3" -> showNext();
                case "4" -> startNextJob();
                case "5" -> finishJob();
                case "6" -> promptForPayment();
                case "7" -> showHistory();
                case "8" -> showTotals();
                case "11" -> clearJobs();
                default -> view.showMessage("Error: Choose an option from the menu.");
            }
        }
    }

    public void addJob() {
        String name = readRequiredText("Customer name: ");
        if (name == null) {
            return;
        }

        String description = readRequiredText("Service description: ");
        if (description == null) {
            return;
        }

        Double hours = readAmount("Estimated hours: ");
        if (hours == null) {
            return;
        }

        Double rate = readAmount("Hourly rate: ");
        if (rate == null) {
            return;
        }

        Double materials = readAmount("Materials cost: ");
        if (materials == null) {
            return;
        }

        try {
            Job added = manager.addJob(name, description, hours, rate, materials);
            view.showMessage("Request added.");
            view.showJob(added);
        }
        catch (IllegalArgumentException | IllegalStateException error) {
            showError(error);
        }
    }

    public Job showNext() {
        try {
            Job next = manager.peek();
            view.showJob(next);
            return next;
        }
        catch (NoSuchElementException error) {
            showError(error);
            return null;
        }
    }

    public Job startNextJob() {
        try {
            Job started = manager.startNextJob();
            view.showJob(started);
            return started;
        }
        catch (IllegalStateException | NoSuchElementException error) {
            showError(error);
            return null;
        }
    }

    public Job finishJob() {
        try {
            Job finished = manager.completeActiveJob();
            view.showJob(finished);
            return finished;
        }
        catch (IllegalStateException error) {
            showError(error);
            return null;
        }
    }

    public void markPaid(int jobId) {
        view.showJob(manager.markPaid(jobId));
        showTotals();
    }

    public String jobList() {
        List<Job> waiting = new ArrayList<>();

        for (Job job : manager) {
            waiting.add(job);
        }

        String formatted = view.formatJobs(waiting.toArray(Job[]::new));
        view.showMessage(formatted);
        return formatted;
    }

    public int jobCount() {
        return manager.jobCount();
    }

    public void showHistory() {
        view.showMessage("Completed and paid jobs");
        view.showJobs(manager.getHistory().toArray(Job[]::new));
    }

    public void showTotals() {
        view.showTotal(manager.getCollectedTotal(), manager.getUnpaidTotal());
    }

    public void clearJobs() {
        String answer = readText("Clear every waiting job? (y/N): ");

        if (answer != null && answer.strip().equalsIgnoreCase("y")) {
            manager.clearWaitingJobs();
            view.showMessage("Waiting jobs cleared.");
        }
    }

    private void promptForPayment() {
        String input = readText("Job ID (blank to cancel): ");

        while (input != null && !input.isBlank()) {
            try {
                markPaid(Integer.parseInt(input.strip()));
                return;
            }
            catch (NumberFormatException error) {
                view.showMessage("Error: Enter a whole-number job ID.");
            }
            catch (IllegalArgumentException error) {
                showError(error);
            }
            catch (IllegalStateException error) {
                showError(error);
                return;
            }

            input = readText("Job ID (blank to cancel): ");
        }
    }

    private String readRequiredText(String prompt) {
        String input = readText(prompt);

        while (input != null && input.isBlank()) {
            view.showMessage("Error: This field is required.");
            input = readText(prompt);
        }

        return input;
    }

    private Double readAmount(String prompt) {
        String input = readText(prompt);

        while (input != null) {
            try {
                BigDecimal decimal = new BigDecimal(input.strip());
                double number = decimal.doubleValue();

                if (Double.isFinite(number) && BigDecimal.valueOf(number).compareTo(decimal) == 0) {
                    return number;
                }

                view.showMessage("Error: Enter a decimal number within the supported range and precision.");
            }
            catch (NumberFormatException error) {
                view.showMessage("Error: Enter a finite decimal number.");
            }

            input = readText(prompt);
        }

        return null;
    }

    private String readText(String prompt) {
        String text = view.readText(prompt);

        if (text == null) {
            running = false;
        }

        return text;
    }

    private void showError(RuntimeException error) {
        view.showMessage("Error: " + error.getMessage());
    }
}
