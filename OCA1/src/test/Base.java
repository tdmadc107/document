package test;

public class Base {

    int var = 10;

    public void m1() {
        System.out.println("Base");
    }
    
    void base() {
        System.out.println("no");
    }

    Base() {
        this.m1();
    }
}
