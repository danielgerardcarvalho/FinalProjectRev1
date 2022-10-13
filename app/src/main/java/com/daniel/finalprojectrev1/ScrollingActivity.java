package com.daniel.finalprojectrev1;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.daniel.finalprojectrev1.databinding.ActivityScrollingBinding;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class ScrollingActivity extends AppCompatActivity {

    private ActivityScrollingBinding binding;

    // Load C++ library into program
    static {
        System.loadLibrary("finalprojectrev1");
    }

    /* General Operation */
    //TODO: maybe change some of these to individual sub-systems later?
    private boolean system_flag;
    private final double MIN_CONST = Math.pow(10, -20);

    /* Audio Capture */
    // audio capture object
    private AudioRecord audio_recorder;
    // audio capture multi-threading
    private Thread mt_capture_thread;
    private Runnable mt_audio_capture_runnable;
    private Runnable mt_file_capture_runnable;
    private boolean mt_audio_capture_flag;
    private ArrayList<byte[]> cap_buffer;
    private boolean cap_first_run;


    /* Audio Pre-Processing */
    // processing multi-threading
    private Thread mt_processing_thread;
    private Runnable mt_audio_processing_runnable;
    private boolean mt_audio_processing_flag;
    private short [] proc_data;
    private ArrayList<double[][]> proc_buffer;

    /* Classifier */
    // classifier multi-thread
    private Thread mt_classifier_thread;
    private Runnable mt_classifier_runnable;            // multi-thread handler (runnable)
    private boolean mt_classifier_flag;                 // multi-thread status flag
    private double[][] classifier_data_prev;      // classifier input (previous) used for plotting
    private double[][] classifier_data;           // classifier input
    private ArrayList<double[][]> classifier_buffer;   // classifier output buffer


    /* Plotting */
    private AnnotatedTimeline annotated_plot;           // TESTING - plot with axis
    private ArrayList<ImageView> plotting_image_views;  // list of active ui interfaces
    private ArrayList<TextView> plotting_text_views;    // list if active ui text views
    private TextView progress_indicator_view;           // text view for progress indicator

    // plotting multi-thread
    private Thread mt_plotting_thread;
    private Runnable mt_plotting_runnable;              // multi-thread handler (runnable)
    private boolean mt_plotting_flag;                   // multi-thread status flag
    private int plotting_update_interval;
    private boolean capture_plotting_flag;
    private boolean processing_plotting_flag;
    private boolean classifier_plotting_flag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityScrollingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle(getTitle());

        /* Basic User Settings */
        EditText cap_time_interval_input = (EditText) findViewById(R.id.input_cap_time_interval);
        EditText classifier_num_iters_input = (EditText) findViewById(R.id.input_classifier_num_iters);

        /* Configure Runnables */
        configureRunnables();

        /* Floating Action Button Logic */
        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(view -> {

            // Check if already running
            if (system_flag){
                // UI displays
                Snackbar.make(view, "Stopping system", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                // Change icon image
                fab.setImageResource(android.R.drawable.ic_media_play);

                // Stop plotting thread
                if (mt_plotting_flag) {
                    stopPlotting();
                }
                // Stop classifier thread
                if (mt_classifier_flag) {
                    stopClassifier();
                }
                // Stop processing thread
                if (mt_audio_processing_flag) {
                    stopProcessing();
                }
                // Stop audio capture thread
                if (mt_audio_capture_flag) {
                    stopCapture();
                }
                system_flag = false;

            } else {
                // Update Settings
                updateSettings(cap_time_interval_input, classifier_num_iters_input);

                // UI displays
                Snackbar.make(view, "Starting system", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                // Change icon image
                fab.setImageResource(android.R.drawable.ic_media_pause);

                /* Starting the system */
                // Configuring and starting the capture sub-system
                configureCapture();
                startCapture();
                // Configure processing system
                configureProcessing();
                // Configure classifier system
                configureClassifier();

                // Starting the sub-system threads
//                startCapture();
                startProcessing();
                startClassifier();

                // Start plotting sub-system
                capture_plotting_flag = false;
                processing_plotting_flag = true;
                classifier_plotting_flag = true;
                configurePlotting();

                system_flag = true;
            }
        });

        /* UI Update */
        if (Globals.sys_settings_flag) {
            uiFieldUpdate(cap_time_interval_input, classifier_num_iters_input);
        }

        /* Checking application permissions */
        checkAllPermissions();
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
            Log.e("SettingsButton", "Clicked");
            Intent intent = new Intent(ScrollingActivity.this, SettingsActivity.class);
            startActivity(intent);
//            setContentView(R.layout.settings_scrolling);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void configureRunnables(){

        // Audio Capture Runnable
        mt_audio_capture_runnable = () -> {
            // Read data from the buffer when the buffer is full
            Log.v("AudioCapture", "Audio read thread is starting...");
            while(mt_audio_capture_flag){
                Log.v("AudioCapture", "Audio read thread is active...");
                readAudioCaptureBuffer();
            }
            Log.v("AudioCapture", "Audio read thread has been closed.");
        };
        // File Capture Runnable
        mt_file_capture_runnable = () -> {
            Log.v("FileCapture", "File read thread is starting...");
            while (mt_audio_capture_flag){
                Log.v("FileCapture", "File read thread is active...");
                readFileCaptureBuffer();
            }
            Log.v("FileCapture", "File read thread has been closed.");
        };
        // Audio Pre-Processing Runnable
        mt_audio_processing_runnable = () -> {
            // Read data from the capture buffer and delete
            Log.v("AudioProcessing", "Audio pre-processing thread is starting...");
            // The infinite while loop is in preProcessing
            preProcessing();
            Log.v("AudioProcessing", "Audio pre-processing thread has been closed.");
        };
        // Classifier Runnable
        mt_classifier_runnable = () -> {
            // Read data from the processing buffer and delete
            Log.v("Classifier", "Classifier thread is starting...");
            // The infinite while loop in in classifier
            try{classifier();}
            catch(Exception e){
                e.printStackTrace();
            }
            Log.v("Classifier", "Classifier thread has been closed.");
        };
        // Plotting Runnable
        mt_plotting_runnable = () -> {
            Log.v("Plotting", "Plotting thread is starting...");
            while(mt_plotting_flag) {
                try {
                    Thread.sleep(plotting_update_interval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                plotting();
            }
            Log.v("Plotting", "Plotting thread has been closed");
        };
    }


    // Settings
    private void uiFieldUpdate(EditText cap_time_interval_input, EditText classifier_num_iters_input){
        cap_time_interval_input.setText(String.format("%d", Globals.cap_time_interval));
        classifier_num_iters_input.setText(String.format("%d", Globals.classifier_num_iters));
    }

    private void updateSettings(EditText cap_time_interval_input, EditText classifier_num_iters_input){
        // Capture time interval
        if (!TextUtils.isEmpty(cap_time_interval_input.getText())) {
            Globals.cap_time_interval = Integer.parseInt(cap_time_interval_input.getText().toString());
        } else {
            Globals.cap_time_interval = Globals.model_clip_len;
        }
        // Classifier number of internal iterations
        if (!TextUtils.isEmpty(classifier_num_iters_input.getText())) {
            Globals.classifier_num_iters = Integer.parseInt(classifier_num_iters_input.getText().toString());
        } else {
            Globals.classifier_num_iters = Integer.parseInt(classifier_num_iters_input.getHint().toString());
        }
    }


    // Permissions
    private void checkAllPermissions(){
        // Checking for required permissions
        ArrayList<String> permissions = new ArrayList<>();
        // Collect required and missing permissions in permissions array
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        }
        // Convert to String array
        String[] temp = new String[permissions.size()];
        temp = permissions.toArray(temp);
        // Request permissions
        for (int i = 0; i < temp.length; i++) {
            ActivityCompat.requestPermissions(this, temp, 0);
        }
        // Special Permissions
        if (! Environment.isExternalStorageManager()){
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", this.getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
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


    // Capture
    private void configureCapture() {
        /* Configures capture variables before the start of any sub-systems*/
        Log.v("Capture", "Starting configCapture");
        // Reset first run flag
        cap_first_run = true;
        // Minimum buffer size
        int min_buffer_size = AudioRecord.getMinBufferSize(Globals.cap_sample_rate, Globals.AUDIO_CHANNELS,
                Globals.cap_format);
        // Calculating buffer size
        int multiplier = 2;
        if (Globals.cap_format == Globals.AUDIO_FORMAT_INT8){
            multiplier = 1;
        } else if (Globals.cap_format == Globals.AUDIO_FORMAT_INT16){
            multiplier = 2;
        } else if (Globals.cap_format == Globals.AUDIO_FORMAT_FLOAT) {
            multiplier = 4;
        }
        Globals.cap_buffer_size = (int) Globals.cap_sample_rate * Globals.cap_time_interval * (multiplier/*cap_format /
                AUDIO_MIN_FORMAT_SIZE*/);

        // Checking if cap_buffer_size is large enough
        if (Globals.cap_buffer_size < min_buffer_size){
            Log.e("AudioCapture", String.format("cap_buffer_size: is too small\n\tcurrent:%d" +
                    "\n\trequired:%d", Globals.cap_buffer_size, min_buffer_size));
            Globals.cap_buffer_size = min_buffer_size;
        }
        // Creating audio record object
        checkPermission(this, this, Manifest.permission.RECORD_AUDIO);
        audio_recorder = new AudioRecord(Globals.AUDIO_SOURCES[1], Globals.cap_sample_rate, Globals.AUDIO_CHANNELS,
                Globals.cap_format, Globals.cap_buffer_size);
        // Buffer clearing / initialisation
        cap_buffer = new ArrayList<>();
        if (Globals.cap_file_import_flag){
            // Convert the File to input stream
            try {
                Globals.cap_imported_file_stream = new FileInputStream(Globals.cap_imported_file);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void startCapture(){
        /* Finalises configuration and starts sub-system */
        Log.v("Capture", "Starting startCapture");
        // Set activity flag to true - as the system is starting
        mt_audio_capture_flag = true;

        // Check if file is imported
        if (Globals.cap_file_import_flag){
            Log.v("Capture", "File mode is active");
            new Thread(mt_file_capture_runnable).start();
            return;
        }

        // Check if audio interface is in appropriate state
        if (audio_recorder.getState() != AudioRecord.STATE_INITIALIZED){
            Log.e("AudioCapture", "Audio interface is in incorrect state: desired:" +
                    "initialised");
            return;
        }
        Log.v("Capture", "Audio mode is active");
        // Starting recording interface
        audio_recorder.startRecording();
        // Starting recording read thread
        mt_classifier_thread = new Thread(mt_audio_capture_runnable);
        mt_classifier_thread.start();
    }

    private void stopCapture(){
        Log.v("Capture", "Starting stopCapture");
        // Stopping recording read thread
        mt_audio_capture_flag = false;

        // Clearing file capture data
        Globals.cap_imported_file = null;
        Globals.cap_imported_file_stream = null;
        // Check if file import was used
        if (Globals.cap_file_import_flag){
            // Stopping file reading interface
            // Reset flags and clear files
            Globals.cap_file_import_flag = false;
            return;
        }

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

        // Stopping recording interface
        audio_recorder.stop();
        audio_recorder.release();
        audio_recorder = null;
    }

    private void readAudioCaptureBuffer(){
        // Clear the input buffer with one byte read
        byte [] temp = new byte[Globals.cap_buffer_size];
        if (cap_buffer.size() == 0 && cap_first_run){
            // This read is only to clear garbage data and is only run of first iteration
            audio_recorder.read(temp, 0, 1);
            cap_first_run = false;
        }
        // Override garbage read with real read of AudioRecord buffer
        int num_read = audio_recorder.read(temp, 0, Globals.cap_buffer_size);

        // Add read data to capture buffer
        while (cap_buffer.size() == Globals.CAP_QUEUE_SIZE){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!mt_audio_capture_flag){
                break;
            }
        }
        cap_buffer.add(temp);
    }

    private void readFileCaptureBuffer(){
        // Clear first run flag
        cap_first_run = false;
        // Read data from the file input stream
        int num_read = 0;
        byte[] temp = new byte[Globals.cap_buffer_size];
        try {
            num_read = Globals.cap_imported_file_stream.read(temp, 0,
                    Globals.cap_buffer_size);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (num_read == -1){
            Log.v("FileCapture", "Read entire file, ending read loop.");
            stopCapture();
        }
        while (cap_buffer.size() == Globals.CAP_QUEUE_SIZE){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!mt_audio_capture_flag){
                break;
            }
        }
        cap_buffer.add(temp);
    }


    // Processing
    private void configureProcessing() {
        Log.v("Processing", "Starting configProcessing");
        /* Configures processing variables before the start of any sub-systems*/
        // Input variable initialisation
        proc_data = null;
        // Output buffer clearing / initialisation
        proc_buffer = new ArrayList<>();
    }

    private void startProcessing(){
        Log.v("Processing", "Starting startProcessing");
        /* Finalises configuration and starts sub-system */

        // Set the activity flag to true as sub-system is starting
        mt_audio_processing_flag = true;
        // Start pre-processing thread
        new Thread(mt_audio_processing_runnable).start();
    }

    private void stopProcessing(){
        Log.v("Processing", "Starting stopProcessing");
        // Stopping pre-processing thread
        mt_audio_processing_flag = false;
    }

    private void preProcessing(){
        // Finding window size
        int window_size = (int) Math.ceil(Globals.proc_window_time * Globals.proc_sample_rate);
        //  Finding hop size
        int hop_size = (int) Math.ceil(Globals.proc_hop_time * Globals.proc_sample_rate);
        //  Finding number of windows
        int num_windows = (int) Math.floor((Globals.cap_time_interval * Globals.proc_sample_rate * 1.0 -
                window_size) / hop_size)+1;
        //  Finding window coefficients for HANN window
        double [] window_coeff = window_func(window_size, Globals.HANN);
        // Pre-calculating the fft twiddle factor values
        FFT fft = new FFT(Globals.proc_fft_size);

        // TODO: (MEL) decide fate of mel-spectrogram things, current implementation is wrong, not
        //  sure if mel-spectrum is needed at all. (removed in mean-time) - WILL NEED CONVERSION ojalgo
//        //  Finding frequency range and melscale range for conversion
//        Primitive64Store frequency_range = createArray(0, proc_sample_rate / 2.0, proc_resolution);
//        Primitive64Store melscale_range = (((toPrimitive(frequency_range.divide(1127)).exp()).sub(1)).mul(700));

        // Thread infinite loop
        while(mt_audio_processing_flag) {
            // Wait if no value is available in the buffer
            while (cap_buffer == null || cap_buffer.size() == 0){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!mt_audio_processing_flag){
                    return;
                }
            }
            // Fetch data from the capture buffer queue
            byte[] temp = cap_buffer.remove(0);
            // Converting from bytes to respective capture format
            if (Globals.cap_format == Globals.AUDIO_FORMAT_INT16) {
                proc_data = bytesToShort(temp, Globals.cap_format);
            } else if (Globals.cap_format == Globals.AUDIO_FORMAT_FLOAT) {
                proc_data = bytesToFloatShort(temp, Globals.cap_format);
            } else {
                proc_data = bytesToShort(temp, Globals.cap_format);
            }
            // Converting from bytes to short
//            proc_data = bytesToShort(temp, cap_format);
            // Normalising the capture data
            double[] norm_proc_data = norm(proc_data, max(abs(proc_data)));
            Log.v("AudioProcessing", String.format("Processing iput:" +
                    "\n\tInput buffer size: %d" +
                    "\n\tByte converted input buffer size: %d" +
                    "\n\tinput buffer max:%d, min:%f" +
                    "\n\tnorm buffer max:%f, min:%f", Globals.cap_buffer_size, proc_data.length,
                    max(proc_data), min(mul(proc_data, 1.0)), max(norm_proc_data), min(norm_proc_data)));

            // Perform STFT
            // TODO: (MEL) removed in mean-time
            double[][] stft = stft(norm_proc_data, window_size, hop_size, num_windows,
                    window_coeff, fft/*, frequency_range, melscale_range*/);
            // Check if processing buffer is full
            while (proc_buffer.size() == Globals.PROC_QUEUE_SIZE){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!mt_audio_processing_flag){
                    break;
                }
            }
            proc_buffer.add(stft);
        }
    }

    private double [] window_func(int window_size, int window_type) {
        double [] ret = new double[window_size];

        // Implementation of the HANN window
        if (window_type == Globals.HANN){
            for(int i = 0; i < window_size; i++){
                ret[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / window_size));
            }
        }

        return ret;
    }

    // TODO: (MEL) removed in mean-time.
    private double[][] stft(double[] data, int window_size, int hop_size, int num_windows,
                             double[] window_coeff, FFT fft/*, Primitive64Store frequency_range,
                             Primitive64Store melscale_range*/) {
        double [] window;
        double [] spec_window_real;
        double [] spec_window_imag;
        double[][] spectrogram;
        double [][] temp_spectrogram = new double[num_windows][Globals.proc_fft_size/2];

        for (int i = 0; i < num_windows; i++) {
            // Extracting window from data
            window = mul(getValuesInRange(data, i*hop_size, i * hop_size + window_size), window_coeff);
            // Calculating the FFT of the window
            fft.fft(window);
            // Use only half of the FFT output
            spec_window_real = getValuesInRange(fft.getFFTReal(), 0, Globals.proc_fft_size/2);
            spec_window_imag = getValuesInRange(fft.getFFTImag(), 0, Globals.proc_fft_size/2);
            // Finding absolute values of complex matrix, scaling
            double temp_scaler = Globals.proc_fft_size/2;
            // TODO: Try and remove
            double [] temp_real_spec_window = abs(divide(spec_window_real, temp_scaler), divide(spec_window_imag, temp_scaler));
//            double [] temp_real_spec_window = abs(spec_window_real, spec_window_imag);
//            temp_real_spec_window = divide(temp_real_spec_window, temp_scaler);
            // Adding the window to the spectrogram
            for (int j = 0; j < Globals.proc_fft_size/2; j++) {
                temp_spectrogram[i][j] = temp_real_spec_window[j];
            }
            // TODO: (MEL) removed in mean-time - NEEDS CONVERSION IF IMPLEMENTED AGAIN ojalgo
//            // Converting spectrum to melscale
//            double [] temp = specToMel(real_spec_window, frequency_range, melscale_range);
//            if (i == 0) {
//                mel_window = new DenseMatrix(temp.length, num_windows);
//            }
//            for (int j = 0; j < temp.length; j++) {
//                mel_window.set(j, i, temp[j]);
//            }
        }
        // Converting from double[][] to Primitive64Storage
//        spectrogram = createMatrix(temp_spectrogram);

        // Scaling the spectrogram
        spectrogram = pow(temp_spectrogram, 2.0);
        // Converting from amplitude to dB and transposing
        double reference = min(abs(spectrogram));
        spectrogram = transpose(ampToDB(spectrogram, reference));

        // Scaling the output to fit between 80 and MIN_CONST
        spectrogram = scale(spectrogram, MIN_CONST, 80);

        Log.v("AudioProcessing", String.format("STFT Summary:" +
                        "\n\tinput fft size (rows):\t\t%d" +
                        "\n\tinput num frames (cols):\t%d" +
                        "\n\toutput fft size (rows):\t\t%d" +
                        "\n\toutput num frames (cols):\t%d" +
                        "\nPre-calculated output sizes" +
                        "\n\tfft size (proc_fft_size/2):\t%d" +
                        "\n\tnum windows (calculated):\t%d" +
                        "\nValues of STFT" +
                        "\n\tmax value:\t\t%f" +
                        "\n\tmin value:\t\t%f", Globals.proc_fft_size, Globals.proc_num_time_frames,
                spectrogram.length, spectrogram[0].length, Globals.proc_fft_size/2, num_windows,
                max(spectrogram), min(spectrogram)));

        return spectrogram;
    }

    // TODO: currently this fft method in not used (Deprecated for use of FFT class) - NEEDS CONVERSION IF USED AGAIN, ojalgo
//    private DenseMatrixComplex fft(DenseMatrix data) {
//        int size = data.getValues().length;
//        if (size == 2) {
//            DenseMatrix temp = toDenseMatrix(new double []{data.getValues()[0] + data.getValues()[1], data.getValues()[0] - data.getValues()[1]});
//            return new DenseMatrixComplex(temp, DenseMatrix.zeros(temp.rows, 1));
//        } else {
//            DenseMatrixComplex data_even = fft(getEvenValues(data));
//            DenseMatrixComplex data_odd = fft(getOddValues(data));
//
//            DenseMatrix factor_real = new DenseMatrix(size, 1);
//            DenseMatrix factor_imag = new DenseMatrix(size, 1);
//
//            for (int i = 0; i < size; i++){
//                double theta = -2 * Math.PI * i / size;
//                factor_real.set(i, Math.cos(theta));
//                factor_imag.set(i, Math.sin(theta));
//            }
//            DenseMatrixComplex factor = new DenseMatrixComplex(factor_real, factor_imag);
//
//            DenseMatrixComplex ret1 = addComplexMatrix(data_even, mulComplexMatrix(getValuesInRange(factor, 0, size/2),data_odd));
//            DenseMatrixComplex ret2 = addComplexMatrix(data_even, mulComplexMatrix(getValuesInRange(factor, size/2, size),data_odd));
//
//            return concatComplexMatrix(ret1, ret2, 0);
//        }
//    }


    // TODO: currently this mel conversion method is not used (Deprecated as mel scale is not used), NEEDS CONVERSION IF USED AGAIN, ojalgo
    // Converts frequency spectrum to melscale
//    private double[] specToMel(DenseMatrix array, DenseMatrix freq_range, DenseMatrix mel_range){
//        int freq_count = 0;
//        int mel_count = 0;
//
//        int mel_temp_avg_iter = 0;
//        // TODO: Change these to array lists to improve space use and remove size quess work
//        double mel_temp_avg_array = 0.0;
////        DenseMatrix mel_temp_avg_array = new DenseMatrix(proc_fft_size, 1);
//
////        int mel_scale_spectrum_iter = 0;
//        // TODO: change this to arraylist as well
//        ArrayList<Double> mel_scale_spectrum = new ArrayList<>();
////        DenseMatrix mel_scale_spectrum = new DenseMatrix(proc_fft_size, 1);
//
//        while (freq_count < (int) (Globals.proc_fft_size/2)) {
//            // Checking if in range
//            if (freq_range.getValues()[freq_count] <= mel_range.getValues()[mel_count]) {
//                mel_temp_avg_array = mel_temp_avg_array + array.getValues()[freq_count];
////                mel_temp_avg_array.set(mel_temp_avg_iter, array.getValues()[freq_count]);
//                mel_temp_avg_iter++;
//                freq_count++;
//            } else {
//                if (mel_temp_avg_iter == 0) {
//                    mel_scale_spectrum.add(array.getValues()[freq_count]);
////                    mel_scale_spectrum.set(mel_scale_spectrum_iter, array.getValues()[freq_count]);
//                } else if (mel_temp_avg_iter == 1) {
//                    mel_scale_spectrum.add(mel_temp_avg_array);
////                    mel_scale_spectrum.set(mel_scale_spectrum_iter,
////                            mel_temp_avg_array.getValues()[0]);
//                    mel_temp_avg_array = 0.0;
//                } else {
//                    mel_scale_spectrum.add(mel_temp_avg_array/(mel_temp_avg_iter*1.0));
////                    mel_scale_spectrum.set(mel_scale_spectrum_iter, getMean(mel_temp_avg_array,
////                            mel_temp_avg_iter));
//                    mel_temp_avg_array = 0.0;
//
//                }
//                mel_count++;
//                mel_temp_avg_iter = 0;
////                mel_scale_spectrum_iter++;
//            }
//        }
//        double[] ret = new double[mel_scale_spectrum.size()];
//        for (int i = 0; i < mel_scale_spectrum.size(); i++){
//            ret[i] = mel_scale_spectrum.get(i);
//        }
//        return ret;
//    }
//

    // Classifier
    private void configureClassifier() {
        Log.v("Classifier", "Starting configClassifier");
        /* Configures classifier variables before the start of any sub-systems*/
        // Input variable clearing / initialisation
        classifier_data = null;
        classifier_data_prev = null;
        // Buffer clearing / initialisation
        classifier_buffer = new ArrayList<>();
    }

    private void startClassifier() {
        Log.v("Classifier", "Starting startClassifier");
        /* Finalises configuration and starts sub-system */
        // Set the activity flag to true as sub-system is starting
        mt_classifier_flag = true;
        // Start pre-processing thread
        mt_classifier_thread = new Thread(mt_classifier_runnable);
        mt_classifier_thread.start();
    }

    private void stopClassifier() {
        Log.v("Classifier", "Starting stopClassifier");
        // Stopping classifier thread
        mt_classifier_flag = false;
        mt_classifier_thread.interrupt();
    }

    private void classifier() {
        // Calculating number of time frames
        int num_windows = (int) (Math.floor((Globals.cap_time_interval * (Globals.proc_sample_rate * 1.0) -
                (int) Math.ceil(Globals.proc_window_time * Globals.proc_sample_rate)) /
                (int) Math.ceil(Globals.proc_hop_time * Globals.proc_sample_rate))+1);
        // Initialising the NMF class
        NMF nmf = new NMF(
                Globals.classifier_fft_size,
                num_windows,
                Globals.classifier_num_classes,
                Globals.classifier_num_inter_comp,
                Globals.classifier_num_iters
        );
        nmf.setProgressUpdateVars(this, progress_indicator_view);
        // Loading Dictionaries
        Log.v("Classifier", "Loading imported model dictionaries...");
        nmf.loadW1(Globals.classifier_imported_nmf_model.W1);
        nmf.loadW2(Globals.classifier_imported_nmf_model.W2);
        nmf.loadTrainingError(Globals.classifier_imported_nmf_model.training_cost);

        while(mt_classifier_flag) {
//            Log.v("Classifier", "Classifier thread is active...");
            // Checking for data in the processing buffer
            while (proc_buffer == null || proc_buffer.size() == 0) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!mt_classifier_flag) {
                    return;
                }
            }
            // Save previous classifier input for plotting
            if (classifier_data != null){
                classifier_data_prev = classifier_data.clone();
            }
            else{
                classifier_data_prev = null;
            }
            // Read newest data from the processing buffer
            classifier_data = proc_buffer.remove(0);
            // Loading the data into the nmf class object
            nmf.loadV1(classifier_data);
            // Starting nmf calculation
            try{
                nmf.start();
            } catch (InterruptedException e){
                e.printStackTrace();
                Log.v("Classifier", "Interrupt Occured, Stopping Thread");
                stopClassifier();
                return;
            }
            // Retrieving results from nmf class object
            double[][] V2_output = nmf.getV2();
            // Basic thresholding implementation
            double V2_mean = mean(V2_output);
            double norm_modifier = 1.8;
            for (int i = 0; i < V2_output.length; i++) {
                for (int j = 0; j < V2_output[0].length; j++){
                    if ((V2_mean * norm_modifier) >= V2_output[i][j]) {
                        V2_output[i][j] = 0;
                    } else {
                        V2_output[i][j] = 1;
                    }
                }
            }
            // Outlier Removal and Correction
            // Adding fillers for small gaps in annotations
            int min_len = 3;
            int curr_len = 0;
            boolean event_inactive = false;
            for (int i = 0; i < V2_output.length; i++){
                for (int j = 0; j < V2_output[i].length; j++) {
                    if (V2_output[i][j] == 0 && !event_inactive) {
                        event_inactive = true;
                        curr_len++;
                    }
                    else if (V2_output[i][j] == 0 && event_inactive) {
                            curr_len++;
                    }
                    else if (V2_output[i][j] == 1 && event_inactive) {
                        event_inactive = false;
                        if (curr_len < min_len) {
                            for (int k = 0; k < curr_len; k++) {
                                V2_output[i][j - curr_len + k] = 1;
                            }
                        }
                        curr_len = 0;
                    }
                }
            }
            // Removing outliers from annotations
            min_len = 3;
            curr_len = 0;
            boolean event_active = false;
            for (int i = 0; i < V2_output.length; i++) {
                for (int j = 0; j < V2_output[i].length; j++) {
                    if (V2_output[i][j] == 1 && !event_active) {
                        event_active = true;
                        curr_len++;
                    }
                    else if (V2_output[i][j] == 1 && event_active) {
                        curr_len++;
                    }
                    else if (V2_output[i][j] == 0 && event_active) {
                        event_active = false;
                        if (curr_len < min_len) {
                            for (int k = 0; k < curr_len; k++){
                                if (j - curr_len + k > V2_output[i].length ||
                                    j - curr_len + k < 0){
                                    continue;
                                }
                                V2_output[i][j - curr_len + k] = 0;
                            }
                        }
                        curr_len = 0;
                    }
                }
            }

            // Check if processing buffer is full
            while (classifier_buffer.size() == Globals.CLASSIFIER_QUEUE_SIZE){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!mt_classifier_flag){
                    break;
                }
            }
            // Add results to classifier buffer
            classifier_buffer.add(V2_output);
        }
    }


    // Result display
    // TODO: deprecated - moved to dedicated plotting sub system - remove
    private void tempTestPlot(ImageView image){
        // Setting the matrix size
        int num_freq_bins = 128;
        int num_time_frames = 500;
        // Creating the matrix
        double[][] spec_matrix = createMatrix(num_freq_bins, num_time_frames, 0.0);
        spec_matrix[100][50] = 0.5;
        spec_matrix[100][51] = 1;

        // Normalise the matrix

        double temp_max = max(spec_matrix);
        for (int i = 0; i < num_freq_bins; i++){
            for (int j = 0; j < num_time_frames; j++){
                temp_max = Math.max(spec_matrix[i][j], temp_max);
            }
        }
//        spec_matrix = spec_matrix.div(temp_max);

        BitMapView sp_view_obj = new BitMapView(this, spec_matrix, image.getWidth());
        image.setImageBitmap(sp_view_obj.bmp);
    }
    private void tempCapPlot(ImageView image){
        while (proc_data == null){}
        int width = Math.min(proc_data.length, 22050);
        int height = 900;
        int max = 0;
        for (int i = 0; i < width; i++){
            if (Math.abs(proc_data[i]) > max)
                max = Math.abs(proc_data[i]);
        }
        // create matrix from proc_data
        double[][] temp = createMatrix(height, Math.abs(width));

        int iter = (int)(-height/2.0);
        for (int i = 0; i < height; i++){
            for (int j = 0; j < width; j++){
                if ((Math.abs(proc_data[j]/(1.0*max))*(height/2.0)) >= Math.abs(iter)){
                    temp[i][j] = 1;
                } else {
                    temp[i][j] = 0;
                }
            }
            iter++;
        }

        BitMapView sp_view_obj = new BitMapView(this, temp, image.getWidth());
        image.setImageBitmap(sp_view_obj.bmp);
    }
    private void tempProcPlot(ImageView image) {
        // Extract spectrogram from the proc_buffer queue
        while (proc_buffer == null || proc_buffer.size() == 0) {}
        double[][] temp = proc_buffer.remove(0);
        double[][] invert_temp = createMatrix(temp.length, temp[0].length);// createMatrix(temp.countRows(), temp.countColumns());
        for (int i = temp.length-1; i >= 0; i--){
            for (int j = 0; j < temp[0].length; j++) {
                invert_temp[i][j]= temp[temp.length-(i+1)][j];
            }
        }
        Log.v("display", String.format("temp -rows:%d, -cols:%d, value:%f", invert_temp.length, invert_temp[0].length, invert_temp[0][0]));
        double[][] val = divide(invert_temp, max(invert_temp));
        Log.v("display1", String.format("temp -rows:%d, -cols:%d, value:%f", val.length, val[0].length, val[0][0]));
        BitMapView sp_view_obj = new BitMapView(this, val, image.getWidth());
        image.setImageBitmap(sp_view_obj.bmp);
    }
    private void tempClassifierPlot(ImageView image) {
        // Extract spectrogram from the classifier_buffer queue
        while (classifier_buffer == null || classifier_buffer.size() == 0) {}
        double[][] temp = classifier_buffer.remove(0);

        BitMapView sp_view_obj = new BitMapView(this, temp, image.getWidth());
        image.setImageBitmap(sp_view_obj.bmp);
    }

    // Data handling


    /* Plotting */
    private void configurePlotting() {
        Log.v("Plotting", "Starting configPlotting");
        // Configure the plotting sub-system
        plotting_image_views = new ArrayList<>();
        plotting_text_views = new ArrayList<>();
        // Enable the multi-thread flags
        mt_plotting_flag = true;
        // Configure update interval
        plotting_update_interval = 500;

        // TODO: implement requested plots and have live updates
        if (capture_plotting_flag){
            plotting_image_views.add(findViewById(R.id.imageView0));
            TextView temp = findViewById(R.id.view_plotting_time_domain);
            temp.setVisibility(TextView.VISIBLE);
        }
        if (processing_plotting_flag) {
            plotting_image_views.add(findViewById(R.id.imageView1));
            TextView temp = findViewById(R.id.view_plotting_spectrogram);
            temp.setVisibility(TextView.VISIBLE);

        }
        if (classifier_plotting_flag) {
            progress_indicator_view = findViewById(R.id.view_progress_indicator);
            TextView temp = findViewById(R.id.view_plotting_timeline);
            temp.setVisibility(TextView.VISIBLE);
//            plotting_image_views.add(findViewById(R.id.imageView2));
            annotated_plot = new AnnotatedTimeline(findViewById(R.id.plot));
        }

        // TODO: Start Plotting Thread Runnable
        new Thread(mt_plotting_runnable).start();
    }

    private void stopPlotting(){
        Log.v("Plotting", "Starting stopPlotting");
        mt_plotting_flag = false;
        capture_plotting_flag = false;
        processing_plotting_flag = false;
        classifier_plotting_flag = false;
    }

    private void plotting() {
        int curr_image_view = 0;
        // Time domain plot
        if (capture_plotting_flag && proc_data != null) {
            // Configure the plot
            short [] temp_data = proc_data.clone();
            int width = Math.min(proc_data.length, 22050);
            int height = 900;
            int max = 0;
            for (int i = 0; i < width; i++){
                if (Math.abs(temp_data[i]) > max)
                    max = Math.abs(temp_data[i]);
            }
            // create matrix from proc_data
            double[][] temp = createMatrix(height, Math.abs(width));

            int iter = (int)(-height/2.0);
            for (int i = 0; i < height; i++){
                for (int j = 0; j < width; j++){
                    if ((Math.abs(temp_data[j]/(1.0*max))*(height/2.0)) >= Math.abs(iter)){
                        temp[i][j] = 1;
                    } else {
                        temp[i][j] = 0;
                    }
                }
                iter++;
            }
            // Update the UI elements
            BitMapView sp_view_obj = new BitMapView(this, temp, plotting_image_views.get(curr_image_view).getWidth());
            plottingUpdate(sp_view_obj, curr_image_view);
            curr_image_view++;
        }
        // STFT plot
        if (processing_plotting_flag && classifier_data_prev != null) {
            double[][] temp = classifier_data_prev.clone();
            double[][] invert_temp = createMatrix(temp.length, temp[0].length);
            for (int i = temp.length-1; i >= 0; i--){
                for (int j = 0; j < temp[0].length; j++) {
                    invert_temp[i][j] = temp[temp.length-(i+1)][j];
                }
            }
//            Log.v("display", String.format("temp -rows:%d, -cols:%d, value:%f", invert_temp.countRows(), invert_temp.countColumns(), invert_temp.get(0)));
            // TODO: cleanup
            double[][] val = divide(invert_temp, max(invert_temp));
//            Log.v("display1", String.format("temp -rows:%d, -cols:%d, value:%f", val.countRows(), val.countColumns(), val.get(0)));
            BitMapView sp_view_obj = new BitMapView(this, val, plotting_image_views.get(curr_image_view).getWidth());
            plottingUpdate(sp_view_obj, curr_image_view);
            curr_image_view++;
        }
        // Classifier output plot
        if (classifier_plotting_flag && classifier_buffer != null && classifier_buffer.size() != 0) {
            double[][] temp = classifier_buffer.remove(0);
//            BitMapView sp_view_obj = new BitMapView(this, temp, plotting_image_views.get(curr_image_view).getWidth());
//            plottingUpdate(sp_view_obj, curr_image_view);
            // TODO: add the iterator the the output string
//            runOnUiThread(() -> progress_indicator_view.setText(getText(R.string.progress_indicator_string) + ));
            annotated_plot.updatePlot(this, temp);
        }
    }

    private void plottingUpdate(BitMapView bitmap_obj, int curr_image_view){
        runOnUiThread(() -> plotting_image_views.get(curr_image_view).setImageBitmap(bitmap_obj.bmp));
    }

    /* Helpers */

    // Returns a portion of a double [], the range is inclusive to min, and exclusive to max.
    private double [] getValuesInRange(double [] array, int min, int max) {
        double [] ret = new double [max - min];
        int iter = 0;
        for(int i = min; i < max; i++){
            ret[iter] = array[i];
            iter++;
        }
        return ret;
    }
    // Convert from amplitude values to dB values
    private double[][] ampToDB(double[][] matrix, double ref) {
        double[][] temp = mul(log(divide(matrix, ref), 10),20.0);
        return temp;
    }
    // Scale values in-between a maximum and minimum, thresholding the minimum
    private double[][] scale(double[][] matrix, double min, double max) {
        double threshold_max = max(matrix) - max;
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++){
                matrix[i][j] = Math.max(matrix[i][j] - threshold_max, MIN_CONST);
            }
        }
        return matrix;
    }

    // Convert from byte array to short []
    private short[] bytesToShort(byte[] array, int format) {
        short [] ret;
        if (format == Globals.AUDIO_FORMAT_INT8){
            ret = new short[(int)(array.length)];
        } else if (format == Globals.AUDIO_FORMAT_INT16) {
            ret = new short[(int) (array.length / 2)];
        } else if (format == Globals.AUDIO_FORMAT_FLOAT) {
            ret = new short[(int) (array.length / 4)];
        } else {
            ret = new short[(int)(array.length/2)];
        }
        // TODO: Forcing the array to int length - just for testing
//        ret = new short[(int)(array.length/4)];

        // TODO: CURRENT -works for recording, just not the data that I create in python
        ByteBuffer buffer = ByteBuffer.wrap(array);
//        buffer.order(ByteOrder.nativeOrder());
//        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int count = 0;
        while(buffer.hasRemaining()){
            ret[count++] = buffer.getShort();
        }
        return ret;

//        // BIG Endian (int to short)
//        for (int i = 0; 4 * i + 3 < array.length; i++){
//            ret[i] = (short) ((array[4*i+3] & 0xff) | ((array[4*i+2] & 0xff) << 8) | ((array[4*i+1]) << 16) | ((array[4*i] & 0xff) << 24));
//        }
//        return ret;
//        // BIG Endian
//        for (int i = 0; 2*i+1 < array.length; i++){
//            ret[i] = (short) ((array[2*i+1] & 0xff) | ((array[2*i+0] & 0xff) << 8));
//        }
//        return ret;

//        // Little Endian
//        for (int i = 0; 2*i+1 < array.length; i++){
//            ret[i] = (short) ((array[2*i+0] & 0xff) | ((array[2*i+1] & 0xff) << 8));
//        }
//        return ret;
    }
    // Convert from byte array to short []
    private short[] bytesToFloatShort(byte[] array, int format) {
        short [] ret = new short[(int) (array.length / 4)];

        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.order(ByteOrder.nativeOrder());
//        buffer.order(ByteOrder.BIG_ENDIAN);
        int count = 0;
        while(buffer.hasRemaining()){
            ret[count++] = (short) (32767 * buffer.getFloat());
        }
        return ret;

//        // BIG Endian (int to short)
//        for (int i = 0; 4 * i + 3 < array.length; i++){
//            ret[i] = (short) ((array[4*i+3] & 0xff) | ((array[4*i+2] & 0xff) << 8) | ((array[4*i+1]) << 16) | ((array[4*i] & 0xff) << 24));
//        }
//        return ret;
//        // BIG Endian
//        for (int i = 0; 2*i+1 < array.length; i++){
//            ret[i] = (short) ((array[2*i+1] & 0xff) | ((array[2*i+0] & 0xff) << 8));
//        }
//        return ret;

//        // Little Endian
//        for (int i = 0; 2*i+1 < array.length; i++){
//            ret[i] = (short) ((array[2*i+0] & 0xff) | ((array[2*i+1] & 0xff) << 8));
//        }
//        return ret;
    }
    // Convert from byte array to int []
    private int[] bytesToInt(byte[] array) {
        int[] ret = new int[(int) (array.length/4)];

        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int count = 0;
        while(buffer.hasRemaining()){
            ret[count++] = buffer.getInt();
        }
        return ret;

//        // Big Endian
//        for (int i = 0; 4 * i + 3 < array.length; i++) {
//            ret[i] = ((array[4 * i] & 0xff) << 24) | ((array[4 * i + 1] & 0xff) << 16) |
//                    ((array[4 * i + 2] & 0xff) << 8) | (array[4 * i + 3] & 0xff);
//        }
//        return ret;

//        // Little Endian
//        for (int i = 0; 4 * i + 3 < array.length; i++) {
//            ret[i] = ((array[4 * i + 3] & 0xff) << 24) | ((array[4 * i + 2] & 0xff) << 16) |
//                    ((array[4 * i + 1] & 0xff) << 8) | (array[4 * i] & 0xff);
//        }
//        return ret;
    }

    // Matrices
    // Create a matrix, filled with scalar value
    private double[][] createMatrix(int rows, int columns, double value) {
        double[][] ret = new double[rows][columns];
        for (int i = 0; i < ret.length; i++){
            for (int j = 0; j < ret[i].length; j++) {
                ret[i][j] = value;
            }
        }
        return ret;
    }
    // Create a matrix - empty, shape only
    private double[][] createMatrix(int rows, int columns) {
        return new double[rows][columns];
    }
    // Creates a double[][] "array", with values in-between min (inclusive) and max
    // (exclusive), with steps of size.
    private double[][] createArray(double min, double max, int step){
        int size = (int) Math.ceil((max - min) / step);
        double[][] temp = createMatrix(size, 1);
        for (int i = 0; i < size; i++){
            temp[i][0] = (step * i) + min;
        }
        return temp;
    }

    // Mathematics
    // Element-wise multiplication of matrix
    private double[][] mul(double[][] matrix1, double[][] matrix2){
        // TODO: check that .size, countRows, countColumns does what you think.
        double[][] ret = createMatrix(matrix1.length, matrix1[0].length);
        for (int i = 0; i < ret.length; i++) {
            for (int j = 0; j < ret[i].length; j++) {
                ret[i][j] = matrix1[i][j] * matrix2[i][j];
            }
        }
        return ret;
    }
    // Element-wises multiplication of matrix and scalar
    private double[][] mul(double[][] matrix, double scalar){
        // TODO: check that .size, countRows, countColumns does what you think.
        double[][] ret = createMatrix(matrix.length, matrix[0].length);
        for (int i = 0; i < ret.length; i++) {
            for (int j = 0; j < ret[i].length; j++) {
                ret[i][j] = matrix[i][j] * scalar;
            }
        }
        return ret;
    }
    // Element-wise divide of matrix
    private double[][] divide(double[][] numerator, double[][] denominator) {
        double[][] ret = createMatrix(numerator.length, numerator[0].length);
        for (int i = 0; i < ret.length; i++) {
            for (int j = 0; j < ret[i].length; j++) {
                ret[i][j] = numerator[i][j] / denominator[i][j];
            }
        }
        return ret;
    }
    // Element-wise divide of matrix and scalar
    private double[][] divide(double[][] matrix, double scalar){
        double[][] ret = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++){
                ret[i][j] = matrix[i][j] / scalar;
            }
        }
        return ret;
    }
    // Element-wise divide of array and scalar
    private double[] divide(double [] array, double scalar) {
        double[] ret = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[i] / scalar;
        }
        return ret;
    }
    // Element-wise multiplication between array and scalar
    private double[] mul(short [] array, double scalar) {
        double [] ret = new double [array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[i] * scalar;
        }
        return ret;
    }
    // Element-wise multiplication between arrays
    private double[] mul(double [] array1, double [] array2) {
        if (array1.length != array2.length){
            Log.e("mul_double[]", String.format("arrays must be of same length, array1:%d, array2:%d", array1.length, array2.length));
            throw new ArithmeticException("array size mismatch");
        }
        int size = array1.length;
        double [] ret = new double [size];
        for (int i = 0; i < size; i++) {
            ret[i] = array1[i] * array2[i];
        }
        return ret;
    }
    // Element-wise power of matrix by scalar
    private double[][] pow(double[][] matrix, double expon) {
        // TODO: ojalgo CHECK - is the .size() the product of rows and cols
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = Math.pow(matrix[i][j], expon);
            }
        }
        return matrix;
    }
    // Element-wise logarithm of matrix, with base
    private double[][] log(double[][] matrix, int log_base) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++){
                matrix[i][j] = Math.log(matrix[i][j]) / Math.log(log_base);
            }
        }
        return matrix;
    }
    // Element-wise logarithm of matrix, natural log
    private double[][] log(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++){
                matrix[i][j] = Math.log(matrix[i][j]);
            }
        }
        return matrix;
    }
    // Transpose a matrix
    public double[][] transpose(double[][] matrix){
        double[][] ret = createMatrix(matrix[0].length, matrix.length);
        for (int i = 0; i < matrix[0].length; i++){
            for (int j = 0; j < matrix.length; j++){
                ret[i][j] = matrix[j][i];
            }
        }
        return ret;
    }

    // Sum of matrix
    private double sum(double[][] matrix) {
        double value = 0;
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++){
                value = value + matrix[i][j];
            }
        }
        return value;
    }
    // Mean of matrix
    private double mean(double[][] array) {
        return sum(array)/(array.length * array[0].length);
    }
    // Absolute of matrix
    private double[] abs(double[][] matrix){
        double [] ret = new double [matrix.length * matrix[0].length];
        int ret_count = 0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                ret[ret_count++] = Math.abs(matrix[i][j]);
            }
        }
        return ret;
    }
    // Absolute of complex array
    private double[] abs(double [] real, double [] imag) {
        double [] ret = new double[real.length];
        for(int i = 0; i < real.length; ++i) {
            ret[i] = Math.sqrt((real[i] * real[i]) + (imag[i] * imag[i]));
        }
        return ret;
    }
    // Absolute values of an array
    private double[] abs(double[] array){
        double [] ret = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = (double) Math.abs(array[i]);
        }
        return ret;
    }
    // Absolute values of an array
    private short[] abs(short[] array){
        short [] ret = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = (short) Math.abs(array[i]);
        }
        return ret;
    }

    // Get smallest value in the matrix
    private double min(double[][] matrix) {
        double ret = matrix[0][0];
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++) {
                ret = Math.min(matrix[i][j], ret);
            }
        }
        return ret;
    }
    private double min(double [] array){
        double ret = array[0];
        for (int i = 0; i < array.length; i++) {
            ret = Math.min(array[i], ret);
        }
        return ret;
    }
    // Get largest value in the matrix
    private double max(double[][] matrix) {
        double ret = matrix[0][0];
        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix[i].length; j++){
                ret = Math.max(matrix[i][j], ret);
            }
        }
        return ret;
    }
    // Get largest value in an array
    private short max(short[] array){
        short value = array[0];
        for (int i = 0; i < array.length; i++) {
            value = (short) Math.max(array[i], value);
        }
        return value;
    }
    // Get largest value in an array
    private double max(double[] array){
        double value = array[0];
        for (int i = 0; i < array.length; i++) {
            value = (double) Math.max(array[i], value);
        }
        return value;
    }
    // Calculate the norm of array
    private double[] norm(short[] array, double reference) {
        double [] ret = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[i]/reference;
        }
        return ret;
    }

    // Type converters
    private double[] toDouble(short[] input_array) {
        double[] ret = new double[input_array.length];
        for (int i = 0; i < input_array.length; i++){
            ret[i] = input_array[i];
        }
        return ret;
    }
}
