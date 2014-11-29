package net.iwebrtc.audioprocess.sdk;

import android.util.Log;

public class AudioProcess {

	private static final String TAG = "AudioProcess";

	static {
		String[] LIBS = new String[] { "audio_process" };
		for (int i = 0; i < LIBS.length; i++) {
			try {
				System.loadLibrary(LIBS[i]);
			} catch (UnsatisfiedLinkError e) {
				Log.e(TAG, "Couldn't load lib: " + LIBS[i] + " - " + e.getMessage());
			}
		}
	}

	private long nativeAudioProcess;

	public AudioProcess() {
		nativeAudioProcess = create();
	}

	private native long create();

	public native boolean init(int sample_rate, int number_bytes_per_sample, int channels);

	public int calculateBufferSize(int sample_rate, int number_bytes_per_sample, int channels) {
		return sample_rate * channels * number_bytes_per_sample / 100;
	}

	public native boolean processStream10msData(byte[] data, int length, byte[] out);

	public native boolean AnalyzeReverseStream10msData(byte[] data, int length);

	public native boolean destroy();
}
