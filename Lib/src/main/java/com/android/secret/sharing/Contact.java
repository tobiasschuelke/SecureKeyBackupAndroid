package com.android.secret.sharing;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.android.share.sharing.R;

/**
 *  Addressee of a {@link KeyPart}.
 */
public interface Contact extends Parcelable {

    /**
     * Set the name of this person.
     *
     * @param name Name of the person.
     */
    void setName(String name);

    /**
     * Get the name of this person.
     *
     * @return Name of this person.
     */
    String getName();

    /**
     * Loads a photo of this contact. Returns null if there is no photo available.
     */
    @Nullable
    Bitmap getPhoto(Context context);

    /**
     * Set the email address.
     */
    void setEmail(String email);

    /**
     * Load the email address of this person that is stored in the address book of the device.
     *
     * @return If available an email that is stored in the address book of the device. Otherwise an already specified email or null.
     */
    String loadEmail(Context context);

    /**
     * Link a key part to this contact. Should be done when the user wants to send the key part now or later.
     */
    void setKeyPart(KeyPart keyPart);

    /**
     * Get the current send status of the linked key part.
     */
    SendStatus getSendStatus();

    /**
     * Set the send status of the linked key part.
     */
    void setSendStatus(SendStatus sendStatus);

    /**
     * Get the specified send method that will be used to transmit the key part.
     */
    SendMethod getSendMethod();

    /**
     * Get the specified send method that will be used to transmit the key part.
     */
    void setSendMethod(SendMethod sendMethod);

    /**
     * Position set via {@link #setListPosition(int)}.
     */
    int getListPosition();

    /**
     * Store the position of this contact in a list.
     *
     * @param position Position of this contact in a list.
     */
    void setListPosition(int position);

    /**
     * Save this contact into the database of this library.
     */
    void save(Context context);

    /**
     * Removes links to key part and container. Resets send method and send status. Updates these
     * values in database of this library.
     */
    void clearSendSelection(Context context);

    /**
     * Loads the linked key part.
     */
    KeyPart getKeyPart(Context context);

    /**
     * True is this contact has an email address.
     */
    boolean hasEmail();

    /**
     * Returns the email address of this contact.
     */
    String getEmail();

    /**
     * True if this contact has a linked key part.
     */
    boolean hasKeyPart();

    /**
     * Remove the linked key part.
     *
     * @param keyPart Needed to update container id of this contact. Can be null.
     */
    void removeKeyPart(KeyPart keyPart);

    /**
     * Method used to transmit the linked key part.
     */
    enum SendMethod {
        NONE (1), QR(2), PRINT(3), EMAIL(4);

        int id;

        SendMethod(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static SendMethod fromId(int id) {
            switch (id) {
                case 1:
                    return NONE;
                case 2:
                    return QR;
                case 3:
                    return PRINT;
                case 4:
                    return EMAIL;
                default:
                    throw new IllegalArgumentException("No send method defined with id " + id);
            }
        }

        public String toString(Context context) {
            switch (id) {
                case 2:
                    return context.getString(R.string.qr_code);
                case 3:
                    return context.getString(R.string.print_qr_code);
                case 4:
                    return context.getString(R.string.email);
            }

            return "NONE";
        }
    }

    /**
     * Status of the transmission of the linked key part.
     */
    enum SendStatus {
        NONE(0),        // this contact is shown in contact list and can be selected
        SELECTED(1),    // user wants to send a key part to this contact but did not send it yet
        SENT(2),        // user sent a key part to this contact
        CONFIRMED(3),   // user confirmed that this contact received the secret part
        RECEIVED(4);    // received secret part back

        int id;

        SendStatus(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static SendStatus getStatus(int id) {
            switch (id) {
                case 0:
                    return NONE;
                case 1:
                    return SELECTED;
                case 2:
                    return SENT;
                case 3:
                    return CONFIRMED;
                case 4:
                    return RECEIVED;
                default:
                    throw new IllegalArgumentException("No send status defined with id = " + id);
            }
        }
    }
}
