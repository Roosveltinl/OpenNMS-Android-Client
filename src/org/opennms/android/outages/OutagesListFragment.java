package org.opennms.android.outages;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import org.opennms.android.MainService;
import org.opennms.android.R;
import org.opennms.android.dao.DatabaseHelper;
import org.opennms.android.outages.dao.OutagesListProvider;

public class OutagesListFragment extends SherlockListFragment
        implements SearchView.OnQueryTextListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 2;
    MainService service;
    boolean bound = false;
    SimpleCursorAdapter adapter;
    ListView list;
    FrameLayout detailsContainer;
    boolean isDualPane = false;
    private MenuItem refreshItem;
    private String currentFilter;
    private OnOutagesListSelectedListener outagesListSelectedListener;
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MainService.LocalBinder binder = (MainService.LocalBinder) service;
            OutagesListFragment.this.service = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }

    };

    @Override
    public void onStart() {
        super.onStart();
        Intent serviceIntent = new Intent(getActivity().getApplicationContext(), MainService.class);
        getSherlockActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        outagesListSelectedListener = new OnOutagesListSelectedListener() {
            @Override
            public void onOutageSelected(Outage outage) {
                displayDetails(outage);
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.outages_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        list = (ListView) getSherlockActivity().findViewById(android.R.id.list);
        detailsContainer = (FrameLayout) getSherlockActivity().findViewById(R.id.details_fragment_container);
        isDualPane = detailsContainer != null && detailsContainer.getVisibility() == View.VISIBLE;

        adapter = new SimpleCursorAdapter(
                getSherlockActivity(),
                android.R.layout.simple_list_item_2,
                null,
                new String[]{DatabaseHelper.COL_IP_ADDRESS, DatabaseHelper.COL_SERVICE_TYPE_NAME},
                new int[]{android.R.id.text1, android.R.id.text2},
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        getListView().setAdapter(adapter);
        getActivity().getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (bound) {
            getSherlockActivity().unbindService(serviceConnection);
            bound = false;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String projection[] = {
                DatabaseHelper.COL_OUTAGE_ID,
                DatabaseHelper.COL_IP_ADDRESS,
                DatabaseHelper.COL_IF_REGAINED_SERVICE,
                DatabaseHelper.COL_SERVICE_TYPE_NAME,
                DatabaseHelper.COL_IF_LOST_SERVICE
        };
        Cursor outagesCursor = getActivity().getContentResolver().query(
                Uri.withAppendedPath(OutagesListProvider.CONTENT_URI, String.valueOf(id)),
                projection, null, null, null);
        if (outagesCursor.moveToFirst()) {
            Integer outageId = outagesCursor.getInt(0);
            String outageIpAddress = outagesCursor.getString(1);
            String outageIfRegainedService = outagesCursor.getString(2);
            String outageServiceTypeName = outagesCursor.getString(3);
            String outageIfLostService = outagesCursor.getString(4);
            Outage outage = new Outage(outageId, outageIpAddress, outageIfLostService, outageIfRegainedService, outageServiceTypeName);
            this.outagesListSelectedListener.onOutageSelected(outage);
        }
        outagesCursor.close();
    }

    private void displayDetails(Outage outage) {
        if (isDualPane) {
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            FrameLayout detailsContainer = (FrameLayout) getSherlockActivity().findViewById(R.id.details_fragment_container);
            detailsContainer.removeAllViews();
            OutageDetailsFragment detailsFragment = new OutageDetailsFragment();
            detailsFragment.bindOutage(outage);
            fragmentTransaction.add(R.id.details_fragment_container, detailsFragment);
            fragmentTransaction.commit();
        } else {
            Intent detailsIntent = new Intent(getSherlockActivity(), OutageDetailsActivity.class);
            detailsIntent.putExtra("outage", outage);
            startActivity(detailsIntent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.outages, menu);
        MenuItem searchItem = menu.add("Search");
        searchItem.setIcon(getResources().getDrawable(R.drawable.ic_action_search));
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        SearchView search = new SearchView(getActivity());
        search.setOnQueryTextListener(this);
        searchItem.setActionView(search);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh_outages:
                refreshItem = item;
                refreshList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshList() {
        if (bound) {
            LayoutInflater inflater = (LayoutInflater) getSherlockActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);
            Animation rotation = AnimationUtils.loadAnimation(getSherlockActivity(), R.anim.refresh);
            rotation.setRepeatCount(Animation.INFINITE);
            iv.startAnimation(rotation);
            refreshItem.setActionView(iv);
            service.refreshOutages();
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        if (currentFilter == null && newFilter == null) {
            return true;
        }
        if (currentFilter != null && currentFilter.equals(newFilter)) {
            return true;
        }
        currentFilter = newFilter;
        getActivity().getSupportLoaderManager().restartLoader(0, null, this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri;
        if (this.currentFilter != null) {
            baseUri = Uri.withAppendedPath(
                    Uri.withAppendedPath(OutagesListProvider.CONTENT_URI, "ipaddress"),
                    Uri.encode(this.currentFilter)
            );
        } else {
            baseUri = OutagesListProvider.CONTENT_URI;
        }
        String[] projection = {
                DatabaseHelper.TABLE_OUTAGES_ID,
                DatabaseHelper.COL_OUTAGE_ID,
                DatabaseHelper.COL_IP_ADDRESS,
                DatabaseHelper.COL_IF_REGAINED_SERVICE,
                DatabaseHelper.COL_SERVICE_TYPE_NAME,
                DatabaseHelper.COL_IF_LOST_SERVICE
        };
        return new CursorLoader(getActivity(), baseUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (refreshItem != null && refreshItem.getActionView() != null) {
            refreshItem.getActionView().clearAnimation();
            refreshItem.setActionView(null);
        }
        adapter.swapCursor(cursor);
        if (cursor.getColumnCount() > 0) {
            // TODO: Activate first item in list
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    public interface OnOutagesListSelectedListener {
        void onOutageSelected(Outage outage);
    }

}