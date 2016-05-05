package jp.ac.titech.itpro.sdl.btscanner;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private ArrayList<BluetoothDevice> devList = null;
    private ArrayAdapter<BluetoothDevice> devListAdapter;
    private final static String KEY_DEVLIST = "MainActivity.devList";

    private BluetoothAdapter btAdapter;
    private BroadcastReceiver btScanReceiver;

    private final static int REQCODE_ENABLE_BT = 1111;
    private final static int REQCODE_PERMISSIONS = 2222;
    private final static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null)
            devList = savedInstanceState.getParcelableArrayList(KEY_DEVLIST);
        if (devList == null)
            devList = new ArrayList<>();

        devListAdapter = new ArrayAdapter<BluetoothDevice>(this, 0, devList) {
            @Override
            public View getView(int pos, View view, ViewGroup parent) {
                if (view == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                BluetoothDevice device = getItem(pos);
                TextView nameView = (TextView) view.findViewById(android.R.id.text1);
                TextView addrView = (TextView) view.findViewById(android.R.id.text2);
                nameView.setText(getString(R.string.format_dev_name, device.getName(),
                        device.getBondState() == BluetoothDevice.BOND_BONDED ? "*" : " "));
                addrView.setText(device.getAddress());
                return view;
            }
        };
        ListView devListView = (ListView) findViewById(R.id.dev_list);
        assert devListView != null;
        devListView.setAdapter(devListAdapter);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, R.string.toast_bt_is_not_available, Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        setupBT();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        stopBT();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        outState.putParcelableArrayList(KEY_DEVLIST, devList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
        case R.id.menu_scan:
            devListAdapter.clear();
            if (btAdapter.isDiscovering())
                btAdapter.cancelDiscovery();
            btAdapter.startDiscovery();
            return true;
        case R.id.menu_stop:
            btAdapter.cancelDiscovery();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        switch (reqCode) {
        case REQCODE_ENABLE_BT:
            if (resCode == Activity.RESULT_OK)
                setupBT1();
            else {
                Toast.makeText(this, R.string.toast_bt_must_be_enabled, Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        }
        super.onActivityResult(reqCode, resCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int reqCode,
                                           @NonNull String[] permissions, @NonNull int[] grants) {
        Log.d(TAG, "onRequestPermissionsResult: " + permissions.length + ", " + grants.length);
        switch (reqCode) {
        case REQCODE_PERMISSIONS:
            for (int i = 0; i < grants.length; i++) {
                if (grants[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                            getString(R.string.error_permission_denied, permissions[i]),
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            setupBT2();
            break;
        }
    }

    private void setupBT() {
        Log.d(TAG, "setupBT");
        if (!btAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQCODE_ENABLE_BT);
            return;
        }
        setupBT1();
    }

    private void setupBT1() {
        Log.d(TAG, "setupBT1");
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQCODE_PERMISSIONS);
                return;
            }
        }
        setupBT2();
    }

    private void setupBT2() {
        Log.d(TAG, "setupBT2");
        if (btScanReceiver == null) {
            Log.d(TAG, "setupBT2: new btScanReceiver");
            btScanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Log.d(TAG, "onReceive: " + action);
                    switch (intent.getAction()) {
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device =
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        devListAdapter.add(device);
                        devListAdapter.notifyDataSetChanged();
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        break;
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(btScanReceiver, filter);
        }
    }

    private void stopBT() {
        Log.d(TAG, "stopBT");
        if (btAdapter != null && btAdapter.isDiscovering()) {
            Log.d(TAG, "stopBT: btAdapter.cancelDiscovery()");
            btAdapter.cancelDiscovery();
        }
        if (btScanReceiver != null) {
            Log.d(TAG, "stopBT: unregister btScanReceiver");
            unregisterReceiver(btScanReceiver);
            btScanReceiver = null;
        }
    }

}
