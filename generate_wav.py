import math
import struct
import os

sample_rate = 44100
duration_1 = 0.15 # seconds for "Ting" (1760 Hz)
duration_2 = 0.35 # seconds for "In" (2637 Hz)

freq1 = 1760.0
freq2 = 2637.0

samples = []

# Tone 1: Ting (1760Hz) with exponential decay
num_samples_1 = int(sample_rate * duration_1)
for i in range(num_samples_1):
    t = float(i) / sample_rate
    decay = math.exp(-i / (sample_rate * 0.05))
    val = math.sin(2.0 * math.pi * freq1 * t) * decay * 0.8
    samples.append(val)

# Tone 2: In (2637Hz) with exponential decay
num_samples_2 = int(sample_rate * duration_2)
for i in range(num_samples_2):
    t = float(i) / sample_rate
    decay = math.exp(-i / (sample_rate * 0.12))
    val = math.sin(2.0 * math.pi * freq2 * t) * decay * 0.9
    samples.append(val)

# Convert to 16-bit PCM
pcm_data = bytearray()
for s in samples:
    # Clamp
    s = max(-1.0, min(1.0, s))
    sample_int = int(s * 32767)
    pcm_data.extend(struct.pack('<h', sample_int))

num_channels = 1
bits_per_sample = 16
byte_rate = sample_rate * num_channels * bits_per_sample // 8
block_align = num_channels * bits_per_sample // 8
data_size = len(pcm_data)
chunk_size = 36 + data_size

wav_header = bytearray()
wav_header.extend(b'RIFF')
wav_header.extend(struct.pack('<I', chunk_size))
wav_header.extend(b'WAVE')
wav_header.extend(b'fmt ')
wav_header.extend(struct.pack('<I', 16)) # Subchunk1Size
wav_header.extend(struct.pack('<H', 1))  # AudioFormat (PCM)
wav_header.extend(struct.pack('<H', num_channels))
wav_header.extend(struct.pack('<I', sample_rate))
wav_header.extend(struct.pack('<I', byte_rate))
wav_header.extend(struct.pack('<H', block_align))
wav_header.extend(struct.pack('<H', bits_per_sample))
wav_header.extend(b'data')
wav_header.extend(struct.pack('<I', data_size))

output_dir = r"C:\Users\User\.gemini\antigravity-ide\scratch\SlaTaskNotifierApp\app\src\main\res\raw"
os.makedirs(output_dir, exist_ok=True)
output_path = os.path.join(output_dir, "tingin.wav")

with open(output_path, "wb") as f:
    f.write(wav_header)
    f.write(pcm_data)

print(f"Generated WAV file at {output_path}, size: {len(wav_header) + len(pcm_data)} bytes")
