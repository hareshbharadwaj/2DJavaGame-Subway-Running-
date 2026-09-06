"""Synthesise the looping jetpack thruster sound (sounds/jetpack.wav).

A rocket 'uuzzz': filtered noise for the thruster hiss, layered over two
detuned saw-ish tones for the low roar, with a short fade in/out so the
clip loops seamlessly while the jetpack power-up is active.

Format matches the existing effects: mono, 22050 Hz, 16-bit PCM.
"""
import math
import struct
import wave

RATE = 22050
DUR = 1.0          # seconds; looped by SoundManager while flying
OUT = "sounds/jetpack.wav"


def main():
    n = int(RATE * DUR)
    # deterministic pseudo-noise so the file is reproducible
    seed = 12345
    lp = 0.0          # low-pass state for the hiss
    bp = 0.0          # band-pass-ish state for body
    samples = []
    for i in range(n):
        t = i / RATE
        seed = (1103515245 * seed + 12345) & 0x7FFFFFFF
        white = (seed / 0x3FFFFFFF) - 1.0

        # thruster hiss: low-passed noise
        lp += 0.22 * (white - lp)
        bp += 0.06 * (lp - bp)
        hiss = (lp - bp) * 1.5

        # low roar: two slightly detuned tones with a slow wobble
        wob = 1.0 + 0.02 * math.sin(2 * math.pi * 7.0 * t)
        roar = (math.sin(2 * math.pi * 62.0 * wob * t) * 0.5
                + math.sin(2 * math.pi * 93.0 * wob * t) * 0.3)
        # soft-clip the roar so it growls rather than beeps
        roar = math.tanh(roar * 1.8) * 0.42

        v = hiss * 0.55 + roar

        # equal-power fade at both ends -> clean loop point
        edge = int(RATE * 0.035)
        if i < edge:
            v *= i / edge
        elif i > n - edge:
            v *= (n - i) / edge

        samples.append(max(-1.0, min(1.0, v)) * 0.72)

    with wave.open(OUT, "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(s * 32767)) for s in samples))
    print("wrote", OUT, "%d frames" % n)


if __name__ == "__main__":
    main()
