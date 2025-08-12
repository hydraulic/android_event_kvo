package com.hydra.framework.event.core;

/**
 * 事件线程接口，事件框架不负责任何线程行为，只保证框架内部的逻辑完备和线程安全
 */
public interface IEventThread {
    boolean post(Runnable r);
}
