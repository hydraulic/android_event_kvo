package com.hydra.framework.event.fw;

/**
 * Created by Hydra.
 *
 * 后续对event加入模块属性
 */
public enum FWEventActionKey {

    FWAction_On_Module_Launch_Finished("app", false),
    FWAction_On_AppDb_Created("datacenter", false),
    FWAction_On_WebSocket_State_Change("websocket", false),
    FWAction_On_Login_Success("login", false),
    FWAction_On_NetState_Changed("net", false),
    ;

    public final String module;
    public final boolean sticky;

    FWEventActionKey(String module, boolean sticky) {
        this.sticky = sticky;
        this.module = module;
    }
}
