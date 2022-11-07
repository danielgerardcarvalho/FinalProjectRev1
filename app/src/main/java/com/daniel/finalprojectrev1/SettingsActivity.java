package com.daniel.finalprojectrev1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.daniel.finalprojectrev1.databinding.SettingsActivityBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

public class SettingsActivity extends AppCompatActivity {

    private SettingsActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = SettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        /* Capture Setting Inputs */
        EditText cap_sample_rate_input = (EditText) findViewById(R.id.input_cap_sample_rate);
        EditText cap_time_interval_input = (EditText) findViewById(R.id.input_cap_time_interval);
        Button cap_file_import_select_input = (Button) findViewById(R.id.input_cap_file_import_select);
        Spinner cap_format_input = (Spinner) findViewById(R.id.input_cap_format_option);

        /* Processing Setting Inputs */
        EditText proc_fft_size_input = (EditText) findViewById(R.id.input_proc_fft_size);
        EditText proc_sample_rate_input = (EditText) findViewById(R.id.input_proc_sample_rate);
        EditText proc_num_time_frames_input = (EditText) findViewById(R.id.input_proc_num_time_frames);
        EditText proc_resolution_input = (EditText) findViewById(R.id.input_proc_resolution);
        EditText proc_window_time_input = (EditText) findViewById(R.id.input_proc_window_time);
        EditText proc_hop_time_input = (EditText) findViewById(R.id.input_proc_hop_time);

        /* Classifier Setting Inputs */
        EditText classifier_fft_size_input = (EditText) findViewById(R.id.input_classifier_fft_size);
        EditText classifier_num_classes_input = (EditText) findViewById(R.id.input_classifier_num_classes);
        EditText classifier_num_inter_comp_input = (EditText) findViewById(R.id.input_classifier_num_inter_comp);
        EditText classifier_num_iters_input = (EditText) findViewById(R.id.input_classifier_num_iters);

