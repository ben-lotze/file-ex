package rocks.informatik.fileex.tools;

import com.google.android.gms.ads.AdRequest;

import java.util.Arrays;
import java.util.List;

public final class AdmobHelper {

    private AdmobHelper() {
    }

    /* PLEASE ADD ALL TEST DEVICE IDs
     * - look for this line in Logcat to get device id (for real device):
     *   "Use AdRequest.Builder.addTestDevice("15A68CF6AA6977CCD019FE4EC38BD7B0") to get test ads on this device."
     *   TODO: use properties file on disk to re-use all test device IDs accross different projects?
     */
    private static final List<String> testDeviceIds = Arrays.asList(
            "15A68CF6AA6977CCD019FE4EC38BD7B0"  // Note 2
    );


    public static AdRequest createAdRequest(boolean includeTestDevices) {
        AdRequest.Builder builder = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR);

        for (String currentTestId : testDeviceIds) {
            builder.addTestDevice(currentTestId); // Galaxy Note 2
        }

        return builder.build();
    }

}
