package com.swordriver.offrecord;

import android.support.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;

import swordriver.com.googledrivemodule.GoogleApiModel;
import timber.log.Timber;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.DriveId;
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
        if (mCurrentFolder==null){
            init(mGModel);
            return;
        }
        if (mListner!=null) {
            // sort current folder first
            Arrays.sort(mCurrentFolder.items, new Comparator<GoogleApiModel.ItemInfo>() {
                @Override
                public int compare(GoogleApiModel.ItemInfo lhs, GoogleApiModel.ItemInfo rhs) {
                    if (lhs.meta.isFolder() == rhs.meta.isFolder()) {
                        return lhs.readableTitle.compareTo(rhs.readableTitle);
                    } else {
                        if (lhs.meta.isFolder()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                }
            });
            mListner.updateListView(mCurrentFolder);
        }
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

    synchronized public void deleteItems(Set<Integer> selections){
        Deque<DriveId> items = new ArrayDeque<>();
        for (Integer selection : selections){
            items.add(mCurrentFolder.items[selection].meta.getDriveId());
        }
        mGModel.deleteMultipleItems(items, new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    Timber.tag(LogAreas.SECURE_NOTES.s()).v("multiple items deleted.");
                }
                mGModel.listFolder(mCurrentFolder.folder, new GoogleApiModel.ListFolderCallback() {
                    @Override
                    public void callback(GoogleApiModel.FolderInfo info) {
                        mCurrentFolder=info;
                        requestUpdate();
                    }
                });
            }
        });
    }
}
