package com.hydra.framework.event.kvo.helper;


import androidx.annotation.NonNull;
import com.hydra.framework.event.core.EventAction;
import com.hydra.framework.event.kvo.KvoFieldAnnotation;
import com.hydra.framework.event.kvo.KvoMethodAnnotation;
import com.hydra.framework.event.kvo.KvoSource;
import com.hydra.framework.event.kvo.list.KvoList;
import com.hydra.framework.event.utils.EventLog;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


import static com.hydra.framework.event.core.helper.EventHelper.getExcludeSystemFields;
import static com.hydra.framework.event.core.helper.EventHelper.getExcludeSystemMethods;

/**
 * 做两个事情
 * 1、把Receiver的method信息缓存下来
 * 2、把source的field信息缓存下来
 * 3、加入了一个给kvo的EventAction的标志位
 */
public class KvoHelper {

    private static final String TAG = "KvoHelper";

    //kvo event action flags
    //这个标志位目前是给kvoList使用，强制同步通知 和 主线程通知
    public static final int KVO_EVENT_ACTION_FLAG_FORCE_SYNC = 1 << 16;
    public static final int KVO_EVENT_ACTION_FLAG_FORCE_MAIN = 1 << 17;
    //TODO: hydra 2020/5/11 5:14 PM add more kvo event action flags

    public static class KvoMethodNode {
        public Method method;
        public KvoMethodAnnotation methodAnnotation;
    }

    public static class KvoField {
        public Field field;
        public KvoFieldAnnotation fieldAnnotation;
        public EventAction eventAction;
    }

    private static ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, ArrayList<KvoMethodNode>>> clazzToKvoMethods
            = new ConcurrentHashMap<>();

    @NonNull
    public static ArrayList<KvoMethodNode> getKvoMethods(@NonNull KvoSource source, @NonNull Object receiverObj) {
        Class<?> receiverClass = receiverObj.getClass();
        Class<?> sourceClass = source.getClass();

        ConcurrentHashMap<Class<?>, ArrayList<KvoMethodNode>> kvoMethods = clazzToKvoMethods.get(receiverClass);

        if (kvoMethods == null) {
            synchronized (receiverClass) {
                kvoMethods = clazzToKvoMethods.get(receiverClass);

                if (kvoMethods == null) {
                    kvoMethods = new ConcurrentHashMap<>();
                    clazzToKvoMethods.put(receiverClass, kvoMethods);
                }
            }
        }

        ArrayList<KvoMethodNode> methodList = kvoMethods.get(sourceClass);

        if (methodList != null) {
            return methodList;
        }

        synchronized (sourceClass) {
            methodList = kvoMethods.get(sourceClass);

            if (methodList != null) {
                return methodList;
            }

            methodList = getReceiverClassKvoMethodNodes(source, receiverClass, sourceClass);

            kvoMethods.put(sourceClass, methodList);
        }

        return methodList;
    }

    @NonNull
    private static ArrayList<KvoMethodNode> getReceiverClassKvoMethodNodes(@NonNull KvoSource source,
                                                                           @NonNull Class<?> receiverClass,
                                                                           @NonNull Class<?> sourceClass) {
        ArrayList<KvoMethodNode> methodList = new ArrayList<>();

        List<Method> methods = getExcludeSystemMethods(receiverClass);

        HashMap<String, Method> nameMethodMap = new HashMap<>();

        for (Method method : methods) {
            KvoMethodAnnotation annotation = method.getAnnotation(KvoMethodAnnotation.class);

            //有可能绑定的是source的父类的field，也需要加进去
            if (annotation == null || !annotation.sourceClass().isAssignableFrom(sourceClass)) {
                continue;
            }

            String annotationName = annotation.name();

            //这里做了一次校验，即这个field是否存在
            KvoField kvoField = source.declaredKvoField(annotationName);

            if (kvoField == null) {
                continue;
            }

            Method preMethod = nameMethodMap.get(annotationName);

            //这里的判断只需要判断名字，因为参数都是KvoEventIntent类型，返回值也都是void
            //所以不存在 重载 的情况；在覆盖时，preMethod也是子类的method，保留即可
            if (preMethod != null && preMethod.getName().equals(method.getName())) {
                EventLog.debug(TAG, "getKvoMethods find method override, subMethod: " +
                        preMethod.getName() + ", superMethod: " + method.getName());
                continue;
            }

            if (!method.isAccessible()) {
                method.setAccessible(true);
            }

            KvoMethodNode node = new KvoMethodNode();
            node.method = method;
            node.methodAnnotation = annotation;

            methodList.add(node);

            nameMethodMap.put(annotationName, method);
        }

        return methodList;
    }

