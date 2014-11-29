package net.iwebrtc.audioprocess.demo;

import net.iwebrtc.audioprocess.sdk.AudioProcess;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;

public class DemoActivity extends Activity implements OnClickListener {

	SeekBar skbVolume;//调节音量
	boolean isProcessing = true;//是否录放的标记
	boolean isRecording = false;//是否录放的标记

	static final int frequency = 16000;
	static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	int recBufSize, playBufSize;
	AudioRecord audioRecord;
	AudioTrack audioTrack;

	AudioProcess mAudioProcess;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demo);
		recBufSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
		Log.e("", "recBufSize:" + recBufSize);
		playBufSize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, recBufSize);
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency, channelConfiguration, audioEncoding, playBufSize, AudioTrack.MODE_STREAM);

		findViewById(R.id.btnRecord).setOnClickListener(this);
		findViewById(R.id.btnStop).setOnClickListener(this);

		skbVolume = (SeekBar) this.findViewById(R.id.skbVolume);
		skbVolume.setMax(100);//音量调节的极限
		skbVolume.setProgress(50);//设置seekbar的位置值
		audioTrack.setStereoVolume(0.7f, 0.7f);//设置当前音量大小
		skbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				float vol = (float) (seekBar.getProgress()) / (float) (seekBar.getMax());
				audioTrack.setStereoVolume(vol, vol);//设置音量
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			}
		});
		((CheckBox) findViewById(R.id.cb_ap)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton view, boolean checked) {
				isProcessing = checked;
			}
		});
		mAudioProcess = new AudioProcess();
		mAudioProcess.init(frequency, 2, 1);
	}

	@Override
	protected void onDestroy() {
		mAudioProcess.destroy();
		android.os.Process.killProcess(android.os.Process.myPid());
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btnRecord) {
			isRecording = true;
			new RecordPlayThread().start();
		} else if (v.getId() == R.id.btnStop) {
			isRecording = false;
		}
	}

	class RecordPlayThread extends Thread {
		public void run() {
			try {
				int bufferSize = mAudioProcess.calculateBufferSize(frequency, 2, 1);
				byte[] buffer = new byte[bufferSize];
				audioRecord.startRecording();//开始录制
				audioTrack.play();//开始播放

				while (isRecording) {
					//setp 1 从MIC保存数据到缓冲区
					int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
					byte[] tmpBuf_src = new byte[bufferReadResult];
					System.arraycopy(buffer, 0, tmpBuf_src, 0, bufferReadResult);

					//setp 2 进行处理
					byte[] tmpBuf_processed = new byte[bufferReadResult];
					if (isProcessing) {
						mAudioProcess.processStream10msData(tmpBuf_src, tmpBuf_src.length, tmpBuf_processed);
					} else {
						tmpBuf_processed = tmpBuf_src;
					}
					//写入数据即播放
					audioTrack.write(tmpBuf_processed, 0, tmpBuf_processed.length);
					mAudioProcess.AnalyzeReverseStream10msData(tmpBuf_processed, tmpBuf_processed.length);
				}
				audioTrack.stop();
				audioRecord.stop();
			} catch (Throwable t) {
				Log.e("", "", t);
			}
		}
	};
}