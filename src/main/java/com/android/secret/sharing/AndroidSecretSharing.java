package com.android.secret.sharing;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.print.PrintManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.android.share.sharing.R;

import net.glxn.qrgen.android.QRCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import github.nisrulz.qreader.QRDataListener;

/**
 * Generates a container, gives access to the database of this library and can show, print and send QR-Codes.
 */
public class AndroidSecretSharing {
    private static final int REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE = 4290;

    static final int MAXIMUM_QR_CODE_SIZE = 2950;

    private static final int PRINT_IMAGE_WIDTH = 595;
    private static final int PRINT_IMAGE_HEIGHT = 841;

    private static final String EMAIL_ATTACHMENT_FOLDER = Environment.getExternalStorageDirectory() + "/KeyPartImpl Backup/";
    private static final String CONTAINER_NAME = "Container 1";

    private Context mContext;
    private QrReader mQrReader;

    public AndroidSecretSharing(Context context) {
        mContext = context;
    }

    /**
     * Load created backups.
     *
     * @return Backups of secrets.
     */
    public Backup[] getSavedBackups() {
        return DatabaseHelper.getHelper(mContext).getBackups();
    }

    /**
     * Get saved key parts of other persons.
     */
    public KeyPart[] getForeignKeyParts() {
        return DatabaseHelper.getHelper(mContext).getForeignKeyParts();
    }

    /**
     * Get saved key parts that belong to the user.
     */
    public KeyPart[] getUserKeyParts() {
        return DatabaseHelper.getHelper(mContext).getMyKeyParts();
    }

    /**
     * Get saved contacts that are selected for receiving a key part. But the user has not
     * confirmed yet that the persons received the key parts.
     */
    public Contact[] getContactsWithStatusSelected() {
        return DatabaseHelper.getHelper(mContext).getContactsWithStatusSelected();
    }

    /**
     * Create a new contact.
     *
     * @see ContactLoader
     */
    public static Contact newContact() {
        return new ContactImpl();
    }

    /**
     * Get Backups with stored encrypted secret.
     */
    public BackupImpl[] getAvailableBackups() {
        return DatabaseHelper.getHelper(mContext).getAvailableBackups();
    }

    /**
     * Load the container.
     */
    public Container getContainer() {
        return DatabaseHelper.getHelper(mContext).getContainer();
    }

    /**
     * @return True if the private key is already shared.
     */
    public boolean keyShared() {
        return DatabaseHelper.getHelper(mContext).keyShared();
    }

    /**
     * Create a new container. Only one container should be created! Use {@link #getContainer()} to
     * load a created container.
     */
    public Container newContainer() {
        Container container = new ContainerImpl();
        container.setName(CONTAINER_NAME);

        return container;
    }

