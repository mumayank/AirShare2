package mumayank.com.airshare2

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import android.content.DialogInterface
import android.support.v7.app.AlertDialog


class JoinerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_joiner)
        startDiscovery()
    }

    private fun startDiscovery() {

    }

    override fun onDestroy() {
        super.onDestroy()
        stopDiscovery()
    }

    private fun stopDiscovery() {
        Nearby.getConnectionsClient(this).stopDiscovery()
    }
}
