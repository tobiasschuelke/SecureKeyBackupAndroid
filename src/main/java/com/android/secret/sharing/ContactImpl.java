package com.android.secret.sharing;


import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;

import java.io.ByteArrayInputStream;

class ContactImpl implements com.android.secret.sharing.Contact {
    private long mId;
    private long mContainerId;
    private long mKeyPartId;

    private String mName;
    private SendStatus mSendStatus = SendStatus.NONE;

    private String mEmail;

    private SendMethod mSendMethod = SendMethod.NONE;
    private long mContactId;
    private String mLookupKey;

    // no database fields
    private Bitmap mPhoto;
    private boolean mPhotoLoaded;
    private int mListPosition;
    private KeyPart mKeyPart;

    public ContactImpl() {

    }

    ContactImpl(Cursor cursor) {
        mId = cursor.getLong(cursor.getColumnIndex(SecretSharingContract.Contact._ID));
        mContainerId = cursor.getLong(cursor.getColumnIndex(SecretSharingContract.Contact.COLUMN_Container_ID));
        mName = cursor.getString(cursor.getColumnIndex(SecretSharingContract.Contact.COLUMN_NAME));
        mEmail = cursor.getString(cursor.getColumnIndex(SecretSharingContract.Contact.COLUMN_EMAIL));
        mSendStatus = SendStatus.getStatus(cursor.getInt(cursor.getColumnIndex(SecretSharingContract.Contact.COLUMN_SEND_STATUS)));
        mSendMethod = SendMethod.valueOf(cursor.getString(cursor.getColumnIndex(SecretSharingContract.Contact.COLUMN_SEND_METHOD)));
        mKeyPartId = cursor.getLong(cursor.getColumnIndex(SecretSharingContract.Contact.COLUMN_KEY_PART_ID));
        mLookupKey = cursor.getString(cursor.getColumnIndex(SecretSharingContract.Contact.COLUMN_LOOKUP_KEY));
        mContactId = cursor.getLong(cursor.getColumnIndex(SecretSharingContract.Contact.COLUMN_CONTACT_ID));
    }

    @Override
    public void setName(String name) {
        mName = name;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public Bitmap getPhoto(Context context) {
        if (!mPhotoLoaded) {
            loadPhoto(context);
            mPhotoLoaded = true;
        }

        return mPhoto;
    }

    @Override
    public void setEmail(String email) {
        mEmail = email;
    }

    @Override
    public String loadEmail(Context context) {
        final String[] projection = new String[]{ ContactsContract.CommonDataKinds.Email.DATA, ContactsContract.CommonDataKinds.Email.TYPE };

        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection, ContactsContract.Data.CONTACT_ID + "=?", new String[]{ String.valueOf(mContactId) }, null);

        if (cursor != null) {
            if (cursor.moveToNext()) {
                final int emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
                mEmail = cursor.getString(emailIndex);
            }

            cursor.close();
        }

        return mEmail;
    }

    @Override
    public SendStatus getSendStatus() {
        return mSendStatus;
    }

    @Override
    public void setSendStatus(SendStatus sendStatus) {
        mSendStatus = sendStatus;
    }

    @Override
    public void setSendMethod(SendMethod sendMethod) {
        mSendMethod = sendMethod;
    }

    @Override
    public SendMethod getSendMethod(){
        return mSendMethod;
    }

    @Override
    public void save(Context context) {
        SQLiteDatabase db = DatabaseHelper.getHelper(context).getDatabase();

        if (mId > 0) {
            String where = SecretSharingContract.Contact._ID + "=?";
            String[] whereArgs = new String[] { String.valueOf(mId) };

            db.update(SecretSharingContract.Contact.TABLE_NAME, toValues(context), where, whereArgs);
        } else {
            mId = db.insert(SecretSharingContract.Contact.TABLE_NAME, null, toValues(context));
        }
    }

    @Override
    public void setKeyPart(KeyPart keyPart) {
        mKeyPart = keyPart;

        KeyPartImpl keyPartImpl = ((KeyPartImpl) mKeyPart);
        mKeyPartId = keyPartImpl.getId();

        if (keyPartImpl.getContainerId() > 0) {
            mContainerId = keyPartImpl.getContainerId();
        }
    }

    @Override
    public void removeKeyPart(KeyPart keyPart) {
        if (keyPart != null) {
            mContainerId = ((KeyPartImpl) keyPart).getContainerId();
        }

        mKeyPart = null;
        mKeyPartId = -1;
    }

    @Override
    public KeyPart getKeyPart(Context context) {
        if (mKeyPart != null) {
            return mKeyPart;
        } else if (mKeyPartId >= 0) {
            DatabaseHelper db = DatabaseHelper.getHelper(context);
            mKeyPart = db.loadKey(mKeyPartId);
            return mKeyPart;
        }
        return null;
    }

