package com.swordriver.offrecord;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

import swordriver.com.googledrivemodule.GoogleApiModel;
import timber.log.Timber;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.gson.reflect.TypeToken;
import com.swordriver.offrecord.JCLogger.LogAreas;

/**
 * Created by jcli on 1/23/17.
 */

public class DataSourceNotes {

    public interface NotesCallback{
        void updateListView (GoogleApiModel.FolderInfo notes);
        void updateNoteDetail (List<NoteItem> content);
    }
    private NotesCallback mListner;

    @Keep
    public static class NoteItem{
        public String line;
        public NoteItem(String l){
            line = l;
        }
    }

    /////////////////////////////////////////////////////////
    // private APIs
    /////////////////////////////////////////////////////////
    private final String SECURE_NOTE_ROOT = "Notes";
    private GoogleApiModel mGModel;
    private GoogleApiModel.FolderInfo mNoteRoot;
    private GoogleApiModel.FolderInfo mCurrentFolder;
//    private List<NoteItem> mCachedContent;
//    private GoogleApiModel.ItemInfo mCachedContentInfo;

    private boolean isValidFile(int itemIndex){
        if (mGModel==null || mCurrentFolder==null) return false;
        if (mCurrentFolder.items.length<=itemIndex) return false;
        if (mCurrentFolder.items[itemIndex].meta.isFolder()) return false;
        return true;
    }

    private void updateWithEmpty(){
        if (mListner!=null) {
            GoogleApiModel.FolderInfo info = new GoogleApiModel.FolderInfo();
            info.items = new GoogleApiModel.ItemInfo[0];
            mListner.updateListView(info);
        }else{
            Trace().v("no listner");
        }

    }

    private static Timber.Tree Trace(){
        return Timber.tag(LogAreas.SECURE_NOTES.s());
    }
    /////////////////////////////////////////////////////////
    // public APIs
    /////////////////////////////////////////////////////////

    public void setListner(NotesCallback listner){
        mListner=listner;
    }

    public void removeListner(NotesCallback listner){
        if (listner == mListner) mListner=null;
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
        }else{
            Trace().v("no listner");
        }
    }

    synchronized public void init(GoogleApiModel gmodel){
        Trace().v("called.");
        mGModel = gmodel;
        if (mGModel.getStatus()!= GoogleApiModel.GoogleApiStatus.INITIALIZED){
            mCurrentFolder=null;
            mNoteRoot=null;
            updateWithEmpty();
            return;
        }
        mGModel.listFolder(mGModel.getAppRootFolder(), new GoogleApiModel.ListFolderCallback(){
            private void processRoot(GoogleApiModel.FolderInfo info){
                if (info!=null) mNoteRoot = info;
                if (mCurrentFolder==null) {
                    mCurrentFolder=mNoteRoot;
                }
                requestUpdate();
            }
            @Override
            public void callback(GoogleApiModel.FolderInfo info) {
                for (GoogleApiModel.ItemInfo item : info.items){
                    if (item.readableTitle.equals(SECURE_NOTE_ROOT) && item.meta.isFolder()){
                        // found note folder
                        Trace().v("Found the Notes folder");
                        mGModel.listFolder(item.meta.getDriveId().asDriveFolder(), new GoogleApiModel.ListFolderCallback() {
                            @Override
                            public void callback(GoogleApiModel.FolderInfo info) {
                                processRoot(info);
                            }
                        });
                        return;
                    }
                }
                Trace().v("Creating the Notes folder");
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

    synchronized public void gotoFolder(DriveFolder folder){
        if (mGModel!=null && mCurrentFolder!=null){
            mGModel.listFolder(folder, new GoogleApiModel.ListFolderCallback() {
                @Override
                public void callback(GoogleApiModel.FolderInfo info) {
                    mCurrentFolder=info;
                    requestUpdate();
                }
            });
        }
    }

    synchronized public boolean goUp(){
        if (mCurrentFolder!=null &&
                !mCurrentFolder.folder.getDriveId().encodeToString().equals(mNoteRoot.folder.getDriveId().encodeToString())){
            gotoFolder(mCurrentFolder.parentFolder);
            return true;
        }else{
            return false;
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
                    Trace().v("multiple items deleted.");
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

    synchronized public void readNote(int itemIndex){
        if (!isValidFile(itemIndex)) return;
        final GoogleApiModel.ItemInfo itemInfo = mCurrentFolder.items[itemIndex];
        mGModel.readTxtFile(itemInfo, new GoogleApiModel.ReadTxtFileCallback() {
            @Override
            public void callback(String fileContent) {
                Type collectionType = new TypeToken<ArrayList<NoteItem>>(){}.getType();
                ArrayList<NoteItem> content = TheGson.getGson().fromJson(fileContent, collectionType);
                if (content==null)content = new ArrayList<NoteItem>();
                if (mListner!=null) mListner.updateNoteDetail(content);
            }
        });
    }

    synchronized public void writeNote(final int itemIndex, final List<NoteItem> content){
        if (!isValidFile(itemIndex)) return;
        // check against the cache to see if content changed.
//        if (mCachedContentInfo!=null && mCachedContentInfo.equals(mCurrentFolder.items[itemIndex])
//                && mCachedContent!=null && mCachedContent.size()==content.size()){
//            boolean mismatch=false;
//            for (int i=0; i<content.size(); i++){
//                Trace().v("cached: %s, matching: %s", mCachedContent.get(i).line, content.get(i).line);
//                if (!mCachedContent.get(i).line.equals(content.get(i).line)){
//                    mismatch=true;
//                    Trace().v("found mismatched content.");
//                    break;
//                }
//            }
//            if (!mismatch) return;
//        }
        String contentStr = TheGson.getGson().toJson(content);
        mGModel.writeTxtFile(mCurrentFolder.items[itemIndex], contentStr, new GoogleApiModel.WriteTxtFileCallback() {
            @Override
            public void callback(boolean success, Metadata newMeta) {
                if (success) {
                    Trace().v("Write file successful.");
                    mCurrentFolder.items[itemIndex].meta=newMeta;
                }else{
                    Trace().e("Write file not successful!");
                }
                readNote(itemIndex);
            }
        });
    }


}
