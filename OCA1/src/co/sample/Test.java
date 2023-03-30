package co.sample;

import java.time.LocalDate;

public class Test {
    static int i;
    static int n;

    static int add() {
        return i + n;
    }

    static int num = n + i;

    public static void main(String... args) {
        StringBuilder sb = new StringBuilder("TOMATO");
        String s = "Misss ";
        System.out.println(s.replace("ss", "t"));
        LocalDate ld = LocalDate.parse("2020-08-31");
        System.out.println(ld.plusMonths(-6));
        byte b1 = 10;
        Integer i1 = (int) b1;
        System.out.println();
//        byte b2 = i1;
        
        String a1 [], a2, a3 = null;
        a1 = new String[2];
        a1[0] = "a";
        a1[1] = "b";
//        a3 = a2 = a1;
        
        ParentOne p = new ChildOne();
        ((ChildOne) p).check();
        ((CheckLog) p).check();
        ((ChildOne)(CheckLog) p).check();
        ((CheckLog)(ChildOne) p).check();
//        ((ParentOne)(ChildOne)(CheckLog) p).check();
    }
}
