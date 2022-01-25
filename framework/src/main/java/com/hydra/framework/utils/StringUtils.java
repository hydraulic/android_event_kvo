package com.hydra.framework.utils;

public final class StringUtils {

    public static String combineStr(Object... objects) {
        StringBuilder sb = new StringBuilder();

        for (Object object : objects) {
            sb.append(object.toString());
        }

        return sb.toString();
    }

    public static String safeCombineStr(Object... objects) {
        StringBuilder sb = new StringBuilder();

        for (Object object : objects) {
            sb.append(object == null ? "" : object.toString());
        }

        return sb.toString();
    }
}
