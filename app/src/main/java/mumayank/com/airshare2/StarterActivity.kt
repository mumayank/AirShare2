package mumayank.com.airshare2

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import kotlinx.android.synthetic.main.activity_joiner.*
import mumayank.com.airshare.AirShare
import mumayank.com.airshare.Utils

class StarterActivity : AppCompatActivity() {

    private var airShare: AirShare? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starter)

        idTextView.text = Utils.getDeviceNickName() + " is your device's ID"

        airShare = AirShare(this, object: AirShare.CommonCallback {

            override fun onPermissionsDenied() {
                Toast.makeText(this@StarterActivity, "Permission denied", Toast.LENGTH_LONG).show()
                finish()
            }

            override fun onStartedAdvertisingOrDiscovery() {
                Toast.makeText(this@StarterActivity, "Advertising started...", Toast.LENGTH_SHORT).show()
            }

            override fun onCouldNotStartAdvertisingOrDiscovery(e: Exception?) {
                Toast.makeText(this@StarterActivity, "Could not start advertising: ${e?.message}", Toast.LENGTH_LONG).show()
                finish()
            }

            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo, connectionInitiatedCallback: AirShare.ConnectionInitiatedCallback) {
                AlertDialog.Builder(this@StarterActivity)
                    .setTitle("Accept connection to " + connectionInfo.endpointName + " ?")
                    .setMessage("Confirm the code matches on both devices: " + connectionInfo.authenticationToken)
                    .setPositiveButton("Accept") { _, _ ->
                        connectionInitiatedCallback.onAcceptConnection()
                    }
                    .setNegativeButton("Reject") { _, _ ->
                        connectionInitiatedCallback.onAcceptConnection()
                    }
                    .setNeutralButton("Cancel") { _, _ ->
                        // do nothing
                    }
                    .setCancelable(false)
                    .show()
            }

            override fun onConnectionRejected(connectionError: AirShare.ConnectionError) {
                Toast.makeText(this@StarterActivity, "Connection rejected", Toast.LENGTH_LONG).show()
            }

            override fun onConnected(endpointId: String) {
                Toast.makeText(this@StarterActivity, "connected!", Toast.LENGTH_SHORT).show()
            }

            override fun onDisconnected(endpointId: String) {
                Toast.makeText(this@StarterActivity, "disconnected", Toast.LENGTH_LONG).show()
            }

            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                // todo
            }

            override fun onPayloadTransferUpdate(endpointId: String, payloadTransferUpdate: PayloadTransferUpdate) {
                // todo
            }

        })

        airShare?.startAdvertising()

    }

    override fun onDestroy() {
        super.onDestroy()
        airShare?.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airShare?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}
