package mumayank.com.airshare2

import android.app.ProgressDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.nearby.connection.*
import kotlinx.android.synthetic.main.activity_joiner.*
import kotlinx.android.synthetic.main.item_joiner.view.*
import mumayank.com.airrecyclerview.AirRecyclerView
import mumayank.com.airshare.AirShare
import mumayank.com.airshare.Utils
import com.google.android.gms.nearby.Nearby
import android.content.DialogInterface
import android.support.v7.app.AlertDialog




class JoinerActivity : AppCompatActivity() {

    data class RvItem(val displayString: String, val endpointId: String)

    private var airShare: AirShare? = null
    private var rvAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null
    private var rvItems = ArrayList<RvItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_joiner)

        setId()
        setupRv()
        setupCallback()

        airShare?.startDiscovery()

    }

    private fun setId() {
        idTextView.text = "Device ID: " + Utils.getDeviceNickName()
    }

    private fun setupRv() {
        rvAdapter = AirRecyclerView.initAndGetAdapter(this, rv,
            object: AirRecyclerView.AirRecyclerViewCallbacks {
                override fun getBindView(viewHolder: RecyclerView.ViewHolder, viewType: Int, position: Int) {

                    val customViewHolder = viewHolder as CustomViewHolder
                    val rvItem = rvItems[position]

                    customViewHolder.textView.text = rvItem.displayString
                    customViewHolder.parentLayout.setOnClickListener {

                        progressLayout.visibility = View.VISIBLE

                        airShare?.requestConnectionToEndpoint(rvItem.endpointId, object: AirShare.RequestConnectionToEndpointCallback {

                            override fun onSuccessfullyRequestedConnectionToEndpoint() {
                                progressLayout.visibility = View.GONE
                                Toast.makeText(this@JoinerActivity, "Connection request is successfully placed", Toast.LENGTH_SHORT).show()
                            }

                            override fun onCouldNotRequestConnectionToEndpoint(e: Exception) {
                                progressLayout.visibility = View.GONE
                                Toast.makeText(this@JoinerActivity, "Could not place a request to connect successfully: ${e.message}", Toast.LENGTH_LONG).show()
                            }

                        })

                    }

                }

                override fun getSize(): Int {
                    return rvItems.size
                }

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                    return CustomViewHolder(view)
                }

                override fun getViewLayout(viewType: Int): Int {
                    return R.layout.item_joiner
                }

                override fun getViewType(position: Int): Int {
                    return 0
                }

            }
        )
    }

    class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val parentLayout: LinearLayout = view.parentLayout
        val textView: TextView = view.textView
    }

    private fun setupCallback() {
        airShare = AirShare(this, object: AirShare.CommonCallback {

            override fun onPermissionsDenied() {
                Toast.makeText(this@JoinerActivity, "Permission denied", Toast.LENGTH_LONG).show()
                finish()
            }

            override fun onStartedAdvertisingOrDiscovery() {
                Toast.makeText(this@JoinerActivity, "Discovery started...", Toast.LENGTH_SHORT).show()
            }

            override fun onCouldNotStartAdvertisingOrDiscovery(e: Exception?) {
                Toast.makeText(this@JoinerActivity, "Could not start discovery: ${e?.message}", Toast.LENGTH_LONG).show()
                finish()
            }

            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo, connectionInitiatedCallback: AirShare.ConnectionInitiatedCallback) {
                AlertDialog.Builder(this@JoinerActivity)
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
                Toast.makeText(this@JoinerActivity, "Connection rejected", Toast.LENGTH_LONG).show()
            }

            override fun onConnected(endpointId: String) {
                Toast.makeText(this@JoinerActivity, "connected!", Toast.LENGTH_SHORT).show()
            }

            override fun onDisconnected(endpointId: String) {
                Toast.makeText(this@JoinerActivity, "disconnected", Toast.LENGTH_LONG).show()
            }

            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                // todo
            }

            override fun onPayloadTransferUpdate(endpointId: String, payloadTransferUpdate: PayloadTransferUpdate) {
                // todo
            }

        }, object: AirShare.JoinerCallback {

            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                rvItems.add(RvItem(info.endpointName, endpointId))
                rvAdapter?.notifyItemInserted(rvItems.size - 1)
            }

            override fun onEndpointLost(endpointId: String) {
                var index = -1
                for (rvItem in rvItems) {
                    if (endpointId == rvItem.endpointId) {
                        index = rvItems.indexOf(rvItem)
                    }
                }
                rvItems.remove(rvItems[index])
                rvAdapter?.notifyItemRemoved(index)
            }

        })
    }

    override fun onDestroy() {
        airShare?.onDestroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airShare?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
