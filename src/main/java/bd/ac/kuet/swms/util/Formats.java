package bd.ac.kuet.swms.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Formats {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Formats() { }

    public static String money(double value) { return String.format("Tk %,.2f", value); }

    public static String wholeMoney(double value) { return String.format("Tk %,.0f", value); }

    public static String money(double value, String currency) { return String.format("%s %,.2f", currency, value); }

    public static String timeAgo(LocalDateTime time) {
        Duration age = Duration.between(time, LocalDateTime.now());
        if (age.toMinutes() < 1) return "just now";
        if (age.toHours() < 1) return age.toMinutes() + " min ago";
        if (age.toDays() < 1) return age.toHours() + " h ago";
        return age.toDays() + " d ago";
    }

    public static String dateTime(LocalDateTime t) { return t == null ? "" : DATE_TIME.format(t); }
}
