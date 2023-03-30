package test2;

public class Word extends Document {

    String type;
    Word(String type) {
        super(20);
        this.type = type;
    }

    Word(int pages, String type) {
        this(type);
        super.pages = pages;
    }
}
