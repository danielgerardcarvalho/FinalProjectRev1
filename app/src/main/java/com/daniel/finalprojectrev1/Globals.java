package com.daniel.finalprojectrev1;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.EditText;

import java.io.File;
import java.io.FileInputStream;

public class Globals {

    /* Model Import */
    // model import constants
    public static final String MODEL_FILE_EXT = ".json";
    public static final String MODEL_DIR_LOC = Environment.getExternalStorageDirectory().toString()
            + "/Project/dictionaries/";
    // model import variables
    public static File model_import_file;
    public static NMF.NMF_Mini classifier_imported_nmf_model; // nmf mini class object

    /* Buffer Constants */
    public static final int CAP_QUEUE_SIZE = 2;         // Capture output buffer
    public static final int PROC_QUEUE_SIZE = 1;        // Processing output buffer
    public static final int CLASSIFIER_QUEUE_SIZE = 1;  // Classifier output buffer

    /* Processing Constants */
    public static final int HANN = 0;         // HANN window constant

    /* Capture Settings Constants*/
    public static final int AUDIO_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT_INT8 = AudioFormat.ENCODING_PCM_8BIT;
    public static final int AUDIO_FORMAT_INT16 = AudioFormat.ENCODING_PCM_16BIT;
    public static final int AUDIO_FORMAT_FLOAT = AudioFormat.ENCODING_PCM_FLOAT;
    public static final int AUDIO_MIN_FORMAT_SIZE = 1;
    public static final int[] AUDIO_SOURCES = {MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.UNPROCESSED};
    // input format options
    public static final String UI_AUDIO_FORMAT_INT16 = "int16";
    public static final String UI_AUDIO_FORMAT_FLOAT = "float";


    /* UI Associated General Variables */
    public static boolean sys_settings_flag = false;

    /* UI Associated Model Import Settings */
    // Values
    public static int model_num_class_events;           // number of event classes in model
    public static int model_clip_len;                   // length of clip in model
    //public static int model_num_overlaps;               // number of overlaps in model
    public static int model_snr_range_min;              // snr range min in model
    //public static int model_snr_range_max;              // snr range max in model
    public static int model_num_training_samples = 5;       // number of training samples in model
    public static int model_num_inter_comp;             // number of componenets in model

    /* UI Associated Capture Settings */
    // Values
    public static int cap_sample_rate;                      // capture sample ratae
    public static int cap_time_interval;                    // capture time interval
    public static int cap_buffer_size;                      // capture buffer size
    public static boolean cap_file_import_flag = false;     // indicates if capture is file mode
    public static File cap_imported_file;                   // capture file
    public static FileInputStream cap_imported_file_stream;
    public static int cap_format;                           // the format of the capture

    /* UI Associated Processing Settings */
    // Values
    public static int proc_fft_size;
    public static int proc_sample_rate;
    public static int proc_num_time_frames;
    public static int proc_resolution;
    public static double proc_window_time;
    public static double proc_hop_time;

    /* UI Associated Classifier Settings */
    // Values
    public static int classifier_fft_size;
    public static int classifier_num_classes;
    public static int classifier_num_inter_comp;
    public static int classifier_num_iters;

    public static void loadDefaults(EditText cap_time, EditText num_iters) {
        // Model import settings
        model_num_class_events = 5;                     // number of event classes in model
        model_clip_len = 15;                            // length of clip in model
        model_snr_range_min = 0;                        // snr range min in model
        model_num_training_samples = 5;                 // number of training samples in model
        model_num_inter_comp = 20;                      // number of componenets in model

        // Capture settings
        cap_sample_rate = 20000;
        if (!TextUtils.isEmpty(cap_time.getText())) {
            cap_time_interval = Integer.parseInt(cap_time.getText().toString());
        } else {
            cap_time_interval = 15;
        }
        cap_format = AUDIO_FORMAT_INT16;

        // Processing settings
        proc_fft_size = 1024;
        proc_sample_rate = cap_sample_rate;
        proc_num_time_frames = cap_time_interval * proc_sample_rate;
        proc_resolution = proc_sample_rate / proc_fft_size;
        proc_window_time = proc_fft_size / (proc_sample_rate * 1.0);
        proc_hop_time = proc_window_time / 2.0;

        // Classifier settings
        classifier_fft_size = 512;
        classifier_num_classes = 5;
        classifier_num_inter_comp = 20 * 5;
        if (!TextUtils.isEmpty(num_iters.getText())) {
            classifier_num_iters = Integer.parseInt(num_iters.getText().toString());
        } else {
            classifier_num_iters = Integer.parseInt(num_iters.getHint().toString());
        }
    }
}