    /**
     *  Delete files in the folder that is used to store the attachment files for emails.
     */
    public void cleanEmailAttachmentFolder() {
        int storagePermission = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (storagePermission == PackageManager.PERMISSION_GRANTED) {
            File path = new File(EMAIL_ATTACHMENT_FOLDER);

            if (path.isDirectory()) {
                for (File file : path.listFiles()) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Set name for user that will be shown to owner of secret parts. Name must be set
     * before first backup!
     */
    public void setUserName(String name) {
        DatabaseHelper.getHelper(mContext).saveUserName(mContext, name);
    }

    /**
     * @return True if the name of the user is set.
     */
    public boolean hasUserName() {
        return DatabaseHelper.getHelper(mContext).hasUserName();
    }

    /**
     * Read a key part that is shown as a QR-Code.
     *
     * @param surfaceView       View that will show the camera to the user.
     * @param listener          Listener to receive the scanned key part.
     * @param readForeignKey    True of a key part of another person should be read.
     */
    public QrReader readSecretPart(Activity activity, SurfaceView surfaceView, QrKeyPartListener listener, boolean readForeignKey) {
        mQrReader =  new QrReader(activity, surfaceView, listener, readForeignKey);
        return mQrReader;
    }

    /**
     * Read a secret that is shown as a QR-Code.
     *
     * @param surfaceView   View that will show the camera to the user.
     * @param listener      Listener to receive the scanned key part.
     */
    public QrReader readQrCode(Activity activity, SurfaceView surfaceView, QRDataListener listener) {
        mQrReader = new QrReader(activity, surfaceView, listener);
        return mQrReader;
    }

    /**
     * Call this method from {@link Activity#onRequestPermissionsResult(int, String[], int[])} to grant the camera
     * permission for the QR-Reader.
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (mQrReader != null) {
            mQrReader.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Helper method that transforms a list of key parts into an array.
     *
     * @param keyParts Key parts in a list.
     * @return Key parts in an array.
     */
    public KeyPart[] keyListToArray(ArrayList<KeyPart> keyParts) {
        KeyPartImpl[] keyPartImpls = new KeyPartImpl[keyParts.size()];

        int i = 0;
        for (KeyPart part : keyParts) {
            keyPartImpls[i++] = (KeyPartImpl) part;
        }

        return keyPartImpls;
    }

    /**
     * Call this when this library is not longer used. It will close its database.
     */
    public void close() {
        DatabaseHelper.getHelper(mContext).close();
    }

    /**
     * Show a QR-Code of a key part.
     *
     * @param key   Key part to show as a QR-Code.
     * @param view  View to show the QR-Code on.
     * @throws QrCodeSizeException If the key part is too large to display it as a QR-Code.
     */
    public void showQrCode(@NonNull KeyPart key, @NonNull ImageView view) throws QrCodeSizeException {
        String qrText = ((KeyPartImpl) key).encode(mContext);
        showQrCode(qrText, view);
    }

    /**
     * Show a text as a QR-Code.
     *
     * @param qrText    Text to show as a QR-Code.
     * @param view      View to display the QR-Code on.
     * @throws QrCodeSizeException QrCodeSizeException If the key part is too large to display it as a QR-Code.
     */
    public void showQrCode(@NonNull String qrText, @NonNull ImageView view) throws QrCodeSizeException {
        int width = view.getWidth();
        int height = view.getHeight();

        Bitmap qrCode = getQrCode(qrText, width, height);
        view.setImageBitmap(qrCode);
    }

    /**
     * Print a key part of a contact. The API level must be at least 19 to print.
     *
     * @param contact                   Contact whose linked key part should be printed.
     * @throws QrCodeSizeException      If the key part is too large to display it as a QR-Code.
     * @throws IllegalStateException    If API Level < 19.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void printQrCode(Context context, Contact contact) throws QrCodeSizeException {
        String qrText = ((KeyPartImpl) contact.getKeyPart(mContext)).encode(mContext);
        print(context, qrText, contact.getName(), false);
    }

    /**
     * Print an encrypted secret. The API level must be at least 19 to print.
     *
     * @param dataBackup                Encrypted secret to print.
     * @throws QrCodeSizeException      If the key part is too large to display it as a QR-Code.
     * @throws IllegalStateException    If API Level < 19.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void printQrCode(Context context, Backup dataBackup) throws QrCodeSizeException {
        print(context, dataBackup.getEncryptedData(), dataBackup.getName(), true);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void print(Context context, String qrText, String name, boolean isBackup) throws QrCodeSizeException {
        if (Build.VERSION.SDK_INT < 19) {
            throw new IllegalStateException("Print service is not available for devices with API level lower than 19");
        }

        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);

        // Set job name, which will be displayed in the print queue
        String jobName = context.getString(R.string.app_name) + " Document";
        Bitmap qrCode = getQrCode(qrText, 400, 400);

        printManager.print(jobName, new QrCodePrintAdapter(context, name, qrCode, isBackup), null);
    }

    private Bitmap getQrCode(String qrText, int width, int height) throws QrCodeSizeException {
        if (qrText.length() > MAXIMUM_QR_CODE_SIZE) {
            throw new QrCodeSizeException(qrText.length());
        }

        return QRCode.from(qrText).withSize(width, height).bitmap();
    }

    /**
     * Send secret part via Email.
     *
     * @return True if Email app is installed and every contact has an Email address and a secret part.
     */
    public boolean sendEmail(Activity activity, Contact contact) throws QrCodeSizeException {
        if (!contact.hasEmail() || !contact.hasKeyPart()) {
            return false;
        }

        String secret = ((KeyPartImpl) contact.getKeyPart(mContext)).encode(mContext);
        return sendEmail(activity, contact.getName(), secret, contact.getEmail(), false);
    }

    /**
     * Send backup via Email.
     *
     * @return True if Email app is installed and every contact has an Email address and a secret part.
     */
    public boolean sendEmail(Activity activity, Backup backup) throws QrCodeSizeException {
        return sendEmail(activity, backup.getName(), backup.getEncryptedData(), null, true);
    }

    public void saveToCloud(Backup backup) {
        Cloud.save(mContext, backup);
    }

    public String getFromCloud(Backup backup) {
        return Cloud.getBackup(backup);
    }

    private boolean sendEmail(Activity activity, String name, String secret, String emailAddress, boolean isBackup) throws QrCodeSizeException {
        int storagePermission = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE);
            return false;
        }

        FileOutputStream fos = null;
        Bitmap bmpBase = Bitmap.createBitmap(PRINT_IMAGE_WIDTH, PRINT_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpBase);
        canvas.drawColor(Color.WHITE);

        Bitmap qrCode = getQrCode(secret, 400, 400);

        String subject;
        String emailBody;

        if (isBackup) {
            QrCodePrintAdapter.drawBackup(mContext, canvas, qrCode, name);

            subject = String.format(mContext.getString(R.string.email_subject_backup), name);
            emailBody = mContext.getString(R.string.email_text_backup);
        } else {
            String userName = DatabaseHelper.getHelper(mContext).getUserName();
            QrCodePrintAdapter.drawKeyPart(mContext, canvas, qrCode, name);

            subject = String.format(mContext.getString(R.string.email_subject), userName);
            emailBody = String.format(mContext.getString(R.string.email_text), name, userName);
        }

        File path = new File(EMAIL_ATTACHMENT_FOLDER);
        String fileName = name + ".jpg";

        if (!path.isDirectory()) {
            path.mkdir();
        }

        // Save Bitmap to File
        try {
            fos = new FileOutputStream(path + "/" + fileName);
            bmpBase.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            fos.flush();
            fos.close();
            fos = null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            bmpBase.recycle();
        }

        File attachment = new File(path + "/" + fileName);
        Uri attachmentUri = Uri.fromFile(attachment);

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));

        if (emailAddress != null) {
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {emailAddress});
        }

        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);
        emailIntent.putExtra(Intent.EXTRA_STREAM, attachmentUri);

        boolean emailAppInstalled = emailIntent.resolveActivity(mContext.getPackageManager()) != null;

        if (emailAppInstalled) {
            mContext.startActivity(emailIntent);
        }

        return emailAppInstalled;
    }
}
