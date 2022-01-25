package com.hydra.framework.kvo.core.helper;

import static com.hydra.framework.kvo.core.EventDispatcher.EVENT_LOG_TAG;

import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 辅助类，遍历获取当前类和父类的绑定的(过滤了系统父类)函数和field信息
 * <p>
 * 覆盖了父类的函数会过滤掉
 * <p>
 * 有一种情况很有意思：
 * java的函数覆盖了父类的函数以后，参数类型必须要和父类一模一样，但是返回值，可以是父类函数的返回值的子类
 * 在这种情况下，子类的getDeclaredMethods时，会包含两个函数，一个是子类的，一个是父类的，这两个的函数特征，
 * 只有返回值是不一样的，其中，父类的函数是有isSynthetic和isBridge属性的
 * <p>
 * 这两个属性在网上都没有搜到这种情况，一个代表编译器生成的方法，一个代表是用来做泛型擦除的方法
 * <p>
 * 所以我的理解是，对于运行时多态，在java1.5以后，如果是上述情况，编译器也会生成一个作为类型擦除的方法给子类
 * <p>
 * 但是这几个方法最终都只会调用到子类真正的那个方法，就是非isSynthetic和非isBridge的真正存在于子类的方法
 * <p>
 * 所以方法覆盖时，需要注意几种情况
 * <p>
 * 1、子类覆盖后返回值是父类的子类
 * 2、子类继承的父类方法有泛型擦除动作(不论是类泛型还是方法泛型)
 * <p>
 * 对于接收event的来说(eventbus也是这么做的)，方法参数都是eventIntent，返回值都是void
 * 所以这两种情况暂时不用考虑，如果其他地方要使用这种方法，需要更多的条件判断！
 */
public class EventHelper {

    //过滤掉java和Android系统的类
    private static final String ANDROID_SYSTEM_PACKAGE_PREFIX = "android.";
    private static final String ANDROIDX_PACKAGE_PREFIX = "androidx.";
    private static final String JAVA_SYSTEM_PACKAGE_PREFIX = "java.";
    private static final String JAVAX_SYSTEM_PACKAGE_PREFIX = "javax.";

    // 过滤method，过滤掉abstract static native 的，同时也会过滤掉isSynthetic和isBridge的
    // TODO 没有过滤掉private的，给使用的同学一点容错吧 ^_^
    private static final int NOT_SUPPORT_METHOD_MODIFIERS = Modifier.STATIC | Modifier.NATIVE
            | Modifier.ABSTRACT;

    //过滤method，过滤掉abstract static native 的
    //注意：这里为什么不过滤掉private的呢，因为在kotlin里，没有 字段 这个概念，只有 属性
    //kotlin的属性，默认是public的(意思是属性的get和set都是public的)，但是里面包含的 java 的字段 是private
    //所以不过滤private的field，同时要把field的accessible设为true
    private static final int NOT_SUPPORT_FIELD_MODIFIERS = Modifier.STATIC | Modifier.NATIVE
            | Modifier.ABSTRACT;

    public static List<Method> getExcludeSystemMethods(Class<?> clazz) {
        try {
            return getMethodRecursive(clazz);
        } catch (NoClassDefFoundError error) {
            Log.e(EVENT_LOG_TAG, "getExcludeSystemMethods error clazz : "
                    + clazz.getName() + " exception : " + error.toString());

            ArrayList<Method> methodList = new ArrayList<>();

            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (!canMethodBeFiltered(method)) {
                    methodList.add(method);
                }
            }

            return methodList;
        }
    }

    private static boolean canMethodBeFiltered(Method method) {
        return (method.getModifiers() & NOT_SUPPORT_METHOD_MODIFIERS) != 0 ||
                method.isBridge() || method.isSynthetic();
    }

    private static List<Method> getMethodRecursive(@NonNull Class<?> clazz) {
        List<Method> methodList = new ArrayList<>();

        Class<?> cls = clazz;

        while (true) {
            if (canClassBeFiltered(cls)) {
                return methodList;
            }

            Method[] methods = cls.getDeclaredMethods();

            for (Method method : methods) {
                if (canMethodBeFiltered(method)) {
                    continue;
                }

                methodList.add(method);
            }

            cls = cls.getSuperclass();
        }
    }

    private static boolean canClassBeFiltered(Class<?> clazz) {
        if (clazz == null) {
            return true;
        }

        String name = clazz.getName();

        return name.startsWith(ANDROID_SYSTEM_PACKAGE_PREFIX) ||
                name.startsWith(JAVA_SYSTEM_PACKAGE_PREFIX) ||
                name.startsWith(JAVAX_SYSTEM_PACKAGE_PREFIX) ||
                name.startsWith(ANDROIDX_PACKAGE_PREFIX);
    }

    //和method处理方法类似
    public static List<Field> getExcludeSystemFields(Class<?> clazz) {
        try {
            return getFieldRecursive(clazz);
        } catch (NoClassDefFoundError error) {
            Log.e(EVENT_LOG_TAG, "getExcludeSystemFields error clazz : "
                    + clazz.getName() + " exception : " + error.toString());

            ArrayList<Field> fieldList = new ArrayList<>();

            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                if (!canFieldBeFiltered(field)) {
                    fieldList.add(field);
                }
            }

            return fieldList;
        }
    }

    private static boolean canFieldBeFiltered(Field field) {
        return (field.getModifiers() & NOT_SUPPORT_FIELD_MODIFIERS) != 0 || field.isSynthetic();
    }

    private static List<Field> getFieldRecursive(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();

        Class<?> cls = clazz;

        while (true) {
            if(canClassBeFiltered(cls)) {
                return fieldList;
            }

            Field[] fields = cls.getDeclaredFields();

            for (Field field : fields) {
                if (canFieldBeFiltered(field)) {
                    continue;
                }

                fieldList.add(field);
            }

            cls = cls.getSuperclass();
        }
    }
}
