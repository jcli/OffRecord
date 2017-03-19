package com.swordriver.offrecord;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import swordriver.com.googledrivemodule.GoogleApiModel;
import timber.log.Timber;

import com.swordriver.offrecord.JCLogger.LogAreas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by jcli on 1/23/17.
 */

public class FragmentNotesList extends Fragment implements OffRecordMainActivity.ControllerServiceInterface, DataSourceNotes.NotesCallback, FragmentBackStackPressed{

    private DataSourceNotes mNotesSource;
    private AlertDialog mAddNoteDialog;
    private NotesListAdapter mNotesListAdapter;
    private Set<Integer> mCurrentSelections;
    private ItemClickListener mItemClickListner;
    private ItemSelectListener mItemSelectListner;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.tag(JCLogger.LogAreas.LIFECYCLE.s()).v("called.");
        View rootView = inflater.inflate(R.layout.fragment_notes_list, container, false);
        mNotesListAdapter = new NotesListAdapter(getActivity(), R.layout.fragment_notes_list_item);
        ListView notesListView = (ListView) rootView.findViewById(R.id.notesListView);
        notesListView.setAdapter(mNotesListAdapter);
        mItemClickListner= new ItemClickListener();
        mItemSelectListner = new ItemSelectListener();
        notesListView.setOnItemClickListener(mItemClickListner);
        setupFloatingButtonsRegular(rootView);

        return rootView;
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void onResume(){
        super.onResume();
        // register itself with main activity
        OffRecordMainActivity activity = (OffRecordMainActivity) getActivity();
        activity.setServiceListener(this);
    }

    @Override
    public void onStop(){  // do all clean up here
        super.onStop();
        OffRecordMainActivity activity = (OffRecordMainActivity) getActivity();
        activity.removeServiceListener(this);
        if (mAddNoteDialog!=null) mAddNoteDialog.cancel();
        if (mNotesSource!=null) mNotesSource.removeListner(this);
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
        Timber.tag(LogAreas.SECURE_NOTES.s()).v("MainActivity called startProcessing()");
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

    @Override
    public void updateNoteDetail(List<String> content) {

    }

    @Override
    public boolean onBackPressed() {
        Timber.tag(LogAreas.SECURE_NOTES.s()).v("back pressed.");
        return mNotesSource.goUp();
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
        final FloatingActionButton bSelect = (FloatingActionButton) rootView.findViewById(R.id.noteButton3);

        // configure add note
        bAddNote.setVisibility(VISIBLE);
        bAddNote.setEnabled(true);
        bAddNote.setLabelText("Add Note");
        bAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.close(true);
                addItemPopup("Note Name", false);
            }
        });

        // configure add folder
        bAddFolder.setLabelText("Add Folder");
        bAddFolder.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                menu.close(true);
                addItemPopup("Folder Name", true);
            }
        });

        // configure item selections
        bSelect.setLabelText("Select");
        bSelect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                menu.close(true);
                mCurrentSelections=new HashSet<Integer>();
                menu.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
                    @Override
                    public void onMenuToggle(boolean opened) {
                        if (!opened) {
                            // config the floating buttons for selection
                            setupFloatingButtonsSelection(rootView);
                            menu.setOnMenuToggleListener(null);
                            ListView itemListView = (ListView) rootView.findViewById(R.id.notesListView);
                            itemListView.setOnItemClickListener(mItemSelectListner);
                        }
                    }
                });
            }
        });
    }

    private void setupFloatingButtonsSelection(final View rootView) {
        // find the floating menu
        final FloatingActionMenu menu = (FloatingActionMenu) rootView.findViewById(R.id.notesListMenu);

        // find the floating buttons
        final FloatingActionButton bHidden = (FloatingActionButton) rootView.findViewById(R.id.noteButton1);
        final FloatingActionButton bDelete = (FloatingActionButton) rootView.findViewById(R.id.noteButton2);
        final FloatingActionButton bCancel = (FloatingActionButton) rootView.findViewById(R.id.noteButton3);

        // hide the first button
        bHidden.setEnabled(false);
        bHidden.setVisibility(GONE);

        // setup delete button
        bDelete.setLabelText("Delete Items");
        bDelete.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                menu.close(true);
                mNotesSource.deleteItems(mCurrentSelections);
                mCurrentSelections=null;
                menu.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
                    @Override
                    public void onMenuToggle(boolean opened) {
                        if (!opened) {
                            // config the floating buttons for regular
                            setupFloatingButtonsRegular(rootView);
                            menu.setOnMenuToggleListener(null);
                            ListView itemListView = (ListView) rootView.findViewById(R.id.notesListView);
                            itemListView.setOnItemClickListener(mItemClickListner);
                        }
                    }
                });
            }
        });

        // setup cancel button
        bCancel.setLabelText("Cancel");
        bCancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                menu.close(true);
                mCurrentSelections=null;
                menu.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
                    @Override
                    public void onMenuToggle(boolean opened) {
                        if (!opened) {
                            // config the floating buttons for regular
                            setupFloatingButtonsRegular(rootView);
                            menu.setOnMenuToggleListener(null);
                            ListView itemListView = (ListView) rootView.findViewById(R.id.notesListView);
                            itemListView.setOnItemClickListener(mItemClickListner);
                            mNotesSource.requestUpdate();
                        }
                    }
                });
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

    private class ItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            GoogleApiModel.ItemInfo item = mNotesListAdapter.getItem(position);
            if (item.meta.isFolder()){
                // go into folder
                mNotesSource.gotoFolder(item.meta.getDriveId().asDriveFolder());
            }else{
                // edit the item
                FragmentTransaction transaction = getParentFragment().getChildFragmentManager().beginTransaction();
                FragmentNotesDetails details = new FragmentNotesDetails();
                details.setNoteIndex(position);
                transaction.replace(R.id.notes_child_fragment, details).addToBackStack(null).commit();
            }
        }
    }

    private class ItemSelectListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!mCurrentSelections.contains(position)){
                //select
                mCurrentSelections.add(position);
                view.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.light_green));
            }else {
                // unselect
                mCurrentSelections.remove(position);
                view.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.white_transparent));
            }
        }
    }

}
