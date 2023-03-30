package oca2;

import java.util.Arrays;
import java.util.List;

import test.DemoExam;

public class ArrayToList {

    public static void main(String[] args) {
        String[] array1 = { "a", "b", "c" };
        String[] array2 = new String[3];

        array2[0] = "one";
        array2[1] = "two";

        List<String> ls1 = Arrays.asList(array1);
        List<String> ls2 = Arrays.asList(array2);
        
//        ls2.add("three");
        
//        System.out.println(ls1);
//        System.out.println(ls2);
//        System.out.println(Arrays.toString(array2));
        
        ClassB cb = new ClassB();
        System.out.println(cb.str.toString());
//        cb.print();
    };
}
