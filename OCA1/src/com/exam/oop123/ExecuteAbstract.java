package com.exam.oop123;

public class ExecuteAbstract {

    public static void main(String[] args) {
        String[] arr = new String[7];
        System.out.println(arr);
        Animal a1 = new Cat();
        Animal a2 = new Mun();
        try {
            System.out.println(a1.getName("Luis"));
            System.out.println(((Cat)a1).getName());
            System.out.println(a2.getName("Mun"));
        } catch (RuntimeException e) {
            System.out.println("Eror");
        }
    }
}
