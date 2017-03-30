package com.android.secret.sharing;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;

import com.secure.key.backup.ContainerImp;
import com.secure.key.backup.KeyPartImp;

import java.security.Key;

class ContainerImpl extends ContainerImp implements Container {
    private long mId;

    protected byte[] mPublicKey;

    public ContainerImpl() {
    }

    protected ContainerImpl(Cursor cursor) {
        mId = cursor.getLong(cursor.getColumnIndex(SecretSharingContract.Container._ID));
        mName = cursor.getString(cursor.getColumnIndex(SecretSharingContract.Container.COLUMN_NAME));
        mMinimumRecoverKeys = cursor.getInt(cursor.getColumnIndex(SecretSharingContract.Container.COLUMN_MINIMAL_KEY_PARTS));
        mTotalParts = cursor.getInt(cursor.getColumnIndex(SecretSharingContract.Container.COLUMN_TOTAL_KEY_PARTS));
        mTimestamp = cursor.getLong(cursor.getColumnIndex(SecretSharingContract.Container.COLUMN_TIMESTAMP));
        mPublicKey = cursor.getBlob(cursor.getColumnIndex(SecretSharingContract.Container.COLUMN_PUBLIC_KEY));
    }

    @Override
    public KeyPart[] createPrivateKeyParts(Context context) {
        com.secure.key.backup.KeyPart[] keyParts = splitPrivateKey();

        save(context);
        return DatabaseHelper.getHelper(context).saveKeyParts(this, keyParts);
    }

    @Override
    public void save(Context context) {
        if (mId <= 0) {
            mId = DatabaseHelper.getHelper(context).save(SecretSharingContract.Container.TABLE_NAME, toValues());
        } else {
            String where = SecretSharingContract.Container._ID + "=?";
            String[] whereArgs = new String[] { String.valueOf(mId) };
            DatabaseHelper.getHelper(context).update(SecretSharingContract.Container.TABLE_NAME, toValues(), where, whereArgs);
        }
    }

    @Override
    public String restoreBackup(KeyPart[] keyParts, String encryptedBackup) {
        byte[] privateKeyBytes = restorePrivateKey((KeyPartImp[]) keyParts);

        BackupImpl backup = new BackupImpl();
        backup.setPrivateKey(privateKeyBytes);
        backup.decrypt(encryptedBackup);

        return backup.getDecryptedData();
    }

    @Override
    public Backup createBackup() {
        BackupImpl backup = new BackupImpl();
        backup.setTimestamp(System.currentTimeMillis());
        backup.setPublicKey(mPublicKey);

        return backup;
    }

    protected long getId() {
        return mId;
    }

    @Override
    protected void setPublicKey(Key key) {
        super.setPublicKey(key);
        mPublicKey = key.getEncoded();
    }

    private ContentValues toValues() {
        ContentValues values = new ContentValues();

        values.put(SecretSharingContract.Container.COLUMN_NAME, mName);
        values.put(SecretSharingContract.Container.COLUMN_MINIMAL_KEY_PARTS, mMinimumRecoverKeys);
        values.put(SecretSharingContract.Container.COLUMN_TOTAL_KEY_PARTS, mTotalParts);
        values.put(SecretSharingContract.Container.COLUMN_TIMESTAMP, mTimestamp);
        values.put(SecretSharingContract.Container.COLUMN_PUBLIC_KEY, mPublicKey);

        return values;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mId);
        dest.writeString(this.mName);
        dest.writeInt(this.mMinimumRecoverKeys);
        dest.writeInt(this.mTotalParts);
        dest.writeLong(this.mTimestamp);
        dest.writeByteArray(this.mPublicKey);
    }

    protected ContainerImpl(Parcel in) {
        this.mId = in.readLong();
        this.mName = in.readString();
        this.mMinimumRecoverKeys = in.readInt();
        this.mTotalParts = in.readInt();
        this.mTimestamp = in.readLong();
        this.mPublicKey = in.createByteArray();
    }

    public static final Creator<ContainerImpl> CREATOR = new Creator<ContainerImpl>() {
        @Override
        public ContainerImpl createFromParcel(Parcel source) {
            return new ContainerImpl(source);
        }

        @Override
        public ContainerImpl[] newArray(int size) {
            return new ContainerImpl[size];
        }
    };

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }
}
