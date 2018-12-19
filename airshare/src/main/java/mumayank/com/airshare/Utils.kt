package mumayank.com.airshare

import com.google.android.gms.nearby.connection.Strategy
import kotlin.random.Random

class Utils {
    companion object {
        val STRATEGY = Strategy.P2P_POINT_TO_POINT
        var SERVICE_ID = BuildConfig.APPLICATION_ID
        val DEVICE_NICK_NAME = Random.nextInt(0, 99).toString()
    }
}