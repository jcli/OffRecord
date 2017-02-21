package com.swordriver.offrecord;

import java.util.ArrayList;
import java.util.Timer;

import swordriver.com.googledrivemodule.GoogleApiModel;
import timber.log.Timber;
import com.swordriver.offrecord.JCLogger.LogAreas;

/**
 * Created by jcli on 1/23/17.
 */

public class DataSourceNotes {

    public interface NotesCallback{
        void updateListView (GoogleApiModel.FolderInfo notes);
    }
    private NotesCallback mListner;

    /////////////////////////////////////////////////////////
    // private APIs
    /////////////////////////////////////////////////////////
    private final String SECURE_NOTE_ROOT = "Notes";
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

    public void requestUpdate(){
        if (mListner!=null) mListner.updateListView(mCurrentFolder);
    }

    synchronized public void init(GoogleApiModel gmodel){
        mGModel = gmodel;
        mGModel.listFolder(mGModel.getAppRootFolder(), new GoogleApiModel.ListFolderCallback(){
            private void processRoot(GoogleApiModel.FolderInfo info){
                if (info!=null) mNoteRoot = info;
                if (mCurrentFolder==null) {
                    mCurrentFolder=mNoteRoot;
                    requestUpdate();
                }
            }
            @Override
            public void callback(GoogleApiModel.FolderInfo info) {
                for (GoogleApiModel.ItemInfo item : info.items){
                    if (item.readableTitle.equals(SECURE_NOTE_ROOT) && item.meta.isFolder()){
                        // found note folder
                        mGModel.listFolder(item.meta.getDriveId().asDriveFolder(), new GoogleApiModel.ListFolderCallback() {
                            @Override
                            public void callback(GoogleApiModel.FolderInfo info) {
                                processRoot(info);
                            }
                        });
                        return;
                    }
                }
                Timber.tag(LogAreas.SECURE_NOTES.s()).v("Creating the Notes folder");
                mGModel.createFolderInFolder(SECURE_NOTE_ROOT, mGModel.getAppRootFolder(), true, new GoogleApiModel.ListFolderCallback() {
                    @Override
                    public void callback(GoogleApiModel.FolderInfo info) {
                        processRoot(info);
                    }
                });
            }
        });
    }

    synchronized public void addNote(String name){
        if (mGModel!=null && mCurrentFolder!=null){
            mGModel.createTxtFileInFolder(name, mCurrentFolder.folder, new GoogleApiModel.ListFolderCallback() {
                @Override
                public void callback(GoogleApiModel.FolderInfo info) {
                    mCurrentFolder=info;
                    requestUpdate();
                }
            });
        }
    }

    synchronized public void addFolder(String name){
        if (mGModel!=null && mCurrentFolder!=null){
            mGModel.createFolderInFolder(name, mCurrentFolder.folder, false, new GoogleApiModel.ListFolderCallback(){
                @Override
                public void callback(GoogleApiModel.FolderInfo info) {
                    mCurrentFolder=info;
                    requestUpdate();
                }
            });
        }
    }
}
