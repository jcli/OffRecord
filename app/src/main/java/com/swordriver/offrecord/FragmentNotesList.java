package com.swordriver.offrecord;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import timber.log.Timber;

/**
 * Created by jcli on 1/23/17.
 */

public class FragmentNotesList extends Fragment implements OffRecordMainActivity.ControllerServiceInterface{

    private DataSourceNotes mNotesSource;
    private AlertDialog mAddNoteDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.tag(JCLogger.LogAreas.LIFECYCLE.s()).v("called.");
        View rootView = inflater.inflate(R.layout.fragment_notes_list, container, false);

        setupFloatingButtonsRegular(rootView);

        // register itself with main activity
        OffRecordMainActivity activity = (OffRecordMainActivity) getActivity();
        activity.setServiceListener(this);

        return rootView;
    }

    @Override
    public void onStop(){
        super.onStop();
        OffRecordMainActivity activity = (OffRecordMainActivity) getActivity();
        activity.removeServiceListener(this);
        if (mAddNoteDialog!=null) mAddNoteDialog.cancel();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // callbacks and interfaces
    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void startProcessing(OffRecordMainService service) {
        mNotesSource = service.getNotesDataSource();
        mNotesSource.init(service.getGoogleApiModel(null));
    }

    ////////////////////////////////////////////////////////////////////////////////
    // private helpers
    ////////////////////////////////////////////////////////////////////////////////

    private void setupFloatingButtonsRegular(final View rootView) {
        // find the floating menu
        final FloatingActionMenu menu = (FloatingActionMenu) rootView.findViewById(R.id.notesListMenu);

        // find the floating buttons
        final FloatingActionButton bAddNote = (FloatingActionButton) rootView.findViewById(R.id.noteButton1);

        // config add note
        bAddNote.setLabelText("Add Note");
        bAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.close(true);
                addNotePopup();
            }
        });
    }

    private void addNotePopup(){
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Enter Name");
            final EditText noteNameView= new EditText(getActivity());
            noteNameView.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(noteNameView);

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = noteNameView.getText().toString();
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                }
            });
            mAddNoteDialog = builder.show();
        }
    }
}