    @Override
    public boolean hasEmail() {
        return mEmail != null;
    }

    @Override
    public String getEmail() {
        return mEmail;
    }

    @Override
    public boolean hasKeyPart() {
        return mKeyPart != null;
    }

    @Override
    public void clearSendSelection(Context context) {
        mKeyPartId = -1;
        mContainerId = -1;
        mKeyPart = null;
        mSendMethod = SendMethod.NONE;
        mSendStatus = SendStatus.NONE;

        String where = SecretSharingContract.Contact._ID + "=?";
        String[] whereArgs = new String[] { String.valueOf(mId) };

        SQLiteDatabase db = DatabaseHelper.getHelper(context).getDatabase();
        db.delete(SecretSharingContract.Contact.TABLE_NAME, where, whereArgs);
    }

    @Override
    public void setListPosition(int position) {
        mListPosition = position;
    }

    @Override
    public int getListPosition() {
        return mListPosition;
    }

    public void setContactId(long contactId) {
        mContactId = contactId;
    }

    public long getContactId() {
        return mContactId;
    }

    public void setLookupKey(String lookupKey) {
        mLookupKey = lookupKey;
    }

    public boolean updateContactId(Context context) {
        long contactId = 0;

        Uri lookupUri = ContactsContract.Contacts.getLookupUri(mContactId, mLookupKey);
        if (lookupUri != null) {
            Cursor contactCursor = context.getContentResolver().query(lookupUri, null, null, null, null);

            if (contactCursor != null) {
                if (contactCursor.moveToNext()) {
                    contactId = contactCursor.getLong(contactCursor.getColumnIndex(ContactsContract.Contacts._ID));
                }

                contactCursor.close();
            }
        }

        if (contactId != mContactId) {
            mContactId = contactId;
            return true;
        }
        return false;
    }

    private void loadPhoto(Context context) {
        if (getContactId() <= 0) {
            return;
        }

        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, getContactId());
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    mPhoto = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
                }
            }
        } finally {
            cursor.close();
        }
    }

    ContentValues toValues(Context context) {
        ContentValues values = new ContentValues();

        if (mContainerId < 1) {
            mContainerId = DatabaseHelper.getHelper(context).getContainerId();
        }

        values.put(SecretSharingContract.Contact.COLUMN_NAME, mName);
        values.put(SecretSharingContract.Contact.COLUMN_EMAIL, mEmail);
        values.put(SecretSharingContract.Contact.COLUMN_SEND_STATUS, mSendStatus.getId());
        values.put(SecretSharingContract.Contact.COLUMN_SEND_METHOD, mSendMethod.toString());
        values.put(SecretSharingContract.Contact.COLUMN_Container_ID, mContainerId);
        values.put(SecretSharingContract.Contact.COLUMN_KEY_PART_ID, mKeyPartId);
        values.put(SecretSharingContract.Contact.COLUMN_CONTACT_ID, mContactId);
        values.put(SecretSharingContract.Contact.COLUMN_LOOKUP_KEY, mLookupKey);

        return values;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mPhoto, flags);
        dest.writeByte(this.mPhotoLoaded ? (byte) 1 : (byte) 0);
        dest.writeInt(this.mSendMethod == null ? -1 : this.mSendMethod.ordinal());
        dest.writeLong(mId);
        dest.writeLong(mContainerId);
        dest.writeString(mName);
        dest.writeInt(mSendStatus.getId());
        dest.writeString(mEmail);
        dest.writeInt(mListPosition);
        dest.writeString(mLookupKey);
        dest.writeLong(mContactId);
    }

    protected ContactImpl(Parcel in) {
        mPhoto = in.readParcelable(Bitmap.class.getClassLoader());
        mPhotoLoaded = in.readByte() != 0;
        int tmpMSend = in.readInt();
        mSendMethod = tmpMSend == -1 ? null : SendMethod.values()[tmpMSend];
        mId = in.readLong();
        mContainerId = in.readLong();
        mName = in.readString();
        mSendStatus = SendStatus.getStatus(in.readInt());
        mEmail = in.readString();
        mListPosition = in.readInt();
        mLookupKey = in.readString();
        mContactId = in.readLong();
    }

    public static final Parcelable.Creator<ContactImpl> CREATOR = new Parcelable.Creator<ContactImpl>() {
        @Override
        public ContactImpl createFromParcel(Parcel source) {
            return new ContactImpl(source);
        }

        @Override
        public ContactImpl[] newArray(int size) {
            return new ContactImpl[size];
        }
    };
}
