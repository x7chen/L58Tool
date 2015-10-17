package com.momo.dev.l58tool;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class TestFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    Intent PacketHandle = new Intent(PacketParser.ACTION_PACKET_HANDLE);

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mtestItemAdapter = new TestItemAdapter(getActivity());
        mtestItemAdapter.addItem(new TestItem("设置时间","通过"));
        mtestItemAdapter.addItem(new TestItem("G-Sensor", "未通过"));


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootview = inflater.inflate(R.layout.fragment_test,container,false);
        ListView listView = (ListView)rootview.findViewById(R.id.listView_TestItem);
        listView.setAdapter(mtestItemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        PacketHandle.putExtra(PacketParser.HANDLE,1);
                        getActivity().sendBroadcast(PacketHandle);
                        break;
                    case 1:
                        break;
                    default:
                        break;
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

    private class TestItemAdapter extends BaseAdapter{
        private ArrayList<TestItem> testItems;
        private LayoutInflater mInflator;

        public TestItemAdapter(Context context){
            super();
            testItems = new ArrayList<TestItem>();
            mInflator = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            if(convertView == null){
                convertView = mInflator.inflate(R.layout.listitem_test, null);
                viewHolder = new ViewHolder();
                viewHolder.item = (TextView) convertView.findViewById(R.id.test_item);
                viewHolder.result = (TextView)convertView.findViewById(R.id.test_result);
                convertView.setTag(viewHolder);
            }
            else{
                viewHolder = (ViewHolder)convertView.getTag();
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
    private class TestItem{
        String testItem;
        String testResult;
        public TestItem(String item,String result){
            super();
            this.testItem = item;
            this.testResult = result;
        }
        public String getTestItem(){
            return this.testItem;
        }

        public void setTestItem(String testItem) {
            this.testItem = testItem;
        }

        public String getTestResult(){
            return this.testResult;
        }

        public void setTestResult(String testResult) {
            this.testResult = testResult;
        }
    }
}
