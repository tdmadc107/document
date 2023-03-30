package test;

import java.util.function.Predicate;

/**
 * @author Tien Dat
 *
 */
public class Student {

    private String name;
    protected int age;

    public static void play() {
        System.out.println("Play");
    }

    Predicate<String> p = (String s) -> s.isEmpty();

    public void hard() {
        System.out.println("hard");
    }

    public void conCat() {
        play();
        hard();
    }

    public Student() {
//        this.name = name;
//        this.age = age;
    }

//    public boolean equals(Student obj) {
//        if (obj instanceof Student) {
//            Student std = (Student) obj;
//            if(this.name.equals(std.name) && this.age == std.age) {
//                return true;
//            }
//        }
//        return false;
////        return super.equals(obj);
//    }

    @Override
    public String toString() {
        return "Std: " + name + " and " + age;
    }

}
