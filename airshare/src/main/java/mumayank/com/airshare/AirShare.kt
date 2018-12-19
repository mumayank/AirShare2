package mumayank.com.airshare

import android.app.Activity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import mumayank.com.airpermissions.AirPermissions

class AirShare constructor(
    private val activity: Activity,
    private val commonCallback: CommonCallback,
    private val joinerCallback: JoinerCallback? = null
) {

    private var connectionLifecycleCallback: ConnectionLifecycleCallback? = null
    private var payloadCallback: PayloadCallback? = null
    private var endpointDiscoveryCallback: EndpointDiscoveryCallback? = null
    private var airPermissions: AirPermissions? = null

    constructor(
        activity: Activity,
        commonCallback: CommonCallback
    ): this(activity, commonCallback, null)

    init {
        defineVars()
    }

    interface CommonCallback {
        fun onPermissionsDenied()
        fun onStartedAdvertisingOrDiscovery()
        fun onCouldNotStartAdvertisingOrDiscovery(e: Exception? = null)
        fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo, connectionInitiatedCallback: ConnectionInitiatedCallback)
        fun onConnectionRejected(connectionError: ConnectionError)
        fun onConnected(endpointId: String)
        fun onDisconnected(endpointId: String)
        fun onPayloadReceived(endpointId: String, payload: Payload)
        fun onPayloadTransferUpdate(endpointId: String, payloadTransferUpdate: PayloadTransferUpdate)
    }

    interface JoinerCallback {
        fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo)
        fun onEndpointLost(endpointId: String)
    }

    interface ConnectionInitiatedCallback {
        fun onAcceptConnection()
        fun onRejectConnection()
    }

    enum class ConnectionError {
        ConnectionRejectedByOneOrBothSides,
        ConnectionBrokeBeforeItCouldBeAccepted,
        ConnectionErrorWithUnknownStatusCode
    }

    interface RequestConnectionToEndpointCallback {
        fun onSuccessfullyRequestedConnectionToEndpoint()
        fun onCouldNotRequestConnectionToEndpoint(e: Exception)
    }

    interface PermissionCallback {
        fun onGrantedPermissions()
        fun onDeniedPermissions()
    }

    private fun defineVars() {

        /**
         * define payload callback
         */
        payloadCallback = object: PayloadCallback() {

            override fun onPayloadReceived(p0: String, p1: Payload) {
                commonCallback.onPayloadReceived(p0, p1)
            }

            override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
                commonCallback.onPayloadTransferUpdate(p0, p1)
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
                        commonCallback.onConnected(endpointId)

                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED ->
                        // The connection was rejected by one or both sides.
                        commonCallback.onConnectionRejected(ConnectionError.ConnectionRejectedByOneOrBothSides)

                    ConnectionsStatusCodes.STATUS_ERROR ->
                        // The connection broke before it was able to be accepted.
                        commonCallback.onConnectionRejected(ConnectionError.ConnectionBrokeBeforeItCouldBeAccepted)

                    else ->
                        // Unknown status code
                        commonCallback.onConnectionRejected(ConnectionError.ConnectionErrorWithUnknownStatusCode)

                }

            }

            override fun onDisconnected(endpointId: String) {
                // We've been disconnected from this endpoint. No more data can be sent or received.
                commonCallback.onDisconnected(endpointId)
            }

            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {

                commonCallback.onConnectionInitiated(endpointId, connectionInfo, object: ConnectionInitiatedCallback {

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

    fun requestConnectionToEndpoint(endpointId: String, requestConnectionToEndpointCallback: RequestConnectionToEndpointCallback) {
        Nearby.getConnectionsClient(activity).requestConnection(Utils.getDeviceNickName(), endpointId, (connectionLifecycleCallback as ConnectionLifecycleCallback))
            .addOnSuccessListener { unused: Void? ->
                requestConnectionToEndpointCallback.onSuccessfullyRequestedConnectionToEndpoint()

            }
            .addOnFailureListener { e: Exception ->
                requestConnectionToEndpointCallback.onCouldNotRequestConnectionToEndpoint(e)
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

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        airPermissions?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun requestPermission(permissionCallbacks: PermissionCallback) {
        airPermissions = AirPermissions(
            activity,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            object: AirPermissions.Callbacks {
                override fun onFailure() {
                    permissionCallbacks.onDeniedPermissions()
                }

                override fun onSuccess() {
                    permissionCallbacks.onGrantedPermissions()
                }

            }
        )
    }

    /**
     * advertising
     */

    fun startAdvertising() {

        requestPermission(object: PermissionCallback {
            override fun onGrantedPermissions() {

                if (connectionLifecycleCallback == null) {
                    commonCallback.onCouldNotStartAdvertisingOrDiscovery()
                } else {
                    Nearby.getConnectionsClient(activity).startAdvertising(Utils.getDeviceNickName(), Utils.getServiceId(activity), (connectionLifecycleCallback as ConnectionLifecycleCallback), AdvertisingOptions.Builder().setStrategy(Utils.STRATEGY).build())
                        .addOnSuccessListener { unused: Void? ->
                            commonCallback.onStartedAdvertisingOrDiscovery()
                        }
                        .addOnFailureListener { e: Exception ->
                            commonCallback.onCouldNotStartAdvertisingOrDiscovery(e)
                        }
                }

            }

            override fun onDeniedPermissions() {
                commonCallback.onPermissionsDenied()
            }

        })

    }

    /**
     * discover
     */

    fun startDiscovery() {

        requestPermission(object: PermissionCallback {
            override fun onGrantedPermissions() {

                if (endpointDiscoveryCallback == null) {
                    commonCallback.onCouldNotStartAdvertisingOrDiscovery()
                } else {
                    Nearby.getConnectionsClient(activity).startDiscovery(Utils.getServiceId(activity), (endpointDiscoveryCallback as EndpointDiscoveryCallback), DiscoveryOptions.Builder().setStrategy(Utils.STRATEGY).build())
                        .addOnSuccessListener { unused: Void? ->
                            commonCallback.onStartedAdvertisingOrDiscovery()
                        }
                        .addOnFailureListener { e: Exception ->
                            commonCallback.onCouldNotStartAdvertisingOrDiscovery(e)
                        }
                }

            }

            override fun onDeniedPermissions() {
                commonCallback.onPermissionsDenied()
            }

        })

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