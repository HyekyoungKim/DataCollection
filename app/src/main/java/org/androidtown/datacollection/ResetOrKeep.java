package org.androidtown.datacollection;

import android.os.Parcel;
import android.os.Parcelable;

public class ResetOrKeep implements Parcelable {
    private int reset;

    public ResetOrKeep(int r) {
        reset = r;
    }

    public ResetOrKeep(Parcel src) {
        reset = src.readInt();
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public ResetOrKeep createFromParcel(Parcel source) {
            return new ResetOrKeep(source);
        }

        @Override
        public ResetOrKeep[] newArray(int size) {
            return new ResetOrKeep[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(reset);
    }

    public int getReset() {
        return reset;
    }

    public void setReset(int reset) {
        this.reset = reset;
    }
}
