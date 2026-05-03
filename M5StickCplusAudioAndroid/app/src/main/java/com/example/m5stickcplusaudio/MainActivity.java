package com.example.m5stickcplusaudio;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9834FB");
    private AudioTrack audioTrack;
    private BluetoothSocket socket;
    private boolean isRunning = true;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private TextView txtViewStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        txtViewStatus = findViewById(R.id.txtViewStatus);
        checkPermissions();
    }

    private void initAudio() {
        int sampleRate = 16000;

        int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;

        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM);
        audioTrack.play();
    }

    private String getMacByName(String targetName) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        // CORREÇÃO: Verificamos se a permissão FOI concedida (== PERMISSION_GRANTED)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

            Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();

            if (pairedDevices != null && pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    // Log para debug no Logcat (ajuda muito no seu vídeo!)
                    Log.d("Jarvis", "Dispositivo encontrado: " + device.getName());

                    if (device.getName() != null && device.getName().equalsIgnoreCase(targetName)) {
                        return device.getAddress();
                    }
                }
            }
        } else {
            Log.e("Jarvis", "Sem permissão BLUETOOTH_CONNECT para listar dispositivos.");
        }

        return null;
    }

    private void connectAndStream() {

        if ( checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Jarvis", "Permissão de Bluetooth ainda não concedida!");
            return;
        }

        String deviceAddress = getMacByName("M5-Jarvis-Unified"); // Nome que você colocou no BluetoothSerial.begin("...")

        if (deviceAddress == null) {
            Log.e("Jarvis", "M5Stick não encontrado nos dispositivos pareados!");
            return;
        }

        new Thread(() -> {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = adapter.getRemoteDevice(deviceAddress);

            try {
                socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device, 1);
                socket.connect();

                InputStream is = socket.getInputStream();
                byte[] buffer = new byte[30000];

                while (isRunning) {

                    runOnUiThread(() -> {
                        txtViewStatus.setText("Aguardando mensagem...");
                    });

                    int bytesRead = is.read(buffer);

                    if (bytesRead > 0) {
                        audioTrack.write(buffer, 0, bytesRead);
                    }

                    runOnUiThread(() -> {
                        txtViewStatus.setText("Mensagem recebida");
                    });

                    Thread.sleep(500);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "JarvisStreamThread").start();

    }

    private void start(){
        initAudio();
        connectAndStream();
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Bluetooth (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
        }

        List<String> listPermissionsList = new ArrayList<>();
        for (String perm : permissionsNeeded) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsList.add(perm);
            }
        }

        if (!listPermissionsList.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsList.toArray(new String[0]), 200);
        } else {
            start(); // Chame sua função que inicia o Bluetooth aqui
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // Sucesso! O usuário liberou tudo.
                start();
            } else {
                // Se ele negou algo vital (como o áudio), o Jarvis não funciona.
                Log.e("Jarvis", "Permissões negadas. Encerrando...");
                finish();
            }
        }
    }


}