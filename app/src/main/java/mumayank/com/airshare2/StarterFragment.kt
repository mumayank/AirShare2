package mumayank.com.airshare2

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import mumayank.com.airshare.Utils
import pl.bclogic.pulsator4droid.library.PulsatorLayout

class StarterFragment: Fragment() {

    private lateinit var transferActivity: TransferActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transferActivity = activity as TransferActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_start, container, false)

        view.findViewById<TextView>(R.id.idTextView).text = Utils.getDeviceNickName()

        view.findViewById<View>(R.id.skip).setOnClickListener { transferActivity.switchToTransferFragment() }

        view.findViewById<PulsatorLayout>(R.id.pulsator).start()

        view.findViewById<View>(R.id.back).setOnClickListener {
            transferActivity.onBackPressed()
        }

        transferActivity.startAdvertising()
        
        return view
    }

}