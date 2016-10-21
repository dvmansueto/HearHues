package net.dvmansueto.hearhues;


import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class AboutFragment extends Fragment {


    public AboutFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setTitle(R.string.title_fragment_about);

        // activate autoLink
        TextView textView = (TextView) activity.findViewById( R.id.about_project_body);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
