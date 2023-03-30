package oca1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ClassA {

    public static final String MONDAY = "MONDAY";

    public static void main(String[] args) {
        String[] strs[] = new String[3][2];
//        Arrays.sort(numbers);
//        Arrays.sort(numbers, ((int num1, int num2) -> num2 - num1));
//        System.out.println(Arrays.binarySearch(numbers, 9));
        strs[0][0] = "a";
        strs[1][0] = "a";
        String s1 = null;
        String s2 = "";
        String s3 = " ";
        String s4 = new String("6");
        System.out.println(s4.isEmpty());
        ArrayList<String> arrays = new ArrayList<>();
        List<String> arrays1 = new LinkedList<>();
        arrays1.add("1");
        arrays1.add("2");
        arrays.add("1");
        arrays.add("2");
        arrays.add("3");
        System.out.println("equal: "+arrays.equals(arrays1));
        System.out.println("contain: "+arrays1.contains("1"));
        System.out.println(arrays1);
        System.out.println(arrays1.set(0, "b"));
        System.out.println(arrays1);
        System.out.println(Arrays.deepToString(strs));
        System.out.println(arrays.isEmpty());
        System.out.println(arrays1.isEmpty());
        System.out.println(arrays.isEmpty());
        int a = Integer.parseInt("2");
        int s5 = String.valueOf(3).concat(s4).indexOf("3");
        System.out.println(s5);
        Boolean isFlag = Boolean.valueOf("");
        char c = Character.valueOf('v');
        byte b = Byte.parseByte("20");
        System.out.println(isFlag);
        System.out.println(c);
        System.out.println(b);
        
        List<Integer> numbers = new ArrayList<>();
        numbers.add(1);
        numbers.add(2);
        numbers.remove(2);
        System.out.println(numbers);
    }
}
