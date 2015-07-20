package org.opennms.android.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import org.opennms.android.App;
import org.opennms.android.data.storage.Contract;
import org.opennms.android.data.storage.Contract.Alarms;
import org.opennms.android.data.storage.Contract.Events;
import org.opennms.android.data.storage.Contract.Nodes;
import org.opennms.android.data.storage.Contract.Outages;
import org.opennms.android.data.storage.Contract.Tables;
import org.opennms.android.data.sync.Updater;

import javax.inject.Inject;

public class AppContentProvider extends ContentProvider {

    private static final String TAG = "AppContentProvider";
    private static final int NODES = 100;
    private static final int NODES_ID = 101;
    private static final int NODES_NAME = 102;
    private static final int ALARMS = 200;
    private static final int ALARMS_ID = 201;
    private static final int EVENTS = 300;
    private static final int EVENTS_ID = 301;
    private static final int OUTAGES = 400;
    private static final int OUTAGES_ID = 401;
    private static UriMatcher uriMatcher;
    @Inject SQLiteDatabase db;
    @Inject Updater updater;

    private static UriMatcher createUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = Contract.CONTENT_AUTHORITY;

        // Nodes
        matcher.addURI(authority, Contract.PATH_NODES, NODES);
        matcher.addURI(authority, Contract.PATH_NODES + "/#", NODES_ID);
        matcher.addURI(authority, Contract.PATH_NODES + "/" + Nodes.LABEL + "/*", NODES_NAME);

        // Alarms
        matcher.addURI(authority, Contract.PATH_ALARMS, ALARMS);
        matcher.addURI(authority, Contract.PATH_ALARMS + "/#", ALARMS_ID);

        // Events
        matcher.addURI(authority, Contract.PATH_EVENTS, EVENTS);
        matcher.addURI(authority, Contract.PATH_EVENTS + "/#", EVENTS_ID);

