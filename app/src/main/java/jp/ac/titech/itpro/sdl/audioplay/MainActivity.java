package jp.ac.titech.itpro.sdl.audioplay;

import java.io.IOException;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioRecord;
import android.media.AudioFormat;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.media.MediaRecorder;
import android.view.View;
import android.widget.Button;
import android.view.Menu;
import android.util.Log;
import android.widget.TextView;

import static java.lang.Math.round;


public class MainActivity extends AppCompatActivity {

    private MediaRecorder recorder;
    private Button recButton;
    private Button playButton;
    private Button testButton;
    private Button analyzeButton;
    private TextView HzView;
    private MediaPlayer player;
    private DrawCircle mCircle;
    private upDateCircle uc;
    private AnimateString aniSt;
    private final int RECORDTIME = 1000;
    private Handler handler;

    // サンプリングレート
    private int SAMPLING_RATE = 44100;
    // FFTのポイント数
    private int FFT_SIZE = 4096;

    // デシベルベースラインの設定
    private double dB_baseline = Math.pow(2, 15) * FFT_SIZE * Math.sqrt(2);

    // 分解能の計算
    private double resol = ((SAMPLING_RATE / (double) FFT_SIZE));

    private AudioRecord audioRec = null;
    private boolean bIsRecording = false;
    private int bufSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recButton = findViewById(R.id.start_Record);
        recButton.setOnClickListener(new startRec());
        playButton = findViewById(R.id.play_Sound);
        playButton.setOnClickListener(new startPlay());
        testButton = findViewById(R.id.test);
        testButton.setOnClickListener(new testPlay());
        analyzeButton = findViewById(R.id.analyze_voice);
        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAnalyze();
            }
        });

        setVolumeControlStream(AudioManager.STREAM_MUSIC);


        //このアプリケーション専用ファイルの保存場所確認用
        File path = this.getFilesDir();
        Log.d("DEBUG", path.getPath());

        //mCircle = findViewById(R.id.circle_view);
        //uc = new upDateCircle(mCircle);

        //aniSt = findViewById(R.id.aniSt_view);

        bufSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        Log.d("DEBUG", Integer.toString(bufSize));

        // Viewの設定
        HzView = findViewById(R.id.hz_view);

        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    //高速フーリエ変換
    protected double[] FastFourieTransform(byte[] buf) {
        //エンディアン変換
        ByteBuffer bf = ByteBuffer.wrap(buf);
        bf.order(ByteOrder.LITTLE_ENDIAN);
        short[] s = new short[(int) bufSize];
        for (int i = bf.position(); i < bf.capacity() / 2; i++) {
            s[i] = bf.getShort();
        }
        //FFTクラスの作成と値の引き渡し
        FFT4g fft = new FFT4g(FFT_SIZE);
        double[] FFTdata = new double[FFT_SIZE];
        for (int i = 0; i < FFT_SIZE; i++) {
            if (i >= 3776) {
                FFTdata[i] = (double) 0.0;
            }
            else {
                FFTdata[i] = (double) s[i];
            }
        }
        fft.rdft(1, FFTdata);
        return FFTdata;
    }

    //デシベルの計算
    protected int maxDeciBel(double[] FFTdata, double[] dbfs) {
        double max_db = -120d;
        int max_i = 0;
        for (int i = 0; i < FFT_SIZE; i += 2) {
            dbfs[i / 2] = (int) (20 * Math.log10(Math.sqrt(Math
                    .pow(FFTdata[i], 2)
                    + Math.pow(FFTdata[i + 1], 2)) / dB_baseline));
            if (max_db < dbfs[i / 2]) {
                max_db = dbfs[i / 2];
                max_i = i / 2;
            }
        }
        return max_i;
    }

    //周波数解析の開始
    private void startAnalyze() {
        // AudioRecordの作成
        audioRec = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufSize * 2);
        audioRec.startRecording();
        bIsRecording = true;
        //フーリエ解析スレッドの作成
        Thread fft = new Thread(new Runnable() {
            @Override
            public void run() {
                byte buf[] = new byte[bufSize * 2];
                while (bIsRecording) {
                    audioRec.read(buf, 0, buf.length);

                    //フーリエ変換
                    double[] FFTdata = FastFourieTransform(buf);

                    //デシベルの計算とそのsono最大値の獲得
                    double[] dbfs = new double[FFT_SIZE/2];
                    int max_index = maxDeciBel(FFTdata, dbfs);
                    double max_db = dbfs[max_index];

                    //最強度の周波数を計算
                    double h = resol * max_index;

                    int a = (int) Math.round(resol * max_index);
                    final String Hz = Integer.toString(a);
                    final String db = Double.toString((int)dbfs[max_index]);

                    //メインスレッドのビューの値を変更
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            HzView.setText("周波数: " + Hz + " Hz\n音量: " + db + " dB");
                        }
                    });
                }
                // 録音停止
                audioRec.stop();
                audioRec.release();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        HzView.setText("");
                    }
                });
            }
        });
        //スレッドのスタート
        fft.start();
        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAnalyze();
            }
        });
        analyzeButton.setText("Stop");
    }

    private void stopAnalyze() {
        bIsRecording = false;
        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAnalyze();
            }
        });
        analyzeButton.setText("Analyze Voice");
    }

    // recButtonのStart時イベントリスナー
    private class startRec implements View.OnClickListener {
        public void onClick(View v) {
            startRecord();
        }
    }

    // recButtonのStop時イベントリスナー
    private class stopRec implements View.OnClickListener {
        public void onClick(View v) {
            stopRecord();
        }
    }

    // playButtonのStart時イベントリスナー
    private class startPlay implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            startPlay();
            playButton.setText(R.string.stopPlay);
            playButton.setOnClickListener(new stopPlay());
        }
    }

    // playButtonのStop時イベントリスナー
    private class stopPlay implements View.OnClickListener {
        public void onClick(View v) {
            stopPlay();
            playButton.setText(R.string.startPlay);
            playButton.setOnClickListener(new startPlay());
        }
    }

    // testButtonのStart時イベントリスナー
    private class testPlay implements  View.OnClickListener {
        public void onClick(View v) {
            testStart();
            testButton.setText(R.string.stopTest);
            testButton.setOnClickListener(new stopTest());
        }
    }

    // testButtonのStop時イベントリスナー
    private class stopTest implements View.OnClickListener {
        public void onClick(View v) {
            stopPlay();
            testButton.setText(R.string.testPlay);
            testButton.setOnClickListener(new testPlay());
        }
    }

    private void startRecord() {
        try {
            String filepath = "/data/data/jp.ac.titech.itpro.sdl.audioplay/files/";
            String filename = "test.wav";
            File file = new File(filepath + filename);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            recorder.setOutputFile(filepath + filename);
            recorder.prepare();
            recorder.start();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        recButton.setText(R.string.stopRec);
        recButton.setOnClickListener(new stopRec());
    }

    private void stopRecord() {
        try {
            recorder.stop();
            recorder.release();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        recButton.setText(R.string.startRec);
        recButton.setOnClickListener(new startRec());
    }

    private void startPlay() {
        try {
            player = new MediaPlayer();
            player.setDataSource("/data/data/jp.ac.titech.itpro.sdl.audioplay/files/test.wav");
            player.prepare();
            player.start();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d("DEBUG", "here");
                    stopPlay();
                    playButton.setText(R.string.startPlay);
                    playButton.setOnClickListener(new startPlay());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopPlay() {
        try {
            player.stop();
            player.prepare();
            player.release();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void testStart() {
        try {
            player = new MediaPlayer();
            player = MediaPlayer.create(this, R.raw.s_logo_a);
            player.start();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d("debug", "here");
                    stopPlay();
                    testButton.setText(R.string.testPlay);
                    testButton.setOnClickListener(new testPlay());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
