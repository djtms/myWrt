package com.liuyh.wrt.mywrt;
// packages for SSH


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.Fragment.SavedState;
//import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.SettingInjectorService;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.test.PerformanceTestCase;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
//import android.R;


//Below is a wrokaround that allow UI executes some time-consuming operation, such as connect to SSH server.
//@SuppressLint("NewApi")
public class MainActivity extends ActionBarActivity implements View.OnClickListener{
	
	public final static int OK = 0;
	public final static int FAIL = -1;
	public final static int CONN_SUCCESS = 1;
	public final static int CONN_FAILURE = 2;
	public final static int DISCONN_SUCCESS = 3;
	public final static int DISCONN_FAILURE = 4;
	public final static int READ_BUF_SIZE = 10240;
	public final static int REQ_ID_SETUP = 1;
	public final static String PERFERENCE_NAME = "user_info";
	//Global handle for SSH connection
	private static Connection sshConnection = null;
	private static String hostname;
	private static String username;
	private static String password;
	private static String port;
	
	Context mContext;
	//Thread that do the real networking operation
	private Thread mThread;
	private Thread mThread2;
	static void SSHDisConnectHost() {
		sshConnection.close();
		sshConnection = null;
	}
	
	static boolean connected() {
		return sshConnection != null;
	}
	
	static int SSHConnectHost() {
		if (hostname == null) {
			System.out.println("no host name provided.");
			return FAIL;
		}
		if(username == null || password  == null) { 
			System.out.println("username or password provided.");
			return FAIL;
		}
		// if current SSH connection is still connected, it need to be disconnected
		if (sshConnection != null)
			sshConnection.close();
		
		try
		{			
			/* Create a connection instance */
			sshConnection = new Connection(hostname);
			if (sshConnection == null)
				throw new IOException("Fatle Error: fail to create new Connection instance.");
			/* Now connect */
			sshConnection.connect();
			/* Authenticate.
			 * If you get an IOException saying something like
			 * "Authentication method password not supported by the server at this stage."
			 * then please check the FAQ.
			 */
			boolean isAuthenticated = sshConnection.authenticateWithPassword(username, password);
			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");

		}
		catch (IOException e)
		{
			e.printStackTrace(System.err);
			System.exit(2);
		}
		
		return OK;
	}
	
	@SuppressWarnings("resource")
	static String SSHExecuteCmd(String cmd) throws IOException {
		if (sshConnection == null) {
			System.out.println("Input not complete");
			return null;
		}
		if (cmd  == null) {
			System.out.println("Input not complete");
			return null;
		}
		/* Create a session */
		Session sshSession = sshConnection.openSession();
		if (sshSession == null)
			throw new IOException("Fatle Error: fail to create new Connection instance.");		
		sshSession.execCommand(cmd);
		
		System.out.println("Here is some information about the remote host:");
		/* 
		 * This basic example does not handle stderr, which is sometimes dangerous
		 * (please read the FAQ).
		 */
		InputStream stdout = new StreamGobbler(sshSession.getStdout());
		BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

		char[] buf = new char[READ_BUF_SIZE];
		int len = br.read(buf, 0, buf.length);
		if (len < 1) {
			System.out.println("!!!!! br.read returns null");
			return null;
		}
		stdout.close();
		br.close();
		sshSession.close();
		return new String(buf);
	}
	
	
	//below code is from the demo code, leave it here for reference.
	static String SSH2ExecuteCmd(String hostname, String username, String password, String cmd)
	{
		String str = null;
		if (hostname == null || username == null || password  == null || cmd  == null) {
			System.out.println("Input not complete");
		}
		try
		{
			/* Create a connection instance */
			sshConnection = new Connection(hostname);

			/* Now connect */
			sshConnection.connect();
			/* Authenticate.
			 * If you get an IOException saying something like
			 * "Authentication method password not supported by the server at this stage."
			 * then please check the FAQ.
			 */
			boolean isAuthenticated = sshConnection.authenticateWithPassword(username, password);
			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");
			/* Create a session */
			Session sess = sshConnection.openSession();
			if (cmd == null)
				sess.execCommand("uname -a && date && uptime && who"); //only for demo
			else
				sess.execCommand(cmd);
			System.out.println("Here is some information about the remote host:");
			/* 
			 * This basic example does not handle stderr, which is sometimes dangerous
			 * (please read the FAQ).
			 */
			InputStream stdout = new StreamGobbler(sess.getStdout());
			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
	
			char[] buf = new char[10000];
			int len = br.read(buf, 0, buf.length);
			if (len < 1) {
				System.out.println("!!!!! br.read returns null");
				System.out.println("length of buffer is " + len);
				System.out.println(len + ":  " + str);
			}
			str = new String(buf);
			//System.out.println(str);
			/* Show exit status, if available (otherwise "null") */
			System.out.println("ExitCode: " + sess.getExitStatus());
			/* Close this session */
			sess.close();
			/* Close the connection */
			sshConnection.close();		
			stdout.close();
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace(System.err);
			System.exit(2);
		}

		return str;
	}	
	private ToggleButton tbOnOffControll;
	private Button btExecuteCmd;
	private EditText etCmd;
	private TextView tvExecuteResult;
	@Override
    protected void onCreate(Bundle savedInstanceState) {
    	StrictMode.ThreadPolicy policy=new StrictMode.ThreadPolicy.Builder().permitAll().build();
    	StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);    
        
