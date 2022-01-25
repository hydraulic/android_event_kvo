package com.hydra.framework.utils;

public class JFlagUtil {

    public static boolean isFlag(int flag, int mask) {
        return (flag & mask) == mask;
    }

    public static int addFlag(int flag, int mask) {
        return flag | mask;
    }
}
