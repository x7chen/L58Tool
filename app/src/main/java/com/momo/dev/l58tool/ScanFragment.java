package com.momo.dev.l58tool;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;


public class ScanFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static boolean BLE_CONNECT_STATUS = false;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public PacketParserService packetParserService;

    Intent GattCommand = new Intent(BluetoothLeService.ACTION_GATT_HANDLE);

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;

    PacketParserService.CallBack callBack = new PacketParserService.CallBack() {
        @Override
        public void onSendSuccess() {
            Log.i(BluetoothLeService.TAG, "Command Send Success!");
        }

        @Override
        public void onSendFailure() {

        }

        @Override
        public void onTimeOut() {

        }

        @Override
        public void onConnectStatusChanged(boolean status) {
            Log.i(BluetoothLeService.TAG, "Connect Status Changed:" + status);
        }

        @Override
        public void onDataReceived(byte category) {

        }

        @Override
        public void onCharacteristicNotFound() {
            Log.i(BluetoothLeService.TAG, "onCharacteristicNotFound.");
        }
    };

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ScanFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ScanFragment newInstance(String param1, String param2) {
        ScanFragment fragment = new ScanFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public ScanFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootview = inflater.inflate(R.layout.fragment_scan, container, false);
        ListView listView = (ListView) rootview.findViewById(R.id.listView_Device);
        mLeDeviceListAdapter = new LeDeviceListAdapter(getActivity());
        listView.setAdapter(mLeDeviceListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                if (packetParserService == null) {
                    packetParserService = ((MainActivity) getActivity()).getPacketParserService();
                    if (packetParserService == null) {
                        Toast.makeText(getActivity(), "getPacketParserService is null.", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        Toast.makeText(getActivity(), "getPacketParserService is ready.", Toast.LENGTH_SHORT).show();
                        packetParserService.registerCallback(callBack);
                    }
                }
                if (ScanFragment.BLE_CONNECT_STATUS == false) {
                    packetParserService.connect(device.getAddress());
                } else {
                    packetParserService.disconnect();
                }
            }
        });
        Button bt_scan = (Button) (rootview.findViewById(R.id.button_scan));
        bt_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ScanFragment.BLE_CONNECT_STATUS == false) {
                    mBluetoothAdapter = MainActivity.mBluetoothAdapter;
                    mHandler = new Handler();
                    // Initializes list view adapter.
                    scanLeDevice(true);
                }

            }
        });
        return rootview;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        //invalidateOptionsMenu();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };


}
