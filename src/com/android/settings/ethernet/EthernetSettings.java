/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
//added for ethernet settings
package com.android.settings.ethernet;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.SystemProperties;

import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.os.Bundle;
import android.os.Handler;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceCategory;
import android.provider.Settings;
import android.util.Log;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.SettingsActivity;

import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.InterfaceConfiguration;
import android.net.NetworkUtils;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkRequest;
import android.net.EthernetManager;
import android.net.StaticIpConfiguration;
import android.net.NetworkInfo.State;
import android.net.RouteInfo;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import java.util.Iterator;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import android.support.v4.text.BidiFormatter;
import java.util.StringJoiner;
import android.os.Looper;

public class EthernetSettings extends SettingsPreferenceFragment implements
    Preference.OnPreferenceChangeListener, OnPreferenceClickListener,SwitchBar.OnSwitchChangeListener{
    private static final String TAG = "EthernetSettings";
    private Context mContext;
    private EthernetManager mEthernetManager;
    private ConnectivityManager mConnManager;
    private IpConfiguration mEthernetInfo;
    //private EthernetEnabler mEthernetEnabler;
    private SwitchBar mSwitchBar;
    private static final String KEY_CONNECTION_MODE = "connection_mode";
    private static final String KEY_DEFAULT_CONNECTION = "default_connection";
    private static final String KEY_CONNECTION_CONFIG = "connection_config";
    private static final String KEY_DHCP = "dhcp_connection";
    private static final String KEY_STATIC_IP = "manual_connection";
    private ListPreference mDefaultConnectionPref;
    private Preference mDhcpPref;
    private Preference mStaticIpPref;
    private PreferenceCategory mConnectionModePref;
    private PreferenceCategory mConnectionConfigPref;
    private static final int DHCP_MODE = 0;
    private static final int STATIC_IP_MODE = 1;
    private static final String SEPRATOR = "---";
    private static final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private Resources mResources;
    // static ip configuration
    private EditText mIpaddr;
    private EditText mDns1;
    private EditText mDns2;
    private EditText mGw;
    private EditText mMask;
    // ethernet is not available view
    TextView mEmptyView;
    private static IpConfiguration config;
	public boolean flag = false;
    //ethernet state
    private static final int ETHERNET_EVENT_CONNECTTING = 0;
    private static final int ETHERNET_EVENT_CONNECTED = 1;
    private static final int ETHERNET_EVENT_SUSPENDED = 2;
    private static final int ETHERNET_EVENT_DISCONNECTED = 3;
    private static final int ETHERNET_EVENT_UNENABLE= 4;
    private static final int ETHERNET_EVENT_UNKNOWN= 5;
    private static State mEthState = NetworkInfo.State.UNKNOWN;
    private Dialog mDialog;
    private static final int DIALOG_DHCP_CONFIGURATION = 0;
    private static final int DIALOG_STATIC_IP_CONFIGURATION = 1;
    private List<String> mConnectionModeList = new ArrayList<String>();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                // There is connectivity
                //if (mEthernetManager.getEthernetEnabledState()){
                    final NetworkInfo netInfo = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
                    if (netInfo != null) {
                        State newState =netInfo.getState();
                        mEthState = newState;
                        Log.i(TAG, "[onReceive] New State: " + newState);
                        int intState = handleStateEmun(newState);
                        Log.i(TAG, "[onReceive] New State: " + intState);
                        handleEthernetStateChanged(intState);
                    } 
                //}else{
        	    //    handleEthernetStateChanged(ETHERNET_EVENT_UNENABLE);
    		    //}
            }
        }
    };
    private int handleStateEmun(State state){
        int tempState = ETHERNET_EVENT_UNKNOWN;
        if (NetworkInfo.State.CONNECTED == state){
            //show ip information
            handleEthernetStateChanged(ETHERNET_EVENT_CONNECTTING);
            tempState = ETHERNET_EVENT_CONNECTED;
        }else if(NetworkInfo.State.DISCONNECTED == state){
            //show ip information
            handleEthernetStateChanged(ETHERNET_EVENT_CONNECTTING);
            tempState = ETHERNET_EVENT_DISCONNECTED;
        }else if(NetworkInfo.State.CONNECTING == state){
            tempState = ETHERNET_EVENT_CONNECTTING;
        }else if (NetworkInfo.State.SUSPENDED == state){
            tempState = ETHERNET_EVENT_SUSPENDED;
        }
        return tempState;
    }
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Log.d(TAG, "onSwitchChanged: " + isChecked);
        if(!isChecked){
            showEmptyView();
        }else{
            //add Preference
            handleEthernetStateChanged(ETHERNET_EVENT_CONNECTTING);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = getActivity();
        mEthernetManager = (EthernetManager) mContext.getSystemService(Context.ETHERNET_SERVICE);
        mConnManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mEthernetInfo = mEthernetManager.getConfiguration("eth0");
        mConnectionModeList.add(KEY_DHCP);
        mConnectionModeList.add(KEY_STATIC_IP);
        mResources = mContext.getResources();
        addPreferencesFromResource(R.xml.ethernet_settings);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
        initPreferences();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        //mEthernetEnabler = new EthernetEnabler(activity, mSwitchBar);
        //mEthernetEnabler.setupSwitchBar(this);
    }
	@Override
    public void onDestroyView() {
        super.onDestroyView();
        //mEthernetEnabler.teardownSwitchBar();
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
		super.onResume();
		//mEthernetEnabler.setupSwitchBar(this);
        //if (mEthernetEnabler != null) {
        //    mEthernetEnabler.resume();
        //}

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        //if (mEthernetEnabler != null) {
        //    mEthernetEnabler.pause();
        //}
        //mContext.unregisterReceiver(mReceiver);
    }


    // tongjun Add for convert between netmask and prefix @{
    public static  String getNetMask(int masks) {
        if(masks == 1)
            return "128.0.0.0";
        else if(masks == 2)
            return "192.0.0.0";
        else if(masks == 3)
            return "224.0.0.0";
        else if(masks == 4)
            return "240.0.0.0";
        else if(masks == 5)
            return "248.0.0.0";
        else if(masks == 6)
            return "252.0.0.0";
        else if(masks == 7)
            return "254.0.0.0";
        else if(masks == 8)
            return "255.0.0.0";
        else if(masks ==9)
            return "255.128.0.0";
        else if(masks == 10)
            return "255.192.0.0";
        else if(masks == 11)
            return "255.224.0.0";
        else if(masks == 12)
            return "255.240.0.0";
        else if(masks == 13)
            return "255.248.0.0";
        else if(masks == 14)
            return "255.252.0.0";
        else if(masks == 15)
            return "255.254.0.0";
        else if(masks == 16)
            return "255.255.0.0";
        else if(masks == 17)
            return "255.255.128.0";
        else if(masks == 18)
            return "255.255.192.0";
        else if(masks == 19)
            return "255.255.224.0";
        else if(masks == 20)
            return "255.255.240.0";
        else if(masks == 21)
            return "255.255.248.0";
        else if(masks == 22)
            return "255.255.252.0";
        else if(masks == 23)
            return "255.255.254.0";
        else if(masks == 24)
            return "255.255.255.0";
        else if(masks == 25)
            return "255.255.255.128";
        else if(masks == 26)
            return "255.255.255.192";
        else if(masks == 27)
            return "255.255.255.224";
        else if(masks == 28)
            return "255.255.255.240";
        else if(masks == 29)
            return "255.255.255.248";
        else if(masks == 30)
            return "255.255.255.252";
        else if(masks == 31)
            return "255.255.255.254";
        else if(masks == 32)
            return "255.255.255.255";
        return "";
    }

    public static int getNetPrefix(String netMask){
        String[] parts = netMask.split("\\.");
        int prefix = 0;
        for (String part: parts){
            if("255".equals(part)){
                prefix += 8;
            }else if("254".equals(part)){
            prefix += 7;
            }else if("252".equals(part)){
                prefix += 6;
            }else if("248".equals(part)){
                prefix += 5;
            }else if("240".equals(part)){
                prefix += 4;
            }else if("224".equals(part)){
                prefix += 3;
            }else if("192".equals(part)){
                prefix += 2;
            }else if("128".equals(part)){
                prefix += 1;
            }
        }
        return prefix;
    }
    // tongjun @}
    private void initPreferences() {
        Log.i(TAG, "initPreferences ");
        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        //getListView().setEmptyView(mEmptyView);
        mConnectionModePref = (PreferenceCategory) findPreference(KEY_CONNECTION_MODE);
        mConnectionConfigPref = (PreferenceCategory) findPreference(KEY_CONNECTION_CONFIG);
        mDefaultConnectionPref = (ListPreference) findPreference(KEY_DEFAULT_CONNECTION);
        mDefaultConnectionPref.setOnPreferenceChangeListener(this);
        mDefaultConnectionPref.setValueIndex(getDefaultConnection());

        mDhcpPref = (Preference) findPreference(KEY_DHCP);
        mDhcpPref.setOnPreferenceClickListener(this);
        mStaticIpPref = (Preference) findPreference(KEY_STATIC_IP);
        mStaticIpPref.setOnPreferenceClickListener(this);
        //Log.d(TAG, "[initPreferences]getState = " + mEthernetManager.getEthernetEnabledState());
        //show ip information
        handleEthernetStateChanged(ETHERNET_EVENT_CONNECTTING);
        //if (mEthernetManager.getEthernetEnabledState()) {
            updatePreference();
        //} else {
        //    showEmptyView();
        //}
    }

    private int getDefaultConnection() {
        config = mEthernetManager.getConfiguration("eth0");
        Log.i(TAG, "getDefaultConnection,config.getIpAssignment() = "+config.getIpAssignment());
        if (config.getIpAssignment() == IpAssignment.STATIC){
            return 1;//staric ip
        }else if(config.getIpAssignment() == IpAssignment.DHCP){
            return 0;//dhcp 
        }
        return 0;
    }

    private void updatePreference() {
        Log.i(TAG, "updatePreference");
        int value = getDefaultConnection();
        mDhcpPref.setEnabled((value == DHCP_MODE) && !flag);
        mStaticIpPref.setEnabled((value == STATIC_IP_MODE) || flag);
        final NetworkInfo netInfo = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        if (netInfo != null) {
            State newState =netInfo.getState();
            mEthState = newState;
            Log.i(TAG, "[updatePreference] New State: " + newState);
            if(newState == NetworkInfo.State.CONNECTED){
                updateDefaultConnectionSummary(true);
            }else{
                updateDefaultConnectionSummary(false);
            }
        }else{
            updateDefaultConnectionSummary(false);
    	}
		if(flag){
			mDefaultConnectionPref.setValueIndex(STATIC_IP_MODE);
		}else{
			mDefaultConnectionPref.setValueIndex(value);
		}
    }

    private void updateDefaultConnectionSummary(boolean success) {
		Log.i(TAG, "updateDefaultConnectionSummary,success = "+success);
		CharSequence sm = null;
		if(flag){
			sm = mResources.getStringArray(R.array.ethernet_connection_mode)[1]
                + SEPRATOR;
		}else{
			sm = mResources.getStringArray(R.array.ethernet_connection_mode)[getDefaultConnection()]
                + SEPRATOR;
		}
        if (success) {
             sm = sm + mResources.getString(R.string.ethernet_on_success);
        } else {
            sm = sm + mResources.getString(R.string.ethernet_on_fail);
        }
        mDefaultConnectionPref.setSummary(sm);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onPreferenceChange");
        if (preference.getKey().equals(KEY_DEFAULT_CONNECTION)) {
            int value = Integer.parseInt(newValue.toString());
            if(1 == value){
                //static ip
                StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                if(staticConfig == null){
                    //user dhcp information update static ip
                    LinkProperties linkProperries = mConnManager.getLinkProperties(ConnectivityManager.TYPE_ETHERNET);
                    if(linkProperries != null){
                        //try{
                            StaticIpConfiguration staticIpConfig = new StaticIpConfiguration();
                            //
                            for (LinkAddress addr : linkProperries.getLinkAddresses()) {
                                if (addr.getAddress() instanceof Inet4Address) {
                                    Log.i(TAG, "updateDhcp,ipAddress = "+ addr.getAddress().getHostAddress());
                                    Log.i(TAG, "updateDhcp,subnet mask = "+getNetMask(addr.getPrefixLength()));
                                    staticIpConfig.ipAddress = addr;
                                }
                            }

                            for (RouteInfo routeInfo : linkProperries.getRoutes()) {
                                if (routeInfo.isIPv4Default() && routeInfo.hasGateway()) {
                                    Log.i(TAG, "1111updateDhcp,Gateway = "+routeInfo.getGateway().getHostAddress());
                                    //mGw.setText(routeInfo.getGateway().getHostAddress());
                                    staticIpConfig.gateway = routeInfo.getGateway();
                                }
                            }


                            List<InetAddress> DnsInfo = linkProperries.getDnsServers();
                                                      
                            staticIpConfig.dnsServers.addAll(DnsInfo);
                            config.setStaticIpConfiguration(staticIpConfig);
                            config.setIpAssignment(IpAssignment.STATIC);
         

                        //}catch (UnknownHostException ex) {
                            // expected
                        //}
                    }else{
                        Log.i(TAG, "onPreferenceChange,linkProperries is null");
						flag = true;
						updatePreference();
						return false;
                    }
                }else{
                    // user the staticip information update static configuration
                    Log.i(TAG, "user the staticip information update static configuration");
                    config.setIpAssignment(IpAssignment.STATIC);
                }
            }else{
                //dhcp
                Log.i(TAG, "dhcp");
                config.setIpAssignment(IpAssignment.DHCP);
				flag = false;
            }
            mEthernetManager.setConfiguration("eth0",config);
            updatePreference();
        }
        return false;
    }

    private void updateStaticIP(View mView) {
        Log.i(TAG, "updateStaticIP");
        mEthernetInfo = mEthernetManager.getConfiguration("eth0");
        mIpaddr = (EditText) mView.findViewById(R.id.ipaddr_edit);
        mDns1 = (EditText) mView.findViewById(R.id.eth_dns1_edit);
        mDns2 = (EditText) mView.findViewById(R.id.eth_dns2_edit);
		TextView mDns2Txt =(TextView) mView.findViewById(R.id.eth_dns2_text);
        mGw = (EditText) mView.findViewById(R.id.eth_gw_edit);
        mMask = (EditText) mView.findViewById(R.id.netmask_edit);
        //upate staic ip information,get information from ethernetmanafer
        if(mEthernetInfo != null){
            if (mEthernetInfo.staticIpConfiguration != null){
                //use staticIpConfiguration to update ip information
				LinkProperties mLinkProperties = mConnManager.getLinkProperties(ConnectivityManager.TYPE_ETHERNET);
                LinkAddress address = mEthernetInfo.staticIpConfiguration.ipAddress;
                InetAddress inetAddress = address.getAddress();
                mIpaddr.setText(inetAddress.getHostAddress());
                mMask.setText(getNetMask(address.getPrefixLength()));				
                InetAddress gatewayAddr = mEthernetInfo.staticIpConfiguration.gateway;
                ArrayList<InetAddress> dnsServers = mEthernetInfo.staticIpConfiguration.dnsServers;

				int mDns1Flag = 0;
				int mDns2Flag = 0;
				for (int i = 0;i < dnsServers.size(); i++) {
					Log.i(TAG, "updateStaticIP,dnsServers["+i+"]: "+dnsServers.get(i).getHostAddress());
					if (dnsServers.get(i) instanceof Inet4Address) {
						if(mDns1Flag!=1){
							mDns1.setText(dnsServers.get(i).getHostAddress());
							mDns1Flag = 1;
							continue;
						}
						if((mDns1Flag==1) && (mDns2Flag!=1)){
							mDns2.setVisibility(View.VISIBLE);
							mDns2Txt.setVisibility(View.VISIBLE);
							mDns2.setText(dnsServers.get(i).getHostAddress());
							mDns2Flag = 1;
						}
					}
				}
				if ( mLinkProperties != null)
				{
					for (RouteInfo routeInfo : mLinkProperties.getRoutes()) {
						if (routeInfo.isIPv4Default() && routeInfo.hasGateway()) {
							Log.i(TAG, "updateDhcp,Gateway = "+routeInfo.getGateway().getHostAddress());
							mGw.setText(routeInfo.getGateway().getHostAddress());
						}
					}					
				}
				else
				{
					mGw.setText(gatewayAddr.getHostAddress());
				}
            }else{
                // use dhcp information to updatestatic info
                LinkProperties mLinkProperties = mConnManager.getLinkProperties(ConnectivityManager.TYPE_ETHERNET);
                if(mLinkProperties != null){
                    for (LinkAddress addr : mLinkProperties.getLinkAddresses()) {
						if (addr.getAddress() instanceof Inet4Address) {
							Log.i(TAG, "updateDhcp,ipAddress = "+ addr.getAddress().getHostAddress());
							Log.i(TAG, "updateDhcp,subnet mask = "+getNetMask(addr.getPrefixLength()));
							mIpaddr.setText(addr.getAddress().getHostAddress());
							mMask.setText(getNetMask(addr.getPrefixLength()));
						}//else if (addr.getAddress() instanceof Inet6Address) {
							//ipv6Addresses.add(addr.getAddress().getHostAddress());
							//if (ipv6Addresses.length() > 0) {
							//	mIpv6addr.setText(BidiFormatter.getInstance().unicodeWrap(ipv6Addresses.toString()));
							//}
						//}
					}
				
					for (RouteInfo routeInfo : mLinkProperties.getRoutes()) {
						if (routeInfo.isIPv4Default() && routeInfo.hasGateway()) {
							Log.i(TAG, "updateDhcp,Gateway = "+routeInfo.getGateway().getHostAddress());
							mGw.setText(routeInfo.getGateway().getHostAddress());
						}
					}

				}
			}
        }else{
            Log.i(TAG, "updateStaticIP,mEthernetInfo is null!!");
        }
        //mMask.setText(mEthernetInfo.getNetMask());
    }

    private void updateDhcp(View mView) {
        Log.i(TAG, "updateDhcp");
        mEthernetInfo = mEthernetManager.getConfiguration("eth0");
        TextView mIpaddr = (TextView) mView.findViewById(R.id.dhcp_address);
        //TextView mIpv6addr = (TextView) mView.findViewById(R.id.dhcp_v6address);
        TextView mMask = (TextView) mView.findViewById(R.id.dhcp_netmask);
        TextView mDns1 = (TextView) mView.findViewById(R.id.dhcp1_dns);
        TextView mDns2 = (TextView) mView.findViewById(R.id.dhcp2_dns);
        TextView mDns2Txt = (TextView) mView.findViewById(R.id.ethernet2_dns_txt);
        TextView mGw = (TextView) mView.findViewById(R.id.dhcp_gateway);   
        //TextView mMac = (TextView) mView.findViewById(R.id.dhcp_mac);   
        //get dhcp network information from ConnectivityManager
        LinkProperties mLinkProperties = mConnManager.getLinkProperties(ConnectivityManager.TYPE_ETHERNET);
        if(mLinkProperties != null){
			//1.Get the Ipv4 address & subnet mask
			StringJoiner ipv6Addresses = new StringJoiner("\n");
			for (LinkAddress addr : mLinkProperties.getLinkAddresses()) {
				if (addr.getAddress() instanceof Inet4Address) {
					Log.i(TAG, "updateDhcp,ipAddress = "+ addr.getAddress().getHostAddress());
					Log.i(TAG, "updateDhcp,subnet mask = "+getNetMask(addr.getPrefixLength()));
					mIpaddr.setText(addr.getAddress().getHostAddress());
					mMask.setText(getNetMask(addr.getPrefixLength()));
				}//else if (addr.getAddress() instanceof Inet6Address) {
					//ipv6Addresses.add(addr.getAddress().getHostAddress());
					//if (ipv6Addresses.length() > 0) {
					//	mIpv6addr.setText(BidiFormatter.getInstance().unicodeWrap(ipv6Addresses.toString()));
					//}
				//}
			}
			
			//2.Get the gateway
			for (RouteInfo routeInfo : mLinkProperties.getRoutes()) {
				if (routeInfo.isIPv4Default() && routeInfo.hasGateway()) {
					Log.i(TAG, "updateDhcp,Gateway = "+routeInfo.getGateway().getHostAddress());
					mGw.setText(routeInfo.getGateway().getHostAddress());
				}
			}
			
			//3.Get the Dns
			int mDns1Flag = 0;
			int mDns2Flag = 0;		
			List<InetAddress> dnsServers = mLinkProperties.getDnsServers();
			for (int i = 0;i < dnsServers.size(); i++) {
				Log.i(TAG, "updateStaticIP,dnsServers["+i+"]: "+dnsServers.get(i).getHostAddress());
				if (dnsServers.get(i) instanceof Inet4Address) {
					if(mDns1Flag!=1){
						mDns1.setText(dnsServers.get(i).getHostAddress());
						mDns1Flag = 1;
						continue;
					}
					if((mDns1Flag==1) && (mDns2Flag!=1)){
						mDns2.setVisibility(View.VISIBLE);
						mDns2Txt.setVisibility(View.VISIBLE);
						mDns2.setText(dnsServers.get(i).getHostAddress());
						mDns2Flag = 1;
					}
				}
			}
			Log.i(TAG, "updateDhcp,DnsInfo = "+dnsServers);
			

			//4.Get the MAC
			//Log.i(TAG, "updateDhcp,DnsInfo = "+dnsServers);
			//mMac.setText(getMacAddress());			
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // TODO Auto-generated method stub
        if (preference.getKey().equals(KEY_STATIC_IP)) {
            showDialog(DIALOG_STATIC_IP_CONFIGURATION);
        } else if (preference.getKey().equals(KEY_DHCP)) {
            showDialog(DIALOG_DHCP_CONFIGURATION);
        } 
        return false;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Log.i(TAG, "onCreateDialog,dialogId= "+dialogId);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        View mView;
        switch (dialogId) {
        case DIALOG_DHCP_CONFIGURATION:
            mView = getActivity().getLayoutInflater().inflate(R.layout.ethernet_dhcp_config, null);
            builder.setView(mView);
            updateDhcp(mView);
            builder.setTitle(mResources.getString(R.string.dhcp_config_title));
            builder.setPositiveButton(mResources.getString(R.string.ok), null);

            break;
        case DIALOG_STATIC_IP_CONFIGURATION:
            mView = getActivity().getLayoutInflater().inflate(R.layout.ethernet_ip_config, null);
            builder.setView(mView);
            updateStaticIP(mView);
            builder.setTitle(mResources.getString(R.string.static_ip_config_title));
            builder.setNegativeButton(mResources.getString(R.string.cancel), null);
            builder.setPositiveButton(mResources.getString(R.string.ok), new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    saveStaticIP();
                }

            });
            break;
        }
		builder.setInverseBackgroundForced(true);
        mDialog = builder.create();

        return mDialog;
    }

	public String getMacAddress() 
	{ 
		try { 
			return loadFileAsString("/sys/class/net/eth0/address").toUpperCase().substring(0, 17); 
		} 
		catch (IOException e) 
		{ 
			e.printStackTrace(); 
			return null; 
		} 
	} 
	
	public static String loadFileAsString(String filePath) throws java.io.IOException 
	{ 
		StringBuffer fileData = new StringBuffer(1000); 
		BufferedReader reader = new BufferedReader(new FileReader(filePath)); 
		char[] buf = new char[1024]; 
		int numRead = 0; 
		while ((numRead = reader.read(buf)) != -1) 
		{ 
			String readData = String.valueOf(buf, 0, numRead); 
			fileData.append(readData); 
		} 
		reader.close(); 
		return fileData.toString(); 
	}

    private void showEmptyView() {
        Log.i(TAG, "showEmptyView ");
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        } //else if (mDefaultConnectionPref.getDialog() != null) {
            //mDefaultConnectionPref.getDialog().dismiss();
        //}
        getPreferenceScreen().removeAll();
        //if (!mEthernetManager.getEthernetEnabledState()){
        //    mEmptyView.setText(R.string.ethernet_turned_off);
        //}
    }

    private void handleEthernetStateChanged(int event) {
		Log.i(TAG, "handleEthernetStateChanged, event = "+event);
		
        if (event == ETHERNET_EVENT_CONNECTTING) {
            if (getPreferenceScreen().getPreferenceCount() == 0) {
                getPreferenceScreen().addPreference(mConnectionModePref);
                getPreferenceScreen().addPreference(mConnectionConfigPref);
                updatePreference();
            }
        } else if (event == ETHERNET_EVENT_CONNECTED){
            updateDefaultConnectionSummary(true);
        } else if (event == ETHERNET_EVENT_SUSPENDED) {
            updateDefaultConnectionSummary(false);
        } else if (event == ETHERNET_EVENT_DISCONNECTED) {
            updateDefaultConnectionSummary(false);
        } else {
            showEmptyView();
    	}
    }

    private void saveStaticIP() {
		Log.i(TAG, "saveStaticIP");
        if (checkValid()) {
            StaticIpConfiguration staticIpConfig = new StaticIpConfiguration();
            InetAddress ipAddress = getInetAddress(mIpaddr.getText().toString());
            String netMask = mMask.getText().toString();
            staticIpConfig.ipAddress = new LinkAddress(ipAddress,getNetPrefix(netMask));   // fix mask
            staticIpConfig.gateway = getInetAddress(mGw.getText().toString());
            if (checkValidKandaoDns1()) {
                staticIpConfig.dnsServers.add(getInetAddress(mDns1.getText().toString()));
            }
            else
            {
                 //staticIpConfig.dnsServers.add(getInetAddress("8.8.8.8"));
            }
            if (checkValidKandaoDns2()) {
                staticIpConfig.dnsServers.add(getInetAddress(mDns2.getText().toString()));
            } 
            else
            {
                 //staticIpConfig.dnsServers.add(getInetAddress("8.8.4.4"));
            }           
			
            config.setIpAssignment(IpAssignment.STATIC);
            config.setStaticIpConfiguration(staticIpConfig);
            //mEthernetInfo.setNetMask(mMask.getText().toString());
        } else {
            Toast.makeText(mContext, R.string.ip_address_invalid, Toast.LENGTH_LONG).show();
        }
        mEthernetManager.setConfiguration("eth0",config);
        Log.i(TAG, "vendor.kandao.ethernet begin ");


        //SystemProperties.set("vendor.kandao.ethernet", "1");

         new Handler(Looper.getMainLooper()).postDelayed(() -> SystemProperties.set("vendor.kandao.ethernet", "1"), 1000);       
        Log.i(TAG, "vendor.kandao.ethernet end ");
       //mEthernetManager.setEthernetEnabled(false);
       //mEthernetManager.setEthernetEnabled(true);
    }
	/**
	  * Get an {@link InetAddress} from a string
	  *
	  * @throws IllegalArgumentException if the string is not a valid IP address.
	  */
    private static InetAddress getInetAddress(String ipAddress) {
        if (!InetAddress.isNumeric(ipAddress)) {
            throw new IllegalArgumentException(
                String.format("IP address %s is not numeric", ipAddress));
        }
        try {
            return InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(
                String.format("IP address %s could not be resolved", ipAddress));
        }
    }

    private boolean checkValid() {
        String[] ipInfos = new String[3];
        ipInfos[0] = mIpaddr.getText().toString();
        ipInfos[1] = mGw.getText().toString();
        //ipInfos[2] = mDns1.getText().toString();
        //1ipInfos[3] = mDns2.getText().toString();
        ipInfos[2] = mMask.getText().toString();
        Pattern mPattern = Pattern.compile(IPADDRESS_PATTERN);
        for (String ipInfo : ipInfos) {
			Log.i(TAG, "checkValid " +ipInfo);
            if (!mPattern.matcher(ipInfo).matches()) {
                return false;
            }
        }
        return true;
    }

    private boolean checkValidKandaoDns1() {
        String[] ipInfos = new String[1];
        //ipInfos[0] = mIpaddr.getText().toString();
        //ipInfos[1] = mGw.getText().toString();
        ipInfos[0] = mDns1.getText().toString();
        //1ipInfos[3] = mDns2.getText().toString();
        //ipInfos[2] = mMask.getText().toString();
        Pattern mPattern = Pattern.compile(IPADDRESS_PATTERN);
        for (String ipInfo : ipInfos) {
            Log.i(TAG, "checkValid " +ipInfo);
            if (!mPattern.matcher(ipInfo).matches()) {
                return false;
            }
        }
        return true;
    }   
    private boolean checkValidKandaoDns2() {
        String[] ipInfos = new String[1];
        //ipInfos[0] = mIpaddr.getText().toString();
        //ipInfos[1] = mGw.getText().toString();
        //ipInfos[0] = mDns1.getText().toString();
        ipInfos[0] = mDns2.getText().toString();
        //ipInfos[2] = mMask.getText().toString();
        Pattern mPattern = Pattern.compile(IPADDRESS_PATTERN);
        for (String ipInfo : ipInfos) {
            Log.i(TAG, "checkValid " +ipInfo);
            if (!mPattern.matcher(ipInfo).matches()) {
                return false;
            }
        }
        return true;
    }      
	
	@Override
    public int getMetricsCategory() {                                                                                                                 
        //return MetricsLogger.PRIVACY;
		return MetricsEvent.METRICS_ETHERNET;
    }
	 
	@Override
    public int getDialogMetricsCategory(int dialogId) {
        return MetricsEvent.METRICS_ETHERNET;
    }
}