        SharedPreferences myData = getSharedPreferences(PERFERENCE_NAME, Activity.MODE_PRIVATE);
        hostname = myData.getString("hostname", "192.168.1.1");
        username = myData.getString("username", "root");
        password = myData.getString("password", "root");
        port = myData.getString("port", "22");
        
        //ToggleButton which connect/disconnect gateway
        tbOnOffControll = (ToggleButton)findViewById(R.id.toggleButtonOnOff);
        //Button which execute the command
    	btExecuteCmd = (Button) findViewById(R.id.Execute);
    	//EditTexit which accept command line to be executed
    	etCmd = (EditText) findViewById(R.id.Command);
    	//String which stores and displays result from SSH client
    	tvExecuteResult = (TextView) findViewById(R.id.ExecuteResult);

    	//Content used by Intent, which will open setup dialog
    	mContext = this;

    	btExecuteCmd.setOnClickListener(this);
    	tbOnOffControll.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				if (isChecked) {
					//Connect to SSH server
					tbOnOffControll.setText("Connecting...");
					tbOnOffControll.setTextColor(Color.MAGENTA);
					
					new Thread(runnable_connect).start();
										
				}
				else {
					tbOnOffControll.setText("Disconnecting...");
					tbOnOffControll.setTextColor(Color.MAGENTA);
					new Thread(runnable_disconnect).start();
				}
			}
			});

    }
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case CONN_SUCCESS:
				//Update button text color
				btExecuteCmd.setEnabled(true);
				tbOnOffControll.setText("Connected");
				tbOnOffControll.setTextColor(Color.GREEN);
				Toast.makeText(getApplicationContext(), "Connection established", Toast.LENGTH_LONG).show();
				break;
			case DISCONN_SUCCESS:
				btExecuteCmd.setEnabled(false);
				tbOnOffControll.setText("Disonnected");
				tbOnOffControll.setTextColor(Color.RED);
				Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_LONG).show();
				break;
			default:
				Toast.makeText(getApplicationContext(), "Disconnection done", Toast.LENGTH_LONG).show();
			}
		};
	};
	Runnable runnable_connect = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				SSHConnectHost();
			} catch (Exception e){
				mHandler.obtainMessage(CONN_FAILURE).sendToTarget();
				//e.printStackTrace(System.err);
			}
			mHandler.obtainMessage(CONN_SUCCESS).sendToTarget();
		}
	};
	
	Runnable runnable_disconnect = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				SSHDisConnectHost();
			} catch (Exception e){
				mHandler.obtainMessage(DISCONN_FAILURE).sendToTarget();
				//e.printStackTrace(System.err);
			}
			mHandler.obtainMessage(DISCONN_SUCCESS).sendToTarget();
		}
	};
	private void save_data() {
		SharedPreferences myData = getSharedPreferences(PERFERENCE_NAME, Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = myData.edit();
		editor.putString("hostname", hostname);
		editor.putString("username", username);
		editor.putString("password", password);
		editor.putString("port", port);
		editor.commit();
	}
	protected void onStop() {
		save_data();
		super.onStop();
	};
	
	
	//Disconnect the SSH session gracefully to avoid junk on SSH server.
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		
		if (sshConnection != null) {
			sshConnection.close();
			sshConnection = null;
		}
		super.onDestroy();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
			Intent intent = new Intent(mContext, setupDialog.class);
			int requestCode = REQ_ID_SETUP;
			startActivityForResult(intent, requestCode);
        	
            return true;
        }
        //Exit trigger by option menu
        if (id == R.id.action_exit)  {
        	save_data();
        	finish();
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.Execute:
			String cmd = (String)etCmd.getText().toString();
			String result = null;
			try {
				result = SSHExecuteCmd(cmd);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tvExecuteResult.setText(result);
			
			break;

		default:
			break;
		}			
	}
	/*
	 * 
	 * receive setup configuration for diaglog setup
	 * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQ_ID_SETUP && resultCode == 1) {
			String gatewayIP = data.getStringExtra("gatewayIP");
			String user = data.getStringExtra("username");
			String passwd = data.getStringExtra("password");
			String portnum = data.getStringExtra("port");
			
			tvExecuteResult.setText("gatewayIP: " + gatewayIP + "\n" + "username: " + username + "\n" + "password: " + password + "\n" + "port: " + port);
			hostname = gatewayIP;
			username = user;
			password = passwd;
			port = portnum;
		}
		
	}
}
