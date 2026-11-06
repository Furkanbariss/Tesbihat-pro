import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * res/raw altındaki sesleri üretir. Proje kökünden çalıştır:
 * java tools/GenerateSounds.java
 */
public class GenerateSounds {
    private static final int SAMPLE_RATE = 44100;

    public static void main(String[] args) throws IOException {
        Path out = Path.of("app/src/main/res/raw");
        Files.createDirectories(out);
        writeWav(out.resolve("tesbih_tick.wav"), tick());
        writeWav(out.resolve("tesbih_step.wav"), step());
    }

    /** Boncuk tıkı: kısa gürültü patlaması ve hızla sönen iki gövde tonu. */
    private static double[] tick() {
        double[] samples = new double[SAMPLE_RATE * 35 / 1000];
        Random random = new Random(33);
        double filtered = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            filtered += 0.35 * (random.nextDouble() * 2 - 1 - filtered);
            double click = filtered * Math.exp(-t / 0.004);
            double body = Math.sin(2 * Math.PI * 1900 * t) * Math.exp(-t / 0.007)
                    + 0.6 * Math.sin(2 * Math.PI * 820 * t) * Math.exp(-t / 0.012);
            samples[i] = 0.5 * click + 0.5 * body;
        }
        return finish(samples, 0.55);
    }

    /** Adım tamamlandı: yükselen iki yumuşak nota. */
    private static double[] step() {
        double[] samples = new double[SAMPLE_RATE * 320 / 1000];
        addNote(samples, 0.000, 880);
        addNote(samples, 0.110, 1320);
        return finish(samples, 0.6);
    }

    private static void addNote(double[] samples, double startSeconds, double frequency) {
        int start = (int) (startSeconds * SAMPLE_RATE);
        for (int i = start; i < samples.length; i++) {
            double t = (double) (i - start) / SAMPLE_RATE;
            double attack = Math.min(1, t / 0.004);
            double decay = Math.exp(-t / 0.07);
            samples[i] += attack * decay * (Math.sin(2 * Math.PI * frequency * t)
                    + 0.25 * Math.sin(2 * Math.PI * frequency * 2 * t));
        }
    }

    /** Tepe değeri [peak]'e getirir, sondaki çıtırtıyı önlemek için 2 ms'de kısar. */
    private static double[] finish(double[] samples, double peak) {
        double max = 0;
        for (double sample : samples) {
            max = Math.max(max, Math.abs(sample));
        }
        int fade = SAMPLE_RATE * 2 / 1000;
        for (int i = 0; i < samples.length; i++) {
            double tail = Math.min(1, (double) (samples.length - 1 - i) / fade);
            samples[i] = samples[i] / max * peak * tail;
        }
        return samples;
    }

    private static void writeWav(Path path, double[] samples) throws IOException {
        int dataSize = samples.length * 2;
        ByteBuffer wav = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + dataSize);
        wav.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        wav.put("fmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16);
        wav.putShort((short) 1).putShort((short) 1);
        wav.putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2);
        wav.putShort((short) 2).putShort((short) 16);
        wav.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(dataSize);
        for (double sample : samples) {
            wav.putShort((short) Math.round(sample * Short.MAX_VALUE));
        }
        Files.write(path, wav.array());
    }
}
