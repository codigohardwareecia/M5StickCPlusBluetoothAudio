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
    //mic_cfg.magnification = 32; // <--- Use magnification para aumentar o volume (ganho)
    //mic_cfg.noise_filter_level = 32; // Ajuda a limpar o chiado do Bluetooth
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