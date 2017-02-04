package com.swordriver.offrecord;

import java.util.ArrayList;

import swordriver.com.googledrivemodule.GoogleApiModel;

/**
 * Created by jcli on 1/23/17.
 */

public class DataSourceNotes {

    static public class NoteInfo{

    }

    public interface NotesCallback{
        void updateListView (ArrayList<NoteInfo> notes);
    }
    private NotesCallback mListner;

    /////////////////////////////////////////////////////////
    // private APIs
    /////////////////////////////////////////////////////////
    private GoogleApiModel mGModel;
    private GoogleApiModel.FolderInfo mNoteRoot;
    private GoogleApiModel.FolderInfo mCurrentFolder;

    /////////////////////////////////////////////////////////
    // public APIs
    /////////////////////////////////////////////////////////

    public void setListner(NotesCallback listner){
        mListner=listner;
    }
    public void removeListner(){
        mListner=null;
    }

    public void init(GoogleApiModel gmodel){
        mGModel = gmodel;

    }
}
