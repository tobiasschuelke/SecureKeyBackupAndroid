package com.android.secret.sharing;

public interface QrKeyPartListener {

    /**
     * Secret part transferred successfully via QR Code from other device.
     */
    void qrCodeDetected(SecretPresentation secret);

    /**
     * Backup and scanned secret part do not match.
     */
    void wrongSecretPart();
}
