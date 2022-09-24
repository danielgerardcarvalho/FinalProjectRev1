package com.daniel.finalprojectrev1;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.Image;
import android.media.MediaRecorder;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.finalprojectrev1.databinding.ActivityScrollingBinding;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import java.util.Random;

public class ScrollingActivity extends AppCompatActivity {

    private ActivityScrollingBinding binding;


    /* General Operation */ //TODO: maybe change some of these to individual sub-systems later?
    private boolean system_flag;

    /* Audio Capture */
    // capture constants
    private static final int[] AUDIO_SOURCES = {
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.UNPROCESSED
    };
    private static final int AUDIO_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT_INT16 = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_FORMAT_FLOAT = AudioFormat.ENCODING_PCM_FLOAT;
    private static final int AUDIO_MIN_FORMAT_SIZE = 1;
    // input format options
    private static final String UI_AUDIO_FORMAT_INT16 = "int16";
    private static final String UI_AUDIO_FORMAT_FLOAT = "float";
    // audio capture object
    private AudioRecord audio_recorder;
    // audio capture multi-threading
    private Runnable mt_audio_capture_runnable;
    private boolean mt_audio_capture_flag;
    private byte [][] cap_buffer;

    /* Model Import Settings */
    // Inputs
    private TextView model_filename_view;
    private EditText num_class_events_text;
    private EditText clip_len_text;
    private EditText num_overlaps_text;
    private EditText snr_range_min_text;
    private EditText snr_range_max_text;
    private EditText num_training_sample_text;
    // Values
    private int model_num_class_events;
    private int model_clip_len;
    private int model_num_overlaps;
    private int model_snr_range_min;
    private int model_snr_range_max;
    private int model_num_training_samples;

    /* Capture Settings */
    // Inputs
    private EditText cap_sample_rate_input;
    private EditText cap_time_interval_input;
    private Spinner cap_format_input;
    // Values
    private int cap_sample_rate;
    private int cap_time_interval;
    private int cap_buffer_size;
    private int cap_queue_loc;
    private int cap_format;
    // Constants
    private int CAP_QUEUE_SIZE = 50;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityScrollingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle(getTitle());

        /* Configure Runnables */
        configureRunnables();

        /* Model Import Setting Inputs */
        model_filename_view = (TextView) findViewById(R.id.import_model_filename_view);
        num_class_events_text = (EditText) findViewById(R.id.input_model_import_num_event_classes);
        clip_len_text = (EditText) findViewById(R.id.input_model_import_clip_len);
        num_overlaps_text = (EditText) findViewById(R.id.input_model_import_num_overlaps);
        snr_range_min_text = (EditText) findViewById(R.id.input_model_import_snr_range_min);
        snr_range_max_text = (EditText) findViewById(R.id.input_model_import_snr_range_max);
        num_training_sample_text = (EditText) findViewById(R.id.input_model_import_training_size);
        ImageView image = findViewById(R.id.imageView);

        /* Capture Setting Inputs */
        cap_sample_rate_input = (EditText) findViewById(R.id.input_cap_sample_rate);
        cap_time_interval_input = (EditText) findViewById(R.id.input_cap_time_interval);
        cap_format_input = (Spinner) findViewById(R.id.input_cap_format_option);

