package com.softwarelab7.fishbowl.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.softwarelab7.fishbowl.models.Sale;
import com.softwarelab7.fishbowl.models.Session;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 */
public class DbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Fishbowl.db";

    private SQLiteDatabase db;

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Schema.Session.CREATE_TABLE);
        db.execSQL(Schema.Sale.CREATE_TABLE);
//        createTestCars(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(Schema.Sale.DELETE_TABLE);
        db.execSQL(Schema.Session.CREATE_TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public long insert(com.softwarelab7.fishbowl.models.Sale transaction) {
        return db.insert(Schema.Sale.TABLE_NAME, null, Schema.Sale.createContentValues(transaction));
    }

    public long insert(Session session) {
        return db.insert(Schema.Session.TABLE_NAME, null, Schema.Session.createContentValues(session));
    }

    public Session getLatestSession() {
        Cursor cursor = db.query(Schema.Session.TABLE_NAME,
                new String[] {Schema.Session._ID, Schema.Session.COLUMN_NAME_ACTIVE},
                Schema.Session.COLUMN_NAME_ACTIVE+"=1", null, null, null, null);
        if (cursor.isAfterLast()) {
            Session session = new Session();
            session.active = true;
            session.id = insert(session);
            return session;
        } else {
            cursor.moveToFirst();
            return Schema.Session.toSession(cursor);
        }
    }

    public long getSoldForSession(Session session) {
        Cursor cursor = db.rawQuery("select count(" + Schema.Sale._ID + ") " +
                        "from " + Schema.Sale.TABLE_NAME + " " +
                        "where " + Schema.Sale.COLUMN_NAME_SESSION + " = " + session.id + ";",
                null
        );
        cursor.moveToFirst();
        return cursor.getLong(0);
    }

    public com.softwarelab7.fishbowl.models.Sale getLatestTransaction() {
        Cursor cursor = db.query(Schema.Sale.TABLE_NAME, Schema.Sale.SELECT_SALE_COLUMNS, null, null, null, null,
                Schema.Sale.COLUMN_NAME_TIMESTAMP + " DESC LIMIT 1");
        if (cursor.isAfterLast()) {
            return null;
        } else {
            cursor.moveToFirst();
            return Schema.Sale.toTransaction(cursor);
        }
    }

    public List <com.softwarelab7.fishbowl.models.Sale> getPastSales() {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);

        Cursor cursor = db.rawQuery("select * " +
                        "from " + Schema.Sale.TABLE_NAME + " " +
                        "where " + Schema.Sale.COLUMN_NAME_TIMESTAMP + " < " + calendar.getTime().getTime() + ";",
                null
        );
        if (cursor.isAfterLast()) {
            return new ArrayList<Sale>();
        } else {
            List<Sale> sales = new ArrayList<Sale>();
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                sales.add(Schema.Sale.toTransaction(cursor));
                cursor.moveToNext();
            }
            return sales;
        }
    }

}
