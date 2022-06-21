package com.confu.mode_verto_kotlin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.confu.mode_verto_kotlin.Constants.ACTION
import com.confu.mode_verto_kotlin.Constants.ADD
import com.confu.mode_verto_kotlin.Constants.BOOT_OBJ
import com.confu.mode_verto_kotlin.Constants.CALL_CREATED
import com.confu.mode_verto_kotlin.Constants.CALL_ENDED
import com.confu.mode_verto_kotlin.Constants.CALL_ID
import com.confu.mode_verto_kotlin.Constants.CHANNEL
import com.confu.mode_verto_kotlin.Constants.CONFERENCE_LIVE_ARRAY_JOIN
import com.confu.mode_verto_kotlin.Constants.DEL
import com.confu.mode_verto_kotlin.Constants.EVENT_CHANNEL
import com.confu.mode_verto_kotlin.Constants.LOGGED_IN
import com.confu.mode_verto_kotlin.Constants.MESSAGE
import com.confu.mode_verto_kotlin.Constants.SDP
import com.confu.mode_verto_kotlin.Constants.SESSION_ID
import com.confu.mode_verto_kotlin.Constants.SESS_ID
import com.confu.mode_verto_kotlin.Constants.SUBSCRIBED_CHANNELS
import com.confu.mode_verto_kotlin.Constants.VERTO_ANSWER
import com.confu.mode_verto_kotlin.Constants.VERTO_BYE
import com.confu.mode_verto_kotlin.Constants.VERTO_CLIENT_READY
import com.confu.mode_verto_kotlin.Constants.VERTO_DISPLAY
import com.confu.mode_verto_kotlin.Constants.VERTO_EVENT
import com.confu.mode_verto_kotlin.Constants.VERTO_INFO
import com.confu.mode_verto_kotlin.Constants.VERTO_INVITE
import com.confu.mode_verto_kotlin.Constants.VERTO_MEDIA
import com.confu.mode_verto_kotlin.Constants.VERTO_PUNT
import com.confu.mode_verto_kotlin.UtilMethods.processCallInvitation
import com.thetransactioncompany.jsonrpc2.*
import com.thetransactioncompany.jsonrpc2.util.NamedParamsRetriever
import net.minidev.json.JSONObject
import org.json.JSONArray
import org.json.JSONException
import org.webrtc.PeerConnection
import java.util.*
import java.util.concurrent.*

class Verto private constructor() : VertoWSTransport.Callbacks {

    private val TAG = "Verto";
    private lateinit var activity: Activity
    private lateinit var context: Context
    private lateinit var userName: String
    private lateinit var password: String
    private lateinit var serverUrl: String
    private lateinit var iceServers: MutableList<PeerConnection.IceServer>
    private var vertoWSTransport: VertoWSTransport? = null
    var ChannelName = "ChannelName"
    private lateinit var subscribedChannels: LinkedList<String>
    private lateinit var executor: ThreadPoolExecutor
    private var exSvc: ExecutorService? = null
    var webRtc: WebRtc? = null
    lateinit var callbacks: Callbacks

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: Verto? = null
//        val instance: Verto?
//            get() {
//                if (INSTANCE == null) {
//                    synchronized(Verto::class.java) {
//                        INSTANCE = Verto()
//                    }
//                }
//                return INSTANCE
//            }

