package test;

import java.util.ArrayList;
import java.util.List;

public class Derived extends Base {

    private int var =5;
    public void m1() {
        base();
        System.out.println("Derived");
        return;
    };

    Derived() {
        super();
    }
    
    public static void main(String[] args) {
        List<Integer> l = new ArrayList<>();
        byte b = 10;
        l.add((int) b);
        
        List<Byte> lb = new ArrayList<>();
        int i = 10;
        lb.add((byte) i);
    }
}
