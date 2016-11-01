package net.dvmansueto.hearhues;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class LandingFragment extends Fragment implements View.OnClickListener {

    private OnLandingViewSelectedListener mListener;

    interface OnLandingViewSelectedListener {
        void OnLandingViewSelected( View view);
    }

    void setLandingViewSelectedListener( OnLandingViewSelectedListener listener) {
        mListener = listener;
    }


    public LandingFragment() {
        mListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate( R.layout.fragment_landing, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        // prepare to capture button presses
        view.findViewById( R.id.landing_hear_hue).setOnClickListener( this);
        view.findViewById( R.id.landing_tread_tone).setOnClickListener( this);
        view.findViewById( R.id.landing_tinker).setOnClickListener( this);
        view.findViewById( R.id.landing_about).setOnClickListener( this);
        view.findViewById( R.id.landing_settings).setOnClickListener( this);
    }

    @Override
    public void onResume() {
        super.onResume();
        //noinspection ConstantConditions
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setIcon( R.mipmap.ic_launcher);
        actionBar.setTitle(R.string.app_name);
    }

    @Override
    public void onClick(View view) {
        if ( mListener != null) mListener.OnLandingViewSelected( view);
    }
}