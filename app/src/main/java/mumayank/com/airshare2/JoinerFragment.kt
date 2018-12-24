package mumayank.com.airshare2

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.item_joiner.view.*
import mumayank.com.airrecyclerview.AirRecyclerView
import mumayank.com.airshare.AirShare
import mumayank.com.airshare.Utils
import mumayank.com.airshare2.events.OnEndpointFound
import mumayank.com.airshare2.events.OnEndpointLost
import mumayank.com.airshare2.events.OnStarted
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import pl.bclogic.pulsator4droid.library.PulsatorLayout

class JoinerFragment: Fragment() {

    private lateinit var transferActivity: TransferActivity
    data class RvItem(val displayString: String, val endpointId: String)
    private var rvAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null
    private var rvItems = ArrayList<RvItem>()

    private val progressTextViewText1 = "Placing connection request..."
    private val progressTextViewText2 = "Waiting for approval..."
    private val progressTextViewText3 = "Initializing..."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transferActivity = activity as TransferActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_join, container, false)

        view.findViewById<TextView>(R.id.progressTextView).text = progressTextViewText3
        view.findViewById<ViewGroup>(R.id.progressLayout).visibility = View.VISIBLE

        view.findViewById<TextView>(R.id.idTextView).text = Utils.getDeviceNickName()

        view.findViewById<PulsatorLayout>(R.id.pulsator).start()

        view.findViewById<View>(R.id.back).setOnClickListener {
            transferActivity.onBackPressed()
        }

        rvAdapter = AirRecyclerView.initAndGetAdapter(transferActivity, view.findViewById(R.id.rv),
            object: AirRecyclerView.AirRecyclerViewCallbacks {
                override fun getBindView(viewHolder: RecyclerView.ViewHolder, viewType: Int, position: Int) {

                    val customViewHolder = viewHolder as CustomViewHolder
                    val rvItem = rvItems[position]

                    customViewHolder.textView.text = rvItem.displayString

                    customViewHolder.parentLayout.setOnClickListener {

                        if (transferActivity.connectedEndpoints.contains(rvItem.endpointId)) {
                            Toast.makeText(transferActivity, "Already connected", Toast.LENGTH_SHORT).show()
                            transferActivity.switchToTransferFragment()
                        } else {
                            view.findViewById<TextView>(R.id.progressTextView).text = progressTextViewText1
                            view.findViewById<ViewGroup>(R.id.progressLayout).visibility = View.VISIBLE

                            transferActivity.airShare?.requestConnectionToEndpoint(rvItem.endpointId, object: AirShare.RequestConnectionToEndpointCallback {

                                override fun onSuccessfullyRequestedConnectionToEndpoint() {
                                    view.findViewById<TextView>(R.id.progressTextView).text = progressTextViewText2
                                    //Toast.makeText(transferActivity, "Connection request is successfully placed.\nWaiting for approval...", Toast.LENGTH_SHORT).show()
                                }

                                override fun onCouldNotRequestConnectionToEndpoint(e: Exception) {
                                    view.findViewById<ViewGroup>(R.id.progressLayout).visibility = View.GONE
                                    Toast.makeText(transferActivity, "Could not place a request to connect successfully: ${e.message}", Toast.LENGTH_LONG).show()
                                }

                            })
                        }

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

        transferActivity.startDiscovering()

        return view
    }

    class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val parentLayout: LinearLayout = view.parentLayout
        val textView: TextView = view.textView
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
    fun OnEndpointFound(onEndpointFound: OnEndpointFound) {
        rvItems.add(RvItem(onEndpointFound.info.endpointName, onEndpointFound.endpointId))
        rvAdapter?.notifyItemInserted(rvItems.size - 1)
    }

    @Subscribe
    fun OnEndpointLost(onEndpointLost: OnEndpointLost) {
        var index = -1
        for (rvItem in rvItems) {
            if (onEndpointLost.endpointId == rvItem.endpointId) {
                index = rvItems.indexOf(rvItem)
            }
        }
        rvItems.remove(rvItems[index])
        rvAdapter?.notifyItemRemoved(index)
    }

    @Subscribe
    fun OnConnectionAcceptedOrRejected() {
        view?.findViewById<ViewGroup>(R.id.progressLayout)?.visibility = View.GONE
    }

    @Subscribe
    fun OnStarted(onStarted: OnStarted) {
        view?.findViewById<ViewGroup>(R.id.progressLayout)?.visibility = View.GONE
    }


}