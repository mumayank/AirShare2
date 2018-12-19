package mumayank.com.airshare

import android.app.Activity
import com.google.android.gms.nearby.connection.Strategy
import kotlin.random.Random

class Utils {
    companion object {
        val STRATEGY: Strategy = Strategy.P2P_POINT_TO_POINT

        fun getServiceId(activity: Activity) = activity.application.packageName

        private var deviceNickName = ""
        fun getDeviceNickName(): String {
            if (deviceNickName == "") {
                deviceNickName = Random.nextInt(0, 99).toString()
            }
            return deviceNickName
        }
    }
}