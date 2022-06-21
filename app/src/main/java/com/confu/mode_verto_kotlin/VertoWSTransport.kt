package com.confu.mode_verto_kotlin

import android.util.Log
import com.confu.mode_verto_kotlin.Constants.CALLER_ID_NAME
import com.confu.mode_verto_kotlin.Constants.CALLER_ID_NUMBER
import com.confu.mode_verto_kotlin.Constants.CALL_ID
import com.confu.mode_verto_kotlin.Constants.CAUSE
import com.confu.mode_verto_kotlin.Constants.CAUSE_CODE
import com.confu.mode_verto_kotlin.Constants.CURRENT_CALL_ID
import com.confu.mode_verto_kotlin.Constants.DESTINATION_NUMBER
import com.confu.mode_verto_kotlin.Constants.DIALOG_PARAMS
import com.confu.mode_verto_kotlin.Constants.EVENT_CHANNEL
import com.confu.mode_verto_kotlin.Constants.LOCAL_SDP
import com.confu.mode_verto_kotlin.Constants.LOGIN_PARAMS
import com.confu.mode_verto_kotlin.Constants.PASSWORD
import com.confu.mode_verto_kotlin.Constants.REMOTE_CALLER_ID_NAME
import com.confu.mode_verto_kotlin.Constants.REMOTE_CALLER_ID_NUMBER
import com.confu.mode_verto_kotlin.Constants.SDP
import com.confu.mode_verto_kotlin.Constants.SESSION_ID
import com.confu.mode_verto_kotlin.Constants.SESS_ID
import com.confu.mode_verto_kotlin.Constants.USER_BUSY
import com.confu.mode_verto_kotlin.Constants.USER_VARIABLES
import com.confu.mode_verto_kotlin.Constants.VERTO_BROADCAST
import com.confu.mode_verto_kotlin.Constants.VERTO_BYE
import com.confu.mode_verto_kotlin.Constants.VERTO_INVITE
import com.confu.mode_verto_kotlin.Constants.VERTO_SUBSCRIBE
import com.confu.mode_verto_kotlin.Constants.VERTO_UNSUBSCRIBE
import com.neovisionaries.ws.client.*
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request
import org.webrtc.SessionDescription
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.*

class VertoWSTransport() {

    private val TAG = "VertoWSTransport";
    private val TIMEOUT = 5000;
    private lateinit var factory: WebSocketFactory;


    public interface Callbacks {
        fun onWSConnectError(error: String)
        fun onWSConnected()
        fun onWSDisconnected(isDisconnected: Boolean)
        fun onWSMessage(message: String)
    }

    private class TLSSocketFactory : SSLSocketFactory() {
        private val mSSLSocketFactory: SSLSocketFactory
        override fun getDefaultCipherSuites(): Array<String> {
            return mSSLSocketFactory.defaultCipherSuites
        }

        override fun getSupportedCipherSuites(): Array<String> {
            return mSSLSocketFactory.supportedCipherSuites
        }

        @Throws(IOException::class)
        override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
            return enableTLS(mSSLSocketFactory.createSocket(s, host, port, autoClose))
        }

        @Throws(IOException::class, UnknownHostException::class)
        override fun createSocket(host: String, port: Int): Socket {
            return enableTLS(mSSLSocketFactory.createSocket(host, port))
        }

