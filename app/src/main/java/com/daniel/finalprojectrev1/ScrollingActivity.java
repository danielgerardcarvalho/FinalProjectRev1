package com.daniel.finalprojectrev1;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.daniel.finalprojectrev1.databinding.ActivityScrollingBinding;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import org.ojalgo.matrix.ComplexMatrix;
import org.ojalgo.matrix.Primitive64Matrix;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.Primitive64Store;
import org.ojalgo.structure.Access2D;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

import jeigen.DenseMatrix;
import jeigen.DenseMatrixComplex;

public class ScrollingActivity extends AppCompatActivity {

    private ActivityScrollingBinding binding;

    /* General Operation */ //TODO: maybe change some of these to individual sub-systems later?
    private boolean system_flag;

    /* Model Import */
    // model import constants
    private static final String MODEL_DIR_LOC = Environment.getExternalStorageDirectory().toString()
            + "/Project/dictionaries/";
    private static final String MODEL_FILE_EXT = ".json";
    // model import variables
    private File model_import_file;

    /* Audio Capture */
    // capture constants
    private static final int[] AUDIO_SOURCES = {
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.UNPROCESSED
    };
    private static final int AUDIO_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT_INT8 = AudioFormat.ENCODING_PCM_8BIT;
    private static final int AUDIO_FORMAT_INT16 = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_FORMAT_FLOAT = AudioFormat.ENCODING_PCM_FLOAT;
    private static final int AUDIO_MIN_FORMAT_SIZE = 1;
    private final int CAP_QUEUE_SIZE = 10;
    // input format options
    private static final String UI_AUDIO_FORMAT_INT16 = "int16";
    private static final String UI_AUDIO_FORMAT_FLOAT = "float";
    // audio capture object
    private AudioRecord audio_recorder;
    // audio capture multi-threading
    private Runnable mt_audio_capture_runnable;
    private Runnable mt_file_capture_runnable;
    private boolean mt_audio_capture_flag;
    private ArrayList<byte[]> cap_buffer;
    private boolean cap_first_run;

    /* Audio Pre-Processing */
    // processing constants
    private final int HANN = 0;
    private final int PROC_QUEUE_SIZE = 10;
    // processing multi-threading
    private Runnable mt_audio_processing_runnable;
    private boolean mt_audio_processing_flag;
    private short [] proc_data;
    // TODO: ojalgo NEEDS CONVERT - STFT output buffer (Normal large matrix)
    private ArrayList<Primitive64Store> proc_buffer;
//    private ArrayList<DenseMatrix> proc_buffer;

    /* Classifier */
    // classifier constants
    private final int DETECT_QUEUE_SIZE = 5;
    // classifier multi-thread
    private Runnable mt_classifier_runnable;            // multi-thread handler (runnable)
    private boolean mt_classifier_flag;                 // multi-thread status flag
    private NMF.NMF_Mini classifier_imported_nmf_model; // nmf mini class object
    private Primitive64Store classifier_data;                // classifier input
    private ArrayList<Primitive64Store> classifier_buffer;   // classifier output buffer


    /* UI Associated Model Import Settings */
    // Inputs - TODO: removing to reduce memory use
//    private TextView model_filename_view;
//    private EditText num_class_events_text;
//    private EditText clip_len_text;
//    private EditText num_overlaps_text;
//    private EditText snr_range_min_text;
//    private EditText snr_range_max_text;
//    private EditText num_training_sample_text;
    // Values
    private int model_num_class_events;
    private int model_clip_len;
    private int model_num_overlaps;
    private int model_snr_range_min;
    private int model_snr_range_max;
    private int model_num_training_samples;
    private int model_num_inter_comp;

    /* UI Associated Capture Settings */
    // Inputs - // TODO: removing to reduce memory use
//    private EditText cap_sample_rate_input;
//    private EditText cap_time_interval_input;
//    private Button cap_file_import_select_input;
//    private Spinner cap_format_input;
    // Values
    private int cap_sample_rate;
    private int cap_time_interval;
    private int cap_buffer_size;
    private boolean cap_file_import_flag = false;
    private File cap_imported_file;
    private FileInputStream cap_imported_file_stream;
    private int cap_format;

    /* UI Associated Processing Settings */
    // Inputs - // TODO: removing to reduce memory use
//    private EditText proc_fft_size_input;
//    private EditText proc_sample_rate_input;
//    private EditText proc_num_time_frames_input;
//    private EditText proc_resolution_input;
//    private EditText proc_window_time_input;
//    private EditText proc_hop_time_input;
    // Values
    private int proc_fft_size;
    private int proc_sample_rate;
    private int proc_num_time_frames;
    private int proc_resolution;
    private double proc_window_time;
    private double proc_hop_time;

    /* UI Associated Classifier Settings */
    // Inputs - // TODO: removing to reduce memory use
//    private EditText classifier_fft_size_input;
//    private EditText classifier_num_classes_input;
//    private EditText classifier_num_inter_comp_input;
//    private EditText classifier_num_iters_input;
    // Values
    private int classifier_fft_size;
    private int classifier_num_classes;
    private int classifier_num_inter_comp;
    private int classifier_num_iters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityScrollingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle(getTitle());

