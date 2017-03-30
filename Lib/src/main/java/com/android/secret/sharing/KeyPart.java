package com.android.secret.sharing;

import android.content.Context;
import android.os.Parcelable;

import com.secure.key.backup.*;

/**
 * Part of a private key of a {@link Container}. Key parts can be stored in the database
 * of this library.
 */
public interface KeyPart extends SecretPresentation, Parcelable {

    /**
     * Get the name of the owner of this key part.
     */
    String getOwner();

    /**
     * Save this key part into the database of this library.
     */
    void save(Context context);

    /**
     * Delete this key part from the database of this library.
     */
    void delete(Context context);

    /**
     * Specify whether this key part belongs to the user or another person.
     */
    void setForeign(boolean foreign);

    /**
     * Loads minimum needed key parts from container.
     */
    int getMinimumKeyParts(Context context);
}
