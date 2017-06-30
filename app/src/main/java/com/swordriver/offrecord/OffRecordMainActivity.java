package com.swordriver.offrecord;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.swordriver.offrecord.JCLogger.LogAreas;

import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;

import swordriver.com.googledrivemodule.GoogleApiModel;
import swordriver.com.googledrivemodule.GoogleApiModelSecure;
import timber.log.Timber;

import static swordriver.com.googledrivemodule.GoogleApiModel.REQUEST_CODE_RESOLUTION;

public class OffRecordMainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // constants
    static final String CACHED_PASSWORD="Cached_Password";
    static final String LAST_ACTIVE_TIME="Last_Active_Time";
    static final long TIMEOUT_DURATION = 10000;  // password timeout in ms


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.tag(LogAreas.LIFECYCLE.s()).v("called.");

        // restore saved state
        if (savedInstanceState != null) {
            mCachedPassword = savedInstanceState.getString(CACHED_PASSWORD);
            mLastActiveTime= savedInstanceState.getLong(LAST_ACTIVE_TIME);
        }

        mServiceListeners = new HashSet<ControllerServiceInterface>();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_off_record_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // setup tabs
        mViewPager = (ViewPager) findViewById(R.id.main_viewpager);
        mPagerAdapter = (OffRecordPagerAdapter) (mViewPager.getAdapter());
        if (mPagerAdapter==null) {
            Timber.tag(LogAreas.UI.s()).v("creating pager adapter");
            mPagerAdapter = new OffRecordPagerAdapter(getSupportFragmentManager());
            mViewPager.setAdapter(mPagerAdapter);
        }
        mTabLayout = (TabLayout) findViewById(R.id.main_tabs);
        mTabLayout.setupWithViewPager(mViewPager);

    }

    @Override
    public void onStart(){
        super.onStart();

        // see if password timeout have passed.
        if (System.currentTimeMillis()-TIMEOUT_DURATION>mLastActiveTime){
            mCachedPassword=null;
            if (mGoogleApiModel!=null) mGoogleApiModel.clearPassword();
            Timber.tag(LogAreas.UI.s()).w("clearing cached password.");
        }

        // start background serivce
        Intent serviceStartIntent = new Intent(this, OffRecordMainService.class);
        startService(serviceStartIntent);

        // bind to the service
        Intent intent = new Intent(this, OffRecordMainService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop(){
        super.onStop();
        if(mNewPassDialog!=null){
            mNewPassDialog.dismiss();
        }
        if(mPassDialog!=null){
            mPassDialog.dismiss();
        }
        if (mGoogleApiModel!=null) mGoogleApiModel.deleteObserver(mGoogleConnectionObserver);
        // unbind the service
        if (mMainService!=null) {
            //mMainService.removeListener(this);
            this.unbindService(mServiceConnection);
            mMainService=null;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //process fragment back stack first
            FragmentBackStackPressed currentFragment = (FragmentBackStackPressed) mPagerAdapter.getItem(mViewPager.getCurrentItem());
            if (!currentFragment.onBackPressed()) {
                int count = getFragmentManager().getBackStackEntryCount();
                if (count == 0) {
                    super.onBackPressed();
                    //additional code
                } else {
                    getFragmentManager().popBackStack();
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        mLastActiveTime=System.currentTimeMillis();
        if (mCachedPassword!=null) savedInstanceState.putString(CACHED_PASSWORD, mCachedPassword);
        savedInstanceState.putLong(LAST_ACTIVE_TIME, mLastActiveTime);
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.off_record_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case (R.id.action_enter_pass):
                Timber.tag(LogAreas.UI.s()).v("enter password clicked.");
                passwordPrompt();
                break;
            case (R.id.action_change_pass):
                Timber.tag(LogAreas.UI.s()).v("change password clicked.");
                newPasswordPrompt(true);
                break;
            case (R.id.action_reset_account):
                Timber.tag(LogAreas.UI.s()).v("account reset clicked.");
                resetAccount();
                break;
            case (R.id.action_about):
                Timber.tag(LogAreas.UI.s()).v("account about clicked.");
                aboutApp();
                break;
            default:

        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id){
            case R.id.switch_user:
                mGoogleApiModel.close();
                break;
            case R.id.signout:
                mGoogleApiModel.signOut();
                break;
            default:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /////////////////////////////////////////////////////////////////////////////
    // Interface definitions
    /////////////////////////////////////////////////////////////////////////////
    public interface ControllerServiceInterface {
        void startProcessing(OffRecordMainService service);
    }


    ////////////////////////////////////
    // pager adapter for the pager views
    ////////////////////////////////////
    private class OffRecordPagerAdapter extends FragmentPagerAdapter {

        final private static int NUM_TABS = 2;
        private Fragment[] mFragmentArray = new Fragment[NUM_TABS];

        public OffRecordPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (mFragmentArray[position]==null){
                switch (position){
                    case 0:
                        mFragmentArray[position]= new FragmentNotes();
                        break;
                    case 1:
                        mFragmentArray[position]= new FragmentPassGenerator();
                        break;
                    default:
                        return null;
                }
            }
            return (Fragment) mFragmentArray[position];
        }

        @Override
        public int getCount() {
            return NUM_TABS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String title = "";
            switch (position) {
                case 0:
                    title = "Secure Notes";
                    break;
                case 1:
                    title = "Password Generator";
                    break;
                default:
                    break;
            }
            ;
            return title;
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // private helper APIs
    /////////////////////////////////////////////////////////////////////////////
    private HashSet<ControllerServiceInterface> mServiceListeners;
    private OffRecordMainService mMainService;
    private GoogleApiModelSecure mGoogleApiModel;
    private String mCachedPassword=null;
    private AlertDialog mNewPassDialog=null;
    private AlertDialog mPassDialog=null;
    private long mLastActiveTime= System.currentTimeMillis();
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private OffRecordPagerAdapter mPagerAdapter;

    private void newPasswordPrompt(final boolean changePassword) {
        LinearLayout passLayout = new LinearLayout(OffRecordMainActivity.this);
        passLayout.setOrientation(LinearLayout.VERTICAL);
        final EditText password = new EditText(OffRecordMainActivity.this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setHint("new password");
        passLayout.addView(password);
        final EditText passwordAgain = new EditText(OffRecordMainActivity.this);
        passwordAgain.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordAgain.setHint("password again");
        passLayout.addView(passwordAgain);

        AlertDialog.Builder builder = new AlertDialog.Builder(OffRecordMainActivity.this);
        builder.setTitle("New Password");
        builder.setView(passLayout);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // check if password match each other
                String pass = password.getText().toString();
                if (changePassword){
                    mGoogleApiModel.changePassword(pass, new GoogleApiModel.ListFolderCallback() {
                        @Override
                        public void callback(GoogleApiModel.FolderInfo info) {
                            Timber.tag(LogAreas.GOOGLEAPI.s()).i("password successfully changed.");
                        }
                    });
                }else {
                    if (pass.length() == 0 || !pass.equals(passwordAgain.getText().toString()) ||
                            !mGoogleApiModel.setPassword(password.getText().toString())) {
                        // password mismatch or failed to set password
                        newPasswordPrompt(false);
                    } else {
                        // cache password
                        mCachedPassword = password.getText().toString();
                    }
                }
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
            }
        });

        mNewPassDialog = builder.show();
        mNewPassDialog.setCanceledOnTouchOutside(false);
    }

    private void passwordPrompt() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(OffRecordMainActivity.this);
        builder.setTitle("Enter Password");
        final EditText password = new EditText(OffRecordMainActivity.this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(password);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String localPassword = password.getText().toString();
                if (!mGoogleApiModel.setPassword(localPassword)){
                    // wrong password
                    passwordPrompt();
                }else{
                    mCachedPassword=localPassword;
                }
            }
        });
        builder.setNeutralButton("cancel", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // not initialized.
                Timber.tag(LogAreas.UI.s()).w("set password cancelled");
                mGoogleApiModel.clearPassword();
            }
        });
        mPassDialog = builder.show();
        mPassDialog.setCanceledOnTouchOutside(false);
    }

    private void resetAccount(){
        mGoogleApiModel.deleteEverythingInAppRoot(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    mGoogleApiModel.clearPasswordValidationData();
                    newPasswordPrompt(false);
                }else{
                    Timber.tag(LogAreas.GOOGLEAPI.s()).e("delete everything failed!! %d, %s", status.getStatusCode(), status.getStatusMessage());
                }
            }
        });
    }

    private void aboutApp(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(OffRecordMainActivity.this);
        builder.setTitle("Off Record");
        String message = String.format("Build: %s, Version: %s", BuildConfig.BUILD_TYPE,
                BuildConfig.VERSION_NAME + "-" + BuildConfig.GitHash);
        builder.setMessage(message);
        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        mPassDialog = builder.show();
        mPassDialog.setCanceledOnTouchOutside(false);
    }

    private void setUserName(){
        TextView title = (TextView) findViewById(R.id.drawer_title);
        title.setText(mGoogleApiModel.getDisplayName());
        TextView email = (TextView) findViewById(R.id.drawer_user_email);
        email.setText(mGoogleApiModel.getEmail());
    }

    private void startProcessingForAll(){
        for (ControllerServiceInterface i : mServiceListeners){
            i.startProcessing(mMainService);
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // public APIs
    /////////////////////////////////////////////////////////////////////////////
    public void setServiceListener(ControllerServiceInterface listener){
        mServiceListeners.add(listener);
        Timber.tag(LogAreas.UI.s()).v("Adding a service listener.");
        if (mMainService!=null &&
                mMainService.getState()== OffRecordMainService.OffRecordServiceState.INITIALIZED){
            Timber.tag(LogAreas.UI.s()).v("push service handle to listener.");
            listener.startProcessing(mMainService);
        }
    }

    public void removeServiceListener(ControllerServiceInterface listener){
        mServiceListeners.remove(listener);
    }

    /////////////////////////////////////////////////////////////////////////////
    // callbacks
    /////////////////////////////////////////////////////////////////////////////

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GoogleApiModel.REQUEST_CODE_SIGNIN) {
            mGoogleApiModel.processSignInResult(data);
        }

        if (requestCode == REQUEST_CODE_RESOLUTION) {
            Timber.tag(LogAreas.GOOGLEAPI.s()).w("Auto resolution returned. " + resultCode);
            if (resultCode==RESULT_OK){
                mGoogleApiModel.connect();
            }
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            OffRecordMainService.LocalBinder binder = (OffRecordMainService.LocalBinder) service;
            mMainService = binder.getService();
            if (mMainService.getState()== OffRecordMainService.OffRecordServiceState.INITIALIZED){
                startProcessingForAll();
            }

            mGoogleApiModel = mMainService.getGoogleApiModel(OffRecordMainActivity.this);
            mGoogleApiModel.addObserver(mGoogleConnectionObserver);
            if (mGoogleApiModel.getStatus()== GoogleApiModel.GoogleApiStatus.DISCONNECTED) {
                mCachedPassword=null;
                mGoogleApiModel.setResolutionActivity(OffRecordMainActivity.this);
                mGoogleApiModel.open();
            }else if(mGoogleApiModel.getStatus()!=GoogleApiModel.GoogleApiStatus.DISCONNECTED &&
                    mCachedPassword==null) {
                if (mGoogleApiModel.needNewPassword()) {
                    newPasswordPrompt(false);
                } else {
                    passwordPrompt();
                }
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private Observer mGoogleConnectionObserver = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            GoogleApiModel.GoogleApiStatus status = (GoogleApiModel.GoogleApiStatus)(arg);
            switch (status){
                case DISCONNECTED:
                    mCachedPassword=null;
                    mGoogleApiModel.setResolutionActivity(OffRecordMainActivity.this);
                    if (mGoogleApiModel.getStatus()== GoogleApiModel.GoogleApiStatus.DISCONNECTED){
                        mGoogleApiModel.open();
                    }
                    break;
                case CONNECTED_UNINITIALIZED:
                    if (mCachedPassword!=null){
                        mGoogleApiModel.setPassword(mCachedPassword);
                    }else {
                        if (mGoogleApiModel.needNewPassword()) {
                            newPasswordPrompt(false);
                        } else {
                            passwordPrompt();
                        }
                    }
                    break;
                case INITIALIZED:
                    setUserName();
                    startProcessingForAll();
                    break;
                default:
                    break;
            }
        }
    };
}
