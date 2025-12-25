package src;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.function.Consumer;

public class VoiceManager {
    // 采样率 8000Hz, 16bit, 单声道 (适合语音传输，带宽占用小)
    private static final AudioFormat FORMAT = new AudioFormat(8000.0f, 16, 1, true, true);
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private boolean isRecording = false;

    public VoiceManager() {
        try {
            // 初始化扬声器
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
            speakers = (SourceDataLine) AudioSystem.getLine(info);
            speakers.open(FORMAT);
            speakers.start();
        } catch (Exception e) {
            System.err.println("音频设备初始化失败: " + e.getMessage());
        }
    }

    // 开始录音，并使用回调函数发送数据
    public void startRecording(Consumer<String> sender) {
        if (isRecording) return;
        isRecording = true;

        new Thread(() -> {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(FORMAT);
                microphone.start();

                byte[] buffer = new byte[1024]; // 缓冲区
                while (isRecording) {
                    int count = microphone.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        byte[] data = new byte[count];
                        System.arraycopy(buffer, 0, data, 0, count);
                        // 转 Base64 字符串
                        String base64 = Base64.getEncoder().encodeToString(data);
                        sender.accept("VOICE:" + base64);
                    }
                }
                microphone.stop();
                microphone.close();
            } catch (Exception e) {
                e.printStackTrace();
                isRecording = false;
            }
        }).start();
    }

    public void stopRecording() {
        isRecording = false;
    }

    // 播放接收到的音频数据
    public void playAudio(String base64Data) {
        try {
            if (speakers != null) {
                byte[] data = Base64.getDecoder().decode(base64Data);
                speakers.write(data, 0, data.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}