    //每一个KvoSource都是一个Event Dispatcher，每一个字段变更都是在这个dispatcher域中的一个事件
    private static ConcurrentHashMap<Class<? extends KvoSource>, HashMap<String, KvoField>> allKvoSourceFields
            = new ConcurrentHashMap<>();

    @NonNull
    public static HashMap<String, KvoField> kvoFieldsContainerFor(@NonNull final Class<? extends KvoSource> clazz) {
        HashMap<String, KvoField> kvoFields = allKvoSourceFields.get(clazz);

        if (kvoFields != null) {
            return kvoFields;
        }

        synchronized (clazz) {
            kvoFields = allKvoSourceFields.get(clazz);

            if (kvoFields != null) {
                return kvoFields;
            }

            kvoFields = buildKvoFieldsMap(clazz);

            allKvoSourceFields.put(clazz, kvoFields);
        }

        return kvoFields;
    }

    private static HashMap<String, KvoField> buildKvoFieldsMap(@NonNull final Class<? extends KvoSource> clazz) {
        HashMap<String, KvoField> kvoFields = new HashMap<>();

        List<Field> fields = getExcludeSystemFields(clazz);

        for (Field field : fields) {
            KvoFieldAnnotation annotation = field.getAnnotation(KvoFieldAnnotation.class);

            if (annotation != null) {
                KvoField preField = kvoFields.get(annotation.name());

                //如果子类和父类中有相同名字的key，使用子类的

                //这里的name，使用的都是annotation的name，而不是field的name
                //因为理论上来说，annotation的名字不一定和field的名字相同，但是我们目前不存在这种情况

                //所以如果，子类的field是覆盖父类的，但是两个field的annotation不一样
                //在这种情况下，会保留两个field，对应不同的通知

                //这个判断可以加上preField.field.getDeclaringClass().isAssignableFrom(field.getDeclaringClass())
                //只不过getExcludeSystemFields的list里field是的顺序已经是从子类到父类的了，所以不加这个
                if (preField == null) {
                    KvoField kvoField = new KvoField();

                    kvoField.field = field;
                    kvoField.fieldAnnotation = annotation;
                    kvoField.eventAction = buildKvoFieldEventAction(annotation.name(), field);

                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }

                    kvoFields.put(annotation.name(), kvoField);
                } else {
                    EventLog.warn(TAG, "two field in sub class and superclass has the same annotation " +
                            "name in" +
                            " souceclass : " + clazz.getSimpleName() +
                            "; we will only pick the field in subclass, field name : " + annotation.name() +
                            "; superclass : " + field.getDeclaringClass().getSimpleName() +
                            "; subclass : " + preField.field.getDeclaringClass().getSimpleName());
                }
            }
        }

        return kvoFields;
    }

    private static EventAction buildKvoFieldEventAction(@NonNull String key, @NonNull Field field) {
        Class<?> type = field.getType();

        int flag = 0;

        if (KvoList.class.isAssignableFrom(type)) {
            flag = KVO_EVENT_ACTION_FLAG_FORCE_SYNC | KVO_EVENT_ACTION_FLAG_FORCE_MAIN;
        } else if (com.yy.base.event.kvo.map.KvoHashMap.class.isAssignableFrom(type)
                || com.yy.base.event.kvo.set.KvoHashSet.class.isAssignableFrom(type)) {
            flag = KVO_EVENT_ACTION_FLAG_FORCE_SYNC;
        }

        return new EventAction(key, flag);
    }
}
