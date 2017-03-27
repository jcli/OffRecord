package com.swordriver.offrecord;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import timber.log.Timber;

/**
 * Created by jcli on 1/22/17.
 */

public class FragmentPassGenerator extends Fragment implements FragmentBackStackPressed{

    private EditText mPassEditText;
    private Button mGenerateButton;
    private ImageButton mCopyButton;
    private DataSourcePassGenerator mGenerator;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.tag(JCLogger.LogAreas.LIFECYCLE.s()).v("called.");
        View rootView = inflater.inflate(R.layout.fragment_generator, container, false);
        mGenerator = new DataSourcePassGenerator();
        mPassEditText = (EditText) rootView.findViewById(R.id.generated_password_edittext);
        mPassEditText.setText(mGenerator.getPass(20));
        mGenerateButton = (Button) rootView.findViewById(R.id.generate_password_button);
        mGenerateButton.setOnClickListener(new GenerateButtonClicked());
        mCopyButton = (ImageButton) rootView.findViewById(R.id.copy_generated_password_button);
        mCopyButton.setOnClickListener(new CopyButtonClicked());
        return rootView;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // private helper
    ////////////////////////////////////////////////////////////////////////////////

    private class GenerateButtonClicked implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            mPassEditText.setText(mGenerator.getPass(20));
        }
    }

    private class CopyButtonClicked implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            String name = getActivity().getString(R.string.app_name);
            ClipData clip = ClipData.newPlainText(name, mPassEditText.getText().toString());
            clipboard.setPrimaryClip(clip);
        }
    }
}
