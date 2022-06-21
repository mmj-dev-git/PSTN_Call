package com.confu.mode_verto_kotlin

data class DtoCallInvitation(
    var callId: String,
    var sdp: String,
    var caller_id_name: String,
    var caller_id_number: String,
    var callee_id_name: String,
    var callee_id_number: String
)
