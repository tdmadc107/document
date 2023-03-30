package oca3;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import static java.util.Arrays.*;

public class StaticAndInstance {

    public static int num;
    public int total;
    private static final ArrayList<String> values = new ArrayList<>();

    public StaticAndInstance() {
        num++;
    }

    public static void a() {
        System.out.println("a");
    }

    public void b() {
        System.out.println("b");
    }

    public static void c() {
        a();
//        b();
    }

    public void d() {
        a();
        c();
        b();
    }

    public static void main(String[] args) {
        StaticAndInstance sat1 = new StaticAndInstance();
        StaticAndInstance sat2 = new StaticAndInstance();
        StaticAndInstance sat3 = new StaticAndInstance();
        List<String> values1 = new ArrayList<>();
        String[] sArray = new String[3];
        values1 = Arrays.asList(sArray);
        values.add(new String("Hello"));
        values1.set(0, "Hello");
        String s = "Hello";
        String sObj = new String("Hello");
        System.out.println("pre: " + values);
        System.out.println("pre: " + values1);
        values.remove(s);
//        values1.remove(sObj);
//        values = values1;
        System.out.println(num);
        System.out.println(values);
        System.out.println(values1);
    }
}
