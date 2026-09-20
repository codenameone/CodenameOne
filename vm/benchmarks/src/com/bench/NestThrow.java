package com.bench;

public class NestThrow {
    static int rethrowFromHandler() {
        int r = 0;
        try {
            try {
                r += 1;
                throw new IllegalStateException("inner");
            } catch (IllegalStateException e) {
                r += 2;
                throw new IllegalArgumentException("outer");
            }
        } catch (IllegalArgumentException e) {
            r += 4;
        }
        return r;
    }

    static int throwFromHandlerNoNest() {
        int r = 0;
        try {
            r += 1;
            throw new IllegalStateException("inner");
        } catch (IllegalStateException e) {
            r += 2;
        }
        return r;
    }

    static int rethrowSame() {
        int r = 0;
        try {
            try {
                throw new IllegalStateException("a");
            } catch (IllegalStateException e) {
                r += 2;
                throw e;
            }
        } catch (IllegalStateException e) {
            r += 4;
        }
        return r;
    }

    static int throwAfterInnerTry() {
        int r = 0;
        try {
            try {
                r += 1;
            } catch (IllegalStateException e) {
                r += 2;
            }
            throw new IllegalArgumentException("outer");
        } catch (IllegalArgumentException e) {
            r += 4;
        }
        return r;
    }

    public static void main(String[] args) {
        System.out.println("throwFromHandlerNoNest " + throwFromHandlerNoNest());
        System.out.println("throwAfterInnerTry     " + throwAfterInnerTry());
        System.out.println("rethrowSame            " + rethrowSame());
        System.out.println("rethrowFromHandler     " + rethrowFromHandler());
    }
}
