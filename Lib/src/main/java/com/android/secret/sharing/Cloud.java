package com.android.secret.sharing;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Fake implementation to simulate a cloud storage. Files will be saved on external storage of
 * the phone.
 */
public class Cloud {
    private static final String DIRECTORY = Environment.getExternalStorageDirectory() + "/Key Backup Cloud/";

    public static void save(Context context, Backup backup) {
        File directory = new File(DIRECTORY);
        if (!directory.isDirectory()) {
            directory.mkdir();
        }

        String path = DIRECTORY + backup.getName();
        File backupFile = new File(path);
        try {
            boolean fileExists = !backupFile.createNewFile();

            if (fileExists) {
                return;
            }

            PrintWriter writer = new PrintWriter(backupFile);
            writer.print(backup.getEncryptedData());
            writer.close();

            ((BackupImpl) backup).setCloudPath(path);
            backup.save(context);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getBackup(Backup backup) {
        File directory = new File(DIRECTORY);
        File backupFile = new File(DIRECTORY + backup.getName());

        if (!directory.isDirectory() || !backupFile.exists()) {
            return null;
        }

        int length = (int) backupFile.length();

        byte[] bytes = new byte[length];

        try {
            FileInputStream  in = new FileInputStream(backupFile);
            in.read(bytes);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new String(bytes);
    }
}
