package mumayank.com.airshare2

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import mumayank.com.airshare.AirShare
import mumayank.com.airshare2.events.OnConnectionAcceptedOrRejected
import mumayank.com.airshare2.events.OnEndpointFound
import mumayank.com.airshare2.events.OnEndpointLost
import org.greenrobot.eventbus.EventBus

class TransferActivity : AppCompatActivity() {

    var airShare: AirShare? = null
    var connectedEndpoints = ArrayList<String>()

    private enum class Stack {
        Select,
        Start,
        Join,
        Transfer
    }

    private var stack: Stack = Stack.Select

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)
        supportActionBar?.hide()
        switchToSelectionFragment()
    }

    fun switchToSelectionFragment() {
        supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out).replace(R.id.parentLayout, SelectionFragment()).commit()
        stack = Stack.Select
        refresh()
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
        stack = Stack.Transfer
        refresh()
    }

    override fun onBackPressed() {
        if (stack == Stack.Transfer || stack == Stack.Start || stack == Stack.Join) {
            stack = Stack.Select
            switchToSelectionFragment()
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
    }

    private fun getCommonCallback(): AirShare.CommonCallback {
        return object: AirShare.CommonCallback {

            override fun onPermissionsDenied() {
                Toast.makeText(this@TransferActivity, "Permission denied", Toast.LENGTH_LONG).show()
                onBackPressed()
            }

            override fun onStartedAdvertisingOrDiscovery() {
                Toast.makeText(this@TransferActivity, "Started...", Toast.LENGTH_SHORT).show()
            }

            override fun onCouldNotStartAdvertisingOrDiscovery(e: Exception?) {
                Toast.makeText(this@TransferActivity, "Could not start: ${e?.message}", Toast.LENGTH_LONG).show()
                onBackPressed()
            }

            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo, connectionInitiatedCallback: AirShare.ConnectionInitiatedCallback) {
                AlertDialog.Builder(this@TransferActivity)
                    .setTitle("Accept connection to " + connectionInfo.endpointName + " ?")
                    .setMessage("Confirm the code matches on both devices:\n\n" + connectionInfo.authenticationToken + "\n\n")
                    .setPositiveButton("Accept") { _, _ ->
                        connectionInitiatedCallback.onAcceptConnection()
                    }
                    .setNegativeButton("Reject") { _, _ ->
                        connectionInitiatedCallback.onRejectConnection()
                    }
                    .setCancelable(false)
                    .show()
            }

            override fun onConnectionRejected(connectionError: AirShare.ConnectionError) {
                Toast.makeText(this@TransferActivity, "Connection rejected", Toast.LENGTH_LONG).show()
                EventBus.getDefault().post(OnConnectionAcceptedOrRejected())
            }

            override fun onConnected(endpointId: String) {
                Toast.makeText(this@TransferActivity, "connected!", Toast.LENGTH_SHORT).show()
                connectedEndpoints.add(endpointId)
                EventBus.getDefault().post(OnConnectionAcceptedOrRejected())
                switchToTransferFragment()
            }

            override fun onDisconnected(endpointId: String) {
                connectedEndpoints.remove(endpointId)
                Toast.makeText(this@TransferActivity, "disconnected", Toast.LENGTH_LONG).show()
            }

            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                // todo
            }

            override fun onPayloadTransferUpdate(endpointId: String, payloadTransferUpdate: PayloadTransferUpdate) {
                // todo
            }

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

}
