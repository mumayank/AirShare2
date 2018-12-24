package mumayank.com.airshare2

import android.app.*
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import mumayank.com.airshare.AirShare
import mumayank.com.airshare.Utils
import org.greenrobot.eventbus.EventBus
import android.support.v4.util.SimpleArrayMap
import java.io.File
import java.nio.charset.StandardCharsets
import mumayank.com.airpermissions.AirPermissions
import android.content.Context
import android.os.Build
import android.util.Log
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import mumayank.com.airshare2.events.*


class TransferActivity : AppCompatActivity() {

    var airShare: AirShare? = null
    var connectedEndpoints = ArrayList<String>()
    private var airPermission: AirPermissions? = null

    val READ_REQUEST_CODE = 42
    lateinit var ENDPOINT_ID_EXTRA: String

    private val incomingFilePayloads = SimpleArrayMap<Long, Payload>()
    private val completedFilePayloads = SimpleArrayMap<Long, Payload>()
    private val filePayloadFilenames = SimpleArrayMap<Long, String>()

    val incomingPayloads = SimpleArrayMap<Long, NotificationCompat.Builder>()
    val outgoingPayloads = SimpleArrayMap<Long, NotificationCompat.Builder>()

    val transferItems = ArrayList<TransferItem>()
    data class TransferItem(val payloadId: Long, var fileName: String, var progress: Int, val isDownloading: Boolean, var isError: Boolean = false)

    lateinit var notificationManager: NotificationManager
    val CHANNEL_ID = "AirShare"

    private enum class Stack {
        Select,
        Start,
        Join,
        Transfer
    }

    private var stack: Stack = Stack.Select

