package serviceflow.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobTest {
    @Test
    void createsTrimmedWaitingQuoteWithNoBill() {
        Job job = new Job(1, " Ana ", " Mow lawn ", 2.5, 40, 15);

        assertAll(
            () -> assertEquals(1, job.getID()),
            () -> assertEquals("Ana", job.getName()),
            () -> assertEquals("Mow lawn", job.getServiceDescription()),
            () -> assertEquals(2.5, job.getHours()),
            () -> assertEquals(40, job.getHourlyRate()),
            () -> assertEquals(15, job.getMaterialsCost()),
            () -> assertEquals(Job.WAITING, job.getStatus()),
            () -> assertEquals(115, job.getCost()),
            () -> assertNull(job.getBill()),
            () -> assertTrue(job.toString().contains("not billed"))
        );
    }

    @Test
    void roundsHalfACentUpAndAcceptsZeroCosts() {
        assertEquals(0.01, new Job(1, "Ana", "Lawn", 0.5, 0.01, 0).getCost());
        assertEquals(0.00, new Job(2, "Luis", "Hedges", 1, 0, 0).getCost());
    }

    @Test
    void acceptsTheDocumentedUpperLimits() {
        Job job = new Job(Integer.MAX_VALUE - 1, "a".repeat(100), "b".repeat(500),
            168, 10_000, 1_000_000);

        assertEquals(Integer.MAX_VALUE - 1, job.getID());
        assertEquals(2_680_000, job.getCost());
    }

    @Test
    void rejectsInvalidIdsAndText() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class,
                () -> new Job(0, "Ana", "Lawn", 1, 1, 1)),
            () -> assertThrows(IllegalArgumentException.class,
                () -> new Job(Integer.MAX_VALUE, "Ana", "Lawn", 1, 1, 1)),
            () -> assertThrows(IllegalArgumentException.class,
                () -> new Job(1, null, "Lawn", 1, 1, 1)),
            () -> assertThrows(IllegalArgumentException.class,
                () -> new Job(1, " ", "Lawn", 1, 1, 1)),
            () -> assertThrows(IllegalArgumentException.class,
                () -> new Job(1, "Ana", " ", 1, 1, 1)),
            () -> assertThrows(IllegalArgumentException.class,
                () -> new Job(1, "a".repeat(101), "Lawn", 1, 1, 1)),
            () -> assertThrows(IllegalArgumentException.class,
                () -> new Job(1, "Ana", "b".repeat(501), 1, 1, 1)),
            () -> assertThrows(IllegalArgumentException.class,
                () -> new Job(1, "Ana|Luis", "Lawn", 1, 1, 1)),
            () -> assertThrows(IllegalArgumentException.class,
                () -> new Job(1, "Ana\t", "Lawn", 1, 1, 1)),
            () -> assertThrows(IllegalArgumentException.class,
                () -> new Job(1, "Ana", "Mow\nlawn", 1, 1, 1))
        );
    }

    @Test
    void rejectsInvalidRangesPrecisionAndNonfiniteNumbers() {
        for (double hours : new double[] {0, -1, 168.01, 1.001, Double.NaN}) {
            assertThrows(IllegalArgumentException.class,
                () -> new Job(1, "Ana", "Lawn", hours, 1, 1));
        }

        for (double rate : new double[] {-0.01, 10_000.01, 1.001, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                () -> new Job(1, "Ana", "Lawn", 1, rate, 1));
        }

        for (double cost : new double[] {-0.01, 1_000_000.01, 1.001, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                () -> new Job(1, "Ana", "Lawn", 1, 1, cost));
        }
    }
}
