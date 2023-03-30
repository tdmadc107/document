package co.oca.java.sample;

public class Execute {

    static public void main(String... argument) {
        Animal a = new Animal();
//        Dog d = a; // Compile error
//        Dog d = (Dog) a; // Throw exception at runtime

        Animal animal = new Dog();

        a.showInfomation();
        animal.showInfomation();
    }
}
