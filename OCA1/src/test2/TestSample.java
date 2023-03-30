package test2;

import java.time.LocalDate;
import java.time.Month;

import test.*;

import static test.Student.*;

public class TestSample {

    public static void main(String[] args) {
        LocalDate ld1 = LocalDate.parse("2022-03-13");
        LocalDate ld2 = LocalDate.parse("2022-03-13");
        LocalDate ld3 = LocalDate.of(2022,Month.MARCH,13);
//        System.out.println(ld1.equals(ld2) + " : " + ld1.equals(ld3));
//        System.out.println(ld1.isEqual(ld2) + " : " + ld1.isEqual(ld3));
        String[] arr = new String[7];
//        System.out.println(arr);
        Student std = null;
        Student std1 = new Student();
        std.play();
        std1.conCat();
//        std.age;
        play();
        Student.play();
        new Student().conCat();
    }
}
