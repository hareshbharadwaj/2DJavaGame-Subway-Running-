package com.javadash.game.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

import java.util.HashMap;
import java.util.Map;

public final class AudioManager {

    private static final Map<String, Sound> sounds = new HashMap<>();
    private static Music bgMusic = null;
    private static Sound jetpackSound = null;
    private static long jetpackSoundId = -1;
    private static boolean muted = false;

    private AudioManager() {}

    private static FileHandle resolve(String name) {
        String path = "sounds/" + name;
        if (Gdx.files.internal(path).exists()) return Gdx.files.internal(path);
        if (Gdx.files.internal("assets/" + path).exists()) return Gdx.files.internal("assets/" + path);
        return Gdx.files.internal(path);
    }

    public static void loadAll() {
        String[] sfx = {"click.wav", "coin.wav", "crash.wav", "gameover.wav", "jetpack.wav", "jump.wav", "powerup.wav"};
        for (String s : sfx) {
            String key = s.substring(0, s.lastIndexOf('.'));
            try {
                FileHandle fh = resolve(s);
                if (fh.exists()) {
                    Sound sound = Gdx.audio.newSound(fh);
                    sounds.put(key, sound);
                    if ("jetpack".equals(key)) {
                        jetpackSound = sound;
                    }
                }
            } catch (Exception e) {
                Gdx.app.error("AudioManager", "Could not load sound: " + s, e);
            }
        }

        try {
            FileHandle fh = resolve("music.wav");
            if (fh.exists()) {
                bgMusic = Gdx.audio.newMusic(fh);
                bgMusic.setLooping(true);
                bgMusic.setVolume(0.65f);
            }
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Could not load music", e);
        }
    }

    public static void play(String soundKey) {
        if (muted) return;
        Sound s = sounds.get(soundKey);
        if (s != null) {
            s.play(1.0f);
        }
    }

    public static void startMusic() {
        if (bgMusic != null && !muted && !bgMusic.isPlaying()) {
            bgMusic.play();
        }
    }

    public static void stopMusic() {
        if (bgMusic != null && bgMusic.isPlaying()) {
            bgMusic.stop();
        }
    }

    public static void startJetpackLoop() {
        if (muted || jetpackSound == null) return;
        if (jetpackSoundId == -1) {
            jetpackSoundId = jetpackSound.loop(0.6f);
        }
    }

    public static void stopJetpackLoop() {
        if (jetpackSound != null && jetpackSoundId != -1) {
            jetpackSound.stop(jetpackSoundId);
            jetpackSoundId = -1;
        }
    }

    public static boolean isMuted() {
        return muted;
    }

    public static void toggleMute() {
        muted = !muted;
        if (muted) {
            stopMusic();
            stopJetpackLoop();
        } else {
            startMusic();
        }
    }

    public static void dispose() {
        for (Sound s : sounds.values()) {
            s.dispose();
        }
        sounds.clear();
        if (bgMusic != null) {
            bgMusic.dispose();
            bgMusic = null;
        }
    }
}
