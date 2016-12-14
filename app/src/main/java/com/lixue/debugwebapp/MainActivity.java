package com.lixue.debugwebapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
/**
 * @author lh
 * @version 1.0.0
 * @filename MainActivity
 * @description --------------------------------------------------------
 * @date 2016/12/12 12:00
 */
public class MainActivity extends AppCompatActivity {

    public static final int CONNECT = 99;
    public static final int SUCCESS = 100;
    public static final int ERROR = 101;

    public static final String KEY_CONNECT = "connect";
    public static final String KEY_SUCCESS = "success";
    public static final String KEY_ERROR = "error";

    public static final String ADDRESS = "192.168.191.1";
    public static final int PORT = 9999;
    private TextView tv;
    private WebView webView;
    private SocketThread socketThread = null;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String str = null;
            switch (msg.what) {
                case CONNECT:
                    str = msg.getData().getString(KEY_CONNECT);
                    tv.append("\n" + str);
                    scrollToBottom();
                    break;
                case SUCCESS:
                    str = msg.getData().getString(KEY_SUCCESS);
                    tv.append("\n" + str);
                    scrollToBottom();
                    if(isUrl(str)) {
                        webView.loadUrl(str);
                    }else{
                        tv.append("\t\t\t不合法的url");
                        scrollToBottom();
                    }
                    break;
                case ERROR:
                    str = msg.getData().getString(KEY_ERROR);
                    tv.append("\n" + str);
                    scrollToBottom();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        connect();
    }

    @Override
    protected void onDestroy() {
        close();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(webView.canGoBack()){
            webView.goBack();
        }else{
            finish();
        }
        return true;
    }

    private void init() {
        tv = (TextView) this.findViewById(R.id.tv_url);
        tv.setMovementMethod(ScrollingMovementMethod.getInstance());
        webView = (WebView) this.findViewById(R.id.wv);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                webView.loadUrl(url);
                return true;
            }
        });
    }

    private void connect() {
        if (socketThread == null) {
            socketThread = new SocketThread(ADDRESS, PORT, handler);
            socketThread.start();
            tv.append("发起建立连接请求");
            tv.append("\n" + ADDRESS + ":" + PORT);
            scrollToBottom();
        }
    }

    private void close() {
        if (socketThread != null) {
            socketThread.closeThread();
            socketThread = null;
        }
    }


    /**
     * TV滚动到底部
     */
    private void scrollToBottom() {
        int offset = tv.getLineCount() * tv.getLineHeight();
        if (offset > tv.getHeight()) {
            tv.scrollTo(0, offset - tv.getHeight());
        }
    }

    private class SocketThread extends Thread {
        private String address;//ip
        private int port;//port >1023

        private Handler handler;
        private DataInputStream in;
        private Socket socket;
        private boolean flag = false;

        public SocketThread(String address, int port, Handler handler) {
            this.address = address;
            this.port = port;
            this.handler = handler;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(address, port);
                in = new DataInputStream(socket.getInputStream());
                String content = in.readUTF();
                //连接成功发送消息
                sendMessage(KEY_CONNECT, content, CONNECT);
                while (!flag) {
                    content = in.readUTF();
                    //数据传输过程发送消息
                    sendMessage(KEY_SUCCESS, content, SUCCESS);
                }
            } catch (IOException e) {
                e.printStackTrace();
                //发送异常消息
                sendMessage(KEY_ERROR, e.getMessage(), ERROR);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    //发送异常消息
                    sendMessage(KEY_ERROR, e.getMessage(), ERROR);
                }
            }
        }

        public void sendMessage(String key, String value, int code) {
            Bundle bundle = new Bundle();
            bundle.putString(key, value);
            Message message = handler.obtainMessage();
            message.what = code;
            message.setData(bundle);
            handler.sendMessage(message);

        }

        public void closeThread() {
            flag = true;
        }
    }

    /**
     * 简单的判断一下是否是网址，严谨性可以自行完善
     * @param str
     * @return
     */
    public static boolean isUrl(String str) {
        boolean isUrl = false;
        if (str != null) {
            if (str.startsWith("http://") || str.startsWith("https://") || str.startsWith("ftp://")) {
                isUrl = true;
            }
        }
        return isUrl;
    }

}
