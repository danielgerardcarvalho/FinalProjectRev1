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

import java.util.ArrayList;

import jeigen.DenseMatrix;
import jeigen.DenseMatrixComplex;

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

    /* Audio Pre-Processing */
    // processing constants
    private final int HANN = 0;
    // processing multi-threading
    private Runnable mt_audio_processing_runnable;
    private boolean mt_audio_processing_flag;
    private int [] proc_data;

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

    /* Processing Settings */
    // Inputs
    private EditText proc_fft_size_input;
    private EditText proc_sample_rate_input;
    private EditText proc_num_time_frames_input;
    private EditText proc_resolution_input;
    private EditText proc_window_time_input;
    private EditText proc_hop_time_input;
    // Values
    private int proc_fft_size;
    private int proc_sample_rate;
    private int proc_num_time_frames;
    private int proc_resolution;
    private double proc_window_time;
    private double proc_hop_time;

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
        // TODO: temporary image view, used to test the plot output and test the capture and
        //  processing functions.
        ImageView image = findViewById(R.id.imageView);

        /* Capture Setting Inputs */
        cap_sample_rate_input = (EditText) findViewById(R.id.input_cap_sample_rate);
        cap_time_interval_input = (EditText) findViewById(R.id.input_cap_time_interval);
        cap_format_input = (Spinner) findViewById(R.id.input_cap_format_option);

        /* Processing Setting Inputs */
        proc_fft_size_input = (EditText) findViewById(R.id.input_proc_fft_size);
        proc_sample_rate_input = (EditText) findViewById(R.id.input_proc_sample_rate);
        proc_num_time_frames_input = (EditText) findViewById(R.id.input_proc_num_time_frames);
        proc_resolution_input = (EditText) findViewById(R.id.input_proc_resolution);
        proc_window_time_input = (EditText) findViewById(R.id.input_proc_window_time);
        proc_hop_time_input = (EditText) findViewById(R.id.input_proc_hop_time);

        /* Floating Action Button Logic */
        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(view -> {

            // Importing input user settings and checking if these settings are valid
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
                if (mt_audio_processing_flag){
                    stopProcessing();
                }
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
                // Configure processing system
                // TODO: implement this method
                configureProcessing();
                // Update UI fields
                uiFieldUpdate();
                //            // Configure detection system
                //            configureDetection();
                //            // Configure monitoring system
                //            configureMonitor();
                // Starting the sub-systems
                startCapture();
                // TODO: implement this method - first do configureProcessing
                startProcessing();
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
        // Loading defaults - processing settings
        proc_fft_size = Integer.parseInt(proc_fft_size_input.getHint().toString());
        proc_sample_rate = cap_sample_rate;
        proc_num_time_frames = model_clip_len * proc_sample_rate;
        proc_resolution = proc_sample_rate * proc_fft_size;
        proc_window_time = proc_fft_size / (proc_sample_rate * 1.0);
        proc_hop_time = proc_window_time / 2.0;

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
        // Processing Settings
        if (!TextUtils.isEmpty(proc_fft_size_input.getText())) {
            proc_fft_size = Integer.parseInt(proc_fft_size_input.getText().toString());
        }
        if (!TextUtils.isEmpty(proc_sample_rate_input.getText())) {
            proc_sample_rate = Integer.parseInt(proc_sample_rate_input.getText().toString());
        }
        proc_num_time_frames = model_clip_len * proc_sample_rate;
        proc_resolution = proc_sample_rate * proc_fft_size;
        proc_window_time = proc_fft_size / (proc_sample_rate * 1.0);
        proc_hop_time = proc_window_time / 2.0;

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
        // Processing
        proc_fft_size_input.setText(String.format("%d", proc_fft_size));
        proc_sample_rate_input.setText(String.format("%d", proc_sample_rate));
        proc_num_time_frames_input.setText(String.format("%d", proc_num_time_frames));
        proc_resolution_input.setText(String.format("%d", proc_resolution));
        proc_window_time_input.setText(String.format("%f", proc_window_time));
        proc_hop_time_input.setText(String.format("%f", proc_hop_time));
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
                }
                Log.v("AudioCapture", "Audio read thread has been closed.");
            }
        };
        // Audio Pre-Processing Runnable
        mt_audio_processing_runnable = new Runnable() {
            @Override
            public void run() {
                // Read data from the capture buffer and delete
                Log.v("AudioProcessing", "Audio pre-processing thread is starting...");
                // The infinite while loop is in preProcessing
                preProcessing();
                Log.v("AudioProcessing", "Audio pre-processing thread has been closed");
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
        // Resetting location of cap_queue
        cap_queue_loc = 0;
        // Minimum buffer size
        int min_buffer_size = AudioRecord.getMinBufferSize(cap_sample_rate, AUDIO_CHANNELS,
                cap_format);
        // Calculating buffer size
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
        // Set activity flag to true - as the system is starting
        mt_audio_capture_flag = true;
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
        num_read = audio_recorder.read(cap_buffer[cap_queue_loc], 0, cap_buffer_size);
        // Incrementing the buffer queue location
        cap_queue_loc++;
        Log.d("AudioCapture", String.format("Read from internal buffer:\n\tAmount read:%d," +
                "\n\tQueue location:%d", num_read, cap_queue_loc));
    }


    // Processing
    private void configureProcessing() {
        // TODO: THIS IS THE CURRENT PART YOU ARE WORKING ON
        // TODO: Need to setup the processing subsystem.
        //  this includes the:
        //  - processing user input stored in the capture buffer
        //  - so reading data from the buffer (its a queue)
        //  - processing the data through the various stages
        //  - creating a spectrogram of the data
        //  - outputting the spectrogram - THIS IS FOR TESTING, will maybe be in final but not sure
        //  - REMEMBER THIS METHOD IS FOR CONFIGURATION ONLY - maybe creation of secondary thread?.

        //
    }

    private void startProcessing(){
        // Set the activity flag to true as sub-system is starting
        mt_audio_processing_flag = true;

        // Start pre-processing thread
        new Thread(mt_audio_processing_runnable).start();
    }

    private void stopProcessing(){
        // Starting pre-processing thread
        mt_audio_processing_flag = false;
    }

    private void preProcessing(){

        // Finding window size
        int window_size = (int) Math.ceil(proc_window_time * proc_sample_rate);
        //  Finding hop size
        int hop_size = (int) Math.ceil(proc_hop_time * proc_sample_rate);
        //  Finding number of windows
        int num_windows = (int) Math.floor((cap_time_interval * proc_sample_rate * 1.0 -
                window_size) / hop_size)+1;
        //  Finding window coefficients for HANN window
        DenseMatrix window_coeff = window_func(window_size, HANN);
//        double [] window_coeff = window_func(window_size, HANN);

        //  Finding frequency range and melscale range for conversion
        DenseMatrix frequency_range = createArray(0, proc_sample_rate / 2.0, proc_resolution);
        DenseMatrix melscale_range = new DenseMatrix(frequency_range.rows, 0);
        melscale_range = melscale_range.div(1127).exp().sub(1).mul(700);

        while(mt_audio_processing_flag) {
            // Perform STFT
            stft(toDenseMatrix(proc_data), window_size, hop_size, num_windows, window_coeff,
                    frequency_range, melscale_range);
        }
    }

    private DenseMatrix window_func(int window_size, int window_type) {
        DenseMatrix ret = new DenseMatrix(window_size,0);

        // Implementation of the HANN window
        if (window_type == HANN){
            for(int i = 0; i < window_size; i++){
                ret.set(i,0, 0.5 * (1 - Math.cos(2 * Math.PI * i / window_size)));
            }
        }

        return ret;
    }

    private DenseMatrix stft(DenseMatrix data, int window_size, int hop_size, int num_windows,
                             DenseMatrix window_coeff, DenseMatrix frequency_range,
                             DenseMatrix melscale_range) {

        // TODO: Read the value from the capture buffer
        DenseMatrix window;
        DenseMatrixComplex spec_window;
        DenseMatrix real_spec_window;

        for (int i = 0; i < num_windows; i++) {
            // Extracting window from data
            window = getValuesInRange(data, i*hop_size, i * hop_size + window_size).mul(
                    window_coeff);
            // Calculating the FFT of the window
            spec_window = fft(window);
            // Use only half of the FFT output
            spec_window = getValuesInRange(spec_window, 0, spec_window.real().rows);
            // Finding absolute values of complex matrix, scaling
            real_spec_window = divComplexMatrix(spec_window, proc_fft_size/2).abs();
        }
        //  - Extract frame from data DONE
        //  - Calculating the FFT DONE
        //  - Formatting the FFT Restults DONE
        //      > half the FFT size DONE
        //      > scaling the FFT values DONE
        //  - Convert spectral data to melscale TODO
        //  - Append window to spectrogram TODO
        //  - Convert to dB with min value as reference. TODO

    # Retrieving frame from data // DONE
    frame = data[i*hop_size : i*hop_size + window_size] * window_coeff
    # Calculating the FFT // DONE
                spec_frame = fft(frame)
    # Using only half of the spectrogram // DONE
        spec_frame = spec_frame[:int(fft_size/2)]
    # Scaling the spectrogram values // DONE
        spec_frame = np.abs(spec_frame/(fft_size/2))

    # Converting spectrum to mel scale //TODO
        mel_frame = []
        mel_temp_frame_array = []
        mel_count = 0
        freq_count = 0

        while freq_count < int(fft_size/2):
            # Checking if in range
            if freq_range[freq_count] <= mel_range[mel_count]:
            mel_temp_frame_array.append(spec_frame[freq_count])
            freq_count += 1
            else:
            if len(mel_temp_frame_array) == 0:
                    # mel_frame.append(mel_frame[-1])
            mel_frame.append(spec_frame[freq_count])
                else:
            mel_frame.append(np.mean(mel_temp_frame_array))
            mel_temp_frame_array = []
            mel_count += 1
        mel_frame = np.abs(np.array(mel_frame)/(fft_size/2))

        return mel_frame
    }

    private DenseMatrixComplex fft(DenseMatrix data) {
        int size = data.getValues().length;
        if (size == 1) {
            return new DenseMatrixComplex(data, DenseMatrix.zeros(data.rows, 1));
        } else {
            DenseMatrixComplex data_even = fft(getEvenValues(data));
            DenseMatrixComplex data_odd = fft(getOddValues(data));

            DenseMatrix factor_real = new DenseMatrix(size, 1);
            DenseMatrix factor_imag = new DenseMatrix(size, 1);
            for (int i = 0; i < size; i++){
                double theta = -2 * Math.PI * i / size;
                factor_real.set(i, Math.cos(theta));
                factor_imag.set(i, Math.sin(theta));
            }
            DenseMatrixComplex factor = new DenseMatrixComplex(factor_real, factor_imag);

            DenseMatrixComplex ret1 = addComplexMatrix(mulComplexMatrix(getValuesInRange(factor, 0, size/2),data_odd),data_even);
            DenseMatrixComplex ret2 = addComplexMatrix(mulComplexMatrix(getValuesInRange(factor, size/2, size),data_odd),data_even);
            return concatComplexMatrix(ret1, ret2, 0);
        }
    }


    // Detection


    // Result display
    private void tempTestPlot(ImageView image){
        // Setting the matrix size
        int num_freq_bins = 128;
        int num_time_frames = 500;
        // Creating the matrix
        DenseMatrix spec_matrix = DenseMatrix.zeros(num_freq_bins, num_time_frames);

        spec_matrix.set(100,50,0.5);
        spec_matrix.set(100,51,1);

        // Normalise the matrix
        double temp_max = 0;
        for (int i = 0; i < num_freq_bins; i++){
            for (int j = 0; j < num_time_frames; j++){
                temp_max = Math.max(spec_matrix.get(i,j), temp_max);
            }
        }
//        spec_matrix = spec_matrix.div(temp_max);

        SpectrogramView sp_view_obj = new SpectrogramView(this, spec_matrix, image.getWidth());
        image.setImageBitmap(sp_view_obj.bmp);
    }

    // Data handling


    // Helpers
    /*
    * Creates a DenseMatrix, with values inbetween min (inclusive) and max (exclusive), with steps
    * of size.
    */
    private DenseMatrix createArray(double min, double max, int step){
        int size = (int) Math.ceil((max - min) / step);
        DenseMatrix temp = new DenseMatrix(size, 0);

        for (int i = 0; i < size; i++){
            temp.set(i,0,(step * i) + min);
        }

        return temp;
    }

    /*
    * Returns a portion of a DenseMatrix, the range is inclusive to min, and exclusive to max.
    */
    private DenseMatrix getValuesInRange(DenseMatrix array, int min, int max) {
        DenseMatrix ret = new DenseMatrix(max - min, 0);
        int iter = 0;
        for(int i = min; i < max; i++){
            ret.set(iter, array.get(i, 0));
            iter++;
        }
        return ret;
    }
    private DenseMatrixComplex getValuesInRange(DenseMatrixComplex array, int min, int max){
        DenseMatrix ret_real = getValuesInRange(array.real(), min, max);
        DenseMatrix ret_imag = getValuesInRange(array.imag(), min, max);
        DenseMatrixComplex ret = new DenseMatrixComplex(ret_real, ret_imag);
        return ret;
    }

    /*
    * Converts an array/matrix into a DenseMatrix
    */
    private DenseMatrix toDenseMatrix(int[] input) {
        // Finding the size of the array
        int size = input.length;
        // Creating the DenseMatrix
        DenseMatrix ret = new DenseMatrix(size, 0);
        // Filling the DenseMatrix
        for(int i = 0; i < size; i++){
            ret.set(i, input[i]);
        }

        return ret;
    }
    private DenseMatrix toDenseMatrix(double[] input) {
        // Finding the size of the array
        int size = input.length;
        // Creating the DenseMatrix
        DenseMatrix ret = new DenseMatrix(size, 0);
        // Filling the DenseMatrix
        for(int i = 0; i < size; i++){
            ret.set(i, input[i]);
        }

        return ret;
    }

    /*
    * Gets the even/odd index values from an array
    */
    private DenseMatrix getEvenValues(DenseMatrix array) {
        DenseMatrix ret;
        int length = array.getValues().length;
        if (length % 2 == 0){
            ret = new DenseMatrix((int)(length/2), 1);
        } else {
            ret = new DenseMatrix((int) Math.ceil(length/2.0), 1);
        }

        // TODO: change to add by two each iter and have checks
        int iter = 0;
        for (int i = 0; i < array.getValues().length; i=i+2){
            ret.set(iter, array.get(i,0));
            iter++;
        }

        return ret;
    }
    private DenseMatrix getOddValues(DenseMatrix array) {
        DenseMatrix ret;
        int length = array.getValues().length;
        if (length % 2 == 0){
            ret = new DenseMatrix((int)(length/2), 0);
        } else {
            ret = new DenseMatrix((int) Math.floor(length/2.0), 0);
        }

        // TODO: change to add by two each iter and have checks
        int iter = 0;
        for (int i = 1; i < array.getValues().length; i=i+2){
                ret.set(iter, array.get(i,0));
                iter++;
        }

        return ret;
    }

    /*
    * Perform operations on one or between two Dense Complex Matrices
    */
    private DenseMatrixComplex addComplexMatrix(DenseMatrixComplex arr1,
                                                     DenseMatrixComplex arr2) {
        DenseMatrix real = arr1.real().add(arr2.real());
        DenseMatrix imag = arr1.imag().add(arr2.imag());

        return new DenseMatrixComplex(real, imag);
    }
    private DenseMatrixComplex mulComplexMatrix(DenseMatrixComplex arr1,
                                                     DenseMatrixComplex arr2) {
        DenseMatrix real1 = arr1.real();
        DenseMatrix imag1 = arr1.imag();
        DenseMatrix real2 = arr2.real();
        DenseMatrix imag2 = arr2.imag();

        // Convert from rectangular form to polar and multiply
        DenseMatrix r = ((real1.pow(2).add(imag1.pow(2))).sqrt()).mul((real2.pow(2).add(
                imag2.pow(2))).sqrt());
        DenseMatrix theta = (tanhArray(imag1.div(real1))).add(tanhArray(imag2.div(real2)));

        // Convert back to rectangular form and combine
        DenseMatrix ret_real = new DenseMatrix(real1.rows, real1.cols);
        DenseMatrix ret_imag = new DenseMatrix(real1.rows, real1.cols);
        for (int i = 0; i < r.getValues().length; i++) {
            ret_real.set(i, r.getValues()[i] * Math.cos(theta.getValues()[i]));
            ret_imag.set(i, r.getValues()[i] * Math.sin(theta.getValues()[i]));
        }
        return new DenseMatrixComplex(ret_real, ret_imag);

    }
    private DenseMatrixComplex divComplexMatrix(DenseMatrixComplex array, int scalar) {
        DenseMatrix real = array.real().div(scalar);
        DenseMatrix imag = array.imag().div(scalar);
        return new DenseMatrixComplex(real, imag);
    }
    private DenseMatrix tanhArray(DenseMatrix array) {
        DenseMatrix ret = new DenseMatrix(array.rows, array.cols);
        for (int i = 0; i < array.getValues().length; i++){
            ret.set(i, Math.tanh(array.getValues()[i]));
        }

        return ret;
    }
    private DenseMatrixComplex concatComplexMatrix(DenseMatrixComplex mat1, DenseMatrixComplex mat2,
                                                   int axis) {
        DenseMatrix real;
        DenseMatrix imag;
        // Determine which axis to concatenate upon
        if (axis == 0){
            real = new DenseMatrix(mat1.real().rows + mat2.real().rows, mat1.real().cols);
            imag = new DenseMatrix(mat1.imag().rows + mat2.imag().rows, mat1.imag().cols);
            int iter = 0;
            for (int j = 0; j < mat1.real().cols; j++) {
                for (int i = 0; i < mat1.real().rows + mat2.real().rows; i++) {
                    if (i < mat1.real().rows){
                        real.set(iter, mat1.getReal(i, j));
                        imag.set(iter, mat1.getImag(i, j));
                    } else {
                        real.set(iter, mat2.getReal(i, j));
                        imag.set(iter, mat2.getImag(i, j));
                    }
                    iter++;
                }
            }
        } else {
            real = new DenseMatrix(mat1.real().rows, mat1.real().cols + mat2.real().cols);
            imag = new DenseMatrix(mat1.imag().rows, mat1.imag().cols + mat2.imag().cols);
            int iter = 0;
            for (int j = 0; j < mat1.real().cols + mat2.real().cols; j++) {
                for (int i = 0; i < mat1.real().rows; i++) {
                    if (j < mat1.real().cols){
                        real.set(iter, mat1.getReal(i, j));
                        imag.set(iter, mat1.getImag(i, j));
                    } else {
                        real.set(iter, mat2.getReal(i, j));
                        imag.set(iter, mat2.getImag(i, j));
                    }
                    iter++;
                }
            }
        }
        return new DenseMatrixComplex(real, imag);
    }
}