package mumayank.com.airshare

import android.app.Activity
import android.widget.Toast
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class AirShare constructor(
    private val activity: Activity,
    private val callbacks: Callbacks,
    private val joinerCallback: JoinerCallback? = null
) {

    private var connectionLifecycleCallback: ConnectionLifecycleCallback? = null
    private var payloadCallback: PayloadCallback? = null
    private var endpointDiscoveryCallback: EndpointDiscoveryCallback? = null

    constructor(
        activity: Activity,
        callbacks: Callbacks
    ): this(activity, callbacks, null)

    init {
        Utils.SERVICE_ID = activity.application.packageName
        defineVars()
    }

    interface Callbacks {
        fun onStartedAdvertisingOrDiscovery()
        fun onCouldNotStartedAdvertisingOrDiscovery(e: Exception? = null)
        fun onConnectionInitiated(connectionInitiatedCallbacks: ConnectionInitiatedCallbacks)
        fun onConnectionRejected(connectionError: ConnectionError)
        fun onConnected(endpointId: String)
        fun onDisconnected(endpointId: String)
        fun onPayloadReceived(endpointId: String, payload: Payload)
        fun onPayloadTransferUpdate(endpointId: String, payloadTransferUpdate: PayloadTransferUpdate)
    }

    interface ConnectionInitiatedCallbacks {
        fun onAcceptConnection()
        fun onRejectConnection()
    }

    enum class ConnectionError {
        ConnectionRejectedByOneOrBothSides,
        ConnectionBrokeBeforeItCouldBeAccepted,
        ConnectionErrorWithUnknownStatusCode
    }

    interface JoinerCallback {
        fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo)
        fun onEndpointLost(endpointId: String)
        fun onSuccessfullyRequestedConnectionToEndpoint()
        fun onCouldNotRequestConnectionToEndpoint(e: Exception)
    }

    private fun defineVars() {

        /**
         * define payload callback
         */
        payloadCallback = object: PayloadCallback() {

            override fun onPayloadReceived(p0: String, p1: Payload) {
                callbacks.onPayloadReceived(p0, p1)
            }

            override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
                callbacks.onPayloadTransferUpdate(p0, p1)
            }

        }

        /**
         * define connection lifecycle callback
         */
        connectionLifecycleCallback = object: ConnectionLifecycleCallback() {

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {

                when (result.status.statusCode) {

                    ConnectionsStatusCodes.STATUS_OK ->
                        // We're connected! Can now start sending and receiving data.
                        callbacks.onConnected(endpointId)

                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED ->
                        // The connection was rejected by one or both sides.
                        callbacks.onConnectionRejected(ConnectionError.ConnectionRejectedByOneOrBothSides)

                    ConnectionsStatusCodes.STATUS_ERROR ->
                        // The connection broke before it was able to be accepted.
                        callbacks.onConnectionRejected(ConnectionError.ConnectionBrokeBeforeItCouldBeAccepted)

                    else ->
                        // Unknown status code
                        callbacks.onConnectionRejected(ConnectionError.ConnectionErrorWithUnknownStatusCode)

                }

            }

            override fun onDisconnected(endpointId: String) {
                // We've been disconnected from this endpoint. No more data can be sent or received.
                callbacks.onDisconnected(endpointId)
            }

            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {

                callbacks.onConnectionInitiated(object: ConnectionInitiatedCallbacks {

                    override fun onAcceptConnection() {
                        Nearby.getConnectionsClient(activity).acceptConnection(endpointId, (payloadCallback as PayloadCallback))
                    }

                    override fun onRejectConnection() {
                        Nearby.getConnectionsClient(activity).rejectConnection(endpointId)
                    }

                })

            }

        }

        /**
         * define endpoint discovery callback
         */

        endpointDiscoveryCallback = object: EndpointDiscoveryCallback() {

            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                joinerCallback?.onEndpointFound(endpointId, info)
            }

            override fun onEndpointLost(endpointId: String) {
                joinerCallback?.onEndpointLost(endpointId)
            }

        }
    }

    fun connectToEndpoint(endpointId: String) {
        Nearby.getConnectionsClient(activity).requestConnection(Utils.DEVICE_NICK_NAME, endpointId, (connectionLifecycleCallback as ConnectionLifecycleCallback))
            .addOnSuccessListener { unused: Void ->
                joinerCallback?.onSuccessfullyRequestedConnectionToEndpoint()

            }
            .addOnFailureListener { e: Exception ->
                joinerCallback?.onCouldNotRequestConnectionToEndpoint(e)
            }
    }

    fun onDestroy() {
        try {
            Nearby.getConnectionsClient(activity).stopAdvertising()
        } catch (e: Exception) {}
        try {
            Nearby.getConnectionsClient(activity).stopDiscovery()
        } catch (e: Exception) {}
    }

    /**
     * advertising
     */

    fun startAdvertising() {
        if (connectionLifecycleCallback == null) {
            callbacks.onCouldNotStartedAdvertisingOrDiscovery()
        } else {
            Nearby.getConnectionsClient(activity).startAdvertising(Utils.DEVICE_NICK_NAME, Utils.SERVICE_ID, (connectionLifecycleCallback as ConnectionLifecycleCallback), AdvertisingOptions.Builder().setStrategy(Utils.STRATEGY).build())
                .addOnSuccessListener { unused: Void ->
                    callbacks.onStartedAdvertisingOrDiscovery()
                }
                .addOnFailureListener { e: Exception ->
                    callbacks.onCouldNotStartedAdvertisingOrDiscovery(e)
                }
        }
    }

    /**
     * discover
     */

    fun startDiscovery() {
        if (endpointDiscoveryCallback == null) {
            callbacks.onCouldNotStartedAdvertisingOrDiscovery()
        } else {
            Nearby.getConnectionsClient(activity).startDiscovery(Utils.SERVICE_ID, (endpointDiscoveryCallback as EndpointDiscoveryCallback), DiscoveryOptions.Builder().setStrategy(Utils.STRATEGY).build())
                .addOnSuccessListener { unused: Void ->
                    callbacks.onStartedAdvertisingOrDiscovery()
                }
                .addOnFailureListener { e: Exception ->
                    callbacks.onCouldNotStartedAdvertisingOrDiscovery(e)
                }
        }
    }

}









/*AlertDialog.Builder(activity)
    .setTitle("Accept connection to " + connectionInfo.endpointName)
    .setMessage("Confirm the code matches on both devices: " + connectionInfo.authenticationToken)
    .setPositiveButton("Accept") { dialog: DialogInterface, which: Int ->

        // The user confirmed, so we can accept the connection.
        Nearby.getConnectionsClient(activity).acceptConnection(endpointId, object: PayloadCallback() {
            override fun onPayloadReceived(p0: String, p1: Payload) {

            }

            override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {

            }

        })

    }
    .setNegativeButton(
        android.R.string.cancel
    ) { dialog: DialogInterface, which: Int ->
        // The user canceled, so we should reject the connection.
        Nearby.getConnectionsClient(activity).rejectConnection(endpointId)
    }
    .setIcon(android.R.drawable.ic_dialog_alert)
    .show()*/