        @Throws(IOException::class, UnknownHostException::class)
        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
            return enableTLS(mSSLSocketFactory.createSocket(host, port, localHost, localPort))
        }

        @Throws(IOException::class)
        override fun createSocket(host: InetAddress, port: Int): Socket {
            return enableTLS(mSSLSocketFactory.createSocket(host, port))
        }

        @Throws(IOException::class)
        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
            return enableTLS(mSSLSocketFactory.createSocket(address, port, localAddress, localPort))
        }

        @Throws(IOException::class)
        override fun createSocket(): Socket {
            return enableTLS(mSSLSocketFactory.createSocket())
        }

        private fun enableTLS(socket: Socket): Socket {
            if (socket != null && socket is SSLSocket) {
                socket.enabledProtocols = arrayOf("TLSv1.1", "TLSv1.2")
            }
            return socket
        }

        init {
            val context = SSLContext.getInstance("TLS")
            context.init(null as Array<KeyManager?>?, null as Array<TrustManager?>?, null as SecureRandom?)
            mSSLSocketFactory = context.socketFactory
        }
    }

    private lateinit var address: String
    private lateinit var listener: Callbacks
    private var wsPingInterval: Int = 0
    private lateinit var tlsSocketFactory: TLSSocketFactory
    var webSocket: WebSocket? = null
    private var jsonRPCId = 0
    private val METHOD_LOGIN = "login"
    private lateinit var remoteSdp: String


    public constructor(address: String, listener: Callbacks) : this() {
        this.address = address
        this.listener = listener
    }

    public constructor(address: String, wsPingInterval: Int, listener: Callbacks) : this() {
        this.address = address
        this.listener = listener
        this.wsPingInterval = wsPingInterval

        try {
            this.tlsSocketFactory = TLSSocketFactory()
        } catch (e: Exception) {
            Log.e(TAG, "TLSSocket Factory Exception: ", e)
        }
        this.factory = WebSocketFactory()
        this.factory.setSSLSocketFactory(this.tlsSocketFactory)
        this.factory.setConnectionTimeout(TIMEOUT)
    }

    public fun connect() {
        if (this.webSocket == null) {
            try {
                this.webSocket = this.factory.createSocket(this.address)
                webSocket?.addListener(object : WebSocketAdapter() {
                    override fun onTextMessage(websocket: WebSocket, message: String) {
                        Log.d(TAG, "onTextMessage: $message")
                        listener.onWSMessage(message)
                    }

                    override fun onFrameSent(websocket: WebSocket, frame: WebSocketFrame) {
                        Log.d(TAG, "onFrameSent: ")
                    }

                    override fun onConnected(websocket: WebSocket, map: Map<String, List<String>>) {
                        Log.d(TAG, "onConnected: ")
                        listener.onWSConnected()
                    }

                    override fun onDisconnected(
                        websocket: WebSocket,
                        serverCloseFrame: WebSocketFrame,
                        clientCloseFrame: WebSocketFrame,
                        closedByServer: Boolean
                    ) {
                        Log.d(TAG, "onDisconnected: ")
                        listener.onWSDisconnected(closedByServer)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "connect: $e")
            }

            webSocket?.pingInterval = (wsPingInterval * 1000).toLong()
            webSocket?.frameQueueSize = 1
            try {
                webSocket?.connect()
            } catch (e2: WebSocketException) {
                Log.e(TAG, "connect: ", e2)
                listener.onWSConnectError(e2.error.toString())
                webSocket?.disconnect()
            }
        }
    }

    fun disconnect() {
        Log.d(TAG, "disconnect: ")
        webSocket?.disconnect()
        webSocket = null
    }

    fun send(request: String?) {
        Log.d(TAG, "send: $request")
        webSocket?.sendText(request)
        webSocket?.flush()
//        webSocket = null
    }

    fun setMethodLogin(username: String, password: String) {
        SESSION_ID = UUID.randomUUID().toString()
        Log.d(TAG, "setMethodLogin: $SESSION_ID")
        val params: MutableMap<String, Any> = HashMap()
        val loginParams: Map<String, Any> = HashMap()
        val userVariables: Map<String, Any> = HashMap()

        params.put(METHOD_LOGIN, username);
        params.put(PASSWORD, password);

//        params.put(METHOD_LOGIN, username + "@verto.confu.info");
//        params.put(PASSWORD, password);

//        params[METHOD_LOGIN] = username
//        params[PASSWORD] = "1234"

        params[LOGIN_PARAMS] = loginParams
        params[USER_VARIABLES] = userVariables
        params[SESS_ID] = SESSION_ID
        callJSONRPC2(METHOD_LOGIN, params)

    }


    fun sendInvite(sessionDescription: SessionDescription, sessionId: String, calleeNumber: String) {
        Log.d(TAG, "sendInvite: ")
        CURRENT_CALL_ID = UUID.randomUUID().toString()
        val params: MutableMap<String, Any> = HashMap()
        val dialogParams: MutableMap<String, Any> = HashMap()
        val videoParams: MutableMap<String, Any> = HashMap()
        dialogParams[CALL_ID] = CURRENT_CALL_ID
        dialogParams[DESTINATION_NUMBER] = calleeNumber
        dialogParams[REMOTE_CALLER_ID_NAME] = "Outbound Call"
        dialogParams[REMOTE_CALLER_ID_NUMBER] = calleeNumber

//        dialogParams.put("login", UtilMethods.getDtoDeviceCred().getUserName() + "@verto.confu.info");
        dialogParams["login"] = UtilMethods.dtoDeviceCred.userName
        dialogParams["useVideo"] = false
        dialogParams["screenShare"] = false
        dialogParams["useStereo"] = false
        dialogParams["useMic"] = "any"
        dialogParams["tag"] = "any"
        dialogParams["useCamera"] = "any"
        dialogParams["useSpeak"] = "any"
        videoParams["minFrameRate"] = 30
        videoParams["minWidth"] = "1280"
        videoParams["minHeight"] = "720"
        dialogParams["videoParams"] = videoParams
        dialogParams[CALLER_ID_NAME] = UtilMethods.dtoDeviceCred.userName
        dialogParams[CALLER_ID_NUMBER] = UtilMethods.dtoDeviceCred.userName
        params[SDP] = sessionDescription.description
        params[SESS_ID] = sessionId
        params[DIALOG_PARAMS] = dialogParams
        callJSONRPC2(VERTO_INVITE, params)
//        UtilityMethods.callJSONRPC2(VERTO_INVITE, params, webSocket)
    }

    fun sendMedia() {
        Log.d(TAG, "sendMedia: ")
        val params: MutableMap<String, Any> = HashMap()
        params[SDP] = LOCAL_SDP
        params[CALL_ID] = CURRENT_CALL_ID
        callJSONRPC2(VERTO_INVITE, params)
    }

    fun sendAnswer(sessionDescription: SessionDescription, sessionId: String) {
        Log.d(TAG, "sendAnswer: ")
        val params2: MutableMap<String, Any> = HashMap()
        val dialogParams2: MutableMap<String, Any> = HashMap()
        dialogParams2["callID"] = UtilMethods.dtoCallInvitation.callId
        dialogParams2["destination_number"] = UtilMethods.dtoCallInvitation.callee_id_number
        dialogParams2["remote_caller_id_name"] = UtilMethods.dtoDeviceCred.userName
        dialogParams2["remote_caller_id_number"] = UtilMethods.dtoDeviceCred.userName
        params2["sdp"] = sessionDescription.description
        params2["session_id"] = sessionId
        params2["dialogParams"] = dialogParams2
        callJSONRPC2("verto.answer", params2)
    }

    fun subscribe(channel: String) {
        Log.d(TAG, "subscribe: ")
        val params: MutableMap<String, Any> = HashMap()
        params[SESSION_ID] = SESSION_ID
        params[EVENT_CHANNEL] = channel
        callJSONRPC2(VERTO_SUBSCRIBE, params)
    }

    fun sendBootstrapRequest(eventChannel: String) {
        Log.d(TAG, "sendBootstrapRequest: Trying to bootstrap on channel: $eventChannel")
        val params = HashMap<String, Any>()
        val data = HashMap<String, Any>()
        val liveArray = HashMap<String, Any>()
        params[SESSION_ID] = SESSION_ID
        params[EVENT_CHANNEL] = eventChannel
        params["data"] = data
        data["liveArray"] = liveArray
        liveArray["command"] = "bootstrap"
        liveArray["context"] = eventChannel
        liveArray["name"] = eventChannel.split("@").toTypedArray()[0].split("\\.").toTypedArray()[1]
        callJSONRPC2(VERTO_BROADCAST, params)
    }

    fun unsubscribe(channel: String) {
        Log.d(TAG, "unsubscribe: ")
        val params: MutableMap<String, Any> = HashMap()
        params[SESSION_ID] = SESSION_ID
        params[EVENT_CHANNEL] = channel
        callJSONRPC2(VERTO_UNSUBSCRIBE, params)
    }

    fun userBusy(namedParams: HashMap<String, Any>) {
        Log.d(TAG, "userBusy: ")
        val params: MutableMap<String, Any> = HashMap()
        val dialogParams: Map<String, Any> = namedParams
        params[SESSION_ID] = SESSION_ID
        params[DIALOG_PARAMS] = dialogParams
        params[CAUSE_CODE] = 17L
        params[CAUSE] = USER_BUSY
        callJSONRPC2(VERTO_BYE, params)
    }

    fun speedTest(bytes: Int) {
        val speedTestData = String(CharArray(1024)).replace(0.toChar(), '.')
//        totalBits = bytes * 8
        val loops = bytes / 1024
        val rem = bytes % 1024
        send("#SPU $bytes")
        for (i in 0 until loops) {
            send("#SPB $speedTestData")
        }
        if (rem != 0) {
            send("#SPB $speedTestData")
        }
        send("#SPE")
    }

    fun handleAnswer(params: HashMap<String?, Any?>) {
        if (params.containsKey(SDP)) {
            remoteSdp = params[SDP] as String
        }
        if (remoteSdp != null) {
//            iSocketToEngine.setRemoteIceCandidate(remoteSdp)
            return
        }
        //todo hangup the call if REMOTE_SDP is not available.
    }

    fun handleMedia(dialogParams: HashMap<String?, Any?>) {
        remoteSdp = dialogParams[SDP] as String
    }

    fun handleDisplay(hashMap: HashMap<String?, Any?>?) {}

    fun handleInfo(hashMap: HashMap<String?, Any?>?) {}


    fun callJSONRPC2(method: String?, params: MutableMap<String, Any>) {
        try {
            val i: Int = this.jsonRPCId + 1
            this.jsonRPCId = i
            val jsonrpc2Request = JSONRPC2Request(method, params, Integer.valueOf(i))
            Log.d(TAG, "callJSONRPC2: \n${jsonrpc2Request.toString()}\n--------------------------------------------\n")
            send(jsonrpc2Request.toString())
        } catch (e: Exception) {
            Log.e(
                TAG, "callJSONRPC2: ", e
            )
        }
    }

    interface NetworkCallback {
        fun JsonData(example: WebRTCModel?)
        fun initializeOutboundCall(example: WebRTCModel?)
    }
}