        /* Apply Setting Button */
        Button apply_settings_button = (Button) findViewById(R.id.apply_select);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        /* Configure User Selection Register */
        ActivityResultLauncher<Intent> activity_launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK){
//                    String file_loc = result.getData().getData().getPath().split(":")[1];
//                    file_loc = file_loc.split(":")[1];
                        assert result.getData() != null;
                        Globals.cap_imported_file = new File(
                                Environment.getExternalStorageDirectory().toString() +
                                        "/" + result.getData().getData().getPath().split(":")[1]
                        );
                    }
                });

        /* File import button logic*/
        cap_file_import_select_input.setOnClickListener(button -> {
            if (!Globals.cap_file_import_flag){
                Globals.cap_file_import_flag = true;
            }
            Globals.cap_imported_file = null;
            Globals.cap_imported_file_stream = null;

            getUserSelectedFile(activity_launcher);
        });

        /* Apply button logic */
        apply_settings_button.setOnClickListener(button -> {
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
            // Checking validity of settings
            if (!valid_flag) {
                Snackbar.make(button, "Settings are not valid", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }
            Snackbar.make(button, "Saving Settings", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            // Import model file
            configureModel(model_filename_view, model_filename_marker_view);

            // Setting system settings applied flag
            Globals.sys_settings_flag = true;
        });

        /* Model Import Filename Update Logic */
        modelFilenameUpdate(model_filename_view,  model_filename_marker_view, proc_fft_size_input,
                num_class_events_text, clip_len_text, num_overlaps_text, snr_range_min_text,
                snr_range_max_text, num_training_sample_text
        );

        if (Globals.sys_settings_flag){
            uiFieldUpdate(model_filename_view, num_class_events_text, clip_len_text,
                    num_overlaps_text, snr_range_min_text, snr_range_max_text,
                    num_training_sample_text, cap_sample_rate_input, cap_time_interval_input,
                    cap_format_input, proc_fft_size_input, proc_sample_rate_input,
                    proc_num_time_frames_input, proc_resolution_input, proc_window_time_input,
                    proc_hop_time_input, classifier_fft_size_input, classifier_num_classes_input,
                    classifier_num_inter_comp_input, classifier_num_iters_input
            );
        }
    }


    /* Interface Related Methods */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If this method breaks try removing the super.
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            return;
        }
        if (data.getClipData() == null) {
            Globals.cap_imported_file = new File(
                    Environment.getExternalStorageDirectory().toString() +
                            "/" + data.getData().getPath().split(":")[1]
            );
//            Globals.cap_imported_file = new File (data.getData().getPath());
        } else {
            Globals.cap_imported_file = new File(data.getClipData().getItemAt(0).getUri().getPath());
        }
    }

    private void modelFilenameUpdate(TextView model_filename_view, ImageView model_filename_marker_view,
                                     EditText proc_fft_size_input, EditText num_class_events_text,
                                     EditText clip_len_text, EditText num_overlaps_text,
                                     EditText snr_range_min_text, EditText snr_range_max_text,
                                     EditText num_training_sample_text){

        // Updating model filename
        proc_fft_size_input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                model_filename_view.setText(
                        // ORIGINAL IMPLEMENTATION
                        // String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        // FFT SIZE REPORTING IMPLEMENTATION
                        String.format("FS%sdictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                                proc_fft_size_input.getText().toString(),
                        // -----
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
            public void afterTextChanged(Editable editable) {}
        });
        num_class_events_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                model_filename_view.setText(
                        // ORIGINAL IMPLEMENTATION
                        // String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        // FFT SIZE REPORTING IMPLEMENTATION
                        String.format("FS%sdictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        proc_fft_size_input.getText().toString(),
                        // -----
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
                model_filename_view.setText(
                        // ORIGINAL IMPLEMENTATION
                        // String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        // FFT SIZE REPORTING IMPLEMENTATION
                        String.format("FS%sdictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        proc_fft_size_input.getText().toString(),
                        // -----
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
                model_filename_view.setText(
                        // ORIGINAL IMPLEMENTATION
                        // String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        // FFT SIZE REPORTING IMPLEMENTATION
                        String.format("FS%sdictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        proc_fft_size_input.getText().toString(),
                        // -----
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
                model_filename_view.setText(
                        // ORIGINAL IMPLEMENTATION
                        // String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        // FFT SIZE REPORTING IMPLEMENTATION
                        String.format("FS%sdictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        proc_fft_size_input.getText().toString(),
                        // -----
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
                model_filename_view.setText(
                        // ORIGINAL IMPLEMENTATION
                        // String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        // FFT SIZE REPORTING IMPLEMENTATION
                        String.format("FS%sdictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        proc_fft_size_input.getText().toString(),
                        // -----
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
                model_filename_view.setText(
                        // ORIGINAL IMPLEMENTATION
                        // String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        // FFT SIZE REPORTING IMPLEMENTATION
                        String.format("FS%sdictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                        proc_fft_size_input.getText().toString(),
                        // -----
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

    private boolean isValidModelFilename(TextView filename, ImageView marker) {
        File temp = new File(Globals.MODEL_DIR_LOC + filename.getText() + Globals.MODEL_FILE_EXT);
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


    /* Saving settings */
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
        // - model import number of event classes
        if (!TextUtils.isEmpty(num_class_events_text.getText())) {
            Globals.model_num_class_events = Integer.parseInt(num_class_events_text.getText().toString());
        } else {
            Globals.model_num_class_events = Integer.parseInt(num_class_events_text.getHint().toString());
        }
        // - model import clip length
        if (!TextUtils.isEmpty(clip_len_text.getText())) {
            Globals.model_clip_len = Integer.parseInt(clip_len_text.getText().toString());
        } else {
            Globals.model_clip_len = Integer.parseInt(clip_len_text.getHint().toString());
        }
        // - model import number of overlaps
        if (!TextUtils.isEmpty(num_overlaps_text.getText())) {
            Globals.model_num_overlaps = Integer.parseInt(num_overlaps_text.getText().toString());
        } else {
            Globals.model_num_overlaps = Integer.parseInt(num_overlaps_text.getHint().toString());
        }
        // - model import snr range min
        if (!TextUtils.isEmpty(snr_range_min_text.getText())) {
            Globals.model_snr_range_min = Integer.parseInt(snr_range_min_text.getText().toString());
        } else {
            Globals.model_snr_range_min = Integer.parseInt(snr_range_min_text.getHint().toString());
        }
        // - model import snr range max
        if (!TextUtils.isEmpty(snr_range_max_text.getText())) {
            Globals.model_snr_range_max = Integer.parseInt(snr_range_max_text.getText().toString());
        } else {
            Globals.model_snr_range_max = Integer.parseInt(snr_range_max_text.getHint().toString());
        }
        // - model import number of training samples
        if (!TextUtils.isEmpty(num_training_sample_text.getText())) {
            Globals.model_num_training_samples = Integer.parseInt(num_training_sample_text.getText().toString());
        } else {
            Globals.model_num_training_samples = Integer.parseInt(num_training_sample_text.getHint().toString());
        }
        // model import number of internal components
        if (!TextUtils.isEmpty(num_inter_comp_text.getText())) {
            Globals.model_num_inter_comp = Integer.parseInt(num_inter_comp_text.getText().toString());
        } else {
            Globals.model_num_inter_comp = Integer.parseInt(num_inter_comp_text.getHint().toString());
        }

        // Capture settings
        // - capture sample rate
        if (!TextUtils.isEmpty(cap_sample_rate_input.getText())) {
            Globals.cap_sample_rate = Integer.parseInt(cap_sample_rate_input.getText().toString());
        } else {
            Globals.cap_sample_rate = Integer.parseInt(cap_sample_rate_input.getHint().toString());
        }
        // - capture time interval
        if (!TextUtils.isEmpty(cap_time_interval_input.getText())) {
            Globals.cap_time_interval = Integer.parseInt(cap_time_interval_input.getText().toString());
        } else {
            Globals.cap_time_interval = Globals.model_clip_len;
        }
        // - capture format
        Globals.cap_format = Globals.AUDIO_FORMAT_INT16;
        String selected_audio_format = cap_format_input.getSelectedItem().toString();
        if (selected_audio_format.matches(Globals.UI_AUDIO_FORMAT_INT16)) {
            Globals.cap_format = Globals.AUDIO_FORMAT_INT16;
        } else if (selected_audio_format.matches(Globals.UI_AUDIO_FORMAT_FLOAT)){
            Globals.cap_format = Globals.AUDIO_FORMAT_FLOAT;
        }

        // Processing Settings
        // - processing fft size
        if (!TextUtils.isEmpty(proc_fft_size_input.getText())) {
            Globals.proc_fft_size = Integer.parseInt(proc_fft_size_input.getText().toString());
        } else {
            Globals.proc_fft_size = Integer.parseInt(proc_fft_size_input.getHint().toString());
        }
        // - processing sample rate
        if (!TextUtils.isEmpty(proc_sample_rate_input.getText())) {
            Globals.proc_sample_rate = Integer.parseInt(proc_sample_rate_input.getText().toString());
        }
        else {
            Globals.proc_sample_rate = Globals.cap_sample_rate;
        }
        // - processing number of time frames
        Globals.proc_num_time_frames = Globals.cap_time_interval * Globals.proc_sample_rate;
        // - processing resolution
        Globals.proc_resolution = Globals.proc_sample_rate / Globals.proc_fft_size;
        // - processing window time
        Globals.proc_window_time = Globals.proc_fft_size / (Globals.proc_sample_rate * 1.0);
        // - processing hop time
        Globals.proc_hop_time = Globals.proc_window_time / 2.0;

        // Classifier Settings
        // - classifier fft size
        if (!TextUtils.isEmpty(classifier_fft_size_input.getText())) {
            Globals.classifier_fft_size = Integer.parseInt(classifier_fft_size_input.getText().toString());
        } else {
            Globals.classifier_fft_size = Globals.proc_fft_size / 2;
        }
        // - classifier number of  classes
        if (!TextUtils.isEmpty(classifier_num_classes_input.getText())) {
            Globals.classifier_num_classes = Integer.parseInt(classifier_num_classes_input.getText().toString());
        } else {
            Globals.classifier_num_classes = Globals.model_num_class_events;
        }
        // - classifier number of internal components
        if (!TextUtils.isEmpty(classifier_num_inter_comp_input.getText())) {
            Globals.classifier_num_inter_comp = Integer.parseInt(classifier_num_inter_comp_input.getText().toString());
        } else {
            Globals.classifier_num_inter_comp = Globals.model_num_inter_comp * Globals.model_num_training_samples;
        }
        // - classifier number of internal iterations
        if (!TextUtils.isEmpty(classifier_num_iters_input.getText())) {
            Globals.classifier_num_iters = Integer.parseInt(classifier_num_iters_input.getText().toString());
        } else {
            Globals.classifier_num_iters = Integer.parseInt(classifier_num_iters_input.getHint().toString());
        }
        // Number of internal components in NMF model, taken from imported data
        Globals.classifier_num_inter_comp = Globals.model_num_inter_comp * Globals.model_num_training_samples;

        isValidModelFilename(model_filename_view, model_filename_marker_view);

        return true;
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
        num_class_events_text.setText(String.format("%d", Globals.model_num_class_events));
        clip_len_text.setText(String.format("%d", Globals.model_clip_len));
        num_overlaps_text.setText(String.format("%d", Globals.model_num_overlaps));
        snr_range_min_text.setText(String.format("%d", Globals.model_snr_range_min));
        snr_range_max_text.setText(String.format("%d", Globals.model_snr_range_max));
        num_training_sample_text.setText(String.format("%d", Globals.model_num_training_samples));
        model_filename_view.setText(String.format("dictW_cl%s_len%s_ol%s_snr(%s, %s)_tsize%s",
                num_class_events_text.getText().toString(),
                clip_len_text.getText().toString(),
                num_overlaps_text.getText().toString(),
                snr_range_min_text.getText().toString(),
                snr_range_max_text.getText().toString(),
                num_training_sample_text.getText().toString()
        ));
        // Capture
        cap_sample_rate_input.setText(String.format("%d", Globals.cap_sample_rate));
        cap_time_interval_input.setText(String.format("%d", Globals.cap_time_interval));
        if (Globals.cap_format == Globals.AUDIO_FORMAT_INT16) {
            cap_format_input.setSelection(0);
        } else if (Globals.cap_format == Globals.AUDIO_FORMAT_FLOAT) {
            cap_format_input.setSelection(1);
        }
        else {
            cap_format_input.setSelection(0);
        }
        // Processing
        proc_fft_size_input.setText(String.format("%d", Globals.proc_fft_size));
        proc_sample_rate_input.setText(String.format("%d", Globals.proc_sample_rate));
        proc_num_time_frames_input.setText(String.format("%d", Globals.proc_num_time_frames));
        proc_resolution_input.setText(String.format("%d", Globals.proc_resolution));
        proc_window_time_input.setText(String.format("%f", Globals.proc_window_time));
        proc_hop_time_input.setText(String.format("%f", Globals.proc_hop_time));
        // Classifier
        classifier_fft_size_input.setText(String.format("%d", Globals.classifier_fft_size));
        classifier_num_classes_input.setText(String.format("%d", Globals.classifier_num_classes));
        classifier_num_inter_comp_input.setText(String.format("%d", Globals.classifier_num_inter_comp));
        classifier_num_iters_input.setText(String.format("%d", Globals.classifier_num_iters));
    }


    /* Model Import */
    private void configureModel(TextView model_filename_view, ImageView model_filename_marker_view){
        Globals.classifier_imported_nmf_model = null;
        if (isValidModelFilename(model_filename_view, model_filename_marker_view)) {
            Globals.classifier_imported_nmf_model = importModelFile(model_filename_view.getText().toString());
        }
        if (Globals.classifier_imported_nmf_model == null) {
            Log.e("Classifier", "The import failed somewhere, the returned object is null");
            return;
        }
        Log.v("Classifier", String.format("Imported values:" +
                        "\n\tW1 (shape): (%d,%d)\n\tW2 (shape): (%d, %d)\n\tW1 example value: %f" +
                        "\n\tW2 example value: %f",
                Globals.classifier_imported_nmf_model.W1.length,
                Globals.classifier_imported_nmf_model.W1[0].length,
                Globals.classifier_imported_nmf_model.W2.length,
                Globals.classifier_imported_nmf_model.W2[0].length,
                Globals.classifier_imported_nmf_model.W1[0][0],
                Globals.classifier_imported_nmf_model.W2[0][0]));
    }

    private NMF.NMF_Mini importModelFile(String filename) {
        Log.v("ImportModelFile", "Attempting to import model file...");
        Log.v("ImportModelFile", "File path: " + Globals.MODEL_DIR_LOC + filename +
                Globals.MODEL_FILE_EXT);
        NMF.NMF_Mini nmf_mini = null;
        File file = new File(Globals.MODEL_DIR_LOC + filename + Globals.MODEL_FILE_EXT);
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

}