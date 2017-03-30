package com.android.secret.sharing;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.secret.sharing.*;

/**
 * Contains helper methods to access the database of this library.
 */
class DatabaseHelper {
    private static final String PREFERENCES = "shared_secret_prefs";
    private static final String PREF_USER_NAME = "user_name";
    private static final String PREF_KEY_SHARED = "key_shared";

    private static DatabaseHelper mHelper;
    private SQLiteDatabase mDatabase;

    private String mUserName;
    private boolean mKeyShared;

    private DatabaseHelper(Context context) {
        mDatabase = new DatabaseOpenHelper(context).getWritableDatabase();

        mUserName = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(PREF_USER_NAME, "");
        mKeyShared = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(PREF_KEY_SHARED, false);
    }

    public static DatabaseHelper getHelper(Context context) {
        if (mHelper == null) {
            mHelper = new DatabaseHelper(context);
        }
        return mHelper;
    }

    public long save(String table, ContentValues values) {
        return mDatabase.insert(table, null, values);
    }

    public long update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return mDatabase.update(table, values, whereClause, whereArgs);
    }

    public boolean keyShared() {
        return mKeyShared;
    }

    public SQLiteDatabase getDatabase() {
        return mDatabase;
    }

    public BackupImpl[] getAvailableBackups() {
        String selection = SecretSharingContract.Backup.COLUMN_DATA + " IS NOT NULL";

        Cursor cursor = mDatabase.query(SecretSharingContract.Backup.TABLE_NAME, null, selection, null, null, null, null);

        BackupImpl[] availableBackups = null;
        if (cursor != null) {
            availableBackups = new BackupImpl[cursor.getCount()];

            int i = 0;
            while (cursor.moveToNext()) {
                availableBackups[i++] = new BackupImpl(cursor);
            }

            cursor.close();
        }

        return availableBackups;
    }

    public Container getContainer() {
        Cursor cursor = mDatabase.query(SecretSharingContract.Container.TABLE_NAME, null, null, null, null, null, null);

        Container container = null;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                container = new ContainerImpl(cursor);
            }

            cursor.close();
        }

        return container;
    }

    public KeyPartImpl[] getMyKeyParts() {

        String sql = "SELECT * FROM " + SecretSharingContract.KeyPart.TABLE_NAME + " WHERE " + SecretSharingContract.KeyPart.COLUMN_IS_FOREIGN + "=? AND "
                + SecretSharingContract.KeyPart._ID + " NOT IN (SELECT "
                + SecretSharingContract.Contact.COLUMN_KEY_PART_ID + " FROM " + SecretSharingContract.Contact.TABLE_NAME + ")";

        String[] selArgs = new String[] { "0" };

        Cursor cursor = mDatabase.rawQuery(sql, selArgs);

        KeyPartImpl[] keyParts = null;
        if (cursor != null) {
            keyParts = new KeyPartImpl[cursor.getCount()];

            int i = 0;
            while (cursor.moveToNext()) {
                keyParts[i++] = new KeyPartImpl(cursor);
            }

            cursor.close();
        }
        return keyParts;
    }

    public long getContainerId() {
        // Always returns 1 because only a single container should be created for now. Can be extended here for
        // use with multiple containers.
        return 1;
    }

    public ContactImpl[] getContactsWithStatusSelected() {
        return getContacts(Contact.SendStatus.SELECTED);
    }

    private ContactImpl[] getContacts(Contact.SendStatus sendStatus) {
        String selection = SecretSharingContract.Contact.COLUMN_SEND_STATUS + "=?";
        String[] selArgs = new String[] { String.valueOf(sendStatus.getId()) };

        Cursor cursor = mDatabase.query(SecretSharingContract.Contact.TABLE_NAME, null, selection, selArgs, null, null, null);

        ContactImpl[] contacts = null;
        if (cursor != null) {
            contacts = new ContactImpl[cursor.getCount()];

            int i = 0;
            while (cursor.moveToNext()) {
                contacts[i++] = new ContactImpl(cursor);
            }

            cursor.close();
        }
        return contacts;
    }

    public KeyPart[] saveKeyParts(ContainerImpl container, com.secret.sharing.KeyPart[] keyParts) {
        KeyPart[] parts = new KeyPart[keyParts.length];

        mDatabase.beginTransaction();

        int i = 0;
        for (com.secret.sharing.KeyPart part : keyParts) {
            KeyPartImpl keyPart = new KeyPartImpl((KeyPartImp) part);
            keyPart.setContainerId(container.getId());
            keyPart.setOwner(mUserName);

            long id = mDatabase.insert(SecretSharingContract.KeyPart.TABLE_NAME, null, keyPart.toValues());
            keyPart.setId(id);

            parts[i++] = keyPart;
        }

        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

        return parts;
    }

    public KeyPartImpl loadKey(long keyId) {
        String selection = SecretSharingContract.KeyPart._ID + "=?";
        String[] selArgs = new String[] { String.valueOf(keyId) };

        Cursor cursor = mDatabase.query(SecretSharingContract.KeyPart.TABLE_NAME, null, selection, selArgs, null, null, null);

        KeyPartImpl key = null;
        if (cursor != null && cursor.moveToNext()) {
            key = new KeyPartImpl(cursor);
            cursor.close();
        }
        return key;
    }

    public String getUserName() {
        return mUserName;
    }

    public boolean hasUserName() {
        return !TextUtils.isEmpty(mUserName);
    }

    public void saveUserName(Context context, String name) {
        mUserName = name;

        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_USER_NAME, name)
                .apply();
    }

    public long getContainerTimestamp() {
        String columns[] = new String[] { SecretSharingContract.Container.COLUMN_TIMESTAMP };

        Cursor cursor = mDatabase.query(SecretSharingContract.Container.TABLE_NAME, columns, null, null, null, null, null);

        long timestamp = -1;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                timestamp = cursor.getLong(cursor.getColumnIndex(SecretSharingContract.Container.COLUMN_TIMESTAMP));
            }
            cursor.close();
        }

        return timestamp;
    }

    public Backup[] getBackups() {
        String order = SecretSharingContract.Backup.COLUMN_TIMESTAMP + " ASC";

        Cursor cursor = mDatabase.query(SecretSharingContract.Backup.TABLE_NAME, null, null, null, null, null, order);
        Backup[] backups = null;
        if (cursor != null) {
            backups = new BackupImpl[cursor.getCount()];

            int i = 0;
            while (cursor.moveToNext()) {
                backups[i++] = new BackupImpl(cursor);
            }

            cursor.close();
        }

        return backups;
    }

    public KeyPart[] getForeignKeyParts() {
        String selection = SecretSharingContract.KeyPart.COLUMN_IS_FOREIGN + "=?";
        String selArgs[] = new String[] { "1" };
        String order = SecretSharingContract.KeyPart.COLUMN_OWNER + " ASC";

        Cursor cursor = mDatabase.query(SecretSharingContract.KeyPart.TABLE_NAME, null, selection, selArgs, null, null, order);

        KeyPart[] keyParts = null;
        if (cursor != null) {
            keyParts = new KeyPart[cursor.getCount()];

            int i = 0;
            while (cursor.moveToNext()) {
                keyParts[i++] = new KeyPartImpl(cursor);
            }

            cursor.close();
        }

        return keyParts;
    }

    public void close() {
        mHelper = null;
        mDatabase.close();
    }
}
