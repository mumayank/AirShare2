package mumayank.com.airshare2

import android.app.Activity.RESULT_OK
import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.NotificationCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.Payload
import kotlinx.android.synthetic.main.item_transfer.*
import kotlinx.android.synthetic.main.item_transfer.view.*
import mumayank.com.airrecyclerview.AirRecyclerView
import mumayank.com.airshare2.events.OnStarted
import mumayank.com.airshare2.events.OnTransferUpdate
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.uiThread
import java.lang.Exception
import java.nio.charset.StandardCharsets




class TransferFragment: Fragment() {

    private lateinit var transferActivity: TransferActivity

    data class RvItem(val fileName: String, var progress: Int, val isDownloading: Boolean)

    private var rvAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transferActivity = activity as TransferActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_transfer, container, false)

        view.findViewById<View>(R.id.upload).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            startActivityForResult(intent, transferActivity.READ_REQUEST_CODE)
        }

        rvAdapter = AirRecyclerView.initAndGetAdapter(transferActivity, view.findViewById(R.id.rv), object: AirRecyclerView.AirRecyclerViewCallbacks {
            override fun getBindView(viewHolder: RecyclerView.ViewHolder, viewType: Int, position: Int) {
                val customViewHolder = viewHolder as CustomViewHolder
                val transferItem = transferActivity.transferItems[position]

                customViewHolder.name.text = transferItem.fileName
                customViewHolder.cardView.setCardBackgroundColor(transferActivity.resources.getColor(R.color.black))

                if (transferItem.isDownloading) {
                    customViewHolder.downloading.visibility = View.VISIBLE
                    customViewHolder.uploading.visibility = View.GONE
                } else {
                    customViewHolder.downloading.visibility = View.GONE
                    customViewHolder.uploading.visibility = View.VISIBLE
                }

                if (transferItem.isError) {
                    customViewHolder.progressBar.visibility = View.GONE
                    customViewHolder.open.visibility = View.GONE
                    customViewHolder.error.visibility = View.VISIBLE
                    customViewHolder.done.visibility = View.GONE
                } else {
                    customViewHolder.error.visibility = View.GONE
                    if (transferItem.progress == 100) {
                        customViewHolder.cardView.setCardBackgroundColor(transferActivity.resources.getColor(R.color.colorAccent))
                        customViewHolder.progressBar.visibility = View.GONE
                        if (transferItem.isDownloading) {
                            customViewHolder.open.visibility = View.VISIBLE
                            customViewHolder.open.setOnClickListener {
                                transferActivity.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                            }
                        } else {
                            customViewHolder.open.visibility = View.GONE
                        }
                        customViewHolder.done.visibility = View.VISIBLE
                        customViewHolder.downloading.visibility = View.GONE
                        customViewHolder.uploading.visibility = View.GONE
                    } else {
                        customViewHolder.progressBar.visibility = View.VISIBLE
                        customViewHolder.progressBar.progress = transferItem.progress.toInt()
                        customViewHolder.open.visibility = View.GONE
                        customViewHolder.done.visibility = View.GONE
                    }
                }
            }

            override fun getSize(): Int {
                return transferActivity.transferItems.size
            }

            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder {
                return CustomViewHolder(view)
            }

            override fun getViewLayout(viewType: Int): Int {
                return R.layout.item_transfer
            }

            override fun getViewType(position: Int): Int {
                return 0
            }

        })

        return view
    }

    class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.name
        val open: View = view.open
        val uploading: View = view.uploading
        val downloading: View = view.downloading
        val progressBar: ProgressBar = view.progressBar
        val error: View = view.error
        val done: View = view.done
        val cardView: CardView = view.cardView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == transferActivity.READ_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            view?.findViewById<View>(R.id.progress)?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.upload)?.visibility = View.INVISIBLE

            doAsync {
                val endpointId = transferActivity.endpointIdCurrentlySelected
                val uri = data.data
                val filePayload: Payload
                try {
                    val pfd = transferActivity.contentResolver.openFileDescriptor(uri, "r")
                    filePayload = Payload.fromFile(pfd)
                } catch (e: Exception) {
                    uiThread {
                        Toast.makeText(transferActivity, "Could not get file. Please try again.", Toast.LENGTH_SHORT).show()
                        view?.findViewById<View>(R.id.progress)?.visibility = View.GONE
                        view?.findViewById<View>(R.id.upload)?.visibility = View.VISIBLE
                    }
                    return@doAsync
                }
                TransferActivity.extractFileProperties(transferActivity, uri, object: TransferActivity.Callbacks {
                    override fun onSuccess(fileDisplayName: String, fileSize: Long) {

                        uiThread {
                            val fileNameMessage = "${filePayload.id}:$fileDisplayName"
                            val fileNameBytesPayload = Payload.fromBytes(fileNameMessage.toByteArray(StandardCharsets.UTF_8))
                            Nearby.getConnectionsClient(transferActivity).sendPayload(endpointId, fileNameBytesPayload)
                            Nearby.getConnectionsClient(transferActivity).sendPayload(endpointId, filePayload)

                            startNotification(fileDisplayName, filePayload)
                            view?.findViewById<View>(R.id.progress)?.visibility = View.GONE
                            view?.findViewById<View>(R.id.upload)?.visibility = View.VISIBLE
                        }
                    }

                    override fun onOperationFailed() {
                        uiThread {
                            Toast.makeText(transferActivity, "Could not get file. Please try again.", Toast.LENGTH_SHORT).show()
                            view?.findViewById<View>(R.id.progress)?.visibility = View.GONE
                            view?.findViewById<View>(R.id.upload)?.visibility = View.VISIBLE
                        }
                    }

                })
            }

        }
    }

    fun startNotification(fileDisplayName: String, payload: Payload) {
        if (payload.type == Payload.Type.BYTES) {
            // return
        }

        transferActivity.transferItems.add(TransferActivity.TransferItem(payload.id, fileDisplayName, 0, false))
        rvAdapter?.notifyDataSetChanged()

        val notification = transferActivity.buildNotification(payload, false)
        //transferActivity.notificationManager.notify(payload.id.toInt(), notification.build())
        transferActivity.outgoingPayloads.put(payload.id, notification)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun OnTransferUpdate(onTransferUpdate: OnTransferUpdate) {
        rvAdapter?.notifyDataSetChanged()
    }

}