package com.daniel.finalprojectrev1;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class globals {
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

}
