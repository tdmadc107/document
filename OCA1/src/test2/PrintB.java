package test2;

public class PrintB  implements PrintA{

    int i = 40;
    
    public static void main(String[] args) {
        PrintA a = new PrintB();
        System.out.println(PrintA.i);
    }
    
}
