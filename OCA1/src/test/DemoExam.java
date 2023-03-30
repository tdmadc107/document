package test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DemoExam {

    public static void main(String[] args) {
        Base b = new Derived();
        Base d = (Derived) b;
        b.m1();
    }

    protected void print(int... b) {
        System.out.println("b: " + Arrays.toString(b));
        if (b.length > 0) {
            for (int i = 0; i < b.length; i++) {
                System.out.println("b[" + i + "] = " + b[i]);
            }
        }
    }

//    static class EmoExam {
//
//        public static void main(String[] args) {
//            print(1, 2);
//        }
//    }
}
