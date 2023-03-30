package com.exam.oop123;

import com.exam.oop1234.Animal;

public class Cat extends com.exam.oop123.Animal implements Animal {

    @Override
    public double getHeigth() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    String getName(String name){
        // TODO Auto-generated method stub
        return "Hi";
    }

    String getName() {
        return "Overload";
    }
}
