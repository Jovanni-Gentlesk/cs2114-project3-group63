package serviceflow.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** The immutable state of one service request at a point in time. */
public final class Job {
    public static final String WAITING = "WAITING";
    public static final String ACTIVE = "ACTIVE";
    public static final String COMPLETED = "COMPLETED";
    public static final String PAID = "PAID";

    private final int id;
    private final String name;
    private final String serviceDescription;
    private final double hours;
    private final double hourlyRate;
    private final double materialsCost;
    private final double cost;
    private final String status;
    private final Double bill;

    public Job(int id, String name, String serviceDescription, double hours,
        double hourlyRate, double materialsCost) {
        if (id <= 0 || id == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Job ID must be between 1 and "
                + (Integer.MAX_VALUE - 1) + ".");
        }

        this.id = id;
        this.name = requireText(name, "Customer name", 100);
        this.serviceDescription = requireText(serviceDescription, "Service description", 500);
        this.hours = requireAmount(hours, "Estimated hours", 168);

        if (this.hours == 0) {
            throw new IllegalArgumentException("Estimated hours must be greater than zero.");
        }

        this.hourlyRate = requireAmount(hourlyRate, "Hourly rate", 10_000);
        this.materialsCost = requireAmount(materialsCost, "Materials cost", 1_000_000);

        cost = BigDecimal.valueOf(this.hours)
            .multiply(BigDecimal.valueOf(this.hourlyRate))
            .add(BigDecimal.valueOf(this.materialsCost))
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();

        status = WAITING;
        bill = null;
    }

    private Job(Job original, String status, Double bill) {
        id = original.id;
        name = original.name;
        serviceDescription = original.serviceDescription;
        hours = original.hours;
        hourlyRate = original.hourlyRate;
        materialsCost = original.materialsCost;
        cost = original.cost;
        this.status = status;
        this.bill = bill;
    }

    Job withStatus(String newStatus, Double newBill) {
        return new Job(this, newStatus, newBill);
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public double getHours() {
        return hours;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public double getMaterialsCost() {
        return materialsCost;
    }

    public double getCost() {
        return cost;
    }

    public String getStatus() {
        return status;
    }

    public Double getBill() {
        return bill;
    }

    @Override
    public String toString() {
        String billText = bill == null ? "not billed"
            : String.format(Locale.ROOT, "bill $%.2f", bill);

        return String.format(Locale.ROOT, "#%d %s | %s | %s | quote $%.2f | %s",
            id, name, serviceDescription, status, cost, billText);
    }

    private static String requireText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }

        if (value.indexOf('|') >= 0 || value.indexOf('\t') >= 0
            || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(
                label + " cannot contain pipes, tabs, or line breaks.");
        }

        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(
                label + " can have at most " + maxLength + " characters.");
        }

        return trimmed;
    }

    private static double requireAmount(double value, String label, int maximum) {
        if (!Double.isFinite(value) || value < 0 || value > maximum) {
            throw new IllegalArgumentException(label + " must be between 0 and " + maximum + ".");
        }

        if (BigDecimal.valueOf(value).stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(label + " can have at most two decimal places.");
        }

        return value == 0 ? 0 : value;
    }
}
