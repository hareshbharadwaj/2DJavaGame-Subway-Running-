import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

public class SoundManager {
    private static Map<String, Clip> clips = new HashMap<>();
    private static boolean muted = false;
    private static final String SOUND_DIR = "sounds/";

    public static void loadAll() {
        String[] sounds = {"coin", "jump", "crash", "click", "powerup", "gameover", "music", "jetpack"};
        for (String sound : sounds) {
            try {
                File file = new File(SOUND_DIR + sound + ".wav");
                if (file.exists()) {
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(file);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    clips.put(sound, clip);
                } else {
                    System.err.println("Sound file not found: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void play(String name) {
        if (muted) return;
        Clip clip = clips.get(name);
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public static void loop(String name) {
        if (muted) return;
        Clip clip = clips.get(name);
        if (clip != null) {
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static void stop(String name) {
        Clip clip = clips.get(name);
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public static void toggleMute() {
        muted = !muted;
        if (muted) {
            for (Clip clip : clips.values()) {
                if (clip.isRunning()) clip.stop();
            }
        } else {
            // If unmuted, start music again if we want, but let's just leave it up to the game logic
            loop("music");
        }
    }
}
