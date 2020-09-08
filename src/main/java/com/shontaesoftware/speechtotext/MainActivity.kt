package com.shontaesoftware.speechtotext


import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.*


class MainActivity : AppCompatActivity() {
    private var mVoiceInputTv: TextView? = null
    private var mSpeakBtn: ImageButton? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mVoiceInputTv = findViewById<View>(R.id.voiceInput) as TextView
        mSpeakBtn = findViewById<View>(R.id.btnSpeak) as ImageButton
        mSpeakBtn!!.setOnClickListener { startVoiceInput() }
    }

    private fun startVoiceInput() {
        /*
            SMS PERMISSIONS
         */
        checkPermission(SEND_SMS, 100);

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?")
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (a: ActivityNotFoundException) {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_CODE_SPEECH_INPUT -> {
                if (resultCode == Activity.RESULT_OK && null != data) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val message = result!![0]
                    mVoiceInputTv!!.text = message
                    WITRequest(message)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun WITRequest(message: String){
        val textView = findViewById<TextView>(R.id.hint)

        val queue = Volley.newRequestQueue(this)
        val url = "https://api.wit.ai/message?q=${message}"

        val accessTokenRequest: JsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET, url, JSONObject(),
            Response.Listener<JSONObject?> {
                val response = it!!.getJSONArray("intents").getJSONObject(0).getString("name")
                textView.text = "Message: ${response}"
                sendMessage(response)
            }, Response.ErrorListener {
                textView.text = "That did not work :("
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["Authorization"] = "Bearer ${BuildConfig.WIT_API_TOKEN}"
                //..add other headers
                return params
            }
        }

        // Add the request to the RequestQueue.
        queue.add(accessTokenRequest)
    }

    private fun sendMessage(message: String){
        val tMgr: TelephonyManager = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        checkPermission(READ_PHONE_STATE, 110)
        checkPermission(READ_SMS, 110)
        checkPermission(READ_PHONE_NUMBERS, 110)
        val mobile: String = tMgr.getLine1Number()
        try {
            val smgr = SmsManager.getDefault()
            smgr.sendTextMessage(
                mobile.toString(),
                null,
                message,
                null,
                null
            )
            Toast.makeText(this@MainActivity, "SMS Sent Successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            println(e)
            Toast.makeText(
                this@MainActivity,
                "SMS Failed to Send, Please try again",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // Function to check and request permission.
    fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this@MainActivity, permission)
            == PackageManager.PERMISSION_DENIED
        ) {

            // Requesting the permission
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf(permission),
                requestCode
            )
        }
    }


    companion object {
        private const val REQ_CODE_SPEECH_INPUT = 100
    }
}