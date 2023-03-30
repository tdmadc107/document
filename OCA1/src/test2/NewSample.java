package test2;

import java.time.DateTimeException;

public class NewSample {

    public NewSample(String s) {
        
    }
    
    private static String s;
    public static void main(String[] args) {
        try {
            System.out.println(s.length());
        } catch (Exception  ex) {
            System.out.println("Done");
        }
    }
}
