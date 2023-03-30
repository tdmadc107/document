package test2;

import java.util.Arrays;

public class TestOCA {

    public static void main(String[] args) {
        String s = "Hello";
        short x = 7, y = 200;
        System.out.println(String.valueOf(x + y));
        System.out.println(String.valueOf(x + y).length());
        final boolean flag;
        flag = false;
        while (flag) {
            System.out.println("out");
        }

        int[][] arr = new int[1][2];
        arr[0][1] = 100;
        System.out.println(Arrays.toString(arr));
    }
}
