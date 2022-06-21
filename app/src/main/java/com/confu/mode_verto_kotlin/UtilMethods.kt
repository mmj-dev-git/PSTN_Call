package com.confu.mode_verto_kotlin

import java.util.*

object UtilMethods {

    var dtoDeviceCred = DtoDeviceCred("", "")
    var dtoCallInvitation = DtoCallInvitation("", "", "", "", "", "")

    fun setDtoDeviceCreds(userName: String, password: String) {
        dtoDeviceCred.userName = userName
        dtoDeviceCred.password = password
    }

    fun isStringValid(str: String?): Boolean {
        return str != null && !str.isEmpty()
    }

    fun processCallInvitation(namedParams: HashMap<String?, Any?>) {
        val callId = namedParams["callID"] as String?
        val callerIdName = namedParams["caller_id_name"] as String?
        val callerIdNumber = namedParams["caller_id_number"] as String?
        val calleeIdName = namedParams["callee_id_name"] as String?
        val calleeIdNumber = namedParams["callee_id_number"] as String?
        val sdp = namedParams["sdp"] as String?
        setDtoCallInvitation(callId!!, callerIdName!!, callerIdNumber!!, calleeIdName!!, calleeIdNumber!!, sdp!!)
    }

    fun setDtoCallInvitation(
        callId: String,
        caller_id_name: String,
        caller_id_number: String,
        callee_id_name: String,
        callee_id_number: String,
        sdp: String
    ) {
        dtoCallInvitation.callId = callId
        dtoCallInvitation.caller_id_name = caller_id_name
        dtoCallInvitation.callee_id_number = caller_id_number
        dtoCallInvitation.callee_id_name = callee_id_name
        dtoCallInvitation.callee_id_number = callee_id_number
        dtoCallInvitation.sdp = sdp
    }
}