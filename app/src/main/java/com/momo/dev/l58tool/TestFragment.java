package com.momo.dev.l58tool;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    Intent PacketHandle = new Intent(PacketParserService.ACTION_PACKET_HANDLE);
    public PacketParserService packetParserService;
    private TestItemAdapter mtestItemAdapter;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TestFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TestFragment newInstance(String param1, String param2) {
        TestFragment fragment = new TestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public TestFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mtestItemAdapter = new TestItemAdapter(getActivity());
        mtestItemAdapter.addItem(new TestItem("设置时间", ""));
        mtestItemAdapter.addItem(new TestItem("获取运动数据", ""));
        mtestItemAdapter.addItem(new TestItem("设置闹钟", ""));
        mtestItemAdapter.addItem(new TestItem("获取闹钟", ""));
        mtestItemAdapter.addItem(new TestItem("设置目标", ""));
        mtestItemAdapter.addItem(new TestItem("设置用户信息", ""));
        mtestItemAdapter.addItem(new TestItem("设置防丢", ""));
        mtestItemAdapter.addItem(new TestItem("设置久坐", ""));
        mtestItemAdapter.addItem(new TestItem("获取当日数据", ""));
        mtestItemAdapter.addItem(new TestItem("设置左右手", ""));
        mtestItemAdapter.addItem(new TestItem("设置时间格式", ""));
        mtestItemAdapter.addItem(new TestItem("模拟数据", ""));
        mtestItemAdapter.addItem(new TestItem("打开实时数据", ""));
        mtestItemAdapter.addItem(new TestItem("发送汉字", ""));
    }

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
            StringBuilder stringBuilder;
            Log.i(BluetoothLeService.TAG, "onDataReceived.");
            switch (category) {
                case PacketParserService.RECEIVED_ALARM:
                    List<PacketParserService.Alarm> alarmList;
                    if (packetParserService != null) {
                        alarmList = packetParserService.getAlarmsList();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("[AlarmList]\n");
                        for (PacketParserService.Alarm alarm : alarmList) {
                            stringBuilder.append("Hour:" + alarm.Hour + "  Minute:" + alarm.Minute + "  Repeat:" + alarm.Repeat + "\n");
                        }
                        stringBuilder.append("\n\n");
                        PacketParserService.writeLog(stringBuilder.toString());
                    }
                    break;
                case PacketParserService.RECEIVED_DAILY_DATA:
                    PacketParserService.DailyData dailyData;
                    dailyData = packetParserService.getDailyDataList();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("[DailyData]\n");
                    stringBuilder.append("Steps:" + dailyData.Steps + "  Distance:" + dailyData.Distance + "  Calories:" + dailyData.Calory);
                    stringBuilder.append("\n\n");
                    PacketParserService.writeLog(stringBuilder.toString());
                    break;
                case PacketParserService.RECEIVED_SPORT_DATA:
                    List<PacketParserService.SportData> sportDataList;
                    List<PacketParserService.SleepData> sleepDataList;

                    if (packetParserService != null) {
                        sportDataList = packetParserService.getSportDataList();
                        stringBuilder = new StringBuilder();
                        if (sportDataList.size() != 0) {
                            stringBuilder.append("[SportData]\n");
                            for (PacketParserService.SportData sportData : sportDataList) {
                                stringBuilder.append(sportData.Year + "." + sportData.Month + "." + sportData.Day + "  ");
                                stringBuilder.append(sportData.Hour + ":" + sportData.Minute + "\n");
                                stringBuilder.append("Steps:" + sportData.Steps + "  Distance:" + sportData.Distance + "  Calories:" + sportData.Calory + "\n");
                            }
                            sportDataList.clear();
                            stringBuilder.append("\n\n");
                            PacketParserService.writeLog(stringBuilder.toString());
                        }
                        sleepDataList = packetParserService.getSleepDataList();
                        stringBuilder = new StringBuilder();
                        if (sleepDataList.size() != 0) {
                            stringBuilder.append("[SleepData]\n");
                            for (PacketParserService.SleepData sleepData : sleepDataList) {
                                stringBuilder.append(sleepData.Year + "." + sleepData.Month + "." + sleepData.Day + "  ");
                                stringBuilder.append(sleepData.Hour + ":" + sleepData.Minute + "  ");
                                stringBuilder.append("Mode:" + sleepData.Mode + "\n");
                            }
                            sleepDataList.clear();
                            stringBuilder.append("\n\n");
                            PacketParserService.writeLog(stringBuilder.toString());
                        }
                    }
                    break;
                default:
                    break;
            }

        }

        @Override
        public void onCharacteristicNotFound() {
            Log.i(BluetoothLeService.TAG, "onCharacteristicNotFound.");
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootview = inflater.inflate(R.layout.fragment_test, container, false);
        ListView listView = (ListView) rootview.findViewById(R.id.listView_TestItem);
        listView.setAdapter(mtestItemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Random random = new Random();
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
                if(packetParserService.isIdle()) {
                    switch (position) {
                        case 0:
                            packetParserService.setTime();
                            break;
                        case 1:
                            packetParserService.getSportData();
                            break;
                        case 2:
                            ArrayList<PacketParserService.Alarm> alarmList = new ArrayList<PacketParserService.Alarm>();
                            for (int i = 0; i < 8; i++) {
                                PacketParserService.Alarm alarm = new PacketParserService.Alarm();
                                alarm.Year = 2015;
                                alarm.Month = 11;
                                alarm.Day = 21;
                                alarm.Hour = 15;
                                alarm.Minute = 0;
                                alarm.Repeat = 0x7F;
                                alarm.ID = i;
                                alarmList.add(alarm);
                            }
                            packetParserService.setAlarmList(alarmList);
                            break;
                        case 3:
                            packetParserService.getAlarms();
                            break;
                        case 4:
                            packetParserService.setTarget(1000);
                            break;
                        case 5:
                            PacketParserService.UserProfile userProfile = new PacketParserService.UserProfile();
                            userProfile.Sex = 1;
                            userProfile.Age = 30;
                            userProfile.Stature = 170;
                            userProfile.Weight = 50;
                            packetParserService.setUserProfile(userProfile);
                            break;
                        case 6:
                            packetParserService.setLossAlert(1);
                            break;
                        case 7:
                            PacketParserService.LongSitSetting longSitSetting = new PacketParserService.LongSitSetting();
                            longSitSetting.Enable = 1;
                            longSitSetting.ThresholdSteps = 100;
                            longSitSetting.DurationTimeMinutes = 30;
                            longSitSetting.StartTimeHour = 8;
                            longSitSetting.EndTimeHour = 23;
                            longSitSetting.Repeat = 0x7F;
                            packetParserService.setLongSit(longSitSetting);
                            break;
                        case 8:
                            packetParserService.getDailyData();
                            break;
                        case 9:
                            packetParserService.setWearHand(1);
                            break;
                        case 10:
                            packetParserService.setHourFormat(Math.abs(random.nextInt()) % 2);
                            break;
                        case 11:
                            packetParserService.mock();
                            break;
                        case 12:
                            packetParserService.setSportNotify(1);
                            break;
                        case 13:
                            try {
                                packetParserService.telNotify("手机发送汉字");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        default:
//                        PacketHandle.putExtra(PacketParserService.HANDLE,position+1);
//                        getActivity().sendBroadcast(PacketHandle);
                            break;
                    }
                }
            }
        });
        return rootview;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private class TestItemAdapter extends BaseAdapter {
        private ArrayList<TestItem> testItems;
        private LayoutInflater mInflator;

        public TestItemAdapter(Context context) {
            super();
            testItems = new ArrayList<TestItem>();
            mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void addItem(TestItem testItem) {
            testItems.add(testItem);
        }

        @Override
        public Object getItem(int position) {
            return testItems.get(position);
        }

        @Override
        public int getCount() {
            return testItems.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = mInflator.inflate(R.layout.listitem_test, null);
                viewHolder = new ViewHolder();
                viewHolder.item = (TextView) convertView.findViewById(R.id.test_item);
                viewHolder.result = (TextView) convertView.findViewById(R.id.test_result);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            final TestItem testItem = testItems.get(position);
            viewHolder.item.setText(testItem.getTestItem());
            viewHolder.result.setText(testItem.getTestResult());

            return convertView;
        }
    }

    static class ViewHolder {
        TextView item;
        TextView result;
    }

    private class TestItem {
        String testItem;
        String testResult;

        public TestItem(String item, String result) {
            super();
            this.testItem = item;
            this.testResult = result;
        }

        public String getTestItem() {
            return this.testItem;
        }

        public void setTestItem(String testItem) {
            this.testItem = testItem;
        }

        public String getTestResult() {
            return this.testResult;
        }

        public void setTestResult(String testResult) {
            this.testResult = testResult;
        }
    }
}