        // Outage
        matcher.addURI(authority, Contract.PATH_OUTAGES, OUTAGES);
        matcher.addURI(authority, Contract.PATH_OUTAGES + "/#", OUTAGES_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        App.get(getContext()).inject(this);
        uriMatcher = createUriMatcher();
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = uriMatcher.match(uri);
        switch (match) {
            case NODES:
                return Nodes.CONTENT_TYPE;
            case NODES_ID:
                return Nodes.CONTENT_ITEM_TYPE;
            case ALARMS:
                return Alarms.CONTENT_TYPE;
            case ALARMS_ID:
                return Alarms.CONTENT_ITEM_TYPE;
            case EVENTS:
                return Events.CONTENT_TYPE;
            case EVENTS_ID:
                return Events.CONTENT_ITEM_TYPE;
            case OUTAGES:
                return Outages.CONTENT_TYPE;
            case OUTAGES_ID:
                return Outages.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        Cursor cursor;

        final int match = uriMatcher.match(uri);
        switch (match) {
            case NODES:
                queryBuilder.setTables(Tables.NODES);
                cursor =
                        queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case NODES_ID:
                queryBuilder.setTables(Tables.NODES);
                queryBuilder.appendWhere(Nodes._ID + "=" + uri.getLastPathSegment());
                cursor =
                        queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                if (!cursor.moveToFirst()) {
                    Log.i(TAG, "Node #" + uri.getLastPathSegment() + " is missing. Updating...");
                    updater.updateNode(Long.parseLong(uri.getLastPathSegment()));
                    cursor =
                            queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                }
                break;

            case NODES_NAME:
                queryBuilder.setTables(Tables.NODES);
                queryBuilder.appendWhere(Nodes.LABEL + " LIKE '%" + uri.getLastPathSegment() + "%'");
                cursor =
                        queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case ALARMS:
                queryBuilder.setTables(Tables.ALARMS);
                cursor =
                        queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case ALARMS_ID:
                queryBuilder.setTables(Tables.ALARMS);
                queryBuilder.appendWhere(Alarms._ID + "=" + uri.getLastPathSegment());
                cursor =
                        queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                if (!cursor.moveToFirst()) {
                    Log.i(TAG, "Alarm #" + uri.getLastPathSegment() + " is missing. Updating...");
                    updater.updateAlarm(Long.parseLong(uri.getLastPathSegment()));
                    cursor =
                            queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                }
                break;

            case EVENTS:
                queryBuilder.setTables(Tables.EVENTS);
                cursor =
                        queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case EVENTS_ID:
                queryBuilder.setTables(Tables.EVENTS);
                queryBuilder.appendWhere(Events._ID + "=" + uri.getLastPathSegment());
                cursor =
                        queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                if (!cursor.moveToFirst()) {
                    Log.i(TAG, "Event #" + uri.getLastPathSegment() + " is missing. Updating...");
                    updater.updateEvent(Long.parseLong(uri.getLastPathSegment()));
                    cursor =
                            queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                }
                break;

            case OUTAGES:
                queryBuilder.setTables(Tables.OUTAGES);
                cursor =
                        queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case OUTAGES_ID:
                queryBuilder.setTables(Tables.OUTAGES);
                queryBuilder.appendWhere(Outages._ID + "=" + uri.getLastPathSegment());
                cursor =
                        queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                if (!cursor.moveToFirst()) {
                    Log.i(TAG, "Outage #" + uri.getLastPathSegment() + " is missing. Updating...");
                    updater.updateOutage(Long.parseLong(uri.getLastPathSegment()));
                    cursor =
                            queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                }
                break;

            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = uriMatcher.match(uri);
        switch (match) {
            case NODES: {
                long
                        id =
                        db.insertWithOnConflict(Tables.NODES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(uri, id);
            }
            case EVENTS: {
                long
                        id =
                        db.insertWithOnConflict(Tables.EVENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(uri, id);
            }
            case ALARMS: {
                long
                        id =
                        db.insertWithOnConflict(Tables.ALARMS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(uri, id);
            }
            case OUTAGES: {
                long
                        id =
                        db.insertWithOnConflict(Tables.OUTAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(uri, id);
            }
            default: {
                throw new UnsupportedOperationException("Unknown URI: " + uri);
            }
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int rowsAffected;
        final int match = uriMatcher.match(uri);
        switch (match) {
            case NODES: {
                rowsAffected = bulkInsertHelper(Tables.NODES, values);
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            }
            case EVENTS: {
                rowsAffected = bulkInsertHelper(Tables.EVENTS, values);
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            }
            case ALARMS: {
                rowsAffected = bulkInsertHelper(Tables.ALARMS, values);
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            }
            case OUTAGES: {
                rowsAffected = bulkInsertHelper(Tables.OUTAGES, values);
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown URI: " + uri);
            }
        }
        return rowsAffected;
    }

    private int bulkInsertHelper(String table, ContentValues[] values) {
        db.beginTransaction();
        try {
            int rowsAffected = values.length;
            for (ContentValues currentValues : values) {
                db.insertWithOnConflict(table, null, currentValues, SQLiteDatabase.CONFLICT_REPLACE);
                db.yieldIfContendedSafely();
            }
            db.setTransactionSuccessful();
            return rowsAffected;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int rowsAffected = 0;
        final int match = uriMatcher.match(uri);
        switch (match) {
            case NODES_ID:
                StringBuilder modSelectionNode =
                        new StringBuilder(BaseColumns._ID + "=" + uri.getLastPathSegment());
                if (!TextUtils.isEmpty(selection)) {
                    modSelectionNode.append(" AND ").append(selection);
                }
                rowsAffected = db.update(Tables.NODES, values, modSelectionNode.toString(), null);
                break;
            case NODES:
                rowsAffected = db.update(Tables.NODES, values, selection, selectionArgs);
                break;
            case ALARMS_ID:
                StringBuilder modSelectionAlarm =
                        new StringBuilder(BaseColumns._ID + "=" + uri.getLastPathSegment());
                if (!TextUtils.isEmpty(selection)) {
                    modSelectionAlarm.append(" AND ").append(selection);
                }
                rowsAffected = db.update(Tables.ALARMS, values, modSelectionAlarm.toString(), null);
                break;
            case ALARMS:
                rowsAffected = db.update(Tables.ALARMS, values, selection, selectionArgs);
                break;
            case EVENTS_ID:
                StringBuilder modSelectionEvent =
                        new StringBuilder(BaseColumns._ID + "=" + uri.getLastPathSegment());
                if (!TextUtils.isEmpty(selection)) {
                    modSelectionEvent.append(" AND ").append(selection);
                }
                rowsAffected = db.update(Tables.ALARMS, values, modSelectionEvent.toString(), null);
                break;
            case EVENTS:
                rowsAffected = db.update(Tables.ALARMS, values, selection, selectionArgs);
                break;
            case OUTAGES_ID:
                StringBuilder modSelectionOutage =
                        new StringBuilder(BaseColumns._ID + "=" + uri.getLastPathSegment());
                if (!TextUtils.isEmpty(selection)) {
                    modSelectionOutage.append(" AND ").append(selection);
                }
                rowsAffected = db.update(Tables.ALARMS, values, modSelectionOutage.toString(), null);
                break;
            case OUTAGES:
                rowsAffected = db.update(Tables.ALARMS, values, selection, selectionArgs);
                break;
            default:
                break;
        }
        getContext().getContentResolver().notifyChange(uri, null);

        return rowsAffected;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rowsAffected = 0;
        final int match = uriMatcher.match(uri);
        switch (match) {
            case NODES:
                rowsAffected = db.delete(Tables.NODES, selection, selectionArgs);
                break;
            case NODES_ID:
                String nodeId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsAffected = db.delete(Tables.NODES, BaseColumns._ID + "=" + nodeId, null);
                } else {
                    rowsAffected =
                            db.delete(Tables.NODES, selection + " AND " + BaseColumns._ID + "=" + nodeId,
                                    selectionArgs);
                }
                break;
            case ALARMS:
                rowsAffected = db.delete(Tables.ALARMS, selection, selectionArgs);
                break;
            case ALARMS_ID:
                String alarmId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsAffected = db.delete(Tables.ALARMS, BaseColumns._ID + "=" + alarmId, null);
                } else {
                    rowsAffected =
                            db.delete(Tables.ALARMS, selection + " AND " + BaseColumns._ID + "=" + alarmId,
                                    selectionArgs);
                }
                break;
            case EVENTS:
                rowsAffected = db.delete(Tables.EVENTS, selection, selectionArgs);
                break;
            case EVENTS_ID:
                String eventId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsAffected = db.delete(Tables.EVENTS, BaseColumns._ID + "=" + eventId, null);
                } else {
                    rowsAffected =
                            db.delete(Tables.EVENTS, selection + " AND " + BaseColumns._ID + "=" + eventId,
                                    selectionArgs);
                }
                break;
            case OUTAGES:
                rowsAffected = db.delete(Tables.OUTAGES, selection, selectionArgs);
                break;
            case OUTAGES_ID:
                String outageId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsAffected = db.delete(Tables.OUTAGES, BaseColumns._ID + "=" + outageId, null);
                } else {
                    rowsAffected =
                            db.delete(Tables.OUTAGES, selection + " AND " + BaseColumns._ID + "=" + outageId,
                                    selectionArgs);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsAffected;
    }

}
