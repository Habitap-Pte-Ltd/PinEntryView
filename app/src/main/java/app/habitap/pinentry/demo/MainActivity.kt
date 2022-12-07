package app.habitap.pinentry.demo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.habitap.pinentry.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            pinEntryOtp1.setOnPinEnteredListener {
                Toast.makeText(
                    this@MainActivity,
                    "PIN: $it",
                    Toast.LENGTH_LONG
                ).show()
            }
            pinEntryOtp2.setOnPinEnteredListener {
                Toast.makeText(
                    this@MainActivity,
                    "PIN: $it",
                    Toast.LENGTH_LONG
                ).show()
            }
            pinEntryOtp3.setOnPinEnteredListener {
                Toast.makeText(
                    this@MainActivity,
                    "PIN: $it",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
