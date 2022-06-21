package com.confu.mode_verto_kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.util.Log
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnection.RTCConfiguration
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class WebRtc private constructor() : PeerConnection.Observer, SdpObserver {


    val TAG = "WebRtcClass"

    //    private static final String[] hardcodedICEServers = {"stun:stun1.l.google.com:19302", "stun:stun2.l.google.com:19302", "stun:stun3.l.google.com:19302", "stun:stun4.l.google.com:19302"};
    private val hardcodedICEServers = arrayOf("stun:stun.l.google.com:19302")
    private var context: Context? = null
    private val deviceId: String? = null
    private var peerId: String? = null

    //    private var iceServers: List<IceServer> = ArrayList()
    private var iceServers: List<IceServer> = ArrayList()
    private val peerConnectionFactory: PeerConnectionFactory? = null
    public var peerConnection: PeerConnection? = null
    private lateinit var sessionDescription: SessionDescription
    private var vertoWSTransport: VertoWSTransport? = null
    private var sessionId: String? = null
    public var factory: PeerConnectionFactory? = null
    private lateinit var executor: ExecutorService
    var isCallInitiator = false
    var isOffer = false
    private var calleeNumber: String? = null
    private var remoteDescription: String? = null
    private lateinit var mediaConstraints: MediaConstraints
    lateinit var remoteAudioTrack: AudioTrack
    private val AUDIO_TRACK_ID = "ARDAMSa0"
    var isOfferCreated = false
    var isAnswerCreated: Boolean = false
    var callCheck = false
    var mediaStream: MediaStream? = null


    private constructor(context: Context, vertoWSTransport: VertoWSTransport, iceServers: List<IceServer>) : this() {
        this.context = context
        this.vertoWSTransport = vertoWSTransport
        this.iceServers = iceServers
        this.executor = Executors.newSingleThreadExecutor()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var webRtc: WebRtc? = null
        public fun getInstance(context: Context, vertoWSTransport: VertoWSTransport, iceServers: List<IceServer>): WebRtc {
            if (webRtc == null) {
                synchronized(WebRtc::class.java) {
                    webRtc = WebRtc(context, vertoWSTransport, iceServers)
                }
            }
            return webRtc!!
        }
    }

    fun initializeWebrtcFactory() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        val options = PeerConnectionFactory.Options()
        factory = PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory()

        Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE)
    }


    fun setCalleeNumber(calleeNumber: String?) {
        this.calleeNumber = calleeNumber
    }

    fun createPeerConnection(sessionId: String?, isOffer: Boolean) {
        Log.d(TAG, "createPeerConnection: ")
        this.sessionId = sessionId
        this.isOffer = isOffer
        initiatePeerConnection(isOffer)
    }

    fun initiatePeerConnection(isOffer: Boolean) {
        peerConnection = createPeerConnection()
        if (isOffer) {
            Log.d(TAG, "initiatePeerConnection: IsOffer")
            isCallInitiator = true
            createOffer()
        } else {
            setRemoteDescription(true, UtilMethods.dtoCallInvitation.sdp)
        }
    }


    private fun createPeerConnection(): PeerConnection? {
        mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))

        //Audio Source
        val audioSource = factory!!.createAudioSource(mediaConstraints)
        val localAudioTrack = factory!!.createAudioTrack("AudioTrack", audioSource)
        localAudioTrack.setEnabled(true)

        //add stream to peer connection
        mediaStream = factory?.createLocalMediaStream("MediaStream")
        mediaStream?.addTrack(localAudioTrack)

        //Audio Manager
        val audioManager = context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = true
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        /*Peer Connection*/

        //[For Native WebRtc]
        /*Peer Connection*/

        //[For Native WebRtc]
        val iceServers = LinkedList<IceServer>()
        for (iceServer in hardcodedICEServers) {
            iceServers.add(IceServer.builder(iceServer).createIceServer()) //for native webrtc
        }
//        val rtcConfig = RTCConfiguration(iceServers)
        val rtcConfig = RTCConfiguration(iceServers)
        //        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL;


        //[Experimental]
//        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED /*Original*/
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED /*updated*/
//        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.BALANCED /*Original*/
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT /*updated*/
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA
        rtcConfig.disableIpv6 = true
        rtcConfig.disableIPv6OnWifi = true
        rtcConfig.enableDtlsSrtp = true
        //        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.PLAN_B;
//        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        //[Experimental]
        val peerConnection = factory!!.createPeerConnection(rtcConfig, this@WebRtc)

        //[End of Native WebRtc]
