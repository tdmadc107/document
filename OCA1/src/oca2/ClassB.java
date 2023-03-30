package oca2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import oca1.ClassA;
import test.DemoExam;

public class ClassB extends DemoExam {
    public static void main(String[] args) {
        List<String> ls = new ArrayList<String>();
        ls.add("a");
        ls.add("c");
        ClassA a;

//        String [] array = ls.toArray(new String[0]);
//        array[1] = "b";
//        System.out.println(Arrays.toString(array));

        System.out.println(ls);
        
//        print(1, 2);
    }
    public void printRa() {
        ClassB clB = new ClassB();
        clB.print(1, 3, 5);
    }
     String str = "Hello";
}
