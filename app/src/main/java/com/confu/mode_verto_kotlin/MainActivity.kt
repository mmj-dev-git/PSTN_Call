package com.confu.mode_verto_kotlin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.webrtc.PeerConnection
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), View.OnClickListener, EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks, VertoWSTransport.NetworkCallback, Verto.Callbacks {

    private val TAG = "MainActivity"

    lateinit var btnLogin: Button
    lateinit var btnDisconnect: Button
    lateinit var btnCall: Button
    lateinit var etUserName: EditText
    lateinit var etPassword: EditText
    lateinit var etCallNumber: EditText

    private lateinit var iceServerBuilder: PeerConnection.IceServer.Builder
    private var iceServers: MutableList<PeerConnection.IceServer> = mutableListOf()
    private val RC_CAMERA = 123
    private val RC_MIC = 456
    private val RC_STORAGE = 789
    private var verto: Verto? = null
    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var switchServer: Switch
    private var isServerChecked: Boolean = false
    private lateinit var executor: ExecutorService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = this
        activity = this
        this.executor = Executors.newSingleThreadExecutor()

        permissionsConsent()

        initComponents()
        initListeners()

//        toggleUI(false)

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private fun toggleUI(isConnected: Boolean) {
        if (isConnected) {
            etUserName.visibility = View.GONE
            etPassword.visibility = View.GONE
            btnLogin.visibility = View.GONE

            etCallNumber.visibility = View.VISIBLE
            btnCall.visibility = View.VISIBLE
        } else {
            etUserName.visibility = View.VISIBLE
            etPassword.visibility = View.VISIBLE
            btnLogin.visibility = View.VISIBLE

            etCallNumber.visibility = View.GONE
            btnCall.visibility = View.GONE
        }
    }

    private fun initComponents() {
        btnCall = findViewById(R.id.btnCall)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        btnLogin = findViewById(R.id.btnLogin)
        etUserName = findViewById(R.id.etUserName)
        etPassword = findViewById(R.id.etPassword)
        etCallNumber = findViewById(R.id.etCallNumber)
        switchServer = findViewById(R.id.switchServer)

        switchServer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                isServerChecked = true
                switchServer.text = "Online"
            } else {
                isServerChecked = false
                switchServer.text = "Local"
            }
        }
    }

    private fun initListeners() {
        btnLogin.setOnClickListener(this)
        btnCall.setOnClickListener(this)
        btnDisconnect.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            (R.id.btnCall) -> {
                val calleeNumber: String = etCallNumber.getText().toString().trim()
                verto?.sendCallInvitation(calleeNumber)
            }
            (R.id.btnLogin) -> {
                startWebSockets(if (isServerChecked) Constants.ONLINE_SERVER_URL else Constants.LOCAL_SERVER_URL)
            }
            (R.id.btnDisconnect) -> {
                runOnUiThread {
                    verto?.disconnectCall()
                    startWebSockets(if (isServerChecked) Constants.ONLINE_SERVER_URL else Constants.LOCAL_SERVER_URL)
                }
            }
        }
    }

    /**
     * Permissions
     */

    private fun hasPermissions(): Boolean {
        return EasyPermissions.hasPermissions(
            this,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun permissionsConsent() {
        if (hasPermissions()) {
            val xyrisisToken = XyrisisToken(this, this)
            xyrisisToken.execute()

        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.camera_permission),
                RC_CAMERA,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private fun hasCameraPermission(): Boolean {
        return EasyPermissions.hasPermissions(
            this,
            Manifest.permission.CAMERA
        )
    }

    private fun hasMicPermission(): Boolean {
        return EasyPermissions.hasPermissions(
            this,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private fun hasStoragePermission(): Boolean {
        return EasyPermissions.hasPermissions(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun cameraPermission() {
        if (hasCameraPermission()) {
            micPermission()
        } else {
            // Ask for one permission
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.camera_permission),
                RC_CAMERA,
                Manifest.permission.CAMERA
            )
        }
    }

    private fun micPermission() {
        if (hasMicPermission()) {
            storagePermission()
        } else {
            // Ask for one permission
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.audio_permission),
                RC_MIC,
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    private fun storagePermission() {
        if (hasStoragePermission()) {
            //TODO("Process further implementation")
        } else {
            // Ask for one permission
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.storage_permission),
                RC_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            Toast.makeText(this, "Returned back from setting page to app.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
//        when (requestCode) {
//            RC_CAMERA -> {
//                Toast.makeText(this, "Camera permission is Granted", Toast.LENGTH_SHORT).show()
//            }
//            RC_MIC -> {
//                Toast.makeText(this, "Mic permission is granted.", Toast.LENGTH_SHORT).show()
//            }
//            RC_STORAGE -> {
//                Toast.makeText(this, "Storage permission is granted.", Toast.LENGTH_SHORT).show()
//            }
//        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
//        when (requestCode) {
//            RC_CAMERA -> {
//                Toast.makeText(this, "Camera permission not granted.", Toast.LENGTH_SHORT).show()
//            }
//            RC_MIC -> {
//                Toast.makeText(this, "Mic permission not granted.", Toast.LENGTH_SHORT).show()
//            }
//            RC_STORAGE -> {
//                Toast.makeText(this, "Storage permission not granted.", Toast.LENGTH_SHORT).show()
//            }
//        }
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }


    }

    override fun onRationaleAccepted(requestCode: Int) {
        TODO("Not yet implemented")
    }

    override fun onRationaleDenied(requestCode: Int) {
        TODO("Not yet implemented")
    }

    /**
     * Callback methods for ice servers
     */

    override fun JsonData(example: WebRTCModel?) {

    }

    override fun initializeOutboundCall(example: WebRTCModel?) {
        Log.d(TAG, "initializeOutboundCall: ${example.toString()}")
        example!!.let {
            if (it.iceServers != null && it.iceServers.isNotEmpty()) {
                for (i: Int in it.iceServers.indices) {
                    iceServerBuilder = if (it.iceServers[i].username.isNotEmpty()) {
                        PeerConnection.IceServer
                            .builder(it.iceServers[i].url)
                            .setUsername(it.iceServers[i].username)
                            .setPassword(it.iceServers[i].credentials)
                    } else {
                        PeerConnection.IceServer.builder(it.iceServers[i].url)
                    }

                    Log.d(TAG, "initializeOutboundCall: ${iceServerBuilder.createIceServer()}")
                    iceServers.add(iceServerBuilder.createIceServer())
                    Log.d(TAG, "initializeOutboundCall: ${iceServers.size}")
                }
            }
        }
    }

    private fun startWebSockets(serverUrl: String) {
//        verto = null
        val userName: String = etUserName.getText().toString().trim()
        val password: String = "Y0unite#019?!"
        Log.d(TAG, "startWebSockets: $serverUrl")
        if (verto == null) {
            verto = Verto.getInstance(activity, context, userName, password, iceServers, serverUrl, this)
        }
        verto?.connectWebSockets()
    }

    override fun onWSConnectError(str: String?) {
        TODO("Not yet implemented")
    }

    override fun onWSConnected() {
//        toggleUI(true)
    }

    override fun onWSDisconnected(z: Boolean) {
//        toggleUI(false)
    }

    override fun onWSMessage(str: String?) {
        TODO("Not yet implemented")
    }
}