package mumayank.com.airshare2.events

import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo

class OnEndpointFound(val endpointId: String, val info: DiscoveredEndpointInfo)