package com.android.secret.sharing;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Base64;

import com.secret.sharing.BackupImp;

class BackupImpl extends BackupImp implements Backup {
    private long mId;
    private Backup.BackupStoreMethod mStoreMethod;
    private String mCloudPath;

    public BackupImpl() {

    }

    public BackupImpl(Cursor cursor) {
        int storeMethodIndex = cursor.getColumnIndex(SecretSharingContract.Backup.COLUMN_STORE_METHOD);

        mId = cursor.getLong(cursor.getColumnIndex(SecretSharingContract.Backup._ID));
        mName = cursor.getString(cursor.getColumnIndex(SecretSharingContract.Backup.COLUMN_NAME));
        mTimestamp = cursor.getLong(cursor.getColumnIndex(SecretSharingContract.Backup.COLUMN_TIMESTAMP));
        mEncryptedData = cursor.getBlob(cursor.getColumnIndex(SecretSharingContract.Backup.COLUMN_DATA));
        mStoreMethod = Backup.BackupStoreMethod.valueOf(cursor.getString(storeMethodIndex));
        mCloudPath = cursor.getString(cursor.getColumnIndex(SecretSharingContract.Backup.COLUMN_CLOUD_PATH));
    }

    public void save(Context context) {
        mId = DatabaseHelper.getHelper(context).save(SecretSharingContract.Backup.TABLE_NAME, toValues());
    }

    @Override
    public BackupStoreMethod getStoreMethod() {
        return mStoreMethod;
    }

    @Override
    public void removeBackupData(Context context) {
        String where = SecretSharingContract.Container._ID + " =?";
        String[] whereArgs = new String[] { String.valueOf(mId) };

        ContentValues values = new ContentValues();
        values.putNull(SecretSharingContract.Container.COLUMN_PUBLIC_KEY);

        DatabaseHelper.getHelper(context).update(SecretSharingContract.Container.TABLE_NAME, values, where, whereArgs);
    }

    @Override
    public boolean isAvailableInCloudStorage() {
        return !TextUtils.isEmpty(mCloudPath);
    }

    @Override
    public String getCloudStoragePath() {
        return mCloudPath;
    }

    long getId() {
        return mId;
    }

    void setCloudPath(String path) {
        mCloudPath = path;
    }

    public ContentValues toValues() {
        ContentValues values = new ContentValues();

        values.put(SecretSharingContract.Backup.COLUMN_NAME, mName);
        values.put(SecretSharingContract.Backup.COLUMN_TIMESTAMP, mTimestamp);
        values.put(SecretSharingContract.Backup.COLUMN_DATA, mEncryptedData);
        values.put(SecretSharingContract.Backup.COLUMN_STORE_METHOD, mStoreMethod.name());
        values.put(SecretSharingContract.Backup.COLUMN_CLOUD_PATH, mCloudPath);

        return values;
    }

    @Override
    public void decrypt(String encryptedData) {
        // use android library for base64 decoding
        try {
            decrypt(Base64.decode(encryptedData, Base64.DEFAULT));
        } catch (IllegalArgumentException e) {
            // exception is raised when user scanned arbitrary qr code instead of base64 encoded backup
        }
    }

    @Override
    public String getEncryptedData() {
        // use android library for base64 encoding
        return Base64.encodeToString(mEncryptedData, Base64.DEFAULT);
    }

    @Override
    public void setStoreMethod(BackupStoreMethod method) {
        mStoreMethod = method;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);

        dest.writeByte(mPublicKey != null ? (byte) 1 : (byte) 0);
        if (mPublicKey != null) {
            dest.writeByteArray(mPublicKey.getEncoded());
        }

        dest.writeByte(mPrivateKey != null ? (byte) 1 : (byte) 0);
        if (mPrivateKey != null) {
            dest.writeByteArray(mPrivateKey.getEncoded());
        }

        dest.writeByteArray(mData);
        dest.writeByteArray(mEncryptedData);
        dest.writeLong(mTimestamp);
        dest.writeString(mCloudPath);
    }

    protected BackupImpl(Parcel in) {
        setName(in.readString());

        if (in.readByte() > 0) {
            setPublicKey(in.createByteArray());
        }

        if (in.readByte() > 0) {
            setPrivateKey(in.createByteArray());
        }

        mData = in.createByteArray();
        mEncryptedData = in.createByteArray();
        setTimestamp(in.readLong());
        mCloudPath = in.readString();
    }

    public static final Creator<BackupImpl> CREATOR = new Creator<BackupImpl>() {
        @Override
        public BackupImpl createFromParcel(Parcel source) {
            return new BackupImpl(source);
        }

        @Override
        public BackupImpl[] newArray(int size) {
            return new BackupImpl[size];
        }
    };
}
