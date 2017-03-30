package com.android.secret.sharing;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import java.util.ArrayList;
import java.util.List;

/**
 * This class loads contacts with key parts of a container.
 */
public class ContactLoader implements LoaderManager.LoaderCallbacks<Cursor> {
    private Context mContext;
    private ContactLoaderListener mListener;
    private long mBackupId;

    public ContactLoader(Context context, ContactLoaderListener listener) {
        mContext = context;
        mListener = listener;
        mBackupId = DatabaseHelper.getHelper(context).getContainerId();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " ASC, " + ContactsContract.Contacts._ID + " ASC";

        return new CursorLoader(mContext, ContactsContract.Contacts.CONTENT_URI, null, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Cursor secretContactsCursor = loadSecretContacts();

        ArrayList<Contact> contacts = new ArrayList<>();
        List<ContactImpl> updatedContacts = new ArrayList<>();

        String[] columnNamesLeft = new String[] { ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID };
        String[] columnNamesRight = new String[] { SecretSharingContract.Contact.COLUMN_NAME, SecretSharingContract.Contact.COLUMN_CONTACT_ID };

        CursorJoiner joiner = new CursorJoiner(data, columnNamesLeft, secretContactsCursor, columnNamesRight);
        for (CursorJoiner.Result joinerResult : joiner) {
            ContactImpl contact = null;

            switch (joinerResult) {
                case LEFT:
                    // contact only in address book of device

                    contact = new ContactImpl();

                    contact.setName(data.getString(data.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)));
                    contact.setContactId(data.getLong(data.getColumnIndexOrThrow(ContactsContract.Contacts._ID)));
                    contact.setLookupKey(data.getString(data.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)));
                    break;
                case RIGHT:
                    // contact only in database of this library => id changed

                    contact = new ContactImpl(secretContactsCursor);
                    boolean newIdFound = contact.updateContactId(mContext);

                    if (newIdFound) {
                        contact.save(mContext);
                        updatedContacts.add(contact);
                        continue;
                    }

                    break;
                case BOTH:
                    contact = new ContactImpl(secretContactsCursor);
                    break;
            }

            contacts.add(contact);
        }

        if (updatedContacts.size() > 0) {

            loop:
            for (int i = 0; i < contacts.size() ; i++) {
                ContactImpl loadedContact = (ContactImpl) contacts.get(i);

                for (int j = 0; j < updatedContacts.size(); j++) {
                    ContactImpl updatedContact = updatedContacts.get(j);

                    if (loadedContact.getContactId() == updatedContact.getContactId()) {
                        contacts.remove(i);
                        contacts.add(i, updatedContact);

                        updatedContacts.remove(j);

                        continue loop;
                    }
                }
            }
        }

        // contacts are deleted from address book they should still be shown in app since they are selected to receive a key part
        contacts.addAll(updatedContacts);

        mListener.contactsLoaded(contacts);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Load contacts that have key parts.
     */
    public void loadContacts() {
        Cursor cursor = loadSecretContacts();

        ArrayList<Contact> contacts = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                contacts.add(new ContactImpl(cursor));
            }

            cursor.close();
        }

        mListener.contactsLoaded(contacts);
    }

    private Cursor loadSecretContacts() {
        String selection = SecretSharingContract.Contact.COLUMN_Container_ID + "=?";
        String[] selectionArgs = new String[] { String.valueOf(mBackupId) };
        String sortOrder = SecretSharingContract.Contact.COLUMN_NAME + " ASC, " + SecretSharingContract.Contact.COLUMN_CONTACT_ID + " ASC";

        SQLiteDatabase db = DatabaseHelper.getHelper(mContext).getDatabase();
        return db.query(SecretSharingContract.Contact.TABLE_NAME, null, selection, selectionArgs, null, null, sortOrder);
    }

    public interface ContactLoaderListener {
        /**
         * Loading contacts is finished.
         */
        void contactsLoaded(ArrayList<Contact> contacts);
    }

}
