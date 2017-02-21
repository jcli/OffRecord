package com.swordriver.offrecord;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import swordriver.com.googledrivemodule.GoogleApiModel;
import timber.log.Timber;

import com.swordriver.offrecord.JCLogger.LogAreas;

/**
 * Created by jcli on 1/23/17.
 */

public class FragmentNotesList extends Fragment implements OffRecordMainActivity.ControllerServiceInterface, DataSourceNotes.NotesCallback{

    private DataSourceNotes mNotesSource;
    private AlertDialog mAddNoteDialog;
    private NotesListAdapter mNotesListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.tag(JCLogger.LogAreas.LIFECYCLE.s()).v("called.");
        View rootView = inflater.inflate(R.layout.fragment_notes_list, container, false);
        mNotesListAdapter = new NotesListAdapter(getActivity(), R.layout.fragment_notes_list_item);
        ListView notesListView = (ListView) rootView.findViewById(R.id.notesListView);
        notesListView.setAdapter(mNotesListAdapter);

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

    private class NotesListAdapter extends ArrayAdapter<GoogleApiModel.ItemInfo> {
        private int mResource;
        private Context mContext;

        public NotesListAdapter(Context context, int resource) {
            super(context, resource);
            mContext = context;
            mResource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            View row = inflater.inflate(mResource, parent, false);
            TextView title = (TextView) row.findViewById(R.id.note_title);
            TextView detail = (TextView) row.findViewById(R.id.note_detail);
            title.setText(getItem(position).readableTitle);
            if (getItem(position).meta.isFolder()) {
                detail.setText("Folder");
            }else{
                detail.setText("Note");
            }
            return row;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // callbacks and interfaces
    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void startProcessing(OffRecordMainService service) {
        mNotesSource = service.getNotesDataSource();
        mNotesSource.setListner(this);
        mNotesSource.init(service.getGoogleApiModel(null));
    }

    @Override
    public void updateListView(GoogleApiModel.FolderInfo notes) {
        Timber.tag(LogAreas.SECURE_NOTES.s()).v("Updating the note list.");
        if (mNotesListAdapter!=null){
            mNotesListAdapter.clear();
            mNotesListAdapter.addAll(notes.items);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // private helpers
    ////////////////////////////////////////////////////////////////////////////////

    private void setupFloatingButtonsRegular(final View rootView) {
        // find the floating menu
        final FloatingActionMenu menu = (FloatingActionMenu) rootView.findViewById(R.id.notesListMenu);

        // find the floating buttons
        final FloatingActionButton bAddNote = (FloatingActionButton) rootView.findViewById(R.id.noteButton1);
        final FloatingActionButton bAddFolder = (FloatingActionButton) rootView.findViewById(R.id.noteButton2);

        // config add note
        bAddNote.setLabelText("Add Note");
        bAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.close(true);
                addItemPopup("Note Name", false);
            }
        });

        // config add folder
        bAddFolder.setLabelText("Add Folder");
        bAddFolder.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                menu.close(true);
                addItemPopup("Folder Name", true);
            }
        });

    }

    private void addItemPopup(String promptText, final Boolean isFolder){
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(promptText);
            final EditText noteNameView= new EditText(getActivity());
            noteNameView.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(noteNameView);

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = noteNameView.getText().toString();
                    if (mNotesSource!=null) {
                        if (isFolder){
                            mNotesSource.addFolder(name);
                        }else {
                            mNotesSource.addNote(name);
                        }
                    }
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
