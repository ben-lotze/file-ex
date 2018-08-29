package rocks.informatik.fileex.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

public final class MusicTagHelpers {

    private MusicTagHelpers() {
    }

    public static Bitmap getArtworkBitmapFromTag(Tag tag) {

        // TODO: artwork in JAdiotagger can have url? but: no valid url for Glide, only binary data works
        // could there be files out there which have an url embedded, but no image?

        Artwork artwork = tag.getFirstArtwork();
//        String artUrlStr = artwork.getImageUrl();
//        Log.d("Browser", "artwork url: " + artUrlStr);
        if (artwork != null) {
            byte[] artBinary = artwork.getBinaryData();
//        Log.d("Browser", "artwork binary length: " + (artBinary!=null ? artBinary.length : "0") );
            return BitmapFactory.decodeByteArray(artBinary, 0, artBinary.length);
        }

        return null;
    }



    public static String getRuntimeStr(AudioHeader header) {
        // TODO: include hours in runtime if necessary (audio books, talks, ...)
        // TODO: minutes may be one digit only, always use two digits?
        int seconds = header.getTrackLength();
        int displayMinutes = (int) (seconds / 60);
        int displaySeconds = seconds % 60;
        String displayLength = "" + displayMinutes + ":" + (displaySeconds<10 ? "0"+displaySeconds : displaySeconds);
        return displayLength;
    }

    /**
     * 0=0 stars,
     * 10 different ratings, each having 25,5 steps,
     * equals to multiplicator of 0.01960784313725490196078431372549 to get float between 0-5 for rating
     *
     * @param tag dudop tag to be analysed
     * @return rating from 0-5 for half-step-stars rating bar
     */
    public static float getRatingStars(Tag tag) {
        float ratingStars = 0f;
        String rating = tag.getFirst(FieldKey.RATING);
        if (rating != null) {
            try {
                int ratingInt = Integer.valueOf(rating);
                ratingStars = (float) (ratingInt * 0.01960784313725490196078431372549f);
            } catch (NumberFormatException e) {
                return 0f;
            }
        }
        return ratingStars;
    }



    public static String getExtraInfoRowContents(Tag tag) {
        // may include different information combined into one HTML string
        // depends on context! -> do not repeat info that is already listed in top header info!
        return "";
    }

}
