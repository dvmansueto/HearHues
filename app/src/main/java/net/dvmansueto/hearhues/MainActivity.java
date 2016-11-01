package net.dvmansueto.hearhues;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private ScalarTone mScalarTone;
    private ToneGenerator mToneGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // register pref listener
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // instantiate global objects, update them and push to ApplicationContext
        mScalarTone = new ScalarTone( this);
        mToneGenerator = new ToneGenerator();
        initialiseFromSharedPreferences();

        // provide content view
        setContentView(R.layout.activity_main);

        // inflate the default fragment
        setFragment( TreadToneFragment.newInstance());

        // setup the action/app bar
        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar);
        setSupportActionBar(toolbar);

        // setup the navigation drawer
        DrawerLayout drawerLayout = (DrawerLayout) findViewById( R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener( toggle);
        toggle.syncState();
    }

    private void initialiseFromSharedPreferences() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mToneGenerator.setLinToLogPower(
                Integer.parseInt( sharedPreferences.getString(
                        getString( R.string.prefs_generator_logarithm_power_key),
                        getString( R.string.prefs_generator_logarithm_power_default))));

        mToneGenerator.setPlaybackSeconds(
                Double.parseDouble( sharedPreferences.getString(
                        getString( R.string.prefs_playback_seconds_key),
                        getString( R.string.prefs_playback_seconds_default))));

        mScalarTone.setFrequencyRange(
                Double.parseDouble( sharedPreferences.getString(
                        getString( R.string.prefs_generator_base_frequency_key),
                        getString( R.string.prefs_generator_base_frequency_default))),
                Double.parseDouble( sharedPreferences.getString(
                        getString( R.string.prefs_generator_peak_frequency_key),
                        getString( R.string.prefs_generator_peak_frequency_default))));

        ApplicationContext applicationContext = (ApplicationContext) getApplicationContext();
        applicationContext.setScalarTone( mScalarTone);
        applicationContext.setToneGenerator( mToneGenerator);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById( R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else if (getFragmentManager().getBackStackEntryCount() > 0 ){
            getFragmentManager().popBackStack();
        }
        else {
            super.onBackPressed();
        }
    }

    private Menu mMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        mMenu = menu;
        return true;
    }

    private void setToolbarActionsVisible(boolean visible) {
        if ( visible ^ mMenu.hasVisibleItems()) {
            for (int i = 0; i < mMenu.size(); i++) {
                mMenu.getItem(i).setVisible(visible);
            }
        }
    }

    private void toggleMute() {
        ActionMenuItemView muteButton = (ActionMenuItemView) findViewById( R.id.action_mute);
        if (mToneGenerator.getMuted()) {
            mToneGenerator.setMuted( false);
            muteButton.setIcon( getResources().getDrawable( R.drawable.ic_volume_unmuted, null));
        } else {
            mToneGenerator.setMuted( true);
            muteButton.setIcon( getResources().getDrawable( R.drawable.ic_volume_muted, null));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key){

        ApplicationContext applicationContext = (ApplicationContext) getApplicationContext();

        if ( key.equals( getString( R.string.prefs_generator_logarithm_power_key))) {
            mToneGenerator.setLinToLogPower(
                    Integer.parseInt( sharedPreferences.getString(
                            getString( R.string.prefs_generator_logarithm_power_key),
                            getString( R.string.prefs_generator_logarithm_power_default))));
            applicationContext.setToneGenerator( mToneGenerator);
        }
        else if ( key.equals( getString( R.string.prefs_playback_seconds_key))) {
            mToneGenerator.setPlaybackSeconds(
                    Double.parseDouble( sharedPreferences.getString(
                            getString( R.string.prefs_playback_seconds_key),
                            getString( R.string.prefs_playback_seconds_default))));
            applicationContext.setToneGenerator( mToneGenerator);
        }
        else if ( key.equals( getString( R.string.prefs_generator_base_frequency_key)) ||
                  key.equals( getString( R.string.prefs_generator_peak_frequency_key))) {
            mScalarTone.setFrequencyRange(
                    Double.parseDouble( sharedPreferences.getString(
                            getString( R.string.prefs_generator_base_frequency_key),
                            getString( R.string.prefs_generator_base_frequency_default))),
                    Double.parseDouble( sharedPreferences.getString(
                            getString( R.string.prefs_generator_peak_frequency_key),
                            getString( R.string.prefs_generator_peak_frequency_default))));
            applicationContext.setScalarTone( mScalarTone);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_mute: {
                toggleMute();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected( @NonNull MenuItem item) {

        Fragment fragment = null;

        int id = item.getItemId();
        if( id == R.id.nav_hearHue) {
            fragment = new HearHueFragment();
            setToolbarActionsVisible( true);
        } else if( id == R.id.nav_treadTunes) {
            fragment = new TreadToneFragment();
            setToolbarActionsVisible( true);
        } else if( id == R.id.nav_tinker) {
            fragment = new TinkerFragment();
            setToolbarActionsVisible( true);
        } else if( id == R.id.nav_settings) {
            fragment = new SettingsFragment();
            setToolbarActionsVisible( false);
        } else if( id == R.id.nav_about) {
            fragment = new AboutFragment();
            setToolbarActionsVisible( false);
        }

        if( fragment != null) setFragment( fragment);
        closeNavigationDrawer();
        return true;
    }

    /**
     * Method for robust fragment management. Only adds new fragments to backstack.
     * Old fragments remaining visible and clickable is fixed in XML layout of each fragment:
     *    • android:background="@color/windowBackground"
     *    • android:clickable="true"
     * @param fragment the new fragment to apply.
     */
    private void setFragment( Fragment fragment) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById( R.id.fragment_container);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace( R.id.fragment_container, fragment, fragment.getClass().getName());
        // only add to backStack if first fragment or different fragment
        if ( currentFragment == null || !currentFragment.getClass().equals( fragment.getClass())) {
            transaction.addToBackStack( fragment.getClass().getName());
        }
        transaction.commit();
    }

    void closeNavigationDrawer() {
        DrawerLayout drawerLayout = ( DrawerLayout) findViewById( R.id.drawer_layout);
        drawerLayout.closeDrawer( GravityCompat.START);
    }

    void openNavigationDrawer() {
        DrawerLayout drawerLayout = ( DrawerLayout) findViewById( R.id.drawer_layout);
        drawerLayout.openDrawer( GravityCompat.START);

    }
}