package gtoken.com.nep2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset

import gtoken.com.nep2.manager.WalletManager

class MainActivity : AppCompatActivity() {


    private var mUserPassPhrase: EditText? = null
    private var mCreateWallet: Button? = null
    private var mRestoreWallet: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mUserPassPhrase = findViewById(R.id.user_passphrase)
        mCreateWallet = findViewById(R.id.btn_create_wallet)
        mRestoreWallet = findViewById(R.id.btn_restore_wallet)

        mCreateWallet!!.setOnClickListener {
            val userPassPhrase = mUserPassPhrase!!.text.toString()
            if (!TextUtils.isEmpty(userPassPhrase)) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                } else {
                    WalletManager.instance.createWallet(mUserPassPhrase!!.text.toString(), WalletManager.WalletType.ACM_WALLET)
                }
            } else {
                Toast.makeText(this@MainActivity, "Passphrase cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        mRestoreWallet!!.setOnClickListener { openFile() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            0 -> WalletManager.instance.createWallet(mUserPassPhrase!!.text.toString(), WalletManager.WalletType.ACM_WALLET)
        }
    }

    private fun openFile() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "*/*"
        startActivityForResult(i, PICK_FILES_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_FILES_REQUEST_CODE -> {

                    var filePath = ""
                    var fileName = ""

                    if (data != null && data.data != null) {
                        filePath = data.data!!.path
                        fileName = data.data!!.lastPathSegment
                        Log.d(TAG, "path=$filePath")
                        Log.d(TAG, "name=$fileName")
                    }

                    if (!TextUtils.isEmpty(filePath)) {
                        try {
                            val jsonWallet = File(filePath)
                            val stream = FileInputStream(jsonWallet)
                            val jsonStr: String
                            try {
                                val fc = stream.channel
                                val bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
                                jsonStr = Charset.defaultCharset().decode(bb).toString()
                                WalletManager.instance.restoreWallet(mUserPassPhrase!!.text.toString(), jsonStr, WalletManager.WalletType.ACM_WALLET)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(this@MainActivity, "Cannot read file", Toast.LENGTH_SHORT).show()
                            } finally {
                                stream.close()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this@MainActivity, "Cannot read file", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Toast.makeText(this@MainActivity, "Cannot read file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    companion object {

        private val TAG = MainActivity::class.java.simpleName

        private const val PICK_FILES_REQUEST_CODE = 1111
    }


}
