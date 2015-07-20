package org.opennms.android.ui.nodes;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.opennms.android.R;
import org.opennms.android.data.ContentValuesGenerator;
import org.opennms.android.data.api.model.Alarm;
import org.opennms.android.data.api.model.Outage;
import org.opennms.android.data.storage.Contract;
import org.opennms.android.data.storage.DatabaseHelper;
import org.opennms.android.ui.ActivityUtils;
import org.opennms.android.ui.DetailsFragment;

import java.util.ArrayList;
import java.util.List;

public class NodeDetailsFragment extends DetailsFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "NodeDetailsFragment";
    private static final int LOADER_ID = 0x4;
    private long nodeId;
    private String nodeName;
    private AlarmsLoader alarmsLoader;
    private OutagesLoader outagesLoader;
    private EventsLoader eventsLoader;
    private SQLiteDatabase db;

    // Do not remove
    public NodeDetailsFragment() {}

    public NodeDetailsFragment(long nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        return new CursorLoader(getActivity(),
                Uri.withAppendedPath(Contract.Nodes.CONTENT_URI,
                        String.valueOf(nodeId)),
                null, null, null, null);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (!isAdded()) {
            return;
        }
        if (cursor != null && cursor.moveToFirst()) {
            updateContent(cursor);
            alarmsLoader = new AlarmsLoader();
            alarmsLoader.execute();
            eventsLoader = new EventsLoader();
            eventsLoader.execute();
            outagesLoader = new OutagesLoader();
            outagesLoader.execute();
        } else {
            showErrorMessage();
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DatabaseHelper(getActivity()).getReadableDatabase();
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        db.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.details_loading, container, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (alarmsLoader != null) {
            alarmsLoader.cancel(true);
        }
        if (outagesLoader != null) {
            outagesLoader.cancel(true);
        }
        if (eventsLoader != null) {
            eventsLoader.cancel(true);
        }
    }

    public void updateContent(Cursor cursor) {
        if (!cursor.moveToFirst()) {
            return;
        }

        RelativeLayout detailsContainer =
                (RelativeLayout) getActivity().findViewById(R.id.details_container);
        if (detailsContainer == null) {
            return;
        }
        detailsContainer.removeAllViews();
        LayoutInflater inflater =
                (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.node_details, detailsContainer);

        LinearLayout detailsLayout = (LinearLayout) getActivity().findViewById(R.id.node_details);

        String name = nodeName = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Nodes.LABEL));
        TextView nameView = (TextView) getActivity().findViewById(R.id.node_name);
        nameView.setText(name);

        int id = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.Nodes._ID));
        TextView idView = (TextView) getActivity().findViewById(R.id.node_id);
        idView.setText(String.valueOf(id));

        String sysContact = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Nodes.SYS_CONTACT));
        TextView sysContactView = (TextView) getActivity().findViewById(R.id.node_contact);
        if (sysContact != null) {
            sysContactView.setText(sysContact);
        } else {
            detailsLayout.removeView(sysContactView);
            TextView title = (TextView) getActivity().findViewById(R.id.node_contact_title);
            detailsLayout.removeView(title);
        }

        String createdTime =
                cursor.getString(cursor.getColumnIndexOrThrow(Contract.Nodes.CREATED_TIME));
        TextView timeView = (TextView) getActivity().findViewById(R.id.node_creation_time);
        timeView.setText(createdTime);

        String labelSource =
                cursor.getString(cursor.getColumnIndexOrThrow(Contract.Nodes.LABEL_SOURCE));
        TextView labelSourceView = (TextView) getActivity().findViewById(R.id.node_label_source);
        if (labelSource != null) {
            if (labelSource.equals("U")) {
                labelSourceView.setText(R.string.node_label_source_userdefined);
            } else if (labelSource.equals("H")) {
                labelSourceView.setText(R.string.node_label_source_hostname);
            } else if (labelSource.equals("S")) {
                labelSourceView.setText(R.string.node_label_source_sysname);
            } else if (labelSource.equals("A")) {
                labelSourceView.setText(R.string.node_label_source_address);
            } else {
                labelSourceView.setText(labelSource);
            }
        } else {
            detailsLayout.removeView(labelSourceView);
            TextView title = (TextView) getActivity().findViewById(R.id.node_label_source_title);
            detailsLayout.removeView(title);
        }

        String desc = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Nodes.DESCRIPTION));
        TextView descView = (TextView) getActivity().findViewById(R.id.node_description);
        if (desc != null) {
            descView.setText(desc);
        } else {
            detailsLayout.removeView(descView);
            TextView title = (TextView) getActivity().findViewById(R.id.node_description_title);
            detailsLayout.removeView(title);
        }

        String location = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Nodes.LOCATION));
        TextView locationView = (TextView) getActivity().findViewById(R.id.node_location);
        if (location != null) {
            locationView.setText(location);
        } else {
            detailsLayout.removeView(locationView);
            TextView title = (TextView) getActivity().findViewById(R.id.node_location_title);
            detailsLayout.removeView(title);
        }

        String sysObjectId =
                cursor.getString(cursor.getColumnIndexOrThrow(Contract.Nodes.SYS_OBJECT_ID));
        TextView sysObjectIdView = (TextView) getActivity().findViewById(R.id.node_sys_object_id);
        if (sysObjectId != null) {
            sysObjectIdView.setText(sysObjectId);
        } else {
            detailsLayout.removeView(sysObjectIdView);
            TextView title = (TextView) getActivity().findViewById(R.id.node_sys_object_id_title);
            detailsLayout.removeView(title);
        }
    }

    /**
     * {@link android.os.AsyncTask} that loads alarms related to current node. Updates local database
     * and generates views.
     */
    private class AlarmsLoader extends AsyncTask<Void, Void, Cursor> {

        /**
         * @param voids No parameters.
         * @return {@link android.database.Cursor} with alarms.
         */
        protected Cursor doInBackground(Void... voids) {
            List<Alarm> alarms = null;
            try {
                alarms = server.alarmsRelatedToNode(nodeName).alarms;
            } catch (Exception e) {
                Log.e(TAG, "Error occurred while loading info from server", e);
            }

            if (alarms != null) {
                ContentResolver contentResolver = getActivity().getContentResolver();
                ArrayList<ContentValues> values = ContentValuesGenerator.fromAlarms(alarms);
                contentResolver.bulkInsert(Contract.Alarms.CONTENT_URI,
                        values.toArray(new ContentValues[values.size()]));
            }

            /** Getting info from DB */
            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
            queryBuilder.setTables(Contract.Tables.ALARMS);
            queryBuilder.appendWhere(Contract.Alarms.NODE_ID + "=" + nodeId
                    + " AND " + Contract.Alarms.ACK_USER + " IS NULL");
            String[] projection = {
                    Contract.Alarms._ID,
                    Contract.Alarms.LOG_MESSAGE,
                    Contract.Alarms.SEVERITY
            };
            return queryBuilder.query(db, projection, null, null, null, null, null);
        }

        protected void onPostExecute(Cursor cursor) {
            LinearLayout detailsLayout = (LinearLayout) getActivity()
                    .findViewById(R.id.node_details);
            if (detailsLayout == null) {
                return;
            }
            TextView alarmsPlaceholder =
                    (TextView) getActivity().findViewById(R.id.node_alarms_placeholder);
            if (!cursor.moveToFirst()) {
                alarmsPlaceholder.setText(getString(R.string.no_outstanding_alarms));
            } else {
                detailsLayout.removeView(alarmsPlaceholder);

                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                LinearLayout container = (LinearLayout) getActivity()
                        .findViewById(R.id.node_details_alarms_container);

                for (boolean b = cursor.moveToFirst(); b; b = cursor.moveToNext()) {
                    View item = inflater.inflate(R.layout.node_details_alarm, null);

                    final int id = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.Alarms._ID));
                    TextView idText = (TextView) item.findViewById(R.id.node_details_alarm_id);
                    idText.setText("#" + id);

                    String severity = cursor.getString(
                            cursor.getColumnIndexOrThrow(Contract.Alarms.SEVERITY));
                    Resources res = getActivity().getResources();
                    int severityColor;
                    if (severity.equals("CLEARED")) {
                        severityColor = res.getColor(R.color.severity_cleared);
                    } else if (severity.equals("MINOR")) {
                        severityColor = res.getColor(R.color.severity_minor);
                    } else if (severity.equals("NORMAL")) {
                        severityColor = res.getColor(R.color.severity_normal);
                    } else if (severity.equals("INDETERMINATE")) {
                        severityColor = res.getColor(R.color.severity_minor);
                    } else if (severity.equals("WARNING")) {
                        severityColor = res.getColor(R.color.severity_warning);
                    } else if (severity.equals("MAJOR")) {
                        severityColor = res.getColor(R.color.severity_major);
                    } else {
                        severityColor = res.getColor(R.color.severity_critical);
                    }
                    idText.setBackgroundColor(severityColor);

                    String message = cursor
                            .getString(cursor.getColumnIndexOrThrow(Contract.Alarms.LOG_MESSAGE));
                    TextView messageText =
                            (TextView) item.findViewById(R.id.node_details_alarm_message);
                    messageText.setText(Html.fromHtml(message));

                    container.addView(item);

                    item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityUtils.showAlarmDetails(getActivity(), id);
                        }
                    });
                }
            }
        }
    }

    /**
     * {@link android.os.AsyncTask} that loads outages related to current node. Updates local database
     * and generates views.
     */
    private class OutagesLoader extends AsyncTask<Void, Void, Cursor> {

        /**
         * @param voids No parameters.
         * @return {@link android.database.Cursor} with outages.
         */
        protected Cursor doInBackground(Void... voids) {
            List<Outage> outages = null;
            try {
                outages = server.outagesRelatedToNode(nodeId).outages;
            } catch (Exception e) {
                Log.e(TAG, "Error occurred while loading info from server", e);
            }

            if (outages != null) {
                ContentResolver contentResolver = getActivity().getContentResolver();
                ArrayList<ContentValues> values = ContentValuesGenerator.fromOutages(outages);
                contentResolver.bulkInsert(Contract.Outages.CONTENT_URI,
                        values.toArray(new ContentValues[values.size()]));
            }

            /** Getting info from DB */
            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
            queryBuilder.setTables(Contract.Tables.OUTAGES);
            queryBuilder.appendWhere(Contract.Outages.NODE_ID + "=" + nodeId);
            String[] projection = {
                    Contract.Outages._ID,
                    Contract.Outages.SERVICE_TYPE_NAME
            };
            return queryBuilder.query(db, projection, null, null, null, null, null);
        }

        protected void onPostExecute(Cursor cursor) {
            LinearLayout detailsLayout = (LinearLayout) getActivity()
                    .findViewById(R.id.node_details);
            if (detailsLayout == null) {
                return;
            }
            TextView outagesPlaceholder =
                    (TextView) getActivity().findViewById(R.id.node_outages_placeholder);
            if (!cursor.moveToFirst()) {
                outagesPlaceholder.setText(getString(R.string.no_outages));
            } else {
                detailsLayout.removeView(outagesPlaceholder);

                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                LinearLayout container = (LinearLayout) getActivity()
                        .findViewById(R.id.node_details_outages_container);

                for (boolean b = cursor.moveToFirst(); b; b = cursor.moveToNext()) {
                    View item = inflater.inflate(R.layout.node_details_outage, null);

                    final int id =
                            cursor.getInt(cursor.getColumnIndexOrThrow(Contract.Outages._ID));
                    TextView idText = (TextView) item.findViewById(R.id.node_details_outage_id);
                    idText.setText("#" + id);

                    String serviceType = cursor.getString(
                            cursor.getColumnIndexOrThrow(Contract.Outages.SERVICE_TYPE_NAME));
                    TextView serviceTypeText =
                            (TextView) item.findViewById(R.id.node_details_outage_service);
                    serviceTypeText.setText(serviceType);

                    container.addView(item);

                    item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityUtils.showOutageDetails(getActivity(), id);
                        }
                    });
                }
            }
        }
    }

    /**
     * {@link android.os.AsyncTask} that loads events related to current node.
     */
    private class EventsLoader extends AsyncTask<Void, Void, Cursor> {

        /**
         * @param voids No parameters.
         * @return {@link android.database.Cursor} with events.
         */
        protected Cursor doInBackground(Void... voids) {
            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
            queryBuilder.setTables(Contract.Tables.EVENTS);
            queryBuilder.appendWhere(Contract.Events.NODE_ID + "=" + nodeId);
            String[] projection = {
                    Contract.Events._ID,
                    Contract.Events.LOG_MESSAGE,
                    Contract.Events.SEVERITY
            };
            return queryBuilder.query(db, projection, null, null, null, null, null);
        }

        protected void onPostExecute(Cursor cursor) {
            LinearLayout detailsLayout = (LinearLayout) getActivity()
                    .findViewById(R.id.node_details);
            if (detailsLayout == null) {
                return;
            }
            TextView eventsPlaceholder =
                    (TextView) getActivity().findViewById(R.id.node_events_placeholder);
            if (!cursor.moveToFirst()) {
                eventsPlaceholder.setText(getString(R.string.no_events));
            } else {
                detailsLayout.removeView(eventsPlaceholder);

                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                LinearLayout container = (LinearLayout) getActivity()
                        .findViewById(R.id.node_details_events_container);

                for (boolean b = cursor.moveToFirst(); b; b = cursor.moveToNext()) {
                    View item = inflater.inflate(R.layout.node_details_event, null);

                    final int id = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.Events._ID));
                    TextView idText = (TextView) item.findViewById(R.id.node_details_event_id);
                    idText.setText("#" + id);

                    String severity = cursor.getString(
                            cursor.getColumnIndexOrThrow(Contract.Events.SEVERITY));
                    Resources res = getActivity().getResources();
                    int severityColor;
                    if (severity.equals("CLEARED")) {
                        severityColor = res.getColor(R.color.severity_cleared);
                    } else if (severity.equals("MINOR")) {
                        severityColor = res.getColor(R.color.severity_minor);
                    } else if (severity.equals("NORMAL")) {
                        severityColor = res.getColor(R.color.severity_normal);
                    } else if (severity.equals("INDETERMINATE")) {
                        severityColor = res.getColor(R.color.severity_minor);
                    } else if (severity.equals("WARNING")) {
                        severityColor = res.getColor(R.color.severity_warning);
                    } else if (severity.equals("MAJOR")) {
                        severityColor = res.getColor(R.color.severity_major);
                    } else {
                        severityColor = res.getColor(R.color.severity_critical);
                    }
                    idText.setBackgroundColor(severityColor);

                    String message = cursor
                            .getString(cursor.getColumnIndexOrThrow(Contract.Events.LOG_MESSAGE));
                    TextView messageText =
                            (TextView) item.findViewById(R.id.node_details_event_message);
                    messageText.setText(Html.fromHtml(message));

                    container.addView(item);

                    item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityUtils.showEventDetails(getActivity(), id);
                        }
                    });
                }
            }
        }
    }

}
