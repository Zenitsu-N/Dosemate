import java.time.*;
import java.util.*;


public class DoseMate {

    record Medicine(int id, String name, String dosage, int dosesPerDay, int days, int tablets) { }

    static class Dose {                          // one scheduled dose of one medicine
        Medicine med;
        LocalDateTime time;
        String status = "PENDING";               // PENDING, TAKEN or MISSED
        Dose(Medicine med, LocalDateTime time) { this.med = med; this.time = time; }
    }

    static List<Medicine> medicines = new ArrayList<>();
    static List<Dose> doses = new ArrayList<>();
    static Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n===== DoseMate =====\n1. Add a Medicine\n2. View Today's Medicine Schedule\n"
                    + "3. Mark a Dose as Taken or Missed\n4. View Adherence Percentage\n"
                    + "5. Check Remaining Medicine Stock\n6. Exit");
            switch (ask("Choose an option: ")) {
                case "1" -> addMedicine();
                case "2" -> showToday();
                case "3" -> markDose();
                case "4" -> showAdherence();
                case "5" -> showStock();
                case "6" -> { System.out.println("Goodbye!"); return; }
                default -> System.out.println("Error: enter a number from 1 to 6.");
            }
        }
    }

    // 1. Add a medicine
    static void addMedicine() {
        String name = ask("Medicine name: ");
        if (name.isEmpty()) { System.out.println("Error: name cannot be empty."); return; }
        String dosage = ask("Dosage (e.g. 500 mg): ");
        if (dosage.isEmpty()) { System.out.println("Error: dosage cannot be empty."); return; }

        TreeSet<LocalTime> times = new TreeSet<>();               // sorted, no duplicates
        try {
            for (String t : ask("Times (HH:mm, comma separated, e.g. 08:00,21:00): ").split(","))
                if (!times.add(LocalTime.parse(t.trim()))) {
                    System.out.println("Error: duplicate time " + t.trim() + ".");
                    return;
                }
        } catch (Exception e) {
            System.out.println("Error: invalid time, use HH:mm like 08:00.");
            return;
        }

        int days = askInt("Number of days: ");
        if (days < 1) { System.out.println("Error: days must be a whole number, at least 1."); return; }
        int tablets = askInt("Total tablets available: ");
        if (tablets < 0) { System.out.println("Error: tablets must be a whole number, 0 or more."); return; }

        Medicine m = new Medicine(medicines.size() + 1, name, dosage, times.size(), days, tablets);
        medicines.add(m);
        for (int d = 0; d < days; d++)                            // one dose per day per time
            for (LocalTime t : times)
                doses.add(new Dose(m, LocalDateTime.of(LocalDate.now().plusDays(d), t)));
        System.out.println("Added " + name + ".");
    }

    // 2. Today's schedule in time order (also used by option 3)
    static List<Dose> showToday() {
        List<Dose> list = new ArrayList<>();
        for (Dose d : doses)
            if (d.time.toLocalDate().equals(LocalDate.now())) list.add(d);
        list.sort((a, b) -> a.time.compareTo(b.time));

        if (list.isEmpty()) System.out.println("No doses scheduled for today. Add a medicine first.");
        for (int i = 0; i < list.size(); i++) {
            Dose d = list.get(i);
            System.out.printf("%d. %s  %-12s %-8s %s%n", i + 1, d.time.toLocalTime(), d.med.name(), d.med.dosage(), d.status);
        }
        return list;
    }

    // 3. Mark a dose as taken or missed
    static void markDose() {
        List<Dose> list = showToday();
        if (list.isEmpty()) return;

        int n = askInt("Dose number: ");
        if (n < 1 || n > list.size()) { System.out.println("Error: invalid dose number."); return; }
        Dose d = list.get(n - 1);
        if (!d.status.equals("PENDING")) {
            System.out.println("Error: this dose is already marked as " + d.status + ".");
            return;
        }

        String c = ask("Taken or Missed? (T/M): ").toUpperCase();
        if (!c.equals("T") && !c.equals("M")) { System.out.println("Error: enter T or M."); return; }
        if (c.equals("T") && remaining(d.med) <= 0) {
            System.out.println("Error: no tablets left for " + d.med.name() + ".");
            return;
        }
        d.status = c.equals("T") ? "TAKEN" : "MISSED";
        System.out.println("Dose marked as " + d.status + ".");
    }

    // 4. Adherence % = taken / (taken + missed) x 100
    static void showAdherence() {
        if (medicines.isEmpty()) System.out.println("No medicines added yet.");
        for (Medicine m : medicines) {
            int taken = count(m, "TAKEN"), missed = count(m, "MISSED");
            if (taken + missed == 0)
                System.out.println(m.name() + ": no doses recorded yet");
            else
                System.out.printf("%s: %.1f%% (taken %d, missed %d)%n", m.name(), taken * 100.0 / (taken + missed), taken, missed);
        }
    }

    // 5. Remaining stock, alert if it lasts fewer than 3 days
    static void showStock() {
        if (medicines.isEmpty()) System.out.println("No medicines added yet.");
        for (Medicine m : medicines) {
            int left = remaining(m);
            double daysLeft = (double) left / m.dosesPerDay();    // 1 tablet per dose
            System.out.printf("%s: %d tablets, %.1f days left%n", m.name(), left, daysLeft);
            if (daysLeft < 3) System.out.println("  *** ALERT: fewer than 3 days of " + m.name() + " left! ***");
        }
    }

    // ---------- helpers ----------
    static int count(Medicine m, String status) {
        int n = 0;
        for (Dose d : doses)
            if (d.med == m && d.status.equals(status)) n++;
        return n;
    }

    static int remaining(Medicine m) { return m.tablets() - count(m, "TAKEN"); }

    static String ask(String prompt) { System.out.print(prompt); return in.nextLine().trim(); }

    static int askInt(String prompt) {                // -1 if the input is not a number
        try { return Integer.parseInt(ask(prompt)); }
        catch (NumberFormatException e) { return -1; }
    }
}