// LunarCalendar.java
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LunarCalendar {
    private static final String[] ZODIAC_SIGNS = {"Овен","Телец","Близнецы","Рак","Лев","Дева",
            "Весы","Скорпион","Стрелец","Козерог","Водолей","Рыбы"};

    private static final Map<String, Map<String, String>> RECOMMENDATIONS = new HashMap<>();
    static {
        Map<String, String> m1 = new HashMap<>();
        m1.put("посадка", "неблагоприятно"); m1.put("полив", "допустимо");
        m1.put("обрезка", "неблагоприятно"); m1.put("урожай", "неблагоприятно"); m1.put("вредители", "благоприятно");
        RECOMMENDATIONS.put("новолуние", m1);
        Map<String, String> m2 = new HashMap<>();
        m2.put("посадка", "благоприятно"); m2.put("полив", "благоприятно");
        m2.put("обрезка", "неблагоприятно"); m2.put("урожай", "неблагоприятно"); m2.put("вредители", "неблагоприятно");
        RECOMMENDATIONS.put("первая четверть", m2);
        Map<String, String> m3 = new HashMap<>();
        m3.put("посадка", "неблагоприятно"); m3.put("полив", "допустимо");
        m3.put("обрезка", "благоприятно"); m3.put("урожай", "благоприятно"); m3.put("вредители", "благоприятно");
        RECOMMENDATIONS.put("полнолуние", m3);
        Map<String, String> m4 = new HashMap<>();
        m4.put("посадка", "неблагоприятно"); m4.put("полив", "неблагоприятно");
        m4.put("обрезка", "благоприятно"); m4.put("урожай", "благоприятно"); m4.put("вредители", "благоприятно");
        RECOMMENDATIONS.put("последняя четверть", m4);
    }

    @Parameter(names = "--date", description = "Дата YYYY-MM-DD")
    private String dateStr;

    @Parameter(names = "--json")
    private String jsonFile;

    @Parameter(names = "--csv")
    private String csvFile;

    @Parameter(names = "--verbose")
    private boolean verbose;

    @Parameter(names = "--no-color")
    private boolean noColor;

    static class LunarData {
        String date;
        int lunarDay;
        String phase;
        String zodiacSign;
        Map<String, String> recommendations;
    }

    private LunarData calculate(LocalDate date) {
        // Базовое новолуние: 2000-01-06 18:14 UTC
        Instant base = Instant.parse("2000-01-06T18:14:00Z");
        Instant target = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        double diff = Duration.between(base, target).toSeconds() / 86400.0;
        double lunarAge = diff % 29.53058867;
        int lunarDay = (int)Math.floor(lunarAge) + 1;
        if (lunarDay > 30) lunarDay = 30;
        String phase;
        if (lunarDay <= 1 || lunarDay > 29) phase = "новолуние";
        else if (lunarDay <= 7) phase = "первая четверть";
        else if (lunarDay <= 14) phase = "полнолуние";
        else if (lunarDay <= 22) phase = "последняя четверть";
        else phase = "новолуние";
        // Знак зодиака
        Instant start = Instant.parse("2000-01-01T00:00:00Z");
        double totalDays = Duration.between(start, target).toSeconds() / 86400.0;
        double longitude = (180 + totalDays * 13.176) % 360;
        int signIndex = (int)Math.floor(longitude / 30) % 12;
        String zodiacSign = ZODIAC_SIGNS[signIndex];

        LunarData data = new LunarData();
        data.date = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        data.lunarDay = lunarDay;
        data.phase = phase;
        data.zodiacSign = zodiacSign;
        data.recommendations = RECOMMENDATIONS.getOrDefault(phase, new HashMap<>());
        return data;
    }

    private void printData(LunarData data, boolean color) {
        if (color) {
            System.out.println("\u001B[36m🌙 Лунный календарь на " + data.date + "\u001B[0m");
            System.out.println("\u001B[33mЛунный день: " + data.lunarDay + "\u001B[0m");
            System.out.println("\u001B[35mФаза: " + data.phase + "\u001B[0m");
            System.out.println("\u001B[32mЗнак зодиака Луны: " + data.zodiacSign + "\u001B[0m");
            if (verbose && data.recommendations != null) {
                System.out.println("\u001B[37mРекомендации:\u001B[0m");
                for (Map.Entry<String, String> e : data.recommendations.entrySet()) {
                    String col = e.getValue().equals("благоприятно") ? "\u001B[32m" :
                            e.getValue().equals("неблагоприятно") ? "\u001B[31m" : "\u001B[33m";
                    System.out.printf("  - %s: %s%s\u001B[0m%n", e.getKey(), col, e.getValue());
                }
            }
        } else {
            System.out.println("🌙 Лунный календарь на " + data.date);
            System.out.println("Лунный день: " + data.lunarDay);
            System.out.println("Фаза: " + data.phase);
            System.out.println("Знак зодиака Луны: " + data.zodiacSign);
            if (verbose && data.recommendations != null) {
                System.out.println("Рекомендации:");
                for (Map.Entry<String, String> e : data.recommendations.entrySet()) {
                    System.out.println("  - " + e.getKey() + ": " + e.getValue());
                }
            }
        }
    }

    private void exportJSON(LunarData data) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(data);
        Files.write(Paths.get(jsonFile), json.getBytes());
        System.out.println("Результат сохранён в " + jsonFile);
    }

    private void exportCSV(LunarData data) throws IOException {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(csvFile)))) {
            pw.println("date,lunar_day,phase,zodiac_sign,recommendations");
            String recStr = "";
            if (data.recommendations != null) {
                List<String> parts = new ArrayList<>();
                for (Map.Entry<String, String> e : data.recommendations.entrySet()) {
                    parts.add(e.getKey() + ":" + e.getValue());
                }
                recStr = String.join("; ", parts);
            }
            pw.printf("%s,%d,%s,%s,\"%s\"%n", data.date, data.lunarDay, data.phase, data.zodiacSign, recStr);
        }
        System.out.println("Результат сохранён в " + csvFile);
    }

    public void run() throws Exception {
        LocalDate date = (dateStr == null) ? LocalDate.now(ZoneOffset.UTC) : LocalDate.parse(dateStr);
        LunarData data = calculate(date);
        boolean color = !noColor && System.console() != null;
        printData(data, color);

        if (jsonFile != null) exportJSON(data);
        if (csvFile != null) exportCSV(data);
    }

    public static void main(String[] args) throws Exception {
        LunarCalendar cal = new LunarCalendar();
        JCommander.newBuilder().addObject(cal).build().parse(args);
        cal.run();
    }
}
