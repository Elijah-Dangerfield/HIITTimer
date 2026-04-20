"""
Generate timer cue sound packs as mono 16-bit PCM WAV.

Three packs × three cues = nine WAV files written to the target dir.
Deterministic, zero deps beyond stdlib. Values tuned by ear — tweak as needed.
"""
from __future__ import annotations

import math
import os
import struct
import sys
import wave

SR = 44100  # sample rate
OUT = sys.argv[1] if len(sys.argv) > 1 else "."


def write_wav(path: str, samples: list[float]) -> None:
    # Clip and convert to 16-bit PCM.
    clipped = [max(-1.0, min(1.0, s)) for s in samples]
    frames = b"".join(struct.pack("<h", int(s * 32767)) for s in clipped)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(frames)


def length_samples(seconds: float) -> int:
    return int(seconds * SR)


def adsr(n: int, attack: float = 0.005, release: float = 0.05) -> list[float]:
    """Attack-sustain-release envelope. Relative to total length n samples."""
    a = int(n * attack) if attack < 1 else int(attack * SR)
    r = int(n * release) if release < 1 else int(release * SR)
    a = max(1, min(a, n // 3))
    r = max(1, min(r, n // 3))
    env: list[float] = []
    for i in range(n):
        if i < a:
            env.append(i / a)
        elif i > n - r:
            env.append((n - i) / r)
        else:
            env.append(1.0)
    return env


def exp_decay(n: int, tau_seconds: float) -> list[float]:
    tau = tau_seconds * SR
    return [math.exp(-i / tau) for i in range(n)]


def sine(freq: float, n: int, phase: float = 0.0) -> list[float]:
    return [math.sin(2 * math.pi * freq * i / SR + phase) for i in range(n)]


def add(a: list[float], b: list[float], gain_a: float = 1.0, gain_b: float = 1.0) -> list[float]:
    n = max(len(a), len(b))
    out = [0.0] * n
    for i in range(n):
        if i < len(a):
            out[i] += a[i] * gain_a
        if i < len(b):
            out[i] += b[i] * gain_b
    return out


def scale(a: list[float], g: float) -> list[float]:
    return [x * g for x in a]


def mix_envelope(tone: list[float], env: list[float]) -> list[float]:
    return [t * e for t, e in zip(tone, env)]


# ───────────────────────── Classic (pure sine, crisp) ─────────────────────────

def classic_short() -> list[float]:
    n = length_samples(0.12)
    return mix_envelope(sine(880, n), adsr(n, attack=0.01, release=0.30))


def classic_long() -> list[float]:
    n = length_samples(0.28)
    tone = add(sine(523.25, n), sine(1046.5, n), gain_a=0.8, gain_b=0.15)  # C5 + overtone
    return mix_envelope(tone, adsr(n, attack=0.008, release=0.20))


def classic_finish() -> list[float]:
    # Ascending arpeggio C5 → E5 → G5, then hold G5 with long decay.
    notes = [523.25, 659.25, 783.99]
    each = length_samples(0.12)
    sequence: list[float] = []
    for f in notes:
        sequence += mix_envelope(sine(f, each), adsr(each, attack=0.005, release=0.20))
    # hold
    hold_n = length_samples(0.45)
    hold = add(sine(783.99, hold_n), sine(1567.98, hold_n), gain_a=0.7, gain_b=0.12)
    hold = mix_envelope(hold, exp_decay(hold_n, tau_seconds=0.18))
    return sequence + hold


# ───────────────────────── Chime (soft, yoga-ish) ─────────────────────────

def chime_partials(fundamental: float, n: int, harmonics: list[tuple[float, float]]) -> list[float]:
    """Sum sine partials: list of (multiplier, gain)."""
    out = [0.0] * n
    for mult, gain in harmonics:
        for i in range(n):
            out[i] += gain * math.sin(2 * math.pi * fundamental * mult * i / SR)
    return out


def chime_short() -> list[float]:
    n = length_samples(0.28)
    tone = chime_partials(660, n, [(1.0, 0.8), (2.0, 0.25), (3.0, 0.08)])
    env = exp_decay(n, tau_seconds=0.10)
    # soften attack
    attack = length_samples(0.025)
    for i in range(min(attack, n)):
        env[i] *= i / attack
    return mix_envelope(tone, env)


def chime_long() -> list[float]:
    n = length_samples(0.50)
    tone = chime_partials(440, n, [(1.0, 0.75), (2.0, 0.30), (3.0, 0.12), (4.0, 0.05)])
    env = exp_decay(n, tau_seconds=0.18)
    attack = length_samples(0.040)
    for i in range(min(attack, n)):
        env[i] *= i / attack
    return mix_envelope(tone, env)


def chime_finish() -> list[float]:
    # Gentle descent A5-G5-E5-D5-C5 fading out
    notes = [880.0, 783.99, 659.25, 587.33, 523.25]
    each = length_samples(0.22)
    gains = [0.9, 0.85, 0.8, 0.75, 0.7]
    sequence: list[float] = []
    for f, g in zip(notes, gains):
        tone = chime_partials(f, each, [(1.0, 0.7), (2.0, 0.18), (3.0, 0.06)])
        env = exp_decay(each, tau_seconds=0.12)
        attack = length_samples(0.025)
        for i in range(min(attack, each)):
            env[i] *= i / attack
        sequence += [s * g for s in mix_envelope(tone, env)]
    return sequence


# ───────────────────────── Bell (metallic, punchy) ─────────────────────────
# Inharmonic partials typical of struck bells: 0.5, 1.0, 1.2, 1.5, 2.0, 2.5, 3.0, 4.2

BELL_PARTIALS = [(0.5, 0.18), (1.0, 0.70), (1.19, 0.35), (1.56, 0.22),
                 (2.0, 0.18), (2.51, 0.12), (2.66, 0.08), (4.16, 0.06)]


def bell_tone(fundamental: float, n: int, tau: float) -> list[float]:
    out = [0.0] * n
    for mult, gain in BELL_PARTIALS:
        partial_tau = tau * (0.9 if mult > 1.0 else 1.0)  # higher partials decay faster
        decay = exp_decay(n, partial_tau)
        for i in range(n):
            out[i] += gain * decay[i] * math.sin(2 * math.pi * fundamental * mult * i / SR)
    # Very short attack for a struck sound.
    attack = length_samples(0.002)
    for i in range(min(attack, n)):
        out[i] *= i / attack
    return out


def bell_short() -> list[float]:
    n = length_samples(0.25)
    return bell_tone(880, n, tau=0.08)


def bell_long() -> list[float]:
    n = length_samples(0.50)
    return bell_tone(523.25, n, tau=0.18)


def bell_finish() -> list[float]:
    # Two strikes, "ding-ding" — like a boxing bell.
    strike_n = length_samples(0.45)
    gap_n = length_samples(0.08)
    s1 = bell_tone(523.25, strike_n, tau=0.20)
    s2 = bell_tone(523.25, strike_n, tau=0.28)
    return s1 + [0.0] * gap_n + s2


# ───────────────────────── Normalize + write ─────────────────────────

def normalize(samples: list[float], peak: float = 0.85) -> list[float]:
    m = max((abs(s) for s in samples), default=0.0) or 1.0
    return [s * peak / m for s in samples]


def gen(pack: str, cue: str, samples: list[float]) -> None:
    path = os.path.join(OUT, f"{pack}_{cue}.wav")
    write_wav(path, normalize(samples))
    size_kb = os.path.getsize(path) / 1024
    print(f"  {pack}_{cue}.wav  {size_kb:.1f} KB")


print(f"Writing to {OUT}")
gen("classic", "short", classic_short())
gen("classic", "long", classic_long())
gen("classic", "finish", classic_finish())
gen("chime", "short", chime_short())
gen("chime", "long", chime_long())
gen("chime", "finish", chime_finish())
gen("bell", "short", bell_short())
gen("bell", "long", bell_long())
gen("bell", "finish", bell_finish())
print("Done.")
