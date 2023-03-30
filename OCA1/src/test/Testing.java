package test;

import java.util.ArrayList;
import java.util.List;

public class Testing {

    public static void main(String[] args) {
        List<String> students = new ArrayList<>();
        String a = "N";
        students.add(a);
        students.add(new String("N"));
        students.add(new String("M"));
        students.add(new String("B"));
        students.add(new String("V"));

        students.remove(new String("N"));

        for (String std : students) {
            System.out.println(std);
        }

        Base b = null;
        Derived d = (Derived) b;
        d.m1();
    }
}
