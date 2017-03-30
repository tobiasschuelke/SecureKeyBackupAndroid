package com.android.secret.sharing;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.secret.sharing.SecretSharingContract.Contact;
import com.android.secret.sharing.SecretSharingContract.Container;
import com.android.secret.sharing.SecretSharingContract.Backup;

class DatabaseOpenHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Secret Sharing.db";

    public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Container.CREATE);
        db.execSQL(Backup.CREATE);
        db.execSQL(Contact.CREATE);
        db.execSQL(SecretSharingContract.KeyPart.CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
