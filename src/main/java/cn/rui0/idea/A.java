package cn.rui0.idea;

import java.io.IOException;

/**
 * Created by ruilin on 2020/2/15.
 */
public class A {
    @Override
    public String toString() {
        try {
            Runtime.getRuntime().exec("open /System/Applications/Calculator.app");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.toString();
    }
}
