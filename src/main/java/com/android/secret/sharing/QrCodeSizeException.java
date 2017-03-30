package com.android.secret.sharing;

/**
 * Exception is thrown when a QR-Code should be created but the content does not fit into the QR-Code.
 */
public class QrCodeSizeException extends Exception {

    public QrCodeSizeException(int textLength) {
        super("Could not create QR Code. Input text length: " + textLength + ", maximum allowed text length: " + AndroidSecretSharing.MAXIMUM_QR_CODE_SIZE);
    }
}
