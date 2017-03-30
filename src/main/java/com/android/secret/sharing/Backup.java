package com.android.secret.sharing;

import android.content.Context;
import android.os.Parcelable;

/**
 *  Encrypts and decrypts a secret. Backups can be stored in a database of this library.
 */
public interface Backup extends com.secret.sharing.Backup, SecretPresentation, Parcelable {

    /**
     * Possible output options for an encrypted backup.
     */
    enum BackupStoreMethod {
        EMAIL, PRINT, CLOUD
    }

    /**
     * Specify how this backup will be stored.
     *
     * @param method Storage method of this backup.
     */
    void setStoreMethod(BackupStoreMethod method);

    /**
     * Get the specified store method.
     */
    BackupStoreMethod getStoreMethod();

    /**
     * Save this backup into the database of this library.
     */
    void save(Context context);

    /**
     * Remove the encrypted data of this backup from the database.
     */
    void removeBackupData(Context context);

    /**
     * Was this backup saved to cloud?
     */
    boolean isAvailableInCloudStorage();

    /**
     * Get path to this backup in cloud storage.
     */
    String getCloudStoragePath();
}
