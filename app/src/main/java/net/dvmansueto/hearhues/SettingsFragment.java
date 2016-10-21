package net.dvmansueto.hearhues;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class SettingsFragment extends Fragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getFragmentManager().beginTransaction()
                .replace( R.id.container_content, new PrefsFragment())
                .commit();
        activity.getSupportActionBar().setTitle( R.string.title_fragment_settings);
    }

    @SuppressLint("ValidFragment")
    private class PrefsFragment extends PreferenceFragment {
        public PrefsFragment() {}

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource( R.xml.preferences);
        }
    }
}