package com.android.secret.sharing;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.secure.key.backup.KeyPartImp;

import java.io.UnsupportedEncodingException;

class KeyPartImpl extends KeyPartImp implements KeyPart {
    private long mId = -1;
    private long mContainerId;
    private String mOwner;

    private int mMinimumKeyParts; // only set when key part is sent!

    KeyPartImpl(KeyPartImp partImp) {
        mKey = partImp.getEncoded();
        mTimestamp = partImp.getTimestamp();
        mIsForeign = partImp.isForeign();
    }

    public KeyPartImpl(Cursor cursor) {
        mId = cursor.getLong(cursor.getColumnIndex(SecretSharingContract.KeyPart._ID));
        mKey = cursor.getBlob(cursor.getColumnIndex(SecretSharingContract.KeyPart.COLUMN_KEY_PART));
        mContainerId = cursor.getLong(cursor.getColumnIndex(SecretSharingContract.KeyPart.COLUMN_Container_ID));
        mTimestamp = cursor.getLong(cursor.getColumnIndex(SecretSharingContract.KeyPart.COLUMN_TIMESTAMP));
        mOwner = cursor.getString(cursor.getColumnIndex(SecretSharingContract.KeyPart.COLUMN_OWNER));
        mIsForeign = cursor.getInt(cursor.getColumnIndex(SecretSharingContract.KeyPart.COLUMN_IS_FOREIGN)) == 1;
    }

    void setId(long id) {
        this.mId = id;
    }

    long getId() {
        return mId;
    }

    public void setContainerId(long containerId) {
        this.mContainerId = containerId;
    }

    public long getContainerId() {
        return mContainerId;
    }

    public ContentValues toValues() {
        ContentValues values = new ContentValues();

        mTimestamp = mTimestamp == 0 ? System.currentTimeMillis() : mTimestamp;

        values.put(SecretSharingContract.KeyPart.COLUMN_KEY_PART, mKey);
        values.put(SecretSharingContract.KeyPart.COLUMN_TIMESTAMP, mTimestamp);
        values.put(SecretSharingContract.KeyPart.COLUMN_Container_ID, mContainerId);
        values.put(SecretSharingContract.KeyPart.COLUMN_OWNER, mOwner);
        values.put(SecretSharingContract.KeyPart.COLUMN_IS_FOREIGN, mIsForeign);

        return values;
    }

    @Override
    public String getOwner() {
        return mOwner;
    }

    @Override
    public String getName() {
        return mOwner;
    }

    @Override
    public void save(Context context) {
        DatabaseHelper helper = DatabaseHelper.getHelper(context);

        if (mContainerId < 1) {
            mContainerId = helper.getContainerId();
        }

        helper.save(SecretSharingContract.KeyPart.TABLE_NAME, toValues());
    }

    @Override
    public void delete(Context context) {
        String where = SecretSharingContract.KeyPart._ID + "=?";
        String[] whereArgs = new String[] { String.valueOf(mId) };

        SQLiteDatabase db = DatabaseHelper.getHelper(context).getDatabase();
        db.delete(SecretSharingContract.KeyPart.TABLE_NAME, where, whereArgs);
    }

    @Override
    public void setForeign(boolean foreign) {
        mIsForeign = foreign;
    }

    @Override
    public int getMinimumKeyParts(Context context) {
        if (mMinimumKeyParts < 1) {
            mMinimumKeyParts = DatabaseHelper.getHelper(context).getContainer().getMinimumRecoverParts();
        }

        return mMinimumKeyParts;
    }

    void setOwner(String owner) {
        mOwner = owner;
    }

    String encode(Context context) {
        if (mMinimumKeyParts < 1) {
            mMinimumKeyParts = DatabaseHelper.getHelper(context).getContainer().getMinimumRecoverParts();
        }
        return new Gson().toJson(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mId);
        dest.writeByteArray(this.mKey);
        dest.writeLong(this.mTimestamp);
        dest.writeLong(mContainerId);
        dest.writeString(mOwner);
        dest.writeByte(mIsForeign ? (byte) 1 : (byte) 0);
        dest.writeInt(mMinimumKeyParts);
    }

    protected KeyPartImpl(Parcel in) {
        mId = in.readLong();
        mKey = in.createByteArray();
        mTimestamp = in.readLong();
        mContainerId = in.readLong();
        mOwner = in.readString();
        mIsForeign = in.readByte() == 1;
        mMinimumKeyParts = in.readInt();
    }

    public static final Parcelable.Creator<KeyPartImpl> CREATOR = new Parcelable.Creator<KeyPartImpl>() {
        @Override
        public KeyPartImpl createFromParcel(Parcel source) {
            return new KeyPartImpl(source);
        }

        @Override
        public KeyPartImpl[] newArray(int size) {
            return new KeyPartImpl[size];
        }
    };
}
