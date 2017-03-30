package com.android.secret.sharing;

/**
 * Information that can be shown to the user (other information should be kept hidden)
 */
public interface SecretPresentation {
    /**
     * @return Name this secret belongs to.
     */
    String getName();

    /**
     * @return Time when backup was created.
     */
    long getTimestamp();
}
