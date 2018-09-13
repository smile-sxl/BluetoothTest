package com.smile.bluetoothtest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * author: smile .
 * date: On 2018/9/10
 */
public class DeviceConnectActivity extends AppCompatActivity {

    private static ActionBar actionbar;
    private BluetoothChatService mBluetoothService = MainActivity.mBluetoothService;
    private static RecyclerView rvMsg;
    private static MsgAdapter msgAdapter;
    private EditText etInputMsg;
    private Button btnSend;
    private static List<Msg> msgLists = new ArrayList<Msg>();

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    @SuppressLint("HandlerLeak")
    public static final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf);
                    Msg msg1 = new Msg(readMessage, Msg.TYPE_RECEIVER);
                    msgLists.add(msg1);
                    msgAdapter.notifyItemInserted(msgLists.size() - 1);
                    // RecyclerView     定位到最后一行
                    rvMsg.scrollToPosition(msgLists.size() - 1);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        actionbar = getSupportActionBar();
        String name = getIntent().getStringExtra("name");
        actionbar.setTitle(name);
        msgLists.clear();
        rvMsg = (RecyclerView) findViewById(R.id.rvMsg);
        etInputMsg = (EditText) findViewById(R.id.etInputMsg);
        btnSend = (Button) findViewById(R.id.btnSend);
        rvMsg.setLayoutManager(new LinearLayoutManager(this));
        msgAdapter = new MsgAdapter();
        rvMsg.setAdapter(msgAdapter);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = etInputMsg.getText().toString();
                if (!"".equals(content)) {
                    Msg msg = new Msg(content, Msg.TYPE_SEND);
                    msgLists.add(msg);
                    msgAdapter.notifyItemInserted(msgLists.size() - 1);
                    // RecyclerView     定位到最后一行
                    rvMsg.scrollToPosition(msgLists.size() - 1);
                    etInputMsg.setText("");
                    sendMessage(content);
                }
            }
        });
    }

    class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(DeviceConnectActivity.this).inflate(R.layout.item_msg, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Msg msg = msgLists.get(position);
            if (msg.getType() == Msg.TYPE_RECEIVER) {
                holder.llLeftMsg.setVisibility(View.VISIBLE);
                holder.llRightMsg.setVisibility(View.GONE);
                holder.tvLeftMsg.setText(msg.getContent());
            } else if (msg.getType() == Msg.TYPE_SEND) {
                holder.llLeftMsg.setVisibility(View.GONE);
                holder.llRightMsg.setVisibility(View.VISIBLE);
                holder.tvRightMsg.setText(msg.getContent());
            }
        }

        @Override
        public int getItemCount() {
            return msgLists.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout llLeftMsg;
            TextView tvLeftMsg;
            LinearLayout llRightMsg;
            TextView tvRightMsg;

            public ViewHolder(View itemView) {
                super(itemView);
                llLeftMsg = (LinearLayout) itemView.findViewById(R.id.llLeftMsg);
                tvLeftMsg = (TextView) itemView.findViewById(R.id.tvLeftMsg);
                llRightMsg = (LinearLayout) itemView.findViewById(R.id.llRightMsg);
                tvRightMsg = (TextView) itemView.findViewById(R.id.tvRightMsg);
            }
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "您未连接到设备", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBluetoothService.write(send);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }
}