        /* Configure User Selection Register */
        ActivityResultLauncher<Intent> activity_launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK){
//                    String file_loc = result.getData().getData().getPath().split(":")[1];
//                    file_loc = file_loc.split(":")[1];
                    assert result.getData() != null;
                    cap_imported_file = new File(
                            Environment.getExternalStorageDirectory().toString() +
                                    "/" + result.getData().getData().getPath().split(":")[1]
                    );
                }
            });

        /* Configure Runnables */
        configureRunnables();

        // TODO: declaring the variables locally to reduce memory use
        /* Model Import Setting Inputs */
        TextView model_filename_view = (TextView) findViewById(R.id.import_model_filename_view);
        ImageView model_filename_marker_view = (ImageView) findViewById(R.id.import_model_filename_marker_view);
        EditText num_class_events_text = (EditText) findViewById(R.id.input_model_import_num_event_classes);
        EditText clip_len_text = (EditText) findViewById(R.id.input_model_import_clip_len);
        EditText num_overlaps_text = (EditText) findViewById(R.id.input_model_import_num_overlaps);
        EditText snr_range_min_text = (EditText) findViewById(R.id.input_model_import_snr_range_min);
        EditText snr_range_max_text = (EditText) findViewById(R.id.input_model_import_snr_range_max);
        EditText num_training_sample_text = (EditText) findViewById(R.id.input_model_import_training_size);
        EditText num_inter_comp_text = (EditText) findViewById(R.id.input_model_import_num_components);

        // TODO: temporary image view, used to test the plot output and test the capture and
        //  processing functions.
        ImageView image = findViewById(R.id.imageView);

        // TODO: declaring the variables locally to reduce memory use
        /* Capture Setting Inputs */
        EditText cap_sample_rate_input = (EditText) findViewById(R.id.input_cap_sample_rate);
        EditText cap_time_interval_input = (EditText) findViewById(R.id.input_cap_time_interval);
        Button cap_file_import_select_input = (Button) findViewById(R.id.input_cap_file_import_select);
        Spinner cap_format_input = (Spinner) findViewById(R.id.input_cap_format_option);

        // TODO: declaring the variables locally to reduce memory use
        /* Processing Setting Inputs */
        EditText proc_fft_size_input = (EditText) findViewById(R.id.input_proc_fft_size);
        EditText proc_sample_rate_input = (EditText) findViewById(R.id.input_proc_sample_rate);
        EditText proc_num_time_frames_input = (EditText) findViewById(R.id.input_proc_num_time_frames);
        EditText proc_resolution_input = (EditText) findViewById(R.id.input_proc_resolution);
        EditText proc_window_time_input = (EditText) findViewById(R.id.input_proc_window_time);
        EditText proc_hop_time_input = (EditText) findViewById(R.id.input_proc_hop_time);

        // TODO: declaring the variables locally to reduce memory use
        /* Classifier Setting Inputs */
        EditText classifier_fft_size_input = (EditText) findViewById(R.id.input_classifier_fft_size);
        EditText classifier_num_classes_input = (EditText) findViewById(R.id.input_classifier_num_classes);
        EditText classifier_num_inter_comp_input = (EditText) findViewById(R.id.input_classifier_num_inter_comp);
        EditText classifier_num_iters_input = (EditText) findViewById(R.id.input_classifier_num_iters);

        /* File import button logic*/
        cap_file_import_select_input.setOnClickListener(button -> {
            if (!cap_file_import_flag){
                cap_file_import_flag = true;
            }
            cap_imported_file = null;
            cap_imported_file_stream = null;

            getUserSelectedFile(activity_launcher);
        });

        /* Floating Action Button Logic */
        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(view -> {

            // Importing input user settings and checking if these settings are valid
            boolean valid_flag = convertSettingInputs( model_filename_view, model_filename_marker_view,
                    num_class_events_text, clip_len_text, num_overlaps_text, snr_range_min_text,
                    snr_range_max_text, num_training_sample_text, num_inter_comp_text,
                    cap_sample_rate_input, cap_time_interval_input, cap_format_input,
                    proc_fft_size_input, proc_sample_rate_input, classifier_fft_size_input,
                    classifier_num_classes_input, classifier_num_inter_comp_input,
                    classifier_num_iters_input
            );
            // Update UI fields
            uiFieldUpdate(model_filename_view, num_class_events_text, clip_len_text,
                    num_overlaps_text, snr_range_min_text, snr_range_max_text,
                    num_training_sample_text, cap_sample_rate_input, cap_time_interval_input,
                    cap_format_input, proc_fft_size_input, proc_sample_rate_input,
                    proc_num_time_frames_input, proc_resolution_input, proc_window_time_input,
                    proc_hop_time_input, classifier_fft_size_input, classifier_num_classes_input,
                    classifier_num_inter_comp_input, classifier_num_iters_input
            );
            if (!valid_flag) {
                Snackbar.make(view, "Settings are not valid", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }

            // Check if already running
            if (system_flag){
                // UI displays
                Snackbar.make(view, "Stopping system", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                // Change icon image
                fab.setImageResource(android.R.drawable.ic_media_play);

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
                // UI displays
                Snackbar.make(view, "Starting system", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                // Change icon image
                fab.setImageResource(android.R.drawable.ic_media_pause);

                // Configure capture system
                configureCapture();
                // Configure processing system
                configureProcessing();
                // Configure classifier system
//                configureClassifier(model_filename_view, model_filename_marker_view);
//                // Configure monitoring system
//                configureMonitor();


                // Starting the sub-system threads
                startCapture();
                startProcessing();
//                startClassifier();

                // Display results
//                tempTestPlot(image);
//                tempCapPlot(image);
                tempProcPlot(image);
//                tempClassifierPlot(image);



                system_flag = true;
            }
        });

        /* Checking application permissions */
        checkAllPermissions();

        /* Model Import Filename Update Logic */
        modelFilenameUpdate(model_filename_view,  model_filename_marker_view, num_class_events_text,
                clip_len_text, num_overlaps_text, snr_range_min_text, snr_range_max_text,
                num_training_sample_text
        );
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If this method breaks try removing the super.
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            return;
        }
        if (data.getClipData() == null) {
            cap_imported_file = new File(
                    Environment.getExternalStorageDirectory().toString() +
                            "/" + data.getData().getPath().split(":")[1]
            );
//            cap_imported_file = new File (data.getData().getPath());
        } else {
            cap_imported_file = new File(data.getClipData().getItemAt(0).getUri().getPath());
        }
    }

    private boolean convertSettingInputs(TextView model_filename_view, ImageView model_filename_marker_view,
                                         EditText num_class_events_text, EditText clip_len_text,
                                         EditText num_overlaps_text, EditText snr_range_min_text,
                                         EditText snr_range_max_text,
                                         EditText num_training_sample_text,
                                         EditText num_inter_comp_text,
                                         EditText cap_sample_rate_input,
                                         EditText cap_time_interval_input,
                                         Spinner cap_format_input, EditText proc_fft_size_input,
                                         EditText proc_sample_rate_input,
                                         EditText classifier_fft_size_input,
                                         EditText classifier_num_classes_input,
                                         EditText classifier_num_inter_comp_input,
                                         EditText classifier_num_iters_input) {

        /* Processing User Inputs */
        // Model import settings
        // TODO: need to add the checks for model import inputs, currently only the default settings
        //  are used.
        // - model import number of event classes
        if (!TextUtils.isEmpty(num_class_events_text.getText())) {
            model_num_class_events = Integer.parseInt(num_class_events_text.getText().toString());
        } else {
            model_num_class_events = Integer.parseInt(num_class_events_text.getHint().toString());
        }
        // - model import clip length
        if (!TextUtils.isEmpty(clip_len_text.getText())) {
            model_clip_len = Integer.parseInt(clip_len_text.getText().toString());
        } else {
            model_clip_len = Integer.parseInt(clip_len_text.getHint().toString());
        }
        // - model import number of overlaps
        if (!TextUtils.isEmpty(num_overlaps_text.getText())) {
            model_num_overlaps = Integer.parseInt(num_overlaps_text.getText().toString());
        } else {
            model_num_overlaps = Integer.parseInt(num_overlaps_text.getHint().toString());
        }
        // - model import snr range min
        if (!TextUtils.isEmpty(snr_range_min_text.getText())) {
            model_snr_range_min = Integer.parseInt(snr_range_min_text.getText().toString());
        } else {
            model_snr_range_min = Integer.parseInt(snr_range_min_text.getHint().toString());
        }
        // - model import snr range max
        if (!TextUtils.isEmpty(snr_range_max_text.getText())) {
            model_snr_range_max = Integer.parseInt(snr_range_max_text.getText().toString());
        } else {
            model_snr_range_max = Integer.parseInt(snr_range_max_text.getHint().toString());
        }
        // - model import number of training samples
        if (!TextUtils.isEmpty(num_training_sample_text.getText())) {
            model_num_training_samples = Integer.parseInt(num_training_sample_text.getText().toString());
        } else {
            model_num_training_samples = Integer.parseInt(num_training_sample_text.getHint().toString());
        }
        // model import number of internal components
        if (!TextUtils.isEmpty(num_inter_comp_text.getText())) {
            model_num_inter_comp = Integer.parseInt(num_inter_comp_text.getText().toString());
        } else {
            model_num_inter_comp = Integer.parseInt(num_inter_comp_text.getHint().toString());
        }

        // Capture settings
        // - capture sample rate
        if (!TextUtils.isEmpty(cap_sample_rate_input.getText())) {
            cap_sample_rate = Integer.parseInt(cap_sample_rate_input.getText().toString());
        } else {
            cap_sample_rate = Integer.parseInt(cap_sample_rate_input.getHint().toString());
        }
        // - capture time interval
        if (!TextUtils.isEmpty(cap_time_interval_input.getText())) {
            cap_time_interval = Integer.parseInt(cap_time_interval_input.getText().toString());
        } else {
            cap_time_interval = model_clip_len;
        }
        // - capture format
        cap_format = AUDIO_FORMAT_INT16;
        String selected_audio_format = cap_format_input.getSelectedItem().toString();
        if (selected_audio_format.matches(UI_AUDIO_FORMAT_INT16)) {
            cap_format = AUDIO_FORMAT_INT16;
        } else if (selected_audio_format.matches(UI_AUDIO_FORMAT_FLOAT)){
            cap_format = AUDIO_FORMAT_FLOAT;
        }

        // Processing Settings
        // - processing fft size
        if (!TextUtils.isEmpty(proc_fft_size_input.getText())) {
            proc_fft_size = Integer.parseInt(proc_fft_size_input.getText().toString());
        } else {
            proc_fft_size = Integer.parseInt(proc_fft_size_input.getHint().toString());
        }
        // - processing sample rate
        if (!TextUtils.isEmpty(proc_sample_rate_input.getText())) {
            proc_sample_rate = Integer.parseInt(proc_sample_rate_input.getText().toString());
        }
        else {
            proc_sample_rate = cap_sample_rate;
        }
        // - processing number of time frames
        proc_num_time_frames = cap_time_interval * proc_sample_rate;
        // - processing resolution
        proc_resolution = proc_sample_rate / proc_fft_size;
        // - processing window time
        proc_window_time = proc_fft_size / (proc_sample_rate * 1.0);
        // - processing hop time
        proc_hop_time = proc_window_time / 2.0;

        // Classifier Settings
        // - classifier fft size
        if (!TextUtils.isEmpty(classifier_fft_size_input.getText())) {
            classifier_fft_size = Integer.parseInt(classifier_fft_size_input.getText().toString());
        } else {
            classifier_fft_size = proc_fft_size / 2;
        }
        // - classifier number of  classes
        if (!TextUtils.isEmpty(classifier_num_classes_input.getText())) {
            classifier_num_classes = Integer.parseInt(classifier_num_classes_input.getText().toString());
        } else {
            classifier_num_classes = model_num_class_events;
        }
        // - classifier number of internal components
        if (!TextUtils.isEmpty(classifier_num_inter_comp_input.getText())) {
            classifier_num_inter_comp = Integer.parseInt(classifier_num_inter_comp_input.getText().toString());
        } else {
            classifier_num_inter_comp = model_num_inter_comp;
        }
        // - classifier number of internal iterations
        if (!TextUtils.isEmpty(classifier_num_iters_input.getText())) {
            classifier_num_iters = Integer.parseInt(classifier_num_iters_input.getText().toString());
        } else {
            classifier_num_iters = Integer.parseInt(classifier_num_iters_input.getHint().toString());
        }

        // TODO: possibly add a check for the validity of the accepted/rejected settings. Maybe make
        //  a system that states which settings are missing or in error. Therefore returning true
        //  or false.

        // TODO: possibly use this function return value and other booleans to check if system is
        //  correctly configured, may have issue if running straight from default values.
        isValidModelFilename(model_filename_view, model_filename_marker_view);

        return true;
    }

    private void modelFilenameUpdate(TextView model_filename_view, ImageView model_filename_marker_view,
                                     EditText num_class_events_text, EditText clip_len_text,
                                     EditText num_overlaps_text, EditText snr_range_min_text,
                                     EditText snr_range_max_text, EditText num_training_sample_text){

        // Updating model filename
        num_class_events_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                model_filename_view.setText(String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        num_class_events_text.getText().toString(),
                        clip_len_text.getText().toString(),
                        num_overlaps_text.getText().toString(),
                        snr_range_min_text.getText().toString(),
                        snr_range_max_text.getText().toString(),
                        num_training_sample_text.getText().toString()
                ));
                isValidModelFilename(model_filename_view, model_filename_marker_view);
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
                model_filename_view.setText(String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        num_class_events_text.getText().toString(),
                        clip_len_text.getText().toString(),
                        num_overlaps_text.getText().toString(),
                        snr_range_min_text.getText().toString(),
                        snr_range_max_text.getText().toString(),
                        num_training_sample_text.getText().toString()
                ));
                isValidModelFilename(model_filename_view, model_filename_marker_view);
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
                model_filename_view.setText(String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        num_class_events_text.getText().toString(),
                        clip_len_text.getText().toString(),
                        num_overlaps_text.getText().toString(),
                        snr_range_min_text.getText().toString(),
                        snr_range_max_text.getText().toString(),
                        num_training_sample_text.getText().toString()
                ));
                isValidModelFilename(model_filename_view, model_filename_marker_view);
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
                model_filename_view.setText(String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        num_class_events_text.getText().toString(),
                        clip_len_text.getText().toString(),
                        num_overlaps_text.getText().toString(),
                        snr_range_min_text.getText().toString(),
                        snr_range_max_text.getText().toString(),
                        num_training_sample_text.getText().toString()
                ));
                isValidModelFilename(model_filename_view, model_filename_marker_view);
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
                model_filename_view.setText(String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        num_class_events_text.getText().toString(),
                        clip_len_text.getText().toString(),
                        num_overlaps_text.getText().toString(),
                        snr_range_min_text.getText().toString(),
                        snr_range_max_text.getText().toString(),
                        num_training_sample_text.getText().toString()
                ));
                isValidModelFilename(model_filename_view, model_filename_marker_view);
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
                model_filename_view.setText(String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        num_class_events_text.getText().toString(),
                        clip_len_text.getText().toString(),
                        num_overlaps_text.getText().toString(),
                        snr_range_min_text.getText().toString(),
                        snr_range_max_text.getText().toString(),
                        num_training_sample_text.getText().toString()
                ));
                isValidModelFilename(model_filename_view, model_filename_marker_view);
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

    }

    private boolean isValidModelFilename(TextView filename, ImageView marker) {
        File temp = new File(MODEL_DIR_LOC + filename.getText() + MODEL_FILE_EXT);
        if (temp.exists()){
            Log.v("ModelImportFile", "The file exists");
            marker.setImageResource(R.drawable.ic_baseline_check_circle_24);
            return true;

        } else {
            Log.v("ModelImportFile", "Current filename does not exist");
            marker.setImageResource(R.drawable.ic_baseline_remove_circle_24);
            return false;
        }
    }

    private void uiFieldUpdate(TextView model_filename_view, EditText num_class_events_text,
                               EditText clip_len_text, EditText num_overlaps_text,
                               EditText snr_range_min_text, EditText snr_range_max_text,
                               EditText num_training_sample_text, EditText cap_sample_rate_input,
                               EditText cap_time_interval_input, Spinner cap_format_input,
                               EditText proc_fft_size_input, EditText proc_sample_rate_input,
                               EditText proc_num_time_frames_input, EditText proc_resolution_input,
                               EditText proc_window_time_input, EditText proc_hop_time_input,
                               EditText classifier_fft_size_input,
                               EditText classifier_num_classes_input,
                               EditText classifier_num_inter_comp_input,
                               EditText classifier_num_iters_input){
        // Update the text field of all input spaces
        // Model import
        num_class_events_text.setText(String.format("%d", model_num_class_events));
        clip_len_text.setText(String.format("%d", model_clip_len));
        num_overlaps_text.setText(String.format("%d", model_num_overlaps));
        snr_range_min_text.setText(String.format("%d", model_snr_range_min));
        snr_range_max_text.setText(String.format("%d", model_snr_range_max));
        num_training_sample_text.setText(String.format("%d", model_num_training_samples));
        model_filename_view.setText(String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
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
        // Classifier
        classifier_fft_size_input.setText(String.format("%d", classifier_fft_size));
        classifier_num_classes_input.setText(String.format("%d", classifier_num_classes));
        classifier_num_inter_comp_input.setText(String.format("%d", classifier_num_inter_comp));
        classifier_num_iters_input.setText(String.format("%d", classifier_num_iters));
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
            classifier();
            Log.v("Classifier", "Classifier thread has been closed.");
        };
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


    // Import
    private NMF.NMF_Mini importModelFile(String filename) {
        Log.v("ImportModelFile", "Attempting to import model file...");
        Log.v("ImportModelFile", "File path: " + MODEL_DIR_LOC + filename + MODEL_FILE_EXT);
        NMF.NMF_Mini nmf_mini = null;
        File file = new File(MODEL_DIR_LOC + filename + MODEL_FILE_EXT);
        try{
            // Final check for files existence
            if (!file.exists()) {
                Log.e("ImportModelFileERROR", "File does not exist");
                return null;
            }
            // Reading in the JSON file
            Reader reader = new FileReader(file);
            // Loading the dictionaries
            nmf_mini = new Gson().fromJson(reader, NMF.NMF_Mini.class);
            Log.v("ImportModelFile", "Imported model from file");
        } catch (Exception e) {
            Log.e("ImportModelFileERROR", "Failed to import model from file");
            e.printStackTrace();
        }
        return nmf_mini;
    }


    // Capture
    private void configureCapture() {
        /* Configures capture variables before the start of any sub-systems*/

        // Reset first run flag
        cap_first_run = true;
        // Minimum buffer size
        int min_buffer_size = AudioRecord.getMinBufferSize(cap_sample_rate, AUDIO_CHANNELS,
                cap_format);
        // Calculating buffer size
        int multiplier = 2;
        if (cap_format == AUDIO_FORMAT_INT8){
            multiplier = 1;
        } else if (cap_format == AUDIO_FORMAT_INT16){
            multiplier = 2;
        } else if (cap_format == AUDIO_FORMAT_FLOAT) {
            multiplier = 4;
        }
        cap_buffer_size = (int) cap_sample_rate * cap_time_interval * (multiplier/*cap_format /
                AUDIO_MIN_FORMAT_SIZE*/);

        // Checking if cap_buffer_size is large enough
        if (cap_buffer_size < min_buffer_size){
            Log.e("AudioCapture", String.format("cap_buffer_size: is too small\n\tcurrent:%d" +
                    "\n\trequired:%d", cap_buffer_size, min_buffer_size));
            cap_buffer_size = min_buffer_size;
        }
        // Creating audio record object
        checkPermission(this, this, Manifest.permission.RECORD_AUDIO);
        audio_recorder = new AudioRecord(AUDIO_SOURCES[1], cap_sample_rate, AUDIO_CHANNELS,
                cap_format, cap_buffer_size);
        // Buffer clearing / initialisation
        cap_buffer = new ArrayList<>();
        if (cap_file_import_flag){
            // Convert the File to input stream
            try {
                cap_imported_file_stream = new FileInputStream(cap_imported_file);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void getUserSelectedFile(ActivityResultLauncher<Intent> activity_launcher){

//        // Old version (Deprecated by google) - working
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("*/*");
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//        startActivityForResult(intent, 0);

        // New version - working
        Intent file_intent = new Intent(Intent.ACTION_GET_CONTENT);
        file_intent.addCategory(Intent.CATEGORY_OPENABLE);
        file_intent.setType("*/*");
        activity_launcher.launch(file_intent);
    }

    private void startCapture(){
        /* Finalises configuration and starts sub-system */

        // Set activity flag to true - as the system is starting
        mt_audio_capture_flag = true;

        // Check if file is imported
        if (cap_file_import_flag){
            Log.d("Capture", "File mode is active");
            new Thread(mt_file_capture_runnable).start();
            return;
        }

        // Check if audio interface is in appropriate state
        if (audio_recorder.getState() != AudioRecord.STATE_INITIALIZED){
            Log.e("AudioCapture", "Audio interface is in incorrect state: desired:" +
                    "initialised");
            return;
        }
        Log.d("Capture", "Audio mode is active");
        // Starting recording interface
        audio_recorder.startRecording();
        // Starting recording read thread
        new Thread(mt_audio_capture_runnable).start();
    }

    private void stopCapture(){

        // Stopping recording read thread
        mt_audio_capture_flag = false;

        // Clearing file capture data
        cap_imported_file = null;
        cap_imported_file_stream = null;
        // Check if file import was used
        if (cap_file_import_flag){
            // Stopping file reading interface
            // Reset flags and clear files
            cap_file_import_flag = false;
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
        byte [] temp = new byte[cap_buffer_size];
        if (cap_buffer.size() == 0 && cap_first_run){
            // This read is only to clear garbage data and is only run of first iteration
            audio_recorder.read(temp, 0, 1);
            cap_first_run = false;
        }
        // Override garbage read with real read of AudioRecord buffer
        int num_read = audio_recorder.read(temp, 0, cap_buffer_size);

        // Add read data to capture buffer
        cap_buffer.add(temp);
        Log.v("AudioCapture", String.format("Read from internal buffer:\n\tAmount read:%d," +
                "\n\tQueue location:%d", num_read, cap_buffer.size()));
    }

    private void readFileCaptureBuffer(){
        // Clear first run flag
        cap_first_run = false;
        // Read data from the file input stream
        int num_read = 0;
        byte[] temp = new byte[cap_buffer_size];
        try {
            num_read = cap_imported_file_stream.read(temp, 0,
                    cap_buffer_size);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (num_read == -1){
            Log.v("FileCapture", "Read entire file, ending read loop.");
            stopCapture();
        }
        while (cap_buffer.size() == CAP_QUEUE_SIZE){
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
        Log.d("FileCapture", String.format("Read from internal buffer:\n\tAmount read:%d," +
                "\n\tQueue location:%d", num_read, cap_buffer.size()));
    }


    // Processing
    private void configureProcessing() {
        /* Configures processing variables before the start of any sub-systems*/

        // Input variable initialisation
        proc_data = null;
        // Output buffer clearing / initialisation
        proc_buffer = new ArrayList<>();
    }

    private void startProcessing(){
        /* Finalises configuration and starts sub-system */

        // Set the activity flag to true as sub-system is starting
        mt_audio_processing_flag = true;
        // Start pre-processing thread
        new Thread(mt_audio_processing_runnable).start();
    }

    private void stopProcessing(){
        // Stopping pre-processing thread
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
        double [] window_coeff = window_func(window_size, HANN);
        // Pre-calculating the fft twiddle factor values
        FFT fft = new FFT(proc_fft_size);

        // TODO: (MEL) decide fate of mel-spectrogram things, current implementation is wrong, not
        //  sure if mel-spectrum is needed at all. (removed in mean-time) - WILL NEED CONVERSION ojalgo
//        //  Finding frequency range and melscale range for conversion
//        DenseMatrix frequency_range = createArray(0, proc_sample_rate / 2.0, proc_resolution);
//        DenseMatrix melscale_range = ((((frequency_range.div(1127)).exp()).sub(1)).mul(700));

        // Thread infinite loop
        while(mt_audio_processing_flag) {
            Log.v("AudioProcessing", "Audio Pre-processing thread is active...");
            // Wait if no value is available in the buffer
            while (cap_buffer == null || cap_buffer.size() == 0){
                try {
                    Log.v("AudioProcessing", "waiting for capture buffer to fill...");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!mt_audio_processing_flag){
                    return;
                }
            }
            // Fetch data from the capture buffer queue
            byte[] temp = cap_buffer.remove(0);
            // Converting from bytes to short
            proc_data = bytesToShort(temp, cap_format);
            Log.v("AudioProcessing", String.format("Conversion from Bytes:" +
                    "\n\tInput from buffer: %d" +
                    "\n\tOutput from byte conversion: %d", cap_buffer_size, proc_data.length));

            // Perform STFT
            // TODO: NEEDS CONVERT - STFT and proc_buffer both include or are jeigen types
            // TODO: (MEL) removed in mean-time
            proc_buffer.add(stft(toDouble(proc_data), window_size, hop_size, num_windows,
                    window_coeff, fft/*, frequency_range, melscale_range*/));
            Log.v("TESTESTESTSET", String.format("%f", min(proc_buffer.get(0))));
        }
    }

    private double [] window_func(int window_size, int window_type) {
        double [] ret = new double[window_size];

        // Implementation of the HANN window
        if (window_type == HANN){
            for(int i = 0; i < window_size; i++){
                ret[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / window_size));
            }
        }

        return ret;
    }

    // TODO: ojalgo: CHECK - certain function calls may not do what you think, .size() .multiply, especially between Matrices and Matrices
    // TODO: (MEL) removed in mean-time.
    private Primitive64Store stft(double[] data, int window_size, int hop_size, int num_windows,
                             double[] window_coeff, FFT fft/*, DenseMatrix frequency_range,
                             DenseMatrix melscale_range*/) {
        double [] window;
        double [] spec_window_real;
        double [] spec_window_imag;
        PhysicalStore.Factory<Double, Primitive64Store> spectrogramFACT = Primitive64Store.FACTORY;
        Primitive64Store spectrogram = spectrogramFACT.make(num_windows, proc_fft_size/2);
        double [][] temp_spectrogram = new double[num_windows][proc_fft_size/2];
        // TODO: (MEL) removed in mean-time
//        DenseMatrix mel_window = new DenseMatrix(1, 1);

        for (int i = 0; i < num_windows; i++) {
            // Extracting window from data
            window = mul(getValuesInRange(data, i*hop_size, i * hop_size + window_size), window_coeff);
            // Calculating the FFT of the window
            fft.fft(window);
            // Use only half of the FFT output
            spec_window_real = getValuesInRange(fft.getFFTReal(), 0, proc_fft_size/2);
            spec_window_imag = getValuesInRange(fft.getFFTImag(), 0, proc_fft_size/2);
            // Finding absolute values of complex matrix, scaling
            double [] temp_real_spec_window = abs(spec_window_real, spec_window_imag);
            // Adding the window to the spectrogram
            for (int j = 0; j < proc_fft_size/2; j++) {
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
        spectrogram = createMatrix(temp_spectrogram);
//        int iter = 0;
//        for (int j = 0; j < temp_spectrogram[0].length; j++){
//            for (int i = 0; i < temp_spectrogram.length; i++){
//                spectrogram.set(iter, temp_spectrogram[i][j]*temp_spectrogram[i][j]);
//                iter++;
//            }
//        }
//        temp_spectrogram = null;
        // Transposing the spectrogram to correct orientation
//        spectrogram = Primitive64Store.FACTORY.transpose(spectrogram);
        spectrogram = pow(Primitive64Store.FACTORY.transpose(spectrogram), 2);
        // Normalising the spectrogram as well as converting to dB
        double reference = min(spectrogram);

        Log.v("AudioProcessing", String.format("STFT Summary:" +
                        "\n\tinput fft size (rows):\t\t%d" +
                        "\n\tinput num frames (cols):\t%d" +
                        "\n\toutput fft size (rows):\t\t%d" +
                        "\n\toutput num frames (cols):\t%d" +
                        "\nPre-calculated output sizes" +
                        "\n\tfft size (proc_fft_size/2):\t%d" +
                        "\n\tnum windows (calculated):\t%d", proc_fft_size, proc_num_time_frames,
                spectrogram.countRows(), spectrogram.countColumns(), proc_fft_size/2, num_windows));

        return spectrogram;//ampToDb(spectrogram, reference);
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
    private double[] specToMel(DenseMatrix array, DenseMatrix freq_range, DenseMatrix mel_range){
        int freq_count = 0;
        int mel_count = 0;

        int mel_temp_avg_iter = 0;
        // TODO: Change these to array lists to improve space use and remove size quess work
        double mel_temp_avg_array = 0.0;
//        DenseMatrix mel_temp_avg_array = new DenseMatrix(proc_fft_size, 1);

//        int mel_scale_spectrum_iter = 0;
        // TODO: change this to arraylist as well
        ArrayList<Double> mel_scale_spectrum = new ArrayList<>();
//        DenseMatrix mel_scale_spectrum = new DenseMatrix(proc_fft_size, 1);

        while (freq_count < (int) (proc_fft_size/2)) {
            // Checking if in range
            if (freq_range.getValues()[freq_count] <= mel_range.getValues()[mel_count]) {
                mel_temp_avg_array = mel_temp_avg_array + array.getValues()[freq_count];
//                mel_temp_avg_array.set(mel_temp_avg_iter, array.getValues()[freq_count]);
                mel_temp_avg_iter++;
                freq_count++;
            } else {
                if (mel_temp_avg_iter == 0) {
                    mel_scale_spectrum.add(array.getValues()[freq_count]);
//                    mel_scale_spectrum.set(mel_scale_spectrum_iter, array.getValues()[freq_count]);
                } else if (mel_temp_avg_iter == 1) {
                    mel_scale_spectrum.add(mel_temp_avg_array);
//                    mel_scale_spectrum.set(mel_scale_spectrum_iter,
//                            mel_temp_avg_array.getValues()[0]);
                    mel_temp_avg_array = 0.0;
                } else {
                    mel_scale_spectrum.add(mel_temp_avg_array/(mel_temp_avg_iter*1.0));
//                    mel_scale_spectrum.set(mel_scale_spectrum_iter, getMean(mel_temp_avg_array,
//                            mel_temp_avg_iter));
                    mel_temp_avg_array = 0.0;

                }
                mel_count++;
                mel_temp_avg_iter = 0;
//                mel_scale_spectrum_iter++;
            }
        }
        double[] ret = new double[mel_scale_spectrum.size()];
        for (int i = 0; i < mel_scale_spectrum.size(); i++){
            ret[i] = mel_scale_spectrum.get(i);
        }
        return ret;
    }


    // Classifier
    private void configureClassifier(TextView model_filename_view,
                                     ImageView model_filename_marker_view) {
        /* Configures classifier variables before the start of any sub-systems*/

        // Importing model file
        classifier_imported_nmf_model = null;
        if (isValidModelFilename(model_filename_view, model_filename_marker_view)) {
            classifier_imported_nmf_model=importModelFile(model_filename_view.getText().toString());
        }
        if (classifier_imported_nmf_model == null) {
            Log.e("Classifier", "The import failed somewhere, the returned object is null");
            // TODO: Handle failures better, maybe inform user and revert to default values?
            return;
        }
        Log.v("Classifier", String.format("Imported values:" +
                        "\n\tW1 (shape): (%d,%d)\n\tW2 (shape): (%d, %d)\n\tW1 example value: %f" +
                        "\n\tW2 example value: %f",
                classifier_imported_nmf_model.W1.length, classifier_imported_nmf_model.W1[0].length,
                classifier_imported_nmf_model.W2.length, classifier_imported_nmf_model.W2[0].length,
                classifier_imported_nmf_model.W1[0][0], classifier_imported_nmf_model.W2[0][0]));

        // Input variable clearing / initialisation
        classifier_data = null;
        // Buffer clearing / initialisation
        classifier_buffer = new ArrayList<>();
    }

    private void startClassifier() {
        /* Finalises configuration and starts sub-system */
        // Set the activity flag to true as sub-system is starting
        mt_classifier_flag = true;
        // Start pre-processing thread
        new Thread(mt_classifier_runnable).start();
    }

    private void stopClassifier() {
        // Stopping classifier thread
        mt_classifier_flag = false;
    }

    private void classifier() {
        // Calculating number of time frames
        int num_windows = (int) Math.floor((cap_time_interval * proc_sample_rate * 1.0 -
                (int) Math.ceil(proc_window_time * proc_sample_rate)) /
                (int) Math.ceil(proc_hop_time * proc_sample_rate))+1;
        // Initialising the NMF class
        NMF nmf = new NMF(
                classifier_fft_size,
                num_windows,
                classifier_num_classes,
                classifier_num_inter_comp,
                classifier_num_iters
        );
        // Loading Dictionaries
        Log.v("Classifier", "Loading imported model dictionaries...");
        nmf.loadW1(classifier_imported_nmf_model.W1);
        nmf.loadW2(classifier_imported_nmf_model.W2);
        nmf.loadTrainingError(classifier_imported_nmf_model.training_cost);

        while(mt_classifier_flag) {
            Log.v("Classifier", "Classifier thread is active...");
            // Reading data from the processing buffer queue
            while (proc_buffer == null || proc_buffer.size() == 0) {
                try {
                    Log.v("Classifier", "waiting for processing buffer to fill...");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!mt_classifier_flag) {
                    return;
                }
            }
//            classifier_data = proc_buffer.remove(0);
            // Loading the data into the nmf class object
//            nmf.loadV1(classifier_data);
            // Starting nmf calculation
//            nmf.start();
            // Retrieving results from nmf class object
//            classifier_buffer.add(nmf.getW2());
            // Adding results to classifier buffer
            // TODO: classifier: implement buffer addition
//            classifier_buffer.add()
        }
    }

    // Result display
    private void tempTestPlot(ImageView image){
        // Setting the matrix size
        int num_freq_bins = 128;
        int num_time_frames = 500;
        // Creating the matrix
        Primitive64Store spec_matrix = (Primitive64Store) Primitive64Store.FACTORY.makeZero(
                num_freq_bins, num_time_frames);
//        DenseMatrix spec_matrix = DenseMatrix.zeros(num_freq_bins, num_time_frames);
        spec_matrix.set(100,50,0.5);
        spec_matrix.set(100,51,1);

        // Normalise the matrix

        double temp_max = max(spec_matrix);
        for (int i = 0; i < num_freq_bins; i++){
            for (int j = 0; j < num_time_frames; j++){
                temp_max = Math.max(spec_matrix.get(i,j), temp_max);
            }
        }
//        spec_matrix = spec_matrix.div(temp_max);

        SpectrogramView sp_view_obj = new SpectrogramView(this, spec_matrix, image.getWidth());
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
        Primitive64Store temp = createMatrix(height, Math.abs(width));

        int iter = (int)(-height/2.0);
        for (int i = 0; i < height; i++){
            for (int j = 0; j < width; j++){
                if ((Math.abs(proc_data[j]/(1.0*max))*(height/2.0)) >= Math.abs(iter)){
                    temp.set(i,j,1);
                } else {
                    temp.set(i, j, 0);
                }
            }
            iter++;
        }

        SpectrogramView sp_view_obj = new SpectrogramView(this, temp, image.getWidth());
        image.setImageBitmap(sp_view_obj.bmp);
    }
    private void tempProcPlot(ImageView image) {
        // Extract spectrogram from the proc_buffer queue
        while (proc_buffer == null || proc_buffer.size() <= 2) {}
        Primitive64Store temp = proc_buffer.remove(2);
        Primitive64Store invert_temp = Primitive64Store.FACTORY.make(temp.countRows(), temp.countColumns());// createMatrix(temp.countRows(), temp.countColumns());
        for (long i = temp.countRows()-1; i >= 0; i--){
            for (long j = 0; j < temp.countColumns(); j++) {
                invert_temp.set(i,j, temp.get(temp.countRows()-(i+1),j));
            }
        }
        Log.v("display", String.format("temp -rows:%d, -cols:%d, value:%f", invert_temp.countRows(), invert_temp.countColumns(), invert_temp.get(0)));
        Primitive64Store val = Primitive64Store.FACTORY.make(invert_temp.divide(max(invert_temp)));
        SpectrogramView sp_view_obj = new SpectrogramView(this, val, image.getWidth());
        image.setImageBitmap(sp_view_obj.bmp);
    }
    private void tempClassifierPlot(ImageView image) {
        // Extract spectrogram from the classifier_buffer queue
        while (classifier_buffer == null || classifier_buffer.size() == 0) {}
        Primitive64Store temp = classifier_buffer.remove(0);
//        DenseMatrix invert_temp = new DenseMatrix(temp.rows, temp.cols);
//        for (int i = temp.rows-1; i >= 0; i--){
//            for (int j = 0; j < temp.cols; j++) {
//                invert_temp.set(i,j, temp.get(temp.rows-(i+1),j));
//            }
//        }
//        Log.v("display", String.format("temp -rows:%d, -cols:%d, value:%f", invert_temp.rows, invert_temp.cols, invert_temp.getValues()[0]));
//        SpectrogramView sp_view_obj = new SpectrogramView(this, invert_temp.div(invert_temp.maxOverCols().maxOverRows().getValues()[0]), image.getWidth());
        SpectrogramView sp_view_obj = new SpectrogramView(this, (Primitive64Store) temp.divide(max(temp)), image.getWidth());
        image.setImageBitmap(sp_view_obj.bmp);
    }

    // Data handling


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
    private Primitive64Store ampToDb(Primitive64Store matrix, double ref) {
        return  Primitive64Store.FACTORY.make(log(Primitive64Store.FACTORY.make(matrix.divide(ref)), 10).multiply(20));
    }

    // Convert from byte array to short []
    private short[] bytesToShort(byte[] array, int format) {
        short [] ret;
        if (format == AUDIO_FORMAT_INT8){
            ret = new short[(int)(array.length)];
        } else if (format == AUDIO_FORMAT_INT16) {
            ret = new short[(int) (array.length / 2)];
        } else if (format == AUDIO_FORMAT_FLOAT) {
            ret = new short[(int) (array.length / 4)];
        } else {
            ret = new short[(int)(array.length/2)];
        }
//        short [] ret = new short[(int)(array.length/2)];

        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.order(ByteOrder.nativeOrder());
        int count = 0;
        while(buffer.hasRemaining()){
            ret[count++] = buffer.getShort();
        }
        return ret;

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

    // Gets the even index values from an array
    private Primitive64Store getEvenValues(Primitive64Store array) {
        Primitive64Store ret;
        int length = array.size();
        if (length % 2 == 0){
            ret = createMatrix((int)(length/2), 1);
        } else {
            ret = createMatrix((int) Math.ceil(length/2.0), 1);
        }

//        int iter = 0;
//        for (int i = 0; i < array.getValues().length; i=i+2){
//            ret.set(iter, array.get(i,0));
//            iter++;
//        }
        for (int i = 0; i < array.size()/2; i++){
            ret.set(i, array.get(2*i));
        }

        return ret;
    }
    // Gets the odd index values from an array
    private Primitive64Store getOddValues(Primitive64Store array) {
        Primitive64Store ret;
        int length = array.size();
        if (length % 2 == 0){
            ret = createMatrix((int)(length/2), 1);
        } else {
            ret = createMatrix((int) Math.floor(length/2.0), 1);
        }

//        int iter = 0;
//        for (int i = 1; i < array.getValues().length; i=i+2){
//                ret.set(iter, array.get(i,0));
//                iter++;
//        }
        for (int i = 0; i < array.size()/2; i++) {
            ret.set(i, array.get(i*2+1));
        }

        return ret;
    }

    // Matrices
    // Create a matrix, filled with scalar value
    private Primitive64Store createMatrix(long rows, long columns, double value) {
        PhysicalStore.Factory<Double, Primitive64Store> storeFactory = Primitive64Store.FACTORY;
        Primitive64Store ret = storeFactory.make(rows, columns);
        for (int i = 0; i < ret.size(); i++){
            ret.set(i, value);
        }
        return ret;
    }
    // Create a matrix - empty, shape only
    private Primitive64Store createMatrix(long rows, long columns) {
        PhysicalStore.Factory<Double, Primitive64Store> storeFactory = Primitive64Store.FACTORY;
        return storeFactory.make(rows, columns);
    }
    // Create a matrix - filled, from 2D double
    private Primitive64Store createMatrix(double[][] matrix) {
        return Primitive64Store.FACTORY.rows(matrix);
    }
    // Creates a Primitive64Store "array", with values in-between min (inclusive) and max
    // (exclusive), with steps of size.
    private Primitive64Store createArray(double min, double max, int step){
        int size = (int) Math.ceil((max - min) / step);
        Primitive64Store temp = createMatrix(size, 1);
        for (int i = 0; i < size; i++){
            temp.set(i,(step * i) + min);
        }
        return temp;
    }

    // Mathematics
    // Element-wise multiplication of matrix
    private Primitive64Store mul(Primitive64Store matrix1, Primitive64Store matrix2){
        // TODO: check that .size, countRows, countColumns does what you think.
        Primitive64Store ret = createMatrix(matrix1.countRows(), matrix1.countColumns());
        for (long i = 0; i < ret.size(); i++) {
            ret.set(i, matrix1.get(i) * matrix2.get(i));
        }
        return ret;
    }
    // Element-wise divide of matrix
    private Primitive64Store divide(Primitive64Store numerator, Primitive64Store denominator) {
        Primitive64Store ret = createMatrix(numerator.countRows(), numerator.countColumns());
        for (long i = 0; i < ret.size(); i++) {
            ret.set(i, numerator.get(i) / denominator.get(i));
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
    private Primitive64Store pow(Primitive64Store matrix, double expon) {
        // TODO: ojalgo CHECK - is the .size() the product of rows and cols
        for (int i = 0; i < matrix.size(); i++){
            matrix.set(i, Math.pow(matrix.get(i), expon));
        }
        return matrix;
    }
    // Element-wise logarithm of matrix, with base
    private Primitive64Store log(Primitive64Store matrix, int log_base) {
        for (long i = 0; i < matrix.size(); i++){
            matrix.set(i, Math.log(matrix.get(i))/Math.log(log_base));
        }
        return matrix;
    }
    // Element-wise logarithm of matrix, natural log
    private Primitive64Store log(Primitive64Store matrix) {
        for (long i = 0; i < matrix.size(); i++){
            matrix.set(i, Math.log(matrix.get(i)));
        }
        return matrix;
    }

    // Sum of matrix
    private double sum(Primitive64Store matrix) {
        // TODO: see that size does what you think
        double value = 0;
        for (int i = 0; i < matrix.size(); i++){
            value = value + matrix.get(i);
        }
        return value;
    }
    // Mean of matrix
    private double mean(Primitive64Store array) {
        return sum(array)/array.size();
    }
    // Absolute of complex array
    private double[] abs(double [] real, double [] imag) {
        double [] ret = new double[real.length];
        for(int i = 0; i < real.length; ++i) {
            ret[i] = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
        }
        return ret;
    }
    // Get smallest value in the matrix
    private double min(Primitive64Store matrix) {
        double ret = matrix.get(0);
        for (long i = 0; i < matrix.size(); i++){
            ret = Math.min(matrix.get(i), ret);
        }
        return ret;
    }
    // Get largest value in the matrix
    private double max(Primitive64Store matrix) {
        double ret = matrix.get(0);
        for (long i = 0; i < matrix.size(); i++){
            ret = Math.max(matrix.get(i), ret);
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

    private DenseMatrix convertPD(Primitive64Store temp){
        DenseMatrix ret = new DenseMatrix((int) temp.countRows(), (int) temp.countColumns());
        for (int i = 0; i < temp.size(); i ++){
            ret.set(i, temp.get(i));
        }
        return ret;
    }
    private Primitive64Store convertDP(DenseMatrix temp) {
        Primitive64Store ret = createMatrix(temp.rows, temp.cols);
        for (int i = 0; i < temp.getValues().length; i ++){
            ret.set(i, temp.getValues()[i]);
        }
        return ret;
    }
}
