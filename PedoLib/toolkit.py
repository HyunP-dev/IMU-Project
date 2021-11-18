import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy import signal

def low_pass_filter(sequence, sampling_frequency, cut_frequency, n):
    nyq = 0.5 * sampling_frequency
    cutoff = cut_frequency / nyq
    b, a = signal.butter(n, cutoff)
    return signal.filtfilt(b, a, sequence)

def peaks_threshold(peaks, min, max):
    return filter(lambda p: min<=p<=max, peaks)

def extremas(sequence: list) -> np.array:
    """
    find zeros of derivative of sequence.
    """
    return np.append(
        signal.find_peaks(-sequence)[0], 
        signal.find_peaks(sequence)[0]
    )

