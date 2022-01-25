package com.hydra.framework.kvo;

import static com.hydra.framework.kvo.helper.KvoHelper.getKvoMethods;

import android.util.Log;

import androidx.annotation.NonNull;

import com.hydra.framework.kvo.core.EventReceiver;
import com.hydra.framework.kvo.core.helper.DefaultEventThreadWrapper;
import com.hydra.framework.kvo.helper.KvoHelper.KvoMethodNode;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by Hydra.
 * <p>
 * 1、支持receiver在父类中绑定和通知，但尽量不要有函数覆盖哦
 * 2、支持KvoSource多层继承中父类的绑定和通知，但是注意父类和子类中可能会出现相同的key(即相同名字的成员变量)，
 * 有相同的key并且在使用子类绑定时，按照java的注解继承规则来使用
 */
public class Kvo {

    public static final String KVO_LOG_TAG = "KvoEvent";

    /**
     * 这个方法 和 带flag参数的方法 的区别是：
     * 这个方法是绑定此KvoSource下所有的方法，不指定flag，即包含所有flag的方法，一般我们使用时不指定flag，那就是所有flag都为0；
     * <p>
     * 所以没有把两个autoBindingTo方法做成统一调用，因为两个方法的本质是不同的
     */
    public static void autoBindingTo(@NonNull KvoSource source, @NonNull Object dst) {
        ArrayList<KvoMethodNode> nodes = getKvoMethods(source, dst);

        for (KvoMethodNode node : nodes) {
            KvoMethodAnnotation annotation = node.methodAnnotation;

            source.addBinding(source.declaredKvoField(annotation.name()).eventAction,
                    buildKvoEventReceiver(annotation, dst, node.method));
        }
    }

    /**
     * 自动绑定此 KvoSource 下的所有 指定flag 的方法
     * <p>
     * 这个是为了这种情况的：
     * 一个receiver类，同时绑定 多个 同样类型 的 不同对象；
     * <p>
     * 比如一个receiver，绑定了两个UserInfo，myUserInfo 和 otherUserInfo，此时就可以通过注解的flag来区分；
     * 如myUserInfo绑定所有flag=1的方法，otherUserInfo绑定所有flag=2的方法
     * <p>
     * 这个flag只用在当前receiver内做区分就可以了，不需要考虑全局唯一
     */
    public static void autoBindingTo(@NonNull KvoSource source, @NonNull Object dst, int flag) {
        ArrayList<KvoMethodNode> nodes = getKvoMethods(source, dst);

        for (KvoMethodNode node : nodes) {
            KvoMethodAnnotation annotation = node.methodAnnotation;

            if (annotation.flag() == flag) {
                source.addBinding(source.declaredKvoField(annotation.name()).eventAction,
                        buildKvoEventReceiver(annotation, dst, node.method));
            }
        }
    }

    /**
     * 用法同
     *
     * @see Kvo#autoBindingTo(KvoSource, Object)
     * 即 解绑所有，不指定flag；
     */
    public static void autoUnbindingFrom(@NonNull KvoSource source, @NonNull Object dst) {
        ArrayList<KvoMethodNode> nodes = getKvoMethods(source, dst);

        for (KvoMethodNode node : nodes) {
            KvoMethodAnnotation annotation = node.methodAnnotation;

            source.removeBinding(source.declaredKvoField(annotation.name()).eventAction,
                    buildKvoEventReceiver(annotation, dst, node.method));
        }
    }

    /**
     * 只解绑指定flag的，不过一般解绑时，也不会说指定flag，都是统一无脑解绑，这个不会常用
     */
    public static void autoUnbindingFrom(@NonNull KvoSource source, @NonNull Object dst, int flag) {
        ArrayList<KvoMethodNode> nodes = getKvoMethods(source, dst);

        for (KvoMethodNode node : nodes) {
            KvoMethodAnnotation annotation = node.methodAnnotation;

            if (annotation.flag() == flag) {
                source.removeBinding(source.declaredKvoField(annotation.name()).eventAction,
                        buildKvoEventReceiver(annotation, dst, node.method));
            }
        }
    }

    public static void addKvoBinding(@NonNull KvoSource source, @NonNull Object target, @NonNull String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName, KvoEventIntent.class);

            addKvoBinding(source, target, method);
        } catch (NoSuchMethodException e) {
            Log.e(KVO_LOG_TAG, "add kvo binding failed :" + e.toString());

            throw new RuntimeException("method " + methodName + " not found in class " +
                    target.getClass().getName() + ", please check again");
        }
    }

    public static void addKvoBinding(@NonNull KvoSource source, @NonNull Object target, @NonNull Method method) {
        KvoMethodAnnotation methodAnnotation = method.getAnnotation(KvoMethodAnnotation.class);

        if (methodAnnotation != null) {
            source.addBinding(source.declaredKvoField(methodAnnotation.name()).eventAction,
                    buildKvoEventReceiver(methodAnnotation, target, method));
        }
    }

    public static void removeKvoBinding(@NonNull KvoSource source, @NonNull Object target, @NonNull String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName, KvoEventIntent.class);

            removeKvoBinding(source, target, method);
        } catch (NoSuchMethodException e) {
            Log.e(KVO_LOG_TAG, "remove kvo binding failed :" + e.toString());
        }
    }

    public static void removeKvoBinding(@NonNull KvoSource source, @NonNull Object target, @NonNull Method method) {
        KvoMethodAnnotation methodAnnotation = method.getAnnotation(KvoMethodAnnotation.class);

        if (methodAnnotation != null) {
            source.removeBinding(source.declaredKvoField(methodAnnotation.name()).eventAction,
                    buildKvoEventReceiver(methodAnnotation, target, method));
        }
    }

    private static EventReceiver buildKvoEventReceiver(@NonNull KvoMethodAnnotation annotation,
                                                       @NonNull Object target, @NonNull Method method) {
        return new KvoEventReceiver(target, method, DefaultEventThreadWrapper.thread(annotation.thread()),
                annotation.priority(), annotation.flag());
    }
}
