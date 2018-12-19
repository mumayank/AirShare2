package mumayank.com.airshare2

import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import mumayank.com.airshare.AirShare

class StarterActivity : AppCompatActivity() {

    private var airShare: AirShare? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starter)

        airShare = AirShare(this, object: AirShare.Callbacks {
            override fun onStartedAdvertisingOrDiscovery() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onCouldNotStartedAdvertisingOrDiscovery(e: Exception?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConnectionInitiated(connectionInitiatedCallbacks: AirShare.ConnectionInitiatedCallbacks) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConnectionRejected(connectionError: AirShare.ConnectionError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onConnected(endpointId: String) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDisconnected(endpointId: String) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onPayloadTransferUpdate(endpointId: String, payloadTransferUpdate: PayloadTransferUpdate) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })

        airShare?.startAdvertising()

    }

    override fun onDestroy() {
        super.onDestroy()
        airShare?.onDestroy()
    }

}
