import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Uygulama ikonunu üretir: halka biçiminde tesbih, altta imame ve püskül.
 * Proje kökünden çalıştır: java tools/GenerateIcon.java
 */
public class GenerateIcon {
    private static final String BACKGROUND = "#3A2212";
    private static final String THREAD = "#8D6E52";
    private static final String BEAD = "#F0A93B";
    private static final String BEAD_SHINE = "#FFE3A8";
    private static final String IMAME = "#C2412F";
    private static final String IMAME_SHINE = "#F2A489";

    private static final double CENTER_X = 54;
    private static final double CENTER_Y = 49;
    private static final double RING_RADIUS = 20;
    private static final double BEAD_RADIUS = 3.7;
    private static final int BEADS = 15;
    private static final double IMAME_Y = CENTER_Y + RING_RADIUS + 2;
    private static final double IMAME_RADIUS_X = 3.9;
    private static final double IMAME_RADIUS_Y = 5.4;
    private static final double TASSEL_TOP = IMAME_Y + IMAME_RADIUS_Y;
    private static final double TASSEL_BOTTOM = TASSEL_TOP + 9;

    private record Circle(double x, double y, double rx, double ry) {
        String path() {
            return String.format(Locale.ROOT, "M%.2f,%.2fa%.2f,%.2f 0 1,0 %.2f,0a%.2f,%.2f 0 1,0 %.2f,0z",
                    x - rx, y, rx, ry, rx * 2, rx, ry, -rx * 2);
        }
    }

    private record Strand(double fromX, double fromY, double toX, double toY) {
        String path() {
            return String.format(Locale.ROOT, "M%.2f,%.2fL%.2f,%.2f", fromX, fromY, toX, toY);
        }
    }

    public static void main(String[] args) throws IOException {
        List<Circle> beads = new ArrayList<>();
        List<Circle> shines = new ArrayList<>();
        // İlk konum en alttadır; orası imameye ayrıldığı için atlanır.
        for (int i = 1; i < BEADS; i++) {
            double angle = Math.PI / 2 + 2 * Math.PI * i / BEADS;
            double x = CENTER_X + RING_RADIUS * Math.cos(angle);
            double y = CENTER_Y + RING_RADIUS * Math.sin(angle);
            beads.add(new Circle(x, y, BEAD_RADIUS, BEAD_RADIUS));
            shines.add(new Circle(x - 1.1, y - 1.1, 1.1, 1.1));
        }
        Circle ring = new Circle(CENTER_X, CENTER_Y, RING_RADIUS, RING_RADIUS);
        Circle imame = new Circle(CENTER_X, IMAME_Y, IMAME_RADIUS_X, IMAME_RADIUS_Y);
        Circle imameShine = new Circle(CENTER_X - 1.2, IMAME_Y - 1.8, 1.2, 1.5);
        List<Strand> tassel = List.of(
                new Strand(CENTER_X, TASSEL_TOP, CENTER_X - 3, TASSEL_BOTTOM),
                new Strand(CENTER_X, TASSEL_TOP, CENTER_X, TASSEL_BOTTOM + 1),
                new Strand(CENTER_X, TASSEL_TOP, CENTER_X + 3, TASSEL_BOTTOM));

        Path drawables = Path.of("app/src/main/res/drawable");
        Files.writeString(drawables.resolve("ic_launcher_background.xml"), vector(
                fill(BACKGROUND, "M0,0h108v108h-108z")));
        Files.writeString(drawables.resolve("ic_launcher_foreground.xml"), vector(
                stroke(THREAD, 1.0, ring.path())
                        + stroke(IMAME, 1.3, tassel.stream().map(Strand::path).reduce("", String::concat))
                        + fill(BEAD, beads.stream().map(Circle::path).reduce("", String::concat))
                        + fill(BEAD_SHINE, shines.stream().map(Circle::path).reduce("", String::concat))
                        + fill(IMAME, imame.path())
                        + fill(IMAME_SHINE, imameShine.path())));
        Files.writeString(drawables.resolve("ic_launcher_monochrome.xml"), vector(
                stroke("#FFFFFF", 1.0, ring.path())
                        + stroke("#FFFFFF", 1.3, tassel.stream().map(Strand::path).reduce("", String::concat))
                        + fill("#FFFFFF", beads.stream().map(Circle::path).reduce("", String::concat))
                        + fill("#FFFFFF", imame.path())));

        Path store = Path.of("docs/store");
        Files.createDirectories(store);
        ImageIO.write(storeIcon(ring, beads, shines, imame, imameShine, tassel), "png",
                store.resolve("icon-512.png").toFile());
    }

    private static String vector(String paths) {
        return """
                <vector xmlns:android="http://schemas.android.com/apk/res/android"
                    android:width="108dp"
                    android:height="108dp"
                    android:viewportWidth="108"
                    android:viewportHeight="108">
                %s</vector>
                """.formatted(paths);
    }

    private static String fill(String color, String data) {
        return """
                    <path
                        android:fillColor="%s"
                        android:pathData="%s" />
                """.formatted(color, data);
    }

    private static String stroke(String color, double width, String data) {
        return """
                    <path
                        android:pathData="%s"
                        android:strokeWidth="%s"
                        android:strokeColor="%s"
                        android:strokeLineCap="round" />
                """.formatted(data, width, color);
    }

    /** Uyarlanabilir ikonda görünen orta 72 birimlik alan 512 piksele ölçeklenir. */
    private static BufferedImage storeIcon(Circle ring, List<Circle> beads, List<Circle> shines,
            Circle imame, Circle imameShine, List<Strand> tassel) {
        int size = 512;
        double scale = size / 72.0;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.decode(BACKGROUND));
        g.fillRect(0, 0, size, size);
        g.scale(scale, scale);
        g.translate(-18, -18);

        g.setStroke(new BasicStroke(1.0f));
        g.setColor(Color.decode(THREAD));
        g.draw(ellipse(ring));
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.decode(IMAME));
        tassel.forEach(s -> g.draw(new Line2D.Double(s.fromX, s.fromY, s.toX, s.toY)));
        g.setColor(Color.decode(BEAD));
        beads.forEach(c -> g.fill(ellipse(c)));
        g.setColor(Color.decode(BEAD_SHINE));
        shines.forEach(c -> g.fill(ellipse(c)));
        g.setColor(Color.decode(IMAME));
        g.fill(ellipse(imame));
        g.setColor(Color.decode(IMAME_SHINE));
        g.fill(ellipse(imameShine));
        g.dispose();
        return image;
    }

    private static Ellipse2D ellipse(Circle c) {
        return new Ellipse2D.Double(c.x - c.rx, c.y - c.ry, c.rx * 2, c.ry * 2);
    }
}
