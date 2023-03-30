package com.exam.oop1234;

import java.sql.SQLException;
import java.util.Arrays;

interface Lion extends Human {

    int heigth = 20;
//    @Override
//    public int getHeigth() {
//        // TODO Auto-generated method stub
//        return 100;
//    }

//    @Override
//    public double getHeigth() throws SQLException {
//        // TODO Auto-generated method stub
//        return 0;
//    }

    @Override
    default double getAge() {
        // TODO Auto-generated method stub
        return 0;
    }

//    @Override
//    public int getAge(int age) {
//        // TODO Auto-generated method stub
//        return age + getHeigth();
//    }

//    public static void main(String[] args) {
//        Animal[] as = new Animal[2];
//        for(Animal arrs: as) {
//            System.out.println(arrs.heigth);
//        }
//        System.out.println(Arrays.toString(as));
//        Animal a = new Lion();
//        System.out.println(a.getHeigth());
//        System.out.println(Animal.age);
//        Animal.setAge(9);
//    }
}
