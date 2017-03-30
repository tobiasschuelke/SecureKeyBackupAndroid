package com.android.secret.sharing;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.SurfaceView;

import com.google.gson.Gson;

import github.nisrulz.qreader.QRDataListener;
import github.nisrulz.qreader.QREader;

/**
 * Reads QR-Codes. Google Play Services must be installed on the device. Version 7.8 is
 * requiered at least.
 */
public class QrReader {
    private static final int REQUEST_CODE_PERMISSION_CAMERA = 4289;

    private QREader mQReader;
    private QrKeyPartListener mListener;
    private Activity mActivity;
    private SurfaceView mSurfaceView;
    private DatabaseHelper mDbHelper;

    private boolean mForeignKey;
    private long mTimestamp;
    private boolean mIgnoreKeyPartOrigin; // ignore timestamp and name of scanned secret. Used to restore backup from another device

    private QRDataListener mQrDataListener;

    /**
     * Scan arbitrary data. Camera permission will be checked. The
     * activity needs to override {@link Activity#onRequestPermissionsResult(int, String[], int[])} and
     * call {@link AndroidSecretSharing#onRequestPermissionsResult(int, String[], int[])} in it.
     *
     * @param surfaceView View to display camera on.
     * @param listener Receives the content of the scanned QR-Code.
     */
    QrReader(Activity activity, SurfaceView surfaceView, QRDataListener listener) {
        this(activity, surfaceView, null, false);

        mQrDataListener = listener;
    }

    /**
     * Scan a key part. Camera permission will be checked. The
     * activity needs to override {@link Activity#onRequestPermissionsResult(int, String[], int[])} and
     * call {@link AndroidSecretSharing#onRequestPermissionsResult(int, String[], int[])} in it.
     *
     * @param surfaceView View to display camera on.
     * @param listener Receives the key part of the scanned QR-Code.
     */
    QrReader(Activity activity, SurfaceView surfaceView, QrKeyPartListener listener, boolean foreignKey) {
        mListener = listener;
        mActivity = activity;
        mSurfaceView = surfaceView;
        mForeignKey = foreignKey;

        mDbHelper = DatabaseHelper.getHelper(activity);
        mTimestamp = mDbHelper.getContainerTimestamp();
    }


    void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showQRReader();
            }
        }
    }

    /**
     * Restart the QR-Reader.
     */
    public void restart() {
        // null-check necessary if user has not granted camera permission yet
        if (mQReader != null) {
            mQReader.start();
        }
    }

    /**
     * Stop the QR-Reader.
     *
     * @param release If true the QR-Reader cannot be restarted later.
     */
    public void stop(boolean release) {
        // null-check necessary if user has not granted camera permission yet
        if (mQReader != null) {
            mQReader.stop();

            if (release) {
                mQReader.releaseAndCleanup();
            }
        }
    }

    /**
     * Ignore timestamp and name of scanned key parts. Needed when backup should be restored on another device,
     */
    public void setIgnoreKeyPartOrigin(boolean ignoreOrigin) {
        mIgnoreKeyPartOrigin = ignoreOrigin;
    }

    /**
     * Show the camera and start scanning. Requests camera permission first.
     */
    public void showQRReader() {
        int cameraPermission = ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA);
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[] {Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSION_CAMERA);
            return;
        }

        mQReader = new QREader.Builder(mActivity, mSurfaceView, listener)
                .build();

        mQReader.init();
        mQReader.start();
    }

    private QRDataListener listener = new QRDataListener() {
        @Override
        public void onDetected(final String data) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mQrDataListener != null) {
                        // arbitrary data found
                        mQrDataListener.onDetected(data);
                        return;
                    }

                    KeyPartImpl keyPart;
                    try {
                        keyPart = new Gson().fromJson(data, KeyPartImpl.class);
                    } catch (Exception e) {
                        // user should scan a secret part but did scan something else
                        mListener.wrongSecretPart();
                        return;
                    }

                    if (mIgnoreKeyPartOrigin) {
                        mListener.qrCodeDetected(keyPart);
                        return;
                    }

                    String userName = mDbHelper.getUserName();
                    String secretPartUserName = keyPart.getOwner();
                    boolean nonEmptyNames = !TextUtils.isEmpty(userName) && !TextUtils.isEmpty(secretPartUserName);

                    if (mForeignKey) {
                        if (nonEmptyNames) {
                            if (userName.equals(secretPartUserName)) {
                                // user tries to store key part from himself as a foreign key
                                mListener.wrongSecretPart();
                                return;
                            }
                        }
                        keyPart.setForeign(true);
                        keyPart.save(mActivity);
                    } else {
                        if (keyPart.getTimestamp() != mTimestamp || (nonEmptyNames && !userName.equals(secretPartUserName))) {
                            // secret part of other person or secret part of other backup of this person
                            mListener.wrongSecretPart();
                            return;
                        }
                        keyPart.setForeign(false);
                        keyPart.save(mActivity);
                    }

                    mListener.qrCodeDetected(keyPart);
                }
            });
        }
    };
}
