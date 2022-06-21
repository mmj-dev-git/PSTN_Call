package com.confu.mode_verto_kotlin

object Constants {
    val LOCAL_SERVER_URL = "ws://192.168.0.169:8081";

//    val ONLINE_SERVER_URL = "wss://verto.confu.info:8082";
    val ONLINE_SERVER_URL = "wss://fs.younite.one:8082";

    //    val ONLINE_SERVER_URL = "ws://13.126.24.109:8081";
//    val ONLINE_SERVER_URL = "ws://verto.confu.info:8081";
//    val ONLINE_SERVER_URL = "ws://fs.confu.info:8081";
//    val ONLINE_SERVER_URL = "wss://fs.confu.info:8082";


    /*For handleJSONRPC2Response*/
    const val MESSAGE = "message"
    const val PASSWORD = "passwd"
    const val LOGIN_PARAMS = "loginParams"
    const val USER_VARIABLES = "userVariables"
    const val SESS_ID = "sessid"
    const val LOGGED_IN = "logged in"
    const val CALL_CREATED = "CALL CREATED"
    const val CALL_ENDED = "CALL ENDED"
    const val SUBSCRIBED_CHANNELS = "subscribedChannels"
    const val UN_SUBSCRIBED_CHANNELS = "unsubscribedChannels"

    /* For handleDialogJSONRPC2Request*/
    const val VERTO_INVITE = "verto.invite"
    const val VERTO_BYE = "verto.bye"
    const val VERTO_INFO = "verto.info"
    const val VERTO_DISPLAY = "verto.display"
    const val VERTO_MEDIA = "verto.media"
    const val VERTO_ANSWER = "verto.answer"
    const val VERTO_EVENT = "verto.event"
    const val VERTO_SUBSCRIBE = "verto.subscribe"
    const val VERTO_UNSUBSCRIBE = "verto.unsubscribe"
    const val VERTO_BROADCAST = "verto.broadcast"
    const val VERTO_PUNT = "verto.punt"
    const val VERTO_CLIENT_READY = "verto.clientReady"

    /*For handleEventJSONRPC2Request*/
    const val ACTION = "action"
    const val CONFERENCE_LIVE_ARRAY_JOIN = "conference-liveArray-join"
    const val CHANNEL = "Channel"
    const val CHAT_CHANNEL = "chatChannel"
    const val INFO_CHANNEL = "infoChannel"
    const val MOD_CHANNEL = "modChannel"
    const val LA_CHANNEL = "laChannel"
    const val CONFERENCE_LIVE_ARRAY_PART = "conference-liveArray-part"
    const val DATA = "data"
    const val ADD = "add"
    const val DEL = "del"
    const val BOOT_OBJ = "bootObj"


    /*General*/
    const val CAUSE_CODE = "causeCode"
    const val CAUSE = "cause"
    const val USER_BUSY = "USER BUSY"
    const val CALL_ID = "callID"
    const val DESTINATION_NUMBER = "destination_number"
    const val DIALOG_PARAMS = "dialogParams"
    const val DIRECTION = "direction"
    const val SDP = "sdp"
    const val SET_REMOTE_SDP = "setRemoteSDP"
    const val KEY = "key"
    const val VALUE = "value"

    const val REMOTE_CALLER_ID_NAME = "remote_caller_id_name"
    const val REMOTE_CALLER_ID_NUMBER = "remote_caller_id_number"
    const val CALLEE_ID_NAME = "callee_id_name"
    const val CALLEE_ID_NUMBER = "callee_id_number"
    const val CALLER_ID_NAME = "caller_id_name"
    const val CALLER_ID_NUMBER = "caller_id_number"


    /*Verto Callback*/
    const val EVENT_CHANNEL = "eventChannel"


    /*util Constants*/
    var SESSION_ID = "session_id"
    var CURRENT_CALL_ID = "call_id"
    var REMOTE_SDP = "remoteSdp"
    var LOCAL_SDP = "localSdp"
}