package rocks.informatik.fileex.data;

import android.os.Parcel;
import android.os.Parcelable;

public class FavoritePlace implements Parcelable {

    protected int id;
    protected String name;
    protected String pathStr;
    protected int drawableResId;


    protected long freeSpace;
    protected long totalSize;


    public FavoritePlace(int id, String name, String pathStr, int drawableResId) {
        this.id = id;
        this.name = name;
        this.pathStr = pathStr;
        this.drawableResId = drawableResId;
    }


    protected FavoritePlace(Parcel in) {
        id = in.readInt();
        name = in.readString();
        pathStr = in.readString();
        drawableResId = in.readInt();
        freeSpace = in.readLong();
        totalSize = in.readLong();
    }

    public static final Creator<FavoritePlace> CREATOR = new Creator<FavoritePlace>() {
        @Override
        public FavoritePlace createFromParcel(Parcel in) {
            return new FavoritePlace(in);
        }

        @Override
        public FavoritePlace[] newArray(int size) {
            return new FavoritePlace[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPathStr() {
        return pathStr;
    }

    public void setPathStr(String pathStr) {
        this.pathStr = pathStr;
    }

    public int getDrawableResId() {
        return drawableResId;
    }

    public void setDrawableResId(int drawableResId) {
        this.drawableResId = drawableResId;
    }




    public long getFreeSpace() {
        return freeSpace;
    }

    public void setFreeSpace(long freeSpace) {
        this.freeSpace = freeSpace;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(pathStr);
        dest.writeInt(drawableResId);
        dest.writeLong(freeSpace);
        dest.writeLong(totalSize);
    }


    @Override
    public String toString() {
        return String.format("Favorite: id=%d, name=%s, path=%s",
                id, name, pathStr);
    }
}
