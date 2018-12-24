package mumayank.com.airshare2

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver

class SelectionFragment: Fragment() {

    private lateinit var transferActivity: TransferActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transferActivity = activity as TransferActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_selection, container, false)
        view.findViewById<View>(R.id.startLayout).setOnClickListener {
            transferActivity.switchToStarterFragment()
        }
        view.findViewById<View>(R.id.joinLayout).setOnClickListener {
            transferActivity.switchToJoinerFragment()
        }

        // set height width programmatically
        view.post {
            val measurement = view.findViewById<View>(R.id.startLayoutParent).measuredHeight

            val layoutParams = view.findViewById<View>(R.id.startLayout).layoutParams
            layoutParams.height = measurement
            layoutParams.width = measurement

            view.findViewById<View>(R.id.startLayout).layoutParams = layoutParams
            view.findViewById<View>(R.id.startLayout).requestLayout()

            view.findViewById<View>(R.id.joinLayout).layoutParams = layoutParams
            view.findViewById<View>(R.id.joinLayout).requestLayout()
        }

        return view
    }

}