        /* Floating Action Button Logic */
        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(view -> {
            if (!convertSettingInputs()) {
                Snackbar.make(view, "Settings are not valid", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }
            Snackbar.make(view, "Starting system", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            // Check if already running
            if (system_flag){
                // Change icon image
                fab.setImageResource(android.R.drawable.ic_media_play);

                // Stop detection thread
                // Stop processing thread
                // Stop audio capture thread
                if (mt_audio_capture_flag){
                    stopCapture();
                }
                system_flag = false;
            } else {
                // Change icon image
                fab.setImageResource(android.R.drawable.ic_media_pause);
                // Configure capture system
                configureCapture();
                // Update UI fields
                uiFieldUpdate();
                //            // Configure detection system
                //            configureDetection();
                //            // Configure monitoring system
                //            configureMonitor();
                // Starting the sub-systems
                startCapture();
                //            startDetection();
                // Display results
                tempTestPlot(image);

                system_flag = true;
            }
        });

        /* Checking application permissions */
        checkAllPermissions();

        /* Model Import Filename Update Logic */
        modelFilenameUpdate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean convertSettingInputs() {
        // Loading defaults - model import settings
        model_num_class_events = Integer.parseInt(num_class_events_text.getHint().toString());
        model_clip_len = Integer.parseInt(clip_len_text.getHint().toString());
        model_num_overlaps = Integer.parseInt(num_overlaps_text.getHint().toString());
        model_snr_range_min = Integer.parseInt(snr_range_min_text.getHint().toString());
        model_snr_range_max = Integer.parseInt(snr_range_max_text.getHint().toString());
        model_num_training_samples = Integer.parseInt(num_training_sample_text.getHint().toString());
        // Loading defaults - capture settings
        cap_sample_rate = Integer.parseInt(cap_sample_rate_input.getHint().toString());
        cap_time_interval = model_clip_len;
        cap_format = AUDIO_FORMAT_INT16;
        String selected_audio_format = cap_format_input.getSelectedItem().toString();

        // Processing User Inputs
        // Model import settings
        // TODO: need to add the checks for model import inputs, currently only the default settings
        //  are used.
        // Capture settings
        if (!TextUtils.isEmpty(cap_sample_rate_input.getText())) {
            cap_sample_rate = Integer.parseInt(cap_sample_rate_input.getText().toString());
        }
        if (!TextUtils.isEmpty(cap_time_interval_input.getText())) {
            cap_time_interval = Integer.parseInt(cap_time_interval_input.getText().toString());
        }
        if (selected_audio_format.matches(UI_AUDIO_FORMAT_INT16)) {
            cap_format = AUDIO_FORMAT_INT16;
        } else {
            cap_format = AUDIO_FORMAT_FLOAT;
        }

        // TODO: possibly add a check for the validity of the accepted/rejected settings. Maybe make
        //  a system that states which settings are missing or in error. Therefore returning true
        //  or false.
        return true;
    }

    private void modelFilenameUpdate() {
        num_class_events_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                model_filename_view.setText(String.format("dictW_c%s_len%s_ol%ssnr(%s, %s)_tsize%s",
                        num_class_events_text.getText().toString(),
                        clip_len_text.getText().toString(),
                        num_overlaps_text.getText().toString(),
                        snr_range_min_text.getText().toString(),
                        snr_range_max_text.getText().toString(),
                        num_training_sample_text.getText().toString()
                ));
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        clip_len_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                model_filename_view.setText(String.format("dictW_c%s_len%s_ol%ssnr(%s, %s)_tsize%s",
                        num_class_events_text.getText().toString(),
                        clip_len_text.getText().toString(),
                        num_overlaps_text.getText().toString(),
                        snr_range_min_text.getText().toString(),
                        snr_range_max_text.getText().toString(),
                        num_training_sample_text.getText().toString()
                ));
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        num_overlaps_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                model_filename_view.setText(String.format("dictW_c%s_len%s_ol%ssnr(%s, %s)_tsize%s",
                        num_class_events_text.getText().toString(),
                        clip_len_text.getText().toString(),
                        num_overlaps_text.getText().toString(),
                        snr_range_min_text.getText().toString(),
                        snr_range_max_text.getText().toString(),
                        num_training_sample_text.getText().toString()
                ));
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        snr_range_min_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                model_filename_view.setText(String.format("dictW_c%s_len%s_ol%ssnr(%s, %s)_tsize%s",
                        num_class_events_text.getText().toString(),
                        clip_len_text.getText().toString(),
                        num_overlaps_text.getText().toString(),
                        snr_range_min_text.getText().toString(),
                        snr_range_max_text.getText().toString(),
                        num_training_sample_text.getText().toString()
                ));
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        snr_range_max_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                model_filename_view.setText(String.format("dictW_c%s_len%s_ol%ssnr(%s, %s)_tsize%s",
                        num_class_events_text.getText().toString(),
                        clip_len_text.getText().toString(),
                        num_overlaps_text.getText().toString(),
                        snr_range_min_text.getText().toString(),
                        snr_range_max_text.getText().toString(),
                        num_training_sample_text.getText().toString()
                ));
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        num_training_sample_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                model_filename_view.setText(String.format("dictW_c%s_len%s_ol%ssnr(%s, %s)_tsize%s",
                        num_class_events_text.getText().toString(),
                        clip_len_text.getText().toString(),
                        num_overlaps_text.getText().toString(),
                        snr_range_min_text.getText().toString(),
                        snr_range_max_text.getText().toString(),
                        num_training_sample_text.getText().toString()
                ));
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void uiFieldUpdate(){
        // Update the text field of all input spaces
        // Model import
        num_class_events_text.setText(String.format("%d", model_num_class_events));
        clip_len_text.setText(String.format("%d", model_clip_len));
        num_overlaps_text.setText(String.format("%d", model_num_overlaps));
        snr_range_min_text.setText(String.format("%d", model_snr_range_min));
        snr_range_max_text.setText(String.format("%d", model_snr_range_max));
        num_training_sample_text.setText(String.format("%d", model_num_training_samples));
        model_filename_view.setText(String.format("dictW_c%s_len%s_ol%ssnr(%s, %s)_tsize%s",
                num_class_events_text.getText().toString(),
                clip_len_text.getText().toString(),
                num_overlaps_text.getText().toString(),
                snr_range_min_text.getText().toString(),
                snr_range_max_text.getText().toString(),
                num_training_sample_text.getText().toString()
        ));
        // Capture
        cap_sample_rate_input.setText(String.format("%d", cap_sample_rate));
        cap_time_interval_input.setText(String.format("%d", cap_time_interval));
        if (cap_format == AUDIO_FORMAT_INT16){
            cap_format_input.setSelection(0);
        } else {
            cap_format_input.setSelection(1);
        }
    }

    private void configureRunnables(){

        // Audio Capture Runnable
        mt_audio_capture_runnable = new Runnable() {
            @Override
            public void run() {
                // Read data from the buffer when the buffer is full
                Log.v("AudioCapture", "Audio read thread is starting...");
                while(mt_audio_capture_flag){
                    Log.v("AudioCapture", "Audio read thread is active...");
                    readAudioCaptureBuffer();
//                    SystemClock.sleep(1000);
                }
                Log.v("AudioCapture", "Audio read thread has been closed.");
            }
        };
    }


    // Permissions
    private void checkAllPermissions(){
        // Checking for required permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String []
                    {Manifest.permission.RECORD_AUDIO}, 0);
        }
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String []
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    private void checkPermission(Context context, Activity activity, String permission){
        // Checking for required permissions
        if (ActivityCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String []
                    {permission}, 0);
        }
    }


    // Import


    // Capture
    private void configureCapture() {
        // Set flags
        mt_audio_capture_flag = true;
        // Resetting location of cap_queue
        cap_queue_loc = 0;
        // Minimum buffer size
        int min_buffer_size = AudioRecord.getMinBufferSize(cap_sample_rate, AUDIO_CHANNELS,
                cap_format);
        cap_buffer_size = (int) cap_sample_rate * cap_time_interval * (cap_format /
                AUDIO_MIN_FORMAT_SIZE);
        // Checking if cap_buffer_size is large enough
        if (cap_buffer_size < min_buffer_size){
            Log.e("AudioCapture", String.format("cap_buffer_size: is too small\n\tcurrent:%d" +
                    "\n\trequired:%d", cap_buffer_size, min_buffer_size));
            cap_buffer_size = min_buffer_size;
        }
        // Creating audio record object
        checkPermission(this, this, Manifest.permission.RECORD_AUDIO);
        audio_recorder = new AudioRecord(AUDIO_SOURCES[0], cap_sample_rate, AUDIO_CHANNELS,
                cap_format, cap_buffer_size);
        cap_buffer = new byte[CAP_QUEUE_SIZE][cap_buffer_size];
    }

    private void startCapture(){
        // Check if audio interface is in appropriate state
        if (audio_recorder.getState() != AudioRecord.STATE_INITIALIZED){
            Log.e("AudioCapture", "Audio interface is in incorrect state: desired:" +
                    "initialised");
            return;
        }
        // Starting recording interface
        audio_recorder.startRecording();
        // Starting recording read thread
        new Thread(mt_audio_capture_runnable).start();
    }

    private void stopCapture(){
        // Check if audio interface is in appropriate state
        int audio_recorder_state = audio_recorder.getRecordingState();
        if (audio_recorder_state != AudioRecord.RECORDSTATE_RECORDING){
            String temp_state = "none";
            switch(audio_recorder_state){
                case 1:
                    temp_state = "stopped";
                    break;
                case 3:
                    temp_state = "running";
                    break;
            }
            Log.e("AudioCapture", "Audio interface is in incorrect state:\n\tdesired: " +
                    "running\n\tcurrent:" + temp_state);
            return;
        }
        // Stopping recording read thread
        mt_audio_capture_flag = false;
        // Stopping recording interface
        audio_recorder.stop();
        audio_recorder.release();
        audio_recorder = null;
    }

    private void readAudioCaptureBuffer(){
        // Read data from the internal buffer
        int num_read = 0;
        num_read = audio_recorder.read(cap_buffer[cap_queue_loc], num_read, cap_buffer_size);
        // Incrementing the buffer queue location
        cap_queue_loc++;
        Log.d("AudioCapture", String.format("Read from internal buffer:\n\tAmount read:%d," +
                "\n\tQueue location:%d", num_read, cap_queue_loc));
    }


    // Processing


    // Detection


    // Result display
    private void tempTestPlot(ImageView image){
        // Create some data
        int num_freq_bins = 128;
        int num_time_frames = 500;
        Random random = new Random();
        double[][] spec_matrix = new double[num_freq_bins][num_time_frames];
        for (int i = 0; i < num_freq_bins; i++){
            for (int j = 0; j < num_time_frames; j++){
                spec_matrix[i][j] = random.nextDouble();
            }
        }
        // normalise the spec_matrix
        double temp_max = 0;
        for (int i = 0; i < num_freq_bins; i++){
            for (int j = 0; j < num_time_frames; j++){
                temp_max = Math.max(spec_matrix[i][j], temp_max);
            }
        }
        for (int i = 0; i < num_freq_bins; i++){
            for (int j = 0; j < num_time_frames; j++){
                spec_matrix[i][j] = spec_matrix[i][j] / temp_max;
            }
        }

        SpectrogramView temp_obj = new SpectrogramView(this, spec_matrix);
        image.setImageBitmap(temp_obj.bmp);
    }

    // Data handling

}