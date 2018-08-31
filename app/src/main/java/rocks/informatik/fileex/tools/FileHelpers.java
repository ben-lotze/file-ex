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

import rocks.informatik.fileex.R;
import rocks.informatik.fileex.data.FavoritePlace;
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
        int startIndex = 0;
        // files beginning with a dot are hidden (rest does not count as extension if it contains no other dot!)
        if (fileName.startsWith(".")) {
            Timber.d("checking extension of hidden file: " + fileName);
            startIndex = 1;
        }
        int indexLastDot = TextUtils.lastIndexOf(fileName, '.', startIndex, fileName.length());
        if (indexLastDot > 0) {
            int length = fileName.length();
            String extension = fileName.substring(indexLastDot + 1);
            return extension.toLowerCase();
        }
        else {
            Timber.d("no dot found in filename, returning null");
        }
        // if no dot in filename OR starting with dot and containing no other dot
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

        if (extension == null) {
            // hidden files like ".nomedia" starting with dot have no extension if they contain no additional dot!
            Toast.makeText(context, context.getString(R.string.text_no_idea_how_to_open_file), Toast.LENGTH_LONG).show();
            return;
        }

//        // file handling with content URIs enforced by >Nougat devices
        String authority = context.getApplicationContext().getPackageName() + ".provider";
        Uri fileUri = FileProvider.getUriForFile(context, authority, clickedFile);

        MimeTypeMap mimeMap = MimeTypeMap.getSingleton();
        Intent intentViewFile = new Intent(Intent.ACTION_VIEW);
        String mimeType = mimeMap.getMimeTypeFromExtension(extension);
//        Timber.d("URI to open: " + fileUri);
//        Timber.d("MIME type: " + mimeType);

        intentViewFile.setDataAndType(fileUri, mimeType);
        intentViewFile.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentViewFile.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intentViewFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intentViewFile);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, context.getString(R.string.text_no_idea_how_to_open_file), Toast.LENGTH_LONG).show();
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



    public static List<FavoritePlace> getLocalPhoneFolders(final Context context) {

        // TODO: choose names/icons depending on which folders were found


        AsyncTask<Void, Void, List<FavoritePlace>> task = new AsyncTask<Void, Void, List<FavoritePlace>>() {
            @Override
            protected List<FavoritePlace> doInBackground(Void... voids) {

                List<FavoritePlace> places = new ArrayList<>();

                // root-folder gets no navigation sidebar entry if not readable for user
                String rootPath = "/";
                File folderRoot = new File(rootPath);
                if (folderRoot.canRead()) {
                    String rootName = context.getString(R.string.fav_name_root);
                    FavoritePlace placeRoot = new FavoritePlace(-1, rootName, rootPath, R.drawable.ic_folders_black_24dp);
                    long rootFreeSpace = getFreeSpaceForPath(rootPath);
                    placeRoot.setFreeSpace(rootFreeSpace);
                    long rootTotalSize = getTotalSizeForPath(rootPath);
                    placeRoot.setTotalSize(rootTotalSize);
                    places.add(placeRoot);
                } else {
                    Timber.d("root folder not readable, navigation entry skipped");
                }


                // does always exist
                String pathExternal = Environment.getExternalStorageDirectory().getAbsolutePath();
                Timber.d("path external: " + pathExternal);
                String externalName = context.getString(R.string.fav_name_external_storage);
                FavoritePlace placeExternal = new FavoritePlace(-1, externalName, pathExternal,
                        R.drawable.ic_phone_android_black_24dp);
                long externalFreeSpace = getFreeSpaceForPath(pathExternal);
                placeExternal.setFreeSpace(externalFreeSpace);
                long externalTotalSize = getTotalSizeForPath(pathExternal);
                placeExternal.setTotalSize(externalTotalSize);
                places.add(placeExternal);


                /* more paths that may be worth checking in the long term?
                 * /mnt/sdcard
                 * /extSdCard
                 */
                // TODO: check if there is more than one (if yes: add their respective name to the item)
                Timber.d("checking for sd cards and more storage");
                File folderStorage = new File("/storage/");
                File[] subfoldersInStorage = folderStorage.listFiles();
                if (subfoldersInStorage != null && subfoldersInStorage.length > 0) {
                    int subfolderCount = subfoldersInStorage.length;
                    for (File subfolder : subfoldersInStorage) {

                        Timber.d("checking storage subfolder: " + subfolder.getAbsolutePath());


                        // try to read contents -> if not possible: skip current folder
                        // TODO: come up with better solution -> could be new empty SD card that has no files yet!
                        // problem: there are multiple folders, that are maybe empty, ut are no sd card...
                        File[] contents = subfolder.listFiles();
                        if (contents == null) {
                            Timber.d("-> contents: NULL");
                            continue;
                        }
                        Timber.d("-> contents: " + Arrays.asList(contents));

                        String currentPath = subfolder.getAbsolutePath();
                        String nameSdCard = context.getString((R.string.fav_name_sd_card));
                        String currentFolderName = subfolder.getName();
                        // TODO: length is >1 in most cases, folders are just not all that interesting...
                        // count folders that CAN be handled
                        // or store in DB and offer possibility to edit names
                        if (subfolderCount > 1) {
                            nameSdCard = String.format("%s (%s)", nameSdCard, currentFolderName);
                        }

                        FavoritePlace place = new FavoritePlace(-1, nameSdCard, currentPath, R.drawable.ic_sd_card_black_24dp);
                        long currentPlaceFreeSpace = getFreeSpaceForPath(currentPath);
                        place.setFreeSpace(currentPlaceFreeSpace);
                        long currentPlaceTotalSize = getTotalSizeForPath(currentPath);
                        place.setTotalSize(currentPlaceTotalSize);

                        places.add(place);
                    }
                }

                return places;
            }
        }.execute();


        try {
            List<FavoritePlace> places = task.get();
            Timber.d("found phone places:");
            if (places != null) {
                for (FavoritePlace place : places) {
                    if (place == null) {
                        continue;
                    }
                    Timber.d("path: " + place.getPathStr());
                }
            }

            return places;

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }


//


        return new ArrayList<>();

    }
}
