package net.dvmansueto.hearhues;


import android.app.Fragment;
import android.os.Bundle;
import android.support.v13.app.FragmentTabHost;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class TinkerFragment extends Fragment {

    private FragmentTabHost mTabHost;

    public TinkerFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView( LayoutInflater inflater,
                              ViewGroup container,
                              Bundle savedInstanceState) {

//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_tinker, container, false);

        View rootView = inflater.inflate( R.layout.fragment_tinker, container, false);

        mTabHost = (FragmentTabHost) rootView.findViewById( android.R.id.tabhost);
        mTabHost.setup( getActivity(), getChildFragmentManager(), R.id.tinker_tabbed_fragment_container);

        mTabHost.addTab( mTabHost
                .newTabSpec( getString(R .string.fragment_touch_hues_tab_tag))
                .setIndicator( getString(R .string.fragment_touch_hues_title)),
                TouchHuesFragment.class, null);
        mTabHost.addTab(mTabHost
                .newTabSpec( getString(R .string.fragment_touch_tones_tab_tag))
                .setIndicator( getString( R.string.fragment_touch_tones_title)),
                TouchTonesFragment.class, null);

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setTitle(R.string.title_fragment_tinker);


    }

}
