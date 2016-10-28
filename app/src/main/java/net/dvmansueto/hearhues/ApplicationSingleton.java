package net.dvmansueto.hearhues;

import android.app.Application;

/**
 * Created by Dave on 28/10/16.
 */

public class ApplicationSingleton extends Application {

    private ScalarTone mScalarTone; // the _ONLY_ ScalarTone for the entire app.
    private ToneGenerator mToneGenerator;  // the _ONLY_ ToneGenerator for the entire app.

    ScalarTone getScalarTone() {
        return mScalarTone;
    }

    ToneGenerator getToneGenerator() {
        return mToneGenerator;
    }

    void setScalarTone( ScalarTone scalarTone) {
        mScalarTone = scalarTone;
    }

    void setToneGenerator( ToneGenerator toneGenerator) {
        mToneGenerator = toneGenerator;
    }

}
