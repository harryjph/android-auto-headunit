package ca.yyx.hu.aap;

/**
 * @author algavris
 * @date 01/10/2016.
 */

class AapMicrophone {
    private int mic_change_status = 0;

    int hu_aap_mic_get() {
        int ret_status = mic_change_status;                                 // Get current mic change status
        if (mic_change_status == 2 || mic_change_status == 1) {             // If start or stop...
            mic_change_status = 0;                                            // Reset mic change status to "No Change"
        }
        return (ret_status);                                                // Return original mic change status
    }

    void setStatus(int status) {
        mic_change_status = status;
    }
}