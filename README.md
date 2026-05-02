####M5Stick - Enviando Audio por Bluetooth para o Android

Cria uma template androd Empty Activity

Adicione as permissões ao manifesto

```
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
```
Imports

```Java
import android.bluetooth.BluetoothAdapter;  
import android.bluetooth.BluetoothDevice;  
import android.bluetooth.BluetoothSocket;  
import android.media.AudioFormat;  
import android.media.AudioManager;  
import android.media.AudioTrack;  
import android.util.Log;  
  
import java.io.InputStream;  
import java.util.ArrayList;  
import java.util.List;  
import java.util.Set;  
import java.util.UUID;  
import androidx.annotation.NonNull;
```

Variáveis de escopo de classe

```Java
private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9834FB");  
private AudioTrack audioTrack;  
private BluetoothSocket socket;  
private boolean isRunning = true;  
private static final int PERMISSIONS_REQUEST_CODE = 100;  
private TextView txtViewStatus;
```
Habilita o Audio para receber os bytes

```Java
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
```

Método para trazer o UID do dispositivo Bluetooth

```Java
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
```

Recebe os bytes vindo do Bluetooth

```Java
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
  
                Thread.sleep(1000);  
            }  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }, "JarvisStreamThread").start();  
    
}
```

Método de entrada

```Java
private void start(){
	initAudio();
	connectAndStream();
}
```

O método para permissionamento deve ser o primeiro a ser chamado no OnCreate

```JAVA
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
```

NO OnRequest Permission

```JAVA
  @Override
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
```

Altere o método OnCreate da Activity
```JAVA
@Override  
protected void onCreate(Bundle savedInstanceState) {  
    super.onCreate(savedInstanceState);  
    EdgeToEdge.enable(this);  
    setContentView(R.layout.activity_main);  
    txtViewStatus = findViewById(R.id.txtViewStatus);  
    checkPermissions();  
}
```

O arquivo de layout activity_main.xml

```XML
<?xml version="1.0" encoding="utf-8"?>  
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"  
    xmlns:app="http://schemas.android.com/apk/res-auto"  
    xmlns:tools="http://schemas.android.com/tools"  
    android:id="@+id/main"  
    android:layout_width="match_parent"  
    android:layout_height="match_parent"  
    tools:context=".MainActivity">  
  
    <TextView        android:id="@+id/txtViewStatus"  
        android:layout_width="wrap_content"  
        android:layout_height="wrap_content"  
        android:text="Hello World!"  
        app:layout_constraintBottom_toBottomOf="parent"  
        app:layout_constraintEnd_toEndOf="parent"  
        app:layout_constraintStart_toStartOf="parent"  
        app:layout_constraintTop_toTopOf="parent" />  
  
</androidx.constraintlayout.widget.ConstraintLayout>
```

Arduino IDE

Adicionar a seguinte URL

	https://static-cdn.m5stack.com/resource/arduino/package_m5stack_index.json

Instalar o Board M5Stack

Instalar o M5Unifified

Código
```C++
#include <M5Unified.h>
#include <BluetoothSerial.h>

BluetoothSerial SerialBT;
uint8_t audioBuffer[30000]; // Buffer reduzido para segurança de RAM
int bufferPointer = 0;

void setup() {
  auto cfg = M5.config();
  M5.begin(cfg);

    auto mic_cfg = M5.Mic.config();
    mic_cfg.sample_rate = 16000;
    M5.Mic.config(mic_cfg);
    M5.Mic.begin();

  SerialBT.begin("M5-Jarvis-Unified");
  M5.Display.println("Hardware OK! Segure A para gravar.");
}

void loop() {
  M5.update();

  if (M5.BtnA.isPressed()) {
    // Captura o áudio usando a M5Unified
    if (M5.Mic.isEnabled() && bufferPointer < sizeof(audioBuffer) - 128) {
      // Lê o áudio diretamente do gravador interno da biblioteca
      if (M5.Mic.record(audioBuffer + bufferPointer, 128, 16000)) {
        bufferPointer += 128; // 256 samples * 2 bytes (16bit)
      }
    }
  }

  if (M5.BtnA.wasReleased()) {
    M5.Display.fillScreen(BLUE);
    M5.Display.setCursor(0, 0);
    M5.Display.print("Enviando...");
    
    if (SerialBT.connected()) {
      SerialBT.write((uint8_t*)&bufferPointer, 4); // Tamanho
      SerialBT.write(audioBuffer, bufferPointer); // Dados
    }
    
    bufferPointer = 0;
    M5.Display.fillScreen(BLACK);
    M5.Display.setCursor(0, 0);
    M5.Display.print("Pronto!");
  }
}
```