    var endpointIdCurrentlySelected = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)
        supportActionBar?.hide()
        ENDPOINT_ID_EXTRA = Utils.getServiceId(this) + ".EndpointId"
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Downloads"
            val description = "This keeps you up-to-dated about your incoming or outgoing files"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }

        airPermission = AirPermissions(
            this,
            arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            object: AirPermissions.Callbacks {
                override fun onSuccess() {
                    switchToSelectionFragment(false)
                }

                override fun onFailure() {
                    finish()
                }
            }
        )
    }

    fun switchToSelectionFragment(shouldFinish: Boolean) {
        if (shouldFinish) {
            finish()
        } else {
            supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out).replace(R.id.parentLayout, SelectionFragment()).commit()
            stack = Stack.Select
            refresh()
        }
    }

    fun switchToStarterFragment() {
        supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out).replace(R.id.parentLayout, StarterFragment()).commit()
        stack = Stack.Start
    }

    fun switchToJoinerFragment() {
        supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out).replace(R.id.parentLayout, JoinerFragment()).commit()
        stack = Stack.Join
    }

    fun switchToTransferFragment() {
        supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out).replace(R.id.parentLayout, TransferFragment()).commit()
        stack = Stack.Select // have done this on purpose
        refresh()
    }

    override fun onBackPressed() {
        if (stack == Stack.Transfer || stack == Stack.Start || stack == Stack.Join) {
            stack = Stack.Select
            switchToSelectionFragment(true)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        refresh()
        super.onDestroy()
    }

    private fun refresh() {
        airShare?.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airShare?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airPermission?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getCommonCallback(): AirShare.CommonCallback {
        return object: AirShare.CommonCallback {

            override fun onPermissionsDenied() {
                Toast.makeText(this@TransferActivity, "Permission denied", Toast.LENGTH_LONG).show()
                onBackPressed()
            }

            override fun onStartedAdvertisingOrDiscovery() {
                //Toast.makeText(this@TransferActivity, "Started...", Toast.LENGTH_SHORT).show()
                EventBus.getDefault().post(OnStarted())
            }

            override fun onCouldNotStartAdvertisingOrDiscovery(e: Exception?) {
                Toast.makeText(this@TransferActivity, "Could not start: ${e?.message}", Toast.LENGTH_LONG).show()
                onBackPressed()
            }

            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo, connectionInitiatedCallback: AirShare.ConnectionInitiatedCallback) {
                /*AlertDialog.Builder(this@TransferActivity)
                    .setTitle("Accept connection to " + connectionInfo.endpointName + " ?")
                    .setMessage("Confirm the code matches on both devices:\n\n" + connectionInfo.authenticationToken + "\n\n")
                    .setPositiveButton("Accept") { _, _ ->
                        connectionInitiatedCallback.onAcceptConnection()
                    }
                    .setNegativeButton("Reject") { _, _ ->
                        connectionInitiatedCallback.onRejectConnection()
                    }
                    .setCancelable(false)
                    .show()*/
                connectionInitiatedCallback.onAcceptConnection()
                //Toast.makeText(this@TransferActivity, "Connected with ${connectionInfo.endpointName}\nSecurity code: ${connectionInfo.authenticationToken}", Toast.LENGTH_LONG).show()
            }

            override fun onConnectionRejected(connectionError: AirShare.ConnectionError) {
                Toast.makeText(this@TransferActivity, "Connection rejected", Toast.LENGTH_LONG).show()
                EventBus.getDefault().post(OnConnectionAcceptedOrRejected())
            }

            override fun onConnected(endpointId: String) {
                Toast.makeText(this@TransferActivity, "connected!", Toast.LENGTH_SHORT).show()
                endpointIdCurrentlySelected = endpointId
                connectedEndpoints.add(endpointId)
                EventBus.getDefault().post(OnConnectionAcceptedOrRejected())
                switchToTransferFragment()
            }

            override fun onDisconnected(endpointId: String) {
                connectedEndpoints.remove(endpointId)
                Toast.makeText(this@TransferActivity, "disconnected", Toast.LENGTH_LONG).show()
                finish()
            }

            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                if (payload.type == Payload.Type.BYTES) {
                    val payloadFileNameMessage = String((payload.asBytes() as ByteArray), StandardCharsets.UTF_8)
                    val payloadId = addPayloadFileName(payloadFileNameMessage)
                    processFilePayload(payloadId)
                } else if (payload.type == Payload.Type.FILE) {
                    incomingFilePayloads.put(payload.id, payload)

                    val notification = buildNotification(payload, true)
                    //notificationManager.notify(payload.id.toInt(), notification.build())
                    incomingPayloads.put(payload.id, notification)

                    transferItems.add(TransferItem(
                        payload.id,
                        filePayloadFilenames.get(payload.id) ?: "Incoming File",
                        0,
                        true
                    ))
                    EventBus.getDefault().post(OnTransferUpdate())
                }
            }

            override fun onPayloadTransferUpdate(endpointId: String, payloadTransferUpdate: PayloadTransferUpdate) {

                // notification

                val payloadId = payloadTransferUpdate.payloadId

                var transferItem: TransferItem? = null
                for (transferItemTemp in transferItems) {
                    if (transferItemTemp.payloadId == payloadId) {
                        transferItem = transferItemTemp
                        break
                    }
                }

                var notification: NotificationCompat.Builder? = null
                if (incomingPayloads.containsKey(payloadId)) {
                    notification = incomingPayloads.get(payloadId)
                    if (payloadTransferUpdate.status != PayloadTransferUpdate.Status.IN_PROGRESS) {
                        incomingPayloads.remove(payloadId)
                    }
                } else if (outgoingPayloads.containsKey(payloadId)) {
                    notification = outgoingPayloads.get(payloadId)
                    if (payloadTransferUpdate.status != PayloadTransferUpdate.Status.IN_PROGRESS) {
                        outgoingPayloads.remove(payloadId)
                    }
                }
                if (notification == null) {
                    // do nothing
                } else {
                    when (payloadTransferUpdate.status) {
                        PayloadTransferUpdate.Status.IN_PROGRESS -> {
                            val size = payloadTransferUpdate.totalBytes
                            if (size == (-1).toLong()) {
                                // This is a stream payload, so we don't need to update anything at this point.
                            } else {
                                val percentTransferred = (100.toFloat() * ( payloadTransferUpdate.bytesTransferred.toFloat() / payloadTransferUpdate.totalBytes.toFloat() )).toInt()
                                if (percentTransferred == 100) {
                                    updateNotificationSuccess(notification, transferItem)
                                } else {
                                    notification.setProgress(100, percentTransferred, false)
                                }

                                if (transferItem != null) {
                                    transferItem.progress = percentTransferred
                                    EventBus.getDefault().post(OnTransferUpdate())
                                }
                            }
                        }
                        PayloadTransferUpdate.Status.SUCCESS -> {
                            updateNotificationSuccess(notification, transferItem)
                        }
                        PayloadTransferUpdate.Status.FAILURE -> {
                            notification
                                .setProgress(0,0,false)
                                .setContentTitle("Transfer failed")

                            if (transferItem != null) {
                                transferItem.isError = true
                                EventBus.getDefault().post(OnTransferUpdate())
                            }
                        }
                        PayloadTransferUpdate.Status.CANCELED -> {
                            notification
                                .setProgress(0,0,false)
                                .setContentTitle("Transfer failed")

                            if (transferItem != null) {
                                transferItem.isError = true
                                EventBus.getDefault().post(OnTransferUpdate())
                            }
                        }
                        else -> {
                            Log.e("airshare", "Unknown status of notification update")
                        }
                    }
                    //notificationManager.notify(payloadId.toInt(), notification.build())
                }

                // actual work
                if (payloadTransferUpdate.status == PayloadTransferUpdate.Status.SUCCESS) {
                    val payload = incomingFilePayloads.remove(payloadId)
                    completedFilePayloads.put(payloadId, payload)
                    if (payload?.type == Payload.Type.FILE) {
                        processFilePayload(payloadId)
                    }
                }

            }
        }
    }

    private fun updateNotificationSuccess(notification: NotificationCompat.Builder, transferItem: TransferItem?) {
        notification
            .setProgress(100, 100, false)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), 0))
            .setContentTitle("Transfer complete!")

        if (transferItem != null) {
            transferItems.get(transferItems.indexOf(transferItem)).progress = 100
            EventBus.getDefault().post(OnTransferUpdate())
        }
    }

    private fun addPayloadFileName(payloadFileNameMessage: String): Long {
        val parts = payloadFileNameMessage.split(":")
        val payloadId = parts[0].toLong()
        val fileName = parts[1]
        filePayloadFilenames.put(payloadId, fileName)
        return payloadId
    }

    private fun processFilePayload(payloadId: Long) {
        val filePayload = completedFilePayloads.get(payloadId)
        val fileName = filePayloadFilenames.get(payloadId)
        if (filePayload != null && fileName != null) {
            completedFilePayloads.remove(payloadId)
            filePayloadFilenames.remove(payloadId)
            val payloadFile = filePayload.asFile()?.asJavaFile()
            payloadFile?.renameTo(File(payloadFile.parentFile, fileName))
        }
    }

    fun startAdvertising() {
        airShare = AirShare(this, getCommonCallback())
        airShare?.startAdvertising()
    }

    fun startDiscovering() {
        airShare = AirShare(this, getCommonCallback(), object: AirShare.JoinerCallback {

            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                EventBus.getDefault().post(OnEndpointFound(endpointId, info))
            }

            override fun onEndpointLost(endpointId: String) {
                EventBus.getDefault().post(OnEndpointLost(endpointId))
            }

        })
        airShare?.startDiscovery()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun buildNotification(payload: Payload, isIncoming: Boolean): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle( if (isIncoming) "Receiving..." else "Sending..." )
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(100, 0, false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    interface Callbacks {
        fun onSuccess(fileDisplayName: String, fileSize: Long)
        fun onOperationFailed()
    }

    companion object {
        fun extractFileProperties(activity: Activity, fileUri: Uri, callbacks: Callbacks) {
            val cursor: Cursor? = activity.contentResolver.query(fileUri, null, null, null, null, null)
            if (cursor == null) {
                callbacks.onOperationFailed()
            } else {
                cursor.use {
                    if (it.moveToFirst()) {
                        val displayName: String = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                        var size = 0L
                        if (!it.isNull(sizeIndex)) {
                            size = it.getLong(sizeIndex)
                        }
                        callbacks.onSuccess(displayName, size)
                    } else {
                        callbacks.onOperationFailed()
                    }
                    cursor.close()
                }
            }
        }
    }
}
