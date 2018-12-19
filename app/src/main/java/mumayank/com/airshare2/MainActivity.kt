package mumayank.com.airshare2

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startLayout.setOnClickListener {
            startActivity(Intent(this, StarterActivity::class.java))
        }

        joinLayout.setOnClickListener {
            startActivity(Intent(this, JoinerActivity::class.java))
        }
    }

}
