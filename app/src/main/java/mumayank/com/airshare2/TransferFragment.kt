package mumayank.com.airshare2

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.NotificationCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.Payload
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.lang.Exception
import java.nio.charset.StandardCharsets




class TransferFragment: Fragment() {

    private lateinit var transferActivity: TransferActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transferActivity = activity as TransferActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_transfer, container, false)

        view.findViewById<Button>(R.id.button).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            startActivityForResult(intent, transferActivity.READ_REQUEST_CODE)
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == transferActivity.READ_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            view?.findViewById<ViewGroup>(R.id.progress)?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.button)?.visibility = View.GONE

            doAsync {
                val endpointId = transferActivity.currentlySelectedEndpoint
                val uri = data.data
                val filePayload: Payload
                try {
                    val pfd = transferActivity.contentResolver.openFileDescriptor(uri, "r")
                    filePayload = Payload.fromFile(pfd)
                } catch (e: Exception) {
                    uiThread {
                        Toast.makeText(transferActivity, "Could not get file. Please try again.", Toast.LENGTH_SHORT).show()
                        view?.findViewById<ViewGroup>(R.id.progress)?.visibility = View.GONE
                        view?.findViewById<View>(R.id.button)?.visibility = View.VISIBLE
                    }
                    return@doAsync
                }
                TransferActivity.extractFileProperties(transferActivity, uri, object: TransferActivity.Callbacks {
                    override fun onSuccess(fileDisplayName: String, fileSizeInMB: Long) {

                        uiThread {
                            val fileNameMessage = "${filePayload.id}:$fileDisplayName"
                            val fileNameBytesPayload = Payload.fromBytes(fileNameMessage.toByteArray(StandardCharsets.UTF_8))
                            Nearby.getConnectionsClient(transferActivity).sendPayload(endpointId, fileNameBytesPayload)
                            Nearby.getConnectionsClient(transferActivity).sendPayload(endpointId, filePayload)

                            startNotification(filePayload)
                            view?.findViewById<ViewGroup>(R.id.progress)?.visibility = View.GONE
                            view?.findViewById<View>(R.id.button)?.visibility = View.VISIBLE
                        }
                    }

                    override fun onOperationFailed() {
                        uiThread {
                            Toast.makeText(transferActivity, "Could not get file. Please try again.", Toast.LENGTH_SHORT).show()
                            view?.findViewById<ViewGroup>(R.id.progress)?.visibility = View.GONE
                            view?.findViewById<View>(R.id.button)?.visibility = View.VISIBLE
                        }
                    }

                })
            }

        }
    }

    fun startNotification(payload: Payload) {
        if (payload.type == Payload.Type.BYTES) {
            // return
        }

        val notification = transferActivity.buildNotification(payload, false)
        transferActivity.notificationManager.notify(payload.id.toInt(), notification.build())
        transferActivity.outgoingPayloads.put(payload.id, notification)
    }

}