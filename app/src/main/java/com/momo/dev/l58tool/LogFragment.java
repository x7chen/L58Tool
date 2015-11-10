package com.momo.dev.l58tool;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LogFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LogFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LogFragment newInstance(String param1, String param2) {
        LogFragment fragment = new LogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public LogFragment() {
        // Required empty public constructor
    }

    static String ReadLog() {
        String logFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/L58Tool/Log.txt";
        File file = new File(logFileName);
        if (!file.exists()) {
            return null;
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder("");
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        tv.setText((ReadLog()));
//        InputStream inputStream = getResources().openRawResource(R.raw.res_hzk16);
//
//        StringBuilder stringBuilder = new StringBuilder();
//        String string = "中文";
//        for (int m = 0; m < string.length(); m++) {
//            String hz = string.substring(m, m + 1);
//            try {
//                byte[] tran = hzk16.getMatrix(inputStream, hz);
//                int i, j, k;
//                for (i = 0; i < 16; i++) {          /* 点阵行数索引 */
//                    for (j = 1; j >= 0; j--) {       /* 点阵左子行右子行索引 */
//                        for (k = 0; k < 8; k++) {   /* 点阵每个点 */
//
//                            // 判断是0、还是1
//                            if (((tran[i * 2 + j] >> (k)) & 0x1) == 1) {
//                                stringBuilder.append("  .");
//                            } else {
//                                stringBuilder.append("   ");
//                            }
//                        }
//                    }
//                    stringBuilder.append("\n");
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        tv.setText(stringBuilder.toString());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    View rootview;
    TextView tv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_log, container, false);
        tv = (TextView) (rootview.findViewById(R.id.log_tv));
        // Inflate the layout for this fragment
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
}
