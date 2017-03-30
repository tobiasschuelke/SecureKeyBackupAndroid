package com.android.secret.sharing;

import android.provider.BaseColumns;

final class SecretSharingContract {
    private static final String CREATE_TABLE = "CREATE TABLE ";
    private static final String PRIMARY_KEY = " PRIMARY KEY";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String BLOB_TYPE = " BLOB";
    private static final String NOT_NULL = " NOT NULL";
    private static final String COMMA_SEP = ",";

    static final class Container implements BaseColumns {
        public static final String TABLE_NAME = "container";

        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_MINIMAL_KEY_PARTS = "minimal_key_parts";
        public static final String COLUMN_TOTAL_KEY_PARTS = "total_key_parts";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_PUBLIC_KEY = "public_key";

        public static final String CREATE =
                CREATE_TABLE + TABLE_NAME + " (" +
                        _ID                            + INTEGER_TYPE + PRIMARY_KEY + COMMA_SEP +
                        COLUMN_NAME                    + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_MINIMAL_KEY_PARTS       + INTEGER_TYPE + COMMA_SEP +
                        COLUMN_TOTAL_KEY_PARTS         + INTEGER_TYPE + COMMA_SEP +
                        COLUMN_TIMESTAMP               + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_PUBLIC_KEY              + BLOB_TYPE +
                ");";
    }

    static final class Backup implements BaseColumns {
        public static final String TABLE_NAME = "backup";

        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_TIMESTAMP = "backup_timestamp";
        public static final String COLUMN_DATA = "data";
        public static final String COLUMN_STORE_METHOD = "store_method";
        public static final String COLUMN_CLOUD_PATH = "cloud_path";

        public static final String CREATE =
                CREATE_TABLE + TABLE_NAME + " (" +
                        _ID                            + INTEGER_TYPE + PRIMARY_KEY + COMMA_SEP +
                        COLUMN_NAME                    + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_TIMESTAMP               + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_DATA                    + BLOB_TYPE + COMMA_SEP +
                        COLUMN_STORE_METHOD            + TEXT_TYPE + COMMA_SEP +
                        COLUMN_CLOUD_PATH              + TEXT_TYPE +
                ");";
    }

    static final class Contact implements BaseColumns {
        public static final String TABLE_NAME = "contact";

        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_SEND_STATUS = "send_status";
        public static final String COLUMN_SEND_METHOD = "send_method";
        public static final String COLUMN_CONTACT_ID = "contact_id";
        public static final String COLUMN_LOOKUP_KEY = "lookup_key";
        public static final String COLUMN_Container_ID = "container_id";
        public static final String COLUMN_KEY_PART_ID = "key_part_id";

        public static final String CREATE =
                CREATE_TABLE + TABLE_NAME + " (" +
                        _ID                     + INTEGER_TYPE + PRIMARY_KEY + COMMA_SEP +
                        COLUMN_NAME             + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_EMAIL            + TEXT_TYPE + COMMA_SEP +
                        COLUMN_SEND_STATUS      + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_Container_ID     + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_KEY_PART_ID      + INTEGER_TYPE + COMMA_SEP +
                        COLUMN_SEND_METHOD      + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_CONTACT_ID       + INTEGER_TYPE + COMMA_SEP +
                        COLUMN_LOOKUP_KEY       + TEXT_TYPE + COMMA_SEP +
                        "FOREIGN KEY(" + COLUMN_Container_ID + ") REFERENCES " + Container.TABLE_NAME + "(" + Container._ID + ")" + COMMA_SEP +
                        "FOREIGN KEY(" + COLUMN_KEY_PART_ID + ") REFERENCES " + KeyPart.TABLE_NAME + "(" + KeyPart._ID + ")" +
                ");";
    }

    static final class KeyPart implements BaseColumns {
        public static final String TABLE_NAME = "key_part";

        public static final String COLUMN_KEY_PART = "key_part";
        public static final String COLUMN_OWNER = "owner";

        // separates key parts of user from received key parts; owner not sufficient because different users can have the same names
        public static final String COLUMN_IS_FOREIGN = "is_foreign";

        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_Container_ID = "container_id";

        public static final String CREATE =
                CREATE_TABLE + TABLE_NAME + " (" +
                        _ID                     + INTEGER_TYPE + PRIMARY_KEY + COMMA_SEP +
                        COLUMN_KEY_PART         + BLOB_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_OWNER            + TEXT_TYPE + COMMA_SEP +
                        COLUMN_IS_FOREIGN       + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_TIMESTAMP        + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_Container_ID     + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        "FOREIGN KEY(" + COLUMN_Container_ID + ") REFERENCES " + Container.TABLE_NAME + "(" + Container._ID + ")" +
                ");";

    }
}
