package org.opennms.android.ui.events;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.opennms.android.R;
import org.opennms.android.data.storage.Contract;
import org.opennms.android.ui.ActivityUtils;
import org.opennms.android.ui.DetailsFragment;

public class EventDetailsFragment extends DetailsFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 0x2;
    private long eventId;

    // Do not remove
    public EventDetailsFragment() {}

    public EventDetailsFragment(long eventId) {
        this.eventId = eventId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        return new CursorLoader(getActivity(),
                Uri.withAppendedPath(Contract.Events.CONTENT_URI,
                        String.valueOf(eventId)),
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
        } else {
            showErrorMessage();
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.details_loading, container, false);
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
        LayoutInflater inflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.event_details, detailsContainer);

        LinearLayout detailsLayout = (LinearLayout) getActivity().findViewById(R.id.event_details);

        // Event ID
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.Events._ID));
        TextView idView = (TextView) getActivity().findViewById(R.id.event_id);
        idView.setText(getString(R.string.event_details_id) + id);

        // Severity
        String severity = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Events.SEVERITY));
        TextView severityView = (TextView) getActivity().findViewById(R.id.event_severity);
        severityView.setText(String.valueOf(severity));
        LinearLayout severityRow = (LinearLayout) getActivity().findViewById(R.id.event_severity_row);
        if (severity.equals("CLEARED")) {
            severityRow.setBackgroundColor(getResources().getColor(R.color.severity_cleared));
        } else if (severity.equals("MINOR")) {
            severityRow.setBackgroundColor(getResources().getColor(R.color.severity_minor));
        } else if (severity.equals("NORMAL")) {
            severityRow.setBackgroundColor(getResources().getColor(R.color.severity_normal));
        } else if (severity.equals("INDETERMINATE")) {
            severityRow.setBackgroundColor(getResources().getColor(R.color.severity_minor));
        } else if (severity.equals("WARNING")) {
            severityRow.setBackgroundColor(getResources().getColor(R.color.severity_warning));
        } else if (severity.equals("MAJOR")) {
            severityRow.setBackgroundColor(getResources().getColor(R.color.severity_major));
        } else if (severity.equals("CRITICAL")) {
            severityRow.setBackgroundColor(getResources().getColor(R.color.severity_critical));
        }

        // Creation time
        String createTimeString =
                cursor.getString(cursor.getColumnIndexOrThrow(Contract.Events.CREATE_TIME));
        TextView timeTextView = (TextView) getActivity().findViewById(R.id.event_create_time);
        timeTextView.setText(createTimeString);

        // Log message
        String logMessage =
                cursor.getString(cursor.getColumnIndexOrThrow(Contract.Events.LOG_MESSAGE));
        TextView logMessageView = (TextView) getActivity().findViewById(R.id.event_log_message);
        logMessageView.setText(Html.fromHtml(logMessage));

        // Description
        String description =
                cursor.getString(cursor.getColumnIndexOrThrow(Contract.Events.DESCRIPTION));
        TextView descriptionView = (TextView) getActivity().findViewById(R.id.event_desc);
        descriptionView.setText(Html.fromHtml(description));

        // Host
        String host = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Events.HOST));
        TextView hostView = (TextView) getActivity().findViewById(R.id.event_host);
        if (host != null) {
            hostView.setText(host);
        } else {
            detailsLayout.removeView(hostView);
            TextView title = (TextView) getActivity().findViewById(R.id.event_host_title);
            detailsLayout.removeView(title);
        }

        // IP address
        String ipAddress = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Events.IP_ADDRESS));
        TextView ipAddressView = (TextView) getActivity().findViewById(R.id.event_ip_address);
        if (ipAddress != null) {
            ipAddressView.setText(ipAddress);
        } else {
            detailsLayout.removeView(ipAddressView);
            TextView title = (TextView) getActivity().findViewById(R.id.event_ip_address_title);
            detailsLayout.removeView(title);
        }

        // Node
        final int nodeId = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.Events.NODE_ID));
        String nodeLabel =
                cursor.getString(cursor.getColumnIndexOrThrow(Contract.Events.NODE_LABEL));
        TextView nodeView = (TextView) getActivity().findViewById(R.id.event_node);
        nodeView.setText(nodeLabel + " (#" + nodeId + ")");
        nodeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityUtils.showNodeDetails(getActivity(), nodeId);
            }
        });

        // Service type
        int serviceTypeId =
                cursor.getInt(cursor.getColumnIndexOrThrow(Contract.Events.SERVICE_TYPE_ID));
        String serviceTypeName =
                cursor.getString(cursor.getColumnIndexOrThrow(Contract.Events.SERVICE_TYPE_NAME));
        TextView serviceTypeView = (TextView) getActivity().findViewById(R.id.event_service_type);
        if (serviceTypeName != null) {
            serviceTypeView.setText(serviceTypeName + " (#" + serviceTypeId + ")");
        } else {
            detailsLayout.removeView(serviceTypeView);
            TextView title = (TextView) getActivity().findViewById(R.id.event_service_type_title);
            detailsLayout.removeView(title);
        }
    }

}
