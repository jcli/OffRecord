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
                new ArrayList<String>());
        ListView noteDetailView = (ListView) rootView.findViewById(R.id.notesDetailListView);
        noteDetailView.setAdapter(mNoteDetailAdapter);
        //noteDetailView.setOnItemLongClickListener(new LineLongClick());
        noteDetailView.setOnItemClickListener(new LineClick());
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
        super.onPause();
    }

    @Override
    public void onStop(){  // do all clean up here
        super.onStop();
        OffRecordMainActivity activity = (OffRecordMainActivity) getActivity();
        activity.removeServiceListener(this);
        if (mNotesSource!=null) mNotesSource.removeListner(this);
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
    public void updateNoteDetail(List<String> content) {
        Timber.tag(LogAreas.SECURE_NOTES.s()).v("called.");
        for (String item : content){
            Timber.tag(LogAreas.SECURE_NOTES.s()).v("line %s ", item);
        }
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

    private static Timber.Tree Trace(){
        return Timber.tag(LogAreas.SECURE_NOTES.s());
    }

    private class NoteDetailAdapter extends ArrayAdapter<String> {
        private int mResource;
        private Context mContext;
        private List<String> mContent;
        public NoteDetailAdapter(Context context, int resource, List<String> objects) {
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
            final EditText lineEdit = (EditText) row.findViewById(R.id.note_detail_line);
            lineEdit.setText(getItem(position));
            lineEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    // try to match original
                    if (!hasFocus && !getItem(position).equals(lineEdit.getText().toString())) {
                        // line changed, write back to
                        mContent.set(position, lineEdit.getText().toString());
                        mNotesSource.writeNote(mNoteIndex, mContent);
                        lineEdit.setOnFocusChangeListener(null);
                    }

                    if (hasFocus){
                        //mark this view.  Need to check data changed onPause.
                    }
                }
            });
            return row;
        }
    }

}
