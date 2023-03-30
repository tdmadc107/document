package oca2;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Period;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class DateAndTimes {

    public static void main(String[] args) {
        System.out.println(LocalDate.now());
        System.out.println(LocalTime.now());
        System.out.println(LocalDateTime.now());
        System.out.println(LocalDate.of(2023, Month.MARCH, 23));
        System.out.println(LocalDate.of(2023, 1, 23));

        LocalDateTime dateTime = LocalDateTime.now().minusDays(2).minusHours(1).plusMonths(2);
        System.out.println("ex1: " + dateTime);

        Period p = Period.ofMonths(2);

        System.out.println("ex2: " + dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        DateTimeFormatter dateTimeFormat1 = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
        DateTimeFormatter dateTimeFormat2 = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
        System.out.println("ex3: " + dateTimeFormat1.format(dateTime));
        System.out.println("ex3: " + dateTimeFormat2.format(dateTime));

        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MM yyyy");
        LocalDate date = LocalDate.parse("01 02 2015", f);
        LocalTime time = LocalTime.parse("11:22");
        System.out.println("ex4:" + date); // 2015-01-02
        System.out.println("ex4:" + time); // 11:22
        
        System.out.println(Period.of(0, 100, 0));
    }
}