        fun getInstance(
            activity: Activity,
            context: Context,
            userName: String,
            password: String,
            iceServers: MutableList<PeerConnection.IceServer>,
            serverUrl: String,
            callbacks: Callbacks
        ): Verto {
            if (INSTANCE == null) {
                synchronized(Verto::class.java) {
                    INSTANCE = Verto(activity, context, userName, password, iceServers, serverUrl, callbacks)
                    UtilMethods.setDtoDeviceCreds(userName, password)
                }
            }
            return INSTANCE as Verto
        }
    }

    private constructor(
        activity: Activity,
        context: Context,
        userName: String,
        password: String,
        iceServers: MutableList<PeerConnection.IceServer>,
        serverUrl: String,
        callbacks: Callbacks
    ) : this() {
        this.activity = activity
        this.context = context
        this.userName = userName
        this.password = password
        this.iceServers = iceServers
        this.serverUrl = serverUrl
        this.callbacks = callbacks

        this.exSvc = Executors.newSingleThreadExecutor()
        executor = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>())
    }

    override fun onWSConnectError(error: String) {
        Log.d(TAG, "onWSConnectError: $error")
    }

    override fun onWSConnected() {
        Log.d(TAG, "onWSConnected: ")
        vertoWSTransport?.setMethodLogin(username = userName, password = password)
        callbacks.onWSConnected()
    }

    override fun onWSDisconnected(isDisconnected: Boolean) {
        callbacks.onWSDisconnected(isDisconnected)
    }

    override fun onWSMessage(message: String) {
        handleWSMessage(message)
    }

    private fun handleWSMessage(str: String) {
        Log.d(TAG, "handleWSMessage: $str")
        try {
            val msg = JSONRPC2Message.parse(str)
            if (msg is JSONRPC2Request) {
                handleJSONRPC2Request(msg)
            } else if (msg is JSONRPC2Response) {
                handleJSONRPC2Response(msg)
            } else if (msg is JSONRPC2Notification) {
//                handleJSONRPC2Notification((JSONRPC2Notification) msg);
            }
        } catch (e: JSONRPC2ParseException) {
            e.printStackTrace()
        }
    }

    private fun handleJSONRPC2Response(resp: JSONRPC2Response) {
        if (resp.indicatesSuccess()) {
            Log.d(TAG, "handleJSONRPC2Response: $resp")
            val result: HashMap<String, Any> = resp.result as HashMap<String, Any>
            resp.toJSONObject()
            if (result.containsKey(MESSAGE)) {
                val str = result[MESSAGE] as String?
                if (UtilMethods.isStringValid(str)) {
                    if (str.equals(LOGGED_IN)) {
//                        vertoWSTransport.speedTest(262144);//speed test
                        if (result.containsKey(SESS_ID)) {
                            SESSION_ID = result[SESS_ID] as String // get session id after login call.
                            //                            initiateWebRtcEngine();
                        }
                    } else if (str.equals(CALL_CREATED)) {
//                        vertoWSTransport.sendMedia();
                        Log.d(TAG, "handleJSONRPC2Response: Call created response,")
                    } else if (str.equals(CALL_ENDED)) {
                        Log.d(TAG, "handleJSONRPC2Response: Call ended response.")
                    }
                } // null condition
                else {
                    Log.d(TAG, "handleJSONRPC2Response: Message value is not valid.")
                }
            } else if (result.containsKey(SUBSCRIBED_CHANNELS)) {
                Log.d(TAG, "handleJSONRPC2Response: Subscribed to ")
                try {
                    val subscribedChannelsJSONArray = JSONArray(result["subscribedChannels"].toString())
                    val strArr = arrayOfNulls<String>(subscribedChannelsJSONArray.length())
                    for (i in 0 until subscribedChannelsJSONArray.length()) {
//                        this.subscribedChannels.add(subscribedChannelsJSONArray.getString(i))
                    }
                } catch (ex: JSONException) {
                    Log.e(TAG, "handleJSONRPC2Response: ", ex)
                }
            }
        }
    }

    private fun handleJSONRPC2Request(req: JSONRPC2Request) {
        if (TextUtils.equals(req.method, VERTO_EVENT)) {
            handleEventJSONRPC2Request(req)
            return
        }
        confirmationResponseJSONRPC2(req)
        val namedParams = req.namedParams
        Log.d(TAG, "handleJSONRPC2Request: $req")
        if (NamedParamsRetriever(namedParams).hasParam(CALL_ID)) {
            handleDialogJSONRPC2Request(req)
        } else if (TextUtils.equals(req.method, VERTO_PUNT)) {
            Log.d(TAG, "handleJSONRPC2Request: verto.punt")
        } else if (TextUtils.equals(req.method, VERTO_INFO)) {
            Log.d(TAG, "handleJSONRPC2Request: verto.info")
        } else if (TextUtils.equals(req.method, VERTO_CLIENT_READY)) {
            Log.d(TAG, "handleJSONRPC2Request: verto.clientReady")
            handleClientReadyJSONRPC2Request(req)
        } else {
            Log.d(TAG, "handleJSONRPC2Request: $req")
        }
    }

    fun confirmationResponseJSONRPC2(req: JSONRPC2Request) {
        Log.d(TAG, "confirmationResponseJSONRPC2: " + req.method)
        vertoWSTransport?.send(JSONRPC2Response(req.method, req.id).toString())
    }

    private fun handleEventJSONRPC2Request(req: JSONRPC2Request) {
        Log.d(TAG, "handleEventJSONRPC2Request: $req")
        if (!req.namedParams.keys.contains(EVENT_CHANNEL)) {
            Log.d(TAG, "handleEventJSONRPC2Request: eventChannel missing !")
            return
        }
        val eventChannel = req.namedParams[EVENT_CHANNEL] as String?
        if (TextUtils.equals(eventChannel, SESSION_ID)) {
            val pvtData = req.namedParams["pvtData"] as JSONObject?
            val action = pvtData!![ACTION] as String?
            if (TextUtils.equals(action, CONFERENCE_LIVE_ARRAY_JOIN)) {
                for (k in pvtData.keys) {
                    if (k.contains(CHANNEL)) {
                        val channelName = (pvtData[k] as String?)!!
                        ChannelName = channelName
                        Log.d(TAG, "handleEventJSONRPC2Request: Trying to subscribe to $channelName")
                        vertoWSTransport?.subscribe(channelName)
                    }
                }
                if (pvtData.containsKey("laChannel")) {
                    vertoWSTransport?.sendBootstrapRequest((pvtData["laChannel"] as String?)!!)
                }
            } else if (TextUtils.equals(action, "conference-liveArray-part")) {
                unsubscribeAllChannels()
            }
        } else if (TextUtils.equals(eventChannel, ChannelName)) {
            val data = req.namedParams["data"] as JSONObject?
            try {
                val dataOrgJSON = org.json.JSONObject(data!!.toJSONString())
                val action2 = data[ACTION] as String?
                if (action2 != null && !action2.isEmpty() &&
                    action2.equals(ADD) || action2.equals(DEL) || action2.equals(BOOT_OBJ)
                ) {
                }
            } catch (ex: JSONException) {
                Log.e(TAG, "handleEventJSONRPC2Request: ", ex)
            }
        }
    }

    private fun handleDialogJSONRPC2Request(req: JSONRPC2Request) {
        Log.d(TAG, "handleDialogJSONRPC2Request: " + String.format("handleDialogJSONRPC2Request %s", req.method))
        val method = req.method
        if (method.equals(VERTO_INVITE)) {
            processCallInvitation(req.namedParams as HashMap<String?, Any?>)
            webRtc?.createPeerConnection(SESSION_ID, false)
        } else if (method.equals(VERTO_BYE)) {
//            vertoWSTransport.userBusy((HashMap<String, Object>) req.getNamedParams());
        } else if (method.equals(VERTO_INFO)) {
            vertoWSTransport?.handleInfo(req.namedParams as HashMap<String?, Any?>)
        } else if (method.equals(VERTO_DISPLAY)) {
            vertoWSTransport?.handleDisplay(req.namedParams as HashMap<String?, Any?>)
        } else if (method.equals(VERTO_MEDIA)) {
            handleMedia(req.namedParams as HashMap<String?, Any?>)
        } else if (method.equals(VERTO_ANSWER)) {
            handleAnswer(req.namedParams as HashMap<String?, Any?>)
        }
    }

    fun unsubscribeAllChannels() {
        for (i in this.subscribedChannels.indices) {
            vertoWSTransport?.unsubscribe(this.subscribedChannels.get(i))
        }
        this.subscribedChannels.clear()
    }

    private fun handleClientReadyJSONRPC2Request(req: JSONRPC2Request) {}


    fun sendCallInvitation(calleeNumber: String?) {
        webRtc?.setCalleeNumber(calleeNumber)
        executor.execute { webRtc?.createPeerConnection(SESSION_ID, true) }
    }

    fun handleAnswer(params: HashMap<String?, Any?>) {
        if (params.containsKey("callID")) {
            val callID = params["callID"] as String?
            Constants.CURRENT_CALL_ID = callID!!
            val remoteSDP: String?
            if (params.containsKey(SDP)) {
                remoteSDP = params[SDP] as String?
                webRtc?.setRemoteDescription(false, remoteSDP)
                return
            }
        }
    }

    fun handleMedia(dialogParams: HashMap<String?, Any?>) {
        val remoteSdp = dialogParams[SDP] as String?
        webRtc?.setRemoteDescriptionValue(remoteSdp)
        webRtc?.setRemoteDescription(false, remoteSdp)
    }

    fun connectWebSockets() {
        if (vertoWSTransport == null) {
            vertoWSTransport = VertoWSTransport(serverUrl, 0, this)
        }
        vertoWSTransport?.connect()
        initiateWebRtcEngine()
    }

    private fun initiateWebRtcEngine() {
        if (webRtc == null) {
            webRtc = WebRtc.getInstance(context, vertoWSTransport!!, iceServers)

            webRtc?.isOfferCreated = false
            webRtc?.isAnswerCreated = false
            webRtc?.isCallInitiator = false
            webRtc?.isOffer = false
            webRtc?.callCheck=false

            webRtc?.initializeWebrtcFactory()
        }
    }

    fun disconnectSocket() {
        vertoWSTransport?.disconnect()
//        vertoWSTransport?.webSocket = null
//        vertoWSTransport = null
    }

    fun disconnectWebRtcEngine() {
//        webRtc!!.factory!!.dispose()
        webRtc!!.peerConnection!!.close()
        webRtc?.factory = null
        webRtc = null
    }

    fun disconnectCall() {
        disconnectWebRtcEngine()
        disconnectSocket()
    }

    interface Callbacks {
        fun onWSConnectError(str: String?)
        fun onWSConnected()
        fun onWSDisconnected(z: Boolean)
        fun onWSMessage(str: String?)
    }

}