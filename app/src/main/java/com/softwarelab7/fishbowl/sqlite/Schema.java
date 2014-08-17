package com.softwarelab7.fishbowl.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;

import com.softwarelab7.fishbowl.models.Sale;

import java.util.Date;

/**
 */
public class Schema {
    public Schema() {}

    public static final String REAL_TYPE = " REAL";
    public static final String TEXT_TYPE = " TEXT";
    public static final String INTEGER_TYPE = " INTEGER";
    public static final String COMMA_SEP = ",";

    public static abstract class Sale implements BaseColumns {
        public static final String TABLE_NAME = "sale";
        public static final String COLUMN_NAME_SESSION = "session";
        public static final String COLUMN_NAME_LOCATION = "location";
        public static final String COLUMN_NAME_LATITUDE = "lat";
        public static final String COLUMN_NAME_LONGITUDE = "lon";
        public static final String COLUMN_NAME_TIMESTAMP = "dateCreated";
        public static final String[] SELECT_SALE_COLUMNS = new String[] {
                _ID,
                COLUMN_NAME_SESSION,
                COLUMN_NAME_LOCATION,
                COLUMN_NAME_LATITUDE,
                COLUMN_NAME_LONGITUDE,
                COLUMN_NAME_TIMESTAMP
        };

        public static final String CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + INTEGER_TYPE + " PRIMARY KEY," +
                        COLUMN_NAME_SESSION + INTEGER_TYPE + COMMA_SEP +
                        COLUMN_NAME_LOCATION + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_LONGITUDE + REAL_TYPE + COMMA_SEP +
                        COLUMN_NAME_LATITUDE + REAL_TYPE + COMMA_SEP +
                        COLUMN_NAME_TIMESTAMP + INTEGER_TYPE +
                        ")";

        public static final String DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static ContentValues createContentValues(com.softwarelab7.fishbowl.models.Sale sale) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_NAME_SESSION, sale.session);
            contentValues.put(COLUMN_NAME_LATITUDE, sale.lat);
            contentValues.put(COLUMN_NAME_LONGITUDE, sale.lon);
            contentValues.put(COLUMN_NAME_TIMESTAMP, sale.dateCreated.getTime());
            return contentValues;
        }

        public static com.softwarelab7.fishbowl.models.Sale toTransaction(Cursor cursor) {
            com.softwarelab7.fishbowl.models.Sale sale = new com.softwarelab7.fishbowl.models.Sale();
            sale.id = cursor.getLong(cursor.getColumnIndex(_ID));
            sale.session = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_SESSION));
            sale.location = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_LOCATION));
            sale.lat = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_LATITUDE));
            sale.lon = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_LONGITUDE));
            sale.dateCreated = new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_TIMESTAMP)));
            return null;
        }

//        public static long insert(com.softwarelab7.cancardrivenow.models.Car car, SQLiteDatabase db) {
//            return db.insert(TABLE_NAME, null, createContentValues(car));
//        }
//
//        public static int update(com.softwarelab7.cancardrivenow.models.Car car, SQLiteDatabase db) {
//            return db.update(TABLE_NAME, createContentValues(car), _ID+"="+car.id, null);
//        }
//
//        public static void delete(com.softwarelab7.cancardrivenow.models.Car car, SQLiteDatabase db) {
//            db.delete(Car.TABLE_NAME, _ID+"="+car.id, null);
//        }
//
//        public static ContentValues createContentValues(com.softwarelab7.cancardrivenow.models.Car car) {
//            ContentValues contentValues = new ContentValues();
//            contentValues.put(Car.COLUMN_NAME_NAME, car.name);
//            contentValues.put(Car.COLUMN_NAME_PLATE_NUMBER, car.plateNumber);
//            return contentValues;
//        }

    }

    public static abstract class Session implements BaseColumns {
        public static final String TABLE_NAME = "session";
        public static final String COLUMN_NAME_ACTIVE = "active";

        public static final String CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + INTEGER_TYPE + " PRIMARY KEY," +
                        COLUMN_NAME_ACTIVE + INTEGER_TYPE +
                        " )";

        public static final String DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static ContentValues createContentValues(com.softwarelab7.fishbowl.models.Session session) {
            ContentValues contentValues = new ContentValues();
            if (session.id != null) {
                contentValues.put(_ID, session.id);
            }
            contentValues.put(COLUMN_NAME_ACTIVE, session.active);
            return contentValues;
        }

        public static com.softwarelab7.fishbowl.models.Session toSession(Cursor cursor) {
            com.softwarelab7.fishbowl.models.Session session = new com.softwarelab7.fishbowl.models.Session();
            session.id = cursor.getLong(cursor.getColumnIndex(Session._ID));
            session.active = cursor.getInt(cursor.getColumnIndex(Session.COLUMN_NAME_ACTIVE)) == 1;
            return session;
        }

    }
}
