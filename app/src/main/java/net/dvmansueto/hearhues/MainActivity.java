package net.dvmansueto.hearhues;


import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // provide content view
        setContentView(R.layout.activity_main);

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container_content, HearHueFragment.newInstance())
                    .commit();
        }

        // setup the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // setup the navigation drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
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

    private void setMenuItemsVisible(boolean visible) {
        if ( visible ^ mMenu.hasVisibleItems()) {
            for (int i = 0; i < mMenu.size(); i++) {
                mMenu.getItem(i).setVisible(visible);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_mute: {
//
//                return true;
//            }
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected( @NonNull MenuItem item) {
        Fragment fragment = null;
        int id = item.getItemId();

        if( id == R.id.nav_hearHue) {
            fragment = new HearHueFragment();
            setMenuItemsVisible( true);
        } else if( id == R.id.nav_seeSound) {
            fragment = new SeeSoundFragment();
            setMenuItemsVisible( true);
        } else if( id == R.id.nav_tinker) {
            fragment = new TinkerFragment();
            setMenuItemsVisible( true);
        } else if( id == R.id.nav_settings) {
            fragment = new SettingsFragment();
            setMenuItemsVisible( false);
        } else if( id == R.id.nav_about) {
            fragment = new AboutFragment();
            setMenuItemsVisible( false);
        }

        if( fragment != null) {
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.container_content, fragment).commit();
        }
        DrawerLayout drawer = ( DrawerLayout) findViewById( R.id.drawer_layout);
        drawer.closeDrawer( GravityCompat.START);
        return true;
    }
}