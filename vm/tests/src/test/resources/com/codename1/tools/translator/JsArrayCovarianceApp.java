public class JsArrayCovarianceApp {
    public static int result;

    static class Animal {
    }

    static class Dog extends Animal {
    }

    public static void main(String[] args) {
        Dog[] dogs = new Dog[1];
        Dog[][] dogGrid = new Dog[1][];
        dogGrid[0] = new Dog[1];
        int score = 0;

        if (dogs instanceof Dog[]) {
            score |= 1;
        }
        if (dogs instanceof Animal[]) {
            score |= 2;
        }
        if (dogs instanceof Object[]) {
            score |= 4;
        }
        if (dogGrid instanceof Dog[][]) {
            score |= 8;
        }
        if (dogGrid instanceof Animal[][]) {
            score |= 16;
        }
        if (dogGrid instanceof Object[]) {
            score |= 32;
        }
        if (dogGrid[0] instanceof Animal[]) {
            score |= 64;
        }
        if (((Animal[]) dogs).length == 1) {
            score |= 128;
        }
        if (((Object[]) dogGrid).length == 1) {
            score |= 256;
        }
        result = score;
    }
}
