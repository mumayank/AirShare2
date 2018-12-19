package mumayank.com.airshare2

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import mumayank.com.airpermissions.AirPermissions


class MainActivity : AppCompatActivity() {

    private var airPermissions: AirPermissions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        airPermissions = AirPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), object: AirPermissions.Callbacks {
            override fun onSuccess() {
                proceed()
            }

            override fun onFailure() {
                finish()
            }
        })
    }

    private fun proceed() {
        startLayout.setOnClickListener {
            startActivity(Intent(this, StarterActivity::class.java))
        }

        joinLayout.setOnClickListener {
            startActivity(Intent(this, JoinerActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airPermissions?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}
