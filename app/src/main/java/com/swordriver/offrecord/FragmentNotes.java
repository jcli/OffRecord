package com.swordriver.offrecord;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import timber.log.Timber;

/**
 * Created by jcli on 1/22/17.
 */

public class FragmentNotes extends Fragment {

    private FragmentNotesList mFragmentNotesList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.tag(JCLogger.LogAreas.LIFECYCLE.s()).v("called.");
        View rootView = inflater.inflate(R.layout.fragment_notes, container, false);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        Fragment fragment = (getChildFragmentManager().findFragmentById(R.id.notes_child_fragment));
        if (fragment!=null){

        }else{
            if (mFragmentNotesList==null){
                mFragmentNotesList = new FragmentNotesList();
            }
            transaction.replace(R.id.notes_child_fragment, mFragmentNotesList).commit();
        }

        return rootView;
    }
}
