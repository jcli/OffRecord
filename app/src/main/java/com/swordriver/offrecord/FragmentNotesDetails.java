package com.swordriver.offrecord;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import swordriver.com.googledrivemodule.GoogleApiModel;
import timber.log.Timber;
import com.swordriver.offrecord.JCLogger.LogAreas;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jcli on 3/16/17.
 */

public class FragmentNotesDetails extends Fragment implements OffRecordMainActivity.ControllerServiceInterface, DataSourceNotes.NotesCallback, FragmentBackStackPressed{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.tag(LogAreas.LIFECYCLE.s()).v("called.");
        View rootView = inflater.inflate(R.layout.fragment_notes_detail, container, false);
        mNoteDetailAdapter = new NoteDetailAdapter(getActivity(), R.layout.fragment_notes_detail_lines,
                new ArrayList<DataSourceNotes.NoteItem>());
//        mNoteDetailAdapter = new NoteDetailAdapter(getActivity(), R.layout.fragment_notes_detail_lines);
        ListView noteDetailView = (ListView) rootView.findViewById(R.id.notesDetailListView);
        noteDetailView.setAdapter(mNoteDetailAdapter);
        //noteDetailView.setOnItemLongClickListener(new LineLongClick());
//        noteDetailView.setOnItemClickListener(new LineClick());
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
    public void onPause(){
        Timber.tag(LogAreas.LIFECYCLE.s()).v("called.");
//        if (activeLineEditText!=null &&
//                !mNoteDetailAdapter.getItem(activeLineIndex)
//                        .equals(activeLineEditText.getText().toString())){
//            mNoteDetailAdapter.setItem(activeLineIndex, activeLineEditText.getText().toString());

//            activeLineEditText.setOnFocusChangeListener(null);
//        }
        mNotesSource.writeNote(mNoteIndex, mNoteDetailAdapter.getList());
        OffRecordMainActivity activity = (OffRecordMainActivity) getActivity();
        activity.removeServiceListener(this);
        if (mNotesSource!=null) mNotesSource.removeListner(this);
        super.onPause();
    }

    @Override
    public void onStop(){  // do all clean up here
        super.onStop();
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // public api
    //////////////////////////////////////////////////////////////////////////////////////////

    public void setNoteIndex(int index){
        mNoteIndex=index;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // callbacks and interfaces
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void updateListView(GoogleApiModel.FolderInfo notes) {
        // this is called when init is done.  now we can get the detailed note
        Timber.tag(LogAreas.SECURE_NOTES.s()).v("called.");
        mNotesSource.readNote(mNoteIndex);
    }

    @Override
    public void updateNoteDetail(List<DataSourceNotes.NoteItem> content) {
        Timber.tag(LogAreas.SECURE_NOTES.s()).v("called.");
        if (mNoteDetailAdapter!=null){
            mNoteDetailAdapter.clear();
            mNoteDetailAdapter.addAll(content);
        }
    }

    @Override
    public boolean onBackPressed() {
        int childBackStackCount = getParentFragment().getChildFragmentManager().getBackStackEntryCount();
        if (childBackStackCount>0) {
            getParentFragment().getChildFragmentManager().popBackStack();
            return true;
        }else {
            return false;
        }
    }

    @Override
    public void startProcessing(OffRecordMainService service) {
        Timber.tag(LogAreas.SECURE_NOTES.s()).v("MainActivity called startProcessing()");
        mNotesSource = service.getNotesDataSource();
        mNotesSource.setListner(this);
        mNotesSource.init(service.getGoogleApiModel(null));
    }

    private class LineLongClick implements AdapterView.OnItemLongClickListener{

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            Timber.tag(LogAreas.SECURE_NOTES.s()).v("editing ...");
            EditText lineEdit = (EditText) view.findViewById(R.id.note_detail_line);
            //lineEdit.setFocusable(true);
            lineEdit.setFocusableInTouchMode(true);
            lineEdit.requestFocus();
            return false;
        }
    }

    private class LineClick implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Timber.tag(LogAreas.SECURE_NOTES.s()).v("clicked ...");
            EditText lineEdit = (EditText) view.findViewById(R.id.note_detail_line);
            lineEdit.setFocusable(true);
            lineEdit.setFocusableInTouchMode(true);
            lineEdit.requestFocus();
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // private class and helpers
    //////////////////////////////////////////////////////////////////////////////////////////

    private NoteDetailAdapter mNoteDetailAdapter;
    //private ArrayList<String> mContent=new ArrayList<>();
    private int mNoteIndex=0;
    private DataSourceNotes mNotesSource;

    private int activeLineIndex;
    private EditText activeLineEditText;

    private static Timber.Tree Trace(){
        return Timber.tag(LogAreas.SECURE_NOTES.s());
    }

    private class NoteDetailAdapter extends ArrayAdapter<DataSourceNotes.NoteItem> {
        private int mResource;
        private Context mContext;
        private List<DataSourceNotes.NoteItem> mContent;
        private class ViewTag {
            DataSourceNotes.NoteItem item;
            EditText editText;
            ImageButton deleteButton;
            ImageButton copyButton;
            public ViewTag(DataSourceNotes.NoteItem i, EditText e, ImageButton d, ImageButton c){
                item = i;
                editText = e;
                deleteButton = d;
                copyButton = c;
            }
        }

        public NoteDetailAdapter(Context context, int resource, List<DataSourceNotes.NoteItem> objects) {
            super(context, resource, objects);
            mContext = context;
            mResource = resource;
            mContent = objects;
        }

//        public NoteDetailAdapter(Context context, int resource) {
//            super(context, resource);
//            mContext = context;
//            mResource = resource;
//        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            View row = inflater.inflate(mResource, parent, false);
            EditText lineEdit = (EditText) row.findViewById(R.id.note_detail_line);
            ImageButton deleteButton = (ImageButton) row.findViewById(R.id.delete_line_button);
            ImageButton copyButton = (ImageButton) row.findViewById(R.id.copy_line_button);
            ViewTag currentViewTag = new ViewTag(getItem(position), lineEdit, deleteButton, copyButton);

            lineEdit.setText(getItem(position).line);
            lineEdit.setTag(currentViewTag);
            lineEdit.setBackground(null);
            lineEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    ViewTag tag = (ViewTag) v.getTag();
                    EditText currentEditText = (EditText) v;
                    // try to match original
                    if (!hasFocus && !tag.item.line.equals(currentEditText.getText().toString())) {
                        // line changed, write back to
                        tag.item.line = currentEditText.getText().toString();
                        // see if I need a new line
                        if (!getItem(getCount()-1).line.equals("")){
                            Trace().v("Adding a new line.");
                            mNoteDetailAdapter.add(new DataSourceNotes.NoteItem(""));
                            mNoteDetailAdapter.notifyDataSetChanged();
                        }
                    }
                }
            });

            deleteButton.setTag(currentViewTag);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTag tag = (ViewTag) v.getTag();
                    remove(tag.item);
                    if (!getItem(getCount()-1).line.equals("")) {
                        Trace().v("Adding a new line.");
                        mNoteDetailAdapter.add(new DataSourceNotes.NoteItem(""));
                    }
                    mNoteDetailAdapter.notifyDataSetChanged();
                }
            });
            return row;
        }

        public List<DataSourceNotes.NoteItem> getList(){
            return mContent;
        }
    }

}
