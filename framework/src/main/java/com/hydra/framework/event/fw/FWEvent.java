package com.hydra.framework.event.fw;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hydra.framework.event.core.EventAction;
import com.hydra.framework.event.core.EventDispatcher;
import com.hydra.framework.event.core.EventIntent;
import com.hydra.framework.event.core.EventReceiver;
import com.hydra.framework.event.core.EventSender;
import com.hydra.framework.event.core.helper.DefaultEventThreadWrapper;
import com.hydra.framework.event.utils.EventLog;
import com.hydra.framework.thread.ThreadBus;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


import static com.hydra.framework.event.core.helper.EventHelper.getExcludeSystemMethods;

/**
 * Created by Hydra.
 * 一个Common的EventDispatcher的自定义中心
 */
public class FWEvent {

    private static final String TAG = "FWEvent";

    private static final EventDispatcher sFWEventDispatcher;

    static {
        sFWEventDispatcher = new EventDispatcher();
    }

    private static class FWMethodNode {
        public Method method;
        public FWEventAnnotation methodAnnotation;
    }

    private static ConcurrentHashMap<Class<?>, ArrayList<FWMethodNode>> receiverClassMethodsCache
            = new ConcurrentHashMap<>();

    //这里面包含dst的父类中的method
    @NonNull
    private static ArrayList<FWMethodNode> getFWEventMethods(@NonNull Object dst) {
        Class<?> dstClass = dst.getClass();

        ArrayList<FWMethodNode> methodList = receiverClassMethodsCache.get(dstClass);

        if (methodList != null) {
            return methodList;
        }

        synchronized (dstClass) {
            methodList = receiverClassMethodsCache.get(dstClass);

            if (methodList != null) {
                return methodList;
            }

            methodList = getDstClassFWMethodNodes(dstClass);

            receiverClassMethodsCache.put(dstClass, methodList);
        }

        return methodList;
    }

    @NonNull
    private static ArrayList<FWMethodNode> getDstClassFWMethodNodes(@NonNull Class<?> dstClass) {
        ArrayList<FWMethodNode> methodList = new ArrayList<>();

        List<Method> methods = getExcludeSystemMethods(dstClass);

        HashMap<FWEventActionKey, Method> nameMethodMap = new HashMap<>();

        for (Method method : methods) {
            FWEventAnnotation annotation = method.getAnnotation(FWEventAnnotation.class);

            if (annotation == null) {
                continue;
            }

            Method preMethod = nameMethodMap.get(annotation.name());

            //这里的判断只需要判断名字，因为参数都是EventIntent类型，返回值也都是void
            //所以不存在 重载 的情况；在覆盖时，preMethod也是子类的method，保留即可
            if (preMethod != null && preMethod.getName().equals(method.getName())) {
                continue;
            }

            if (!method.isAccessible()) {
                method.setAccessible(true);
            }

            FWMethodNode node = new FWMethodNode();
            node.method = method;
            node.methodAnnotation = annotation;

            methodList.add(node);

            nameMethodMap.put(annotation.name(), method);
        }

        return methodList;
    }

    //因为FWEvent的action都是固定的，所以可以cache起来，减少生成很多对象
    private static ConcurrentHashMap<FWEventActionKey, EventAction> fwEventActionCache = new ConcurrentHashMap<>();

    @NonNull
    private static EventAction fwEventAction(@NonNull final FWEventActionKey fwEventActionKey) {
        EventAction fwEventAction = fwEventActionCache.get(fwEventActionKey);

        if (fwEventAction != null) {
            return fwEventAction;
        }

        synchronized (fwEventActionKey) {
            fwEventAction = fwEventActionCache.get(fwEventActionKey);

            if (fwEventAction != null) {
                return fwEventAction;
            }

            fwEventAction = new EventAction(fwEventActionKey, fwEventActionKey.sticky ?
                    EventAction.EVENTACTION_FLAG_STICKY : 0);

            fwEventActionCache.put(fwEventActionKey, fwEventAction);
        }

        return fwEventAction;
    }

    public static void autoBindingEvent(@NonNull Object target) {
        ArrayList<FWMethodNode> nodes = getFWEventMethods(target);

        for (FWMethodNode node : nodes) {
            sFWEventDispatcher.addBinding(fwEventAction(node.methodAnnotation.name()),
                    buildFWEventReceiver(node.methodAnnotation, target, node.method));
        }
    }

    public static void autoRemoveEvent(@NonNull Object target) {
        ArrayList<FWMethodNode> nodes = getFWEventMethods(target);

        for (FWMethodNode node : nodes) {
            sFWEventDispatcher.removeBinding(fwEventAction(node.methodAnnotation.name()),
                    buildFWEventReceiver(node.methodAnnotation, target, node.method));
        }
    }

    /**
     * 绑定单个函数
     */
    public static void bindMethodToEvent(@NonNull Object target, @NonNull String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName, EventIntent.class);

            bindEventTo(target, method);
        } catch (NoSuchMethodException | SecurityException e) {
            EventLog.error(TAG, "bind event failed no method : " + methodName
                    + " in " + target.toString());
        }
    }

    /**
     * 绑定单个函数
     */
    public static void bindEventTo(@NonNull Object target, @NonNull Method method) {
        FWEventAnnotation annotation = method.getAnnotation(FWEventAnnotation.class);

        if (annotation != null) {
            sFWEventDispatcher.addBinding(fwEventAction(annotation.name()),
                    buildFWEventReceiver(annotation, target, method));
        }
    }

    /**
     * 解绑单个函数
     */
    public static void removeMethodFromEvent(@NonNull Object target, @NonNull String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName, EventIntent.class);

            removeEventFrom(target, method);
        } catch (NoSuchMethodException | SecurityException e) {
            EventLog.error(TAG, "remove event failed no method : " + methodName
                    + " in " + target.toString());
        }
    }

    /**
     * 解绑单个函数
     */
    public static void removeEventFrom(@NonNull Object target, @NonNull Method method) {
        FWEventAnnotation annotation = method.getAnnotation(FWEventAnnotation.class);

        if (annotation != null) {
            sFWEventDispatcher.removeBinding(fwEventAction(annotation.name()),
                    buildFWEventReceiver(annotation, target, method));
        }
    }

    /**
     * @param thread 参考thread bus中的线程id
     */
    public static void sendEventAsync(int thread, @Nullable final Object senderObj,
                                      @NonNull final FWEventActionKey fwEventActionKey,
                                      @Nullable final Object... args) {
        ThreadBus.post(thread, () -> sendEvent(senderObj, fwEventActionKey, args));
    }

    public static void sendEvent(@Nullable Object senderObj, @NonNull FWEventActionKey fwEventActionKey,
                                 @Nullable Object... args) {
        EventIntent eventIntent = buildFWEventIntent(senderObj, fwEventActionKey);

        eventIntent.addArgs(args);

        sFWEventDispatcher.notifyEvent(eventIntent);
    }

    private static EventReceiver buildFWEventReceiver(@NonNull FWEventAnnotation annotation,
                                                      @NonNull Object target, @NonNull Method method) {
        return new EventReceiver(target, method, DefaultEventThreadWrapper.thread(annotation.thread()),
                annotation.priority(), annotation.flag());
    }

    private static EventIntent buildFWEventIntent(@Nullable Object senderObj,
                                                  @NonNull FWEventActionKey fwEventActionKey) {
        EventSender eventSender = new EventSender(senderObj);
        EventAction fwEventAction = fwEventAction(fwEventActionKey);

        return new EventIntent(eventSender, fwEventAction, false);
    }
}
