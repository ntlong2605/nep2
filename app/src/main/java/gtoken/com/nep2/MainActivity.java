package gtoken.com.nep2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import gtoken.com.nep2.manager.WalletManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();


    private EditText mUserPassPhrase;
    private Button mCreateWallet, mRestoreWallet;
    private TextView mCreatedJsonView, mRestoreJsonView, mCreatedPrivateKey, mRestorePrivateKey;

    private static final int PICK_FILES_REQUEST_CODE = 1111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUserPassPhrase = findViewById(R.id.user_passphrase);
        mCreateWallet = findViewById(R.id.btn_create_wallet);
        mRestoreWallet = findViewById(R.id.btn_restore_wallet);
        mCreatedJsonView = findViewById(R.id.output_json);
        mRestoreJsonView = findViewById(R.id.output_restore_json);
        mCreatedPrivateKey = findViewById(R.id.create_new_private_key);
        mRestorePrivateKey = findViewById(R.id.restore_private_key);

        mCreateWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String userPassPhrase = mUserPassPhrase.getText().toString();
                if (!TextUtils.isEmpty(userPassPhrase)) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    } else {
                        WalletManager.getInstance().createWallet(mUserPassPhrase.getText().toString(), WalletManager.WalletType.ACM_WALLET);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Passphrase cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mRestoreWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0:
                WalletManager.getInstance().createWallet(mUserPassPhrase.getText().toString(), WalletManager.WalletType.ACM_WALLET);
                break;
        }
    }

    private void openFile() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, PICK_FILES_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PICK_FILES_REQUEST_CODE:

                    String filePath = "", fileName = "";

                    if (data != null && data.getData() != null) {
                        filePath = data.getData().getPath();
                        fileName = data.getData().getLastPathSegment();
                        Log.d(TAG, "path=" + filePath);
                        Log.d(TAG, "name=" + fileName);
                    }

                    if (!TextUtils.isEmpty(filePath)) {
                        try {
                            File jsonWallet = new File(filePath);
                            FileInputStream stream = new FileInputStream(jsonWallet);
                            String jsonStr;
                            try {
                                FileChannel fc = stream.getChannel();
                                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                                jsonStr = Charset.defaultCharset().decode(bb).toString();
                                WalletManager.getInstance().restoreWallet(mUserPassPhrase.getText().toString(), jsonStr, WalletManager.WalletType.ACM_WALLET);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, "Cannot read file", Toast.LENGTH_SHORT).show();
                            } finally {
                                stream.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Cannot read file", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(MainActivity.this, "Cannot read file", Toast.LENGTH_SHORT).show();
                    }

                    break;
            }
        }
    }


}