//        PeerConnection peerConnection = peerConnectionFactory.createPeerConnection(iceServers, new MediaConstraints(), this);// for LibJingle
        peerConnection!!.addStream(mediaStream)
        return peerConnection
    }

    private fun createOffer() {
        if (!isOfferCreated) {
            Log.d(TAG, "createOffer: ")
            isOfferCreated = true
            executor.execute {
                peerConnection!!.createOffer(this@WebRtc, mediaConstraints)
            }
        }
    }

    private fun createAnswer() {
        Log.d(TAG, "createAnswer: ")
        if (!isAnswerCreated) {
            isAnswerCreated = true
            executor.execute {
                peerConnection!!.createAnswer(this@WebRtc, mediaConstraints)
            }
        }
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        Log.d(TAG, "onSignalingChange: ${p0?.name}")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        Log.d(TAG, "onIceConnectionChange: ${p0?.name}")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        Log.d(TAG, "onIceGatheringChange: $p0")
        if (p0 == PeerConnection.IceGatheringState.COMPLETE) {
//            Log.d(TAG, "onIceGatheringChange: " + peerConnection!!.localDescription.description)
//            handleCallInvitationAndResponse()
        }
    }

    override fun onIceCandidate(p0: IceCandidate?) {
        Log.d(TAG, "onIceCandidate: $p0")
        if (p0 != null) {
//            parseIceCandidate(p0)
//            if (UtilityMethods.parseIceCandidate(p0)) {
//                handleCallInvitationAndResponse()
//            }
            Log.d(TAG, "onIceCandidate: " + peerConnection!!.localDescription.description)
            UtilityMethods.parseIceCandidate(p0, webRtc)
        }
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
    }

    override fun onAddStream(p0: MediaStream?) {
        Log.d(TAG, "onAddStream: ")
        if (p0!!.audioTracks.size > 0) {
            remoteAudioTrack = p0.audioTracks.get(0)
            remoteAudioTrack.setEnabled(true)
        }
    }

    override fun onRemoveStream(p0: MediaStream?) {

    }

    override fun onDataChannel(p0: DataChannel?) {

    }

    override fun onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded: ")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        Log.d(TAG, "onAddTrack: ")
    }

    override fun onCreateSuccess(p0: SessionDescription?) {
        Log.d(TAG, "onCreateSuccess: ")
        peerConnection!!.setLocalDescription(this, p0)
    }

    override fun onSetSuccess() {
        Log.d(TAG, "onSetSuccess: ")
    }

    override fun onCreateFailure(p0: String?) {
        Log.d(TAG, "onCreateFailure: ")
    }

    override fun onSetFailure(p0: String?) {
        Log.d(TAG, "onSetFailure: ")
    }

    fun setRemoteDescription(isOffer: Boolean, remoteDescription: String?) {
        Log.d(TAG, "setRemoteDescription: ")
        this.remoteDescription = remoteDescription
        if (isOffer) {
            Log.d(TAG, "setRemoteDescription: As Offer \t\t${remoteDescription.toString()}")
            executor.execute {
                peerConnection!!.setRemoteDescription(
                    this@WebRtc,
                    SessionDescription(SessionDescription.Type.OFFER, remoteDescription)
                )
            }
            if (!isCallInitiator) {
                createAnswer()
            }
        } else {
            Log.d(TAG, "setRemoteDescription: As Answer ${remoteDescription.toString()}")
//            var rd = remoteDescription.toString()
//            rd?.replace("172.1.0.165", "65.0.190.219")
//            Log.d(TAG, "setRemoteDescription Modified: As Answer ${rd.toString()}")
            executor.execute {
                peerConnection!!.setRemoteDescription(
                    this@WebRtc,
                    SessionDescription(SessionDescription.Type.ANSWER, remoteDescription)
                )
            }
//            parseRemoteSdp(this.remoteDescription);
        }
    }

    fun ipInCGN(ip: String): Boolean {
        val octets = ip.split("\\.")
        return octets[0].toInt() == 100 && octets[1].toInt() > 63 && octets[1].toInt() < 128
    }

    private fun parseIceCandidate(iceCandidate: IceCandidate) {
        Log.d(TAG, "parseIceCandidate: ${iceCandidate.sdp}")
        val ip = iceCandidate.sdp.split("\\s".toRegex())[4]
        try {
            val ia = InetAddress.getByName(ip)
            if (ia.isSiteLocalAddress || ia.isLoopbackAddress || UtilityMethods.ipInCGN(ip)) {
                Log.d(TAG, "parseIceCandidate: Invalid IceCandidate.")
                return
            }
            handleCallInvitationAndResponse()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
    }

    public fun handleCallInvitationAndResponse() {
        if (!callCheck) {
            Log.d(TAG, "handleCallInvitationAndResponse: ")
            callCheck = true
            this.sessionDescription = peerConnection!!.localDescription
            Constants.LOCAL_SDP = sessionDescription.description
            if (isOffer) {
                Log.d(TAG, "handleCallInvitationAndResponse: Send Invitation: \t\t ${sessionDescription}")
                vertoWSTransport!!.sendInvite(sessionDescription, sessionId!!, calleeNumber!!)
            } else {
                Log.d(TAG, "handleCallInvitationAndResponse: SendAnswer: \t\t $sessionDescription")
                vertoWSTransport!!.sendAnswer(sessionDescription, sessionId!!)
            }
        }
    }

    fun setRemoteDescriptionValue(remoteDescription: String?) {
        Log.d(TAG, "setRemoteDescriptionValue: $remoteDescription")
        this.remoteDescription = remoteDescription
    }
}