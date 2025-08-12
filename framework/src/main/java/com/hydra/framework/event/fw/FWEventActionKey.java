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

    FWAction_On_ChannelExit("channel", false),

    FWAction_On_Bbs_Like_Changed("bbs", false),
    FWAction_On_Visitor_Add("user", false),
    FWAction_On_DiscoverScrollIdle("bbs", false),

    FWAction_On_WorldFightingSendInvite("channel-game", false),
    FWAction_On_WorldFightingReceiveInvite("channel-game", false),

    FWAction_On_WorldFightingStateChange("channel-game", false),

    FWAction_On_SearchUserResult("search", false),

    FWAction_OnChannelFloatGameStartClick("channel-float-game", false),

    FWAction_OnChannelExitFloatGame("channel-float-game", false),

    FWAction_OnFloatGameAutoStatusChange("channel-float-game", false),
    FWAction_OnFloatGameExpModeChange("channel-float-game", false),
    FWAction_OnFloatGameBackModeChange("channel-float-game", false),

    FWAction_OnGameOpenProfile("channel-float-game", false),
    FWAction_OnGameOpenGiftPanel("channel-float-game", false),
    ;

    public final String module;
    public final boolean sticky;

    FWEventActionKey(String module, boolean sticky) {
        this.sticky = sticky;
        this.module = module;
    }
}
