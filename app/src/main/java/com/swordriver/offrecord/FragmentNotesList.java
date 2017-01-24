package com.swordriver.offrecord;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import timber.log.Timber;

/**
 * Created by jcli on 1/23/17.
 */

public class FragmentNotesList extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.tag(JCLogger.LogAreas.LIFECYCLE.s()).v("called.");
        View rootView = inflater.inflate(R.layout.fragment_notes_list, container, false);
        return rootView;
    }
}
