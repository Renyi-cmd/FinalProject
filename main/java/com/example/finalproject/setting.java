package com.example.finalproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.example.finalproject.MainActivity.*;

public class setting extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Button saveBtn = findViewById(R.id.saveBtn);
        EditText ID = findViewById(R.id.idNum);
        EditText validTime = findViewById(R.id.validTime);

        String savedData;
        checkNeedPermissions();
        if(!(savedData = readFile()).equals("")) {
            String[] datas = savedData.split("\n");
            ID.setText(datas[1]);
            validTime.setText(datas[2]);
        }

        saveBtn.setOnClickListener(view -> {
            String resStr = ID.getText().toString() + "\n" + validTime.getText().toString() + "\n";
            checkNeedPermissions();
            try {
                OutputStreamWriter osw = new OutputStreamWriter(setting.this.openFileOutput("RenyiNuclearAcid.txt", Context.MODE_PRIVATE));
                osw.write(resStr);
                osw.close();
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("WriteError", e.toString());
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String readFile() {
        Context context = setting.this;
        String ret = "";
        try {
            InputStream is = context.openFileInput("RenyiNuclearAcid.txt");
            if(is != null) {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String recvStr;
                StringBuilder strBuilder = new StringBuilder();
                while ((recvStr = br.readLine()) != null) {
                    strBuilder.append("\n").append(recvStr);
                }
                is.close();
                ret = strBuilder.toString();
            }
        } catch (Exception e) {
            Log.e("ReadError", e.toString());
        }
        return ret;
    }

    private void checkNeedPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        }
    }
}