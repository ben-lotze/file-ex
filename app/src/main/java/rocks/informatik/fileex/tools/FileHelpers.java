package rocks.informatik.fileex.tools;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import timber.log.Timber;

public final class FileHelpers {

    private static final String TAG = FileHelpers.class.getSimpleName();

    private static final String[] sizeNames = {"Byte", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    private static List<String> audioExtensions = Arrays.asList(
            "mp3", "flac", "m4a", "ogg", "wav", "aiff", "ape", "aac", "wma"
    );

    private FileHelpers() {
    }


    public static String getExtensionString(File file) {
        if (file.isDirectory()) {
            return null;
        }
        // extensions may have different lengths -> use everything after last dot
        // TODO: edge case: how to handle files with dots in their name that may not have an extension?
        String fileName = file.getName();
        int indexLastDot = TextUtils.lastIndexOf(fileName, '.');
        // files beginning with a dot are hidden (rest does not count as extension!)
        if (indexLastDot > 0) {
            int length = fileName.length();
            String extension = fileName.substring(indexLastDot + 1);
            return extension.toLowerCase();
        }
        // if no dot in filename
        return null;
    }

    public static List<File> getFolderContents(final String folderStr) {

        // TODO: API>21 can check for specific path!
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {

            AsyncTask<Void, Void, List<File>> task = new AsyncTask<Void, Void, List<File>>() {
                @Override
                protected List<File> doInBackground(Void... voids) {
                    Timber.d("getFolderContents(): doInBackground() called for " + folderStr);
                    File folder = new File(folderStr);
                    if (folder != null) {
                        File[] filesArray = folder.listFiles();
                        if (filesArray != null) {
                            Timber.d("listFiles() count="+filesArray.length);
                            List<File> files = new ArrayList<>(Arrays.asList(filesArray));
                            Collections.sort(files);
                            return files;
                        } else {
                            Timber.d("listFiles() result is NULL!");
                        }
                        return null;

                        // TODO: add filter possibility (maybe based on predicates from Guava)
                    }
                    else {
                        Timber.d("folder is null!");
                    }

                    return null;
                }

            }.execute();


            try {
                List<File> files = task.get();
                Timber.d("getFolderContents() for " + folderStr + ": count="
                        + (files==null ? "NULL" : files.size())
                );
                return files;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        } else {
            return new ArrayList<>();
        }
        // in case of errors
        return new ArrayList<>();
    }


    public static void startFileInAssociatedApp(Context context, File clickedFile) {
        String extension = FileHelpers.getExtensionString(clickedFile);

//        // file handling with content URIs enforced by >Nougat devices
        String authority = context.getApplicationContext().getPackageName() + ".provider";
        Timber.d("FileProvider authority: " + authority);
        Uri fileUri = FileProvider.getUriForFile(context, authority, clickedFile);

        MimeTypeMap mimeMap = MimeTypeMap.getSingleton();
        Intent intentViewFile = new Intent(Intent.ACTION_VIEW);
        String mimeType = mimeMap.getMimeTypeFromExtension(extension);
//        Timber.d("URI to open: " + fileUri);
//        Timber.d("MIME type: " + mimeType);

        intentViewFile.setDataAndType(fileUri, mimeType);
        if (mimeType.contains("image")) {
            intentViewFile.setDataAndType(fileUri, "image/*");
        }
        intentViewFile.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentViewFile.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intentViewFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intentViewFile);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No idea how to handle that file... yet... sorry!",
                    Toast.LENGTH_LONG).show();
        }
    }




    // TODO: test all supported extensions -> WAV has no tags (should get normal file view)
    public static List<String> getAudioFileExtensions() {
        return audioExtensions;
    }


    public static long getFreeSpaceForPath(String pathStr) {
        File path = new File(pathStr);
        // getUsableSpace() instead of getFreeSpace() which is only free to be used by root!
        // working with StatFs is deprecated, only necessary for API<9
        return path.getUsableSpace();

    }

    public static long getTotalSizeForPath(String pathStr) {
        File path = new File(pathStr);
        // working with StatFs is deprecated, only necessary for API<9
        return path.getTotalSpace();
    }


    /**
     *
     * @param sizeInBytes size in bytes, surprise :-)
     * @return nicely formatted with two digits and most appropriate size descriptor (MB, GB, ...),
     * handles sizes up to YB (YotaByte), should be future proof enough to handle upcoming
     * phone/storage sizes ;-)
     */
    public static String formatSize(long sizeInBytes) {
        long size = sizeInBytes;
        long rest = 0;
        int numberOfLoops = 0;
        // coffee infused smart loop, no if-nesting!!!
        while (size > 1024) {
            long sizeNew = size / 1024;
            rest = size % 1024;
            numberOfLoops++;
            size = sizeNew;
        }
        float sizeForDisplay = Float.parseFloat(size + "." + rest);
        return String.format("%.2f %s", sizeForDisplay, sizeNames[numberOfLoops]);
    }


    public static long calculatePercentageUsed(long freeSpace, long totalSpace) {
        int percentageUsed= 100 - (int) (0.5d + ((double) freeSpace / (double) totalSpace) * 100);
        return percentageUsed;
    }
}
