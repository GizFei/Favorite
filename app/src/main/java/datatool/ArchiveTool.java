package datatool;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.giz.favorite.MainActivity;
import com.giz.favorite.R;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.FavoriteItemLib;
import utility.RoundedBottomSheetDialog;
import viewtool.CustomToast;

import static android.content.Context.MODE_PRIVATE;

public class ArchiveTool {

    private static final String TAG = "ArchiveTool";
    private static ArchiveTool sArchiveTool;

    public static final String ARCHIVE_NAME_ALL = "所有收藏";
    public static final String ARCHIVE_NAME_STAR = "星标收藏";
    public static final String ARCHIVE_NAME_SELF = "我的原创";

    private static final String ARCHIVE_FILE = "archives.txt";

    public static ArchiveTool getInstance(){
        if(sArchiveTool == null){
            sArchiveTool = new ArchiveTool();
        }
        return sArchiveTool;
    }

    // 用户自定义收藏夹名称列表
    public List<String> getCustomArchiveTitleList(Context context){
        String archiveStr = FavoriteTool.getArchivePreferences(context);
        List<String> archiveTitleList = new ArrayList<>();
        if(archiveStr != null && !archiveStr.equals(""))
            archiveTitleList.addAll(Arrays.asList(archiveStr.split(",")));

        return archiveTitleList;
    }

    // 所有收藏夹（包含默认的三个）名称列表
    public List<String> getAllArchiveTitleList(Context context){
        List<String> archiveTitleList = getCustomArchiveTitleList(context);
        archiveTitleList.addAll(0, Arrays.asList(ARCHIVE_NAME_ALL,  ARCHIVE_NAME_STAR, ARCHIVE_NAME_SELF));

        return archiveTitleList;
    }

    // 用户自定义的收藏夹（包含标题和数量信息）
    public List<Archive> getCustomArchiveList(Context context){
        String archiveStr = FavoriteTool.getArchivePreferences(context);
//        String archiveStr = getArchiveString();
        List<String> archiveTitleList = new ArrayList<>();
        if(archiveStr != null && !archiveStr.equals(""))
            archiveTitleList.addAll(Arrays.asList(archiveStr.split(",")));

        Map<String, Archive> archiveMap = new HashMap<>();

        for(String a : archiveTitleList){
            Archive archive = new Archive();
            archive.title = a;
            archive.count = 0;
            archiveMap.put(a, archive);
        }

        List<FavoriteItem> itemList = FavoriteItemLib.get(context).getFavoriteItemList();
        List<Archive> archiveList = new ArrayList<>();

        for(FavoriteItem item : itemList){
            String itemArchive = item.getArchive();
            Archive archive = archiveMap.get(itemArchive);
            if(archive != null){
                archive.count += 1;
            }
        }
        for(String archiveTitle : archiveTitleList){
            archiveList.add(archiveMap.get(archiveTitle));
        }

        return archiveList;
    }

    public List<Archive> getAllArchiveList(Context context){
        String archiveStr = FavoriteTool.getArchivePreferences(context);
//        String archiveStr = getArchiveString();
        List<String> archiveTitleList = getAllArchiveTitleList(context);

        Map<String, Archive> archiveMap = new HashMap<>();

        for(String a : archiveTitleList){
            Archive archive = new Archive();
            archive.title = a;
            archive.count = 0;
            archiveMap.put(a, archive);
        }

        List<FavoriteItem> itemList = FavoriteItemLib.get(context).getFavoriteItemList();
        List<Archive> archiveList = new ArrayList<>();

        archiveMap.get(ARCHIVE_NAME_ALL).count = itemList.size();
        for(FavoriteItem item : itemList){
            String itemArchive = item.getArchive();
            Archive archive = archiveMap.get(itemArchive);
            if(archive != null){
                archive.count += 1;
            }
            if(item.isStarred()){
                archiveMap.get(ARCHIVE_NAME_STAR).count += 1;
            }
            if(item.getSource().equals(SourceApp.APP_SELF)){
                archiveMap.get(ARCHIVE_NAME_SELF).count += 1;
            }
        }
        for(String archiveTitle : archiveTitleList){
            archiveList.add(archiveMap.get(archiveTitle));
        }

        return archiveList;
    }

    public void showNewArchiveDialog(final Context context, @NonNull final OnNewArchiveDialogListener listener){
        listener.onShown();

        final List<String> archiveTitleList = getCustomArchiveTitleList(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_new_archive, null);
        final EditText titleEt = sheetView.findViewById(R.id.new_archive_title);

        final RoundedBottomSheetDialog bottomSheetDialog = new RoundedBottomSheetDialog(context, sheetView, R.style.BottomSheetDialog);
        bottomSheetDialog.setActionViewIDs(new RoundedBottomSheetDialog.OnActionViewListener() {
            @Override
            public void onClick(int viewID) {
                switch (viewID){
                    case R.id.new_archive_ok_btn:
                        if(titleEt != null){
                            String archiveTitle = titleEt.getText().toString();
                            if(archiveTitle.equals(""))
                                CustomToast.make(context, "名称不能为空").show();
                            else if(archiveTitle.equals("") || archiveTitle.equals(ARCHIVE_NAME_ALL) || archiveTitle.equals(ARCHIVE_NAME_STAR)
                                    || archiveTitle.equals(ARCHIVE_NAME_SELF)){
                                CustomToast.make(context, "不能使用内置名称").show();
                                titleEt.setText("");
                            }else if(archiveTitleList.contains(archiveTitle)){
                                CustomToast.make(context, "该收藏夹名已存在").show();
                            }else{
                                addArchive(context, archiveTitle);

                                bottomSheetDialog.dismiss();
                                listener.onArchived(archiveTitle);
                            }
                        }
                        break;
                    case R.id.new_archive_cancel_btn:
                        bottomSheetDialog.dismiss();
                        listener.onCancelled();
                        break;
                }
            }
        }, R.id.new_archive_ok_btn, R.id.new_archive_cancel_btn);

        bottomSheetDialog.show();
    }

    // 添加自定义收藏夹
    public void addArchive(Context context, String archive){
        String archiveStr = FavoriteTool.getArchivePreferences(context);
//        String archiveStr = getArchiveString();
        if(archiveStr == null || archiveStr.equals(""))
            archiveStr = archive;
        else
            archiveStr += "," + archive;
        FavoriteTool.setArchivePreferences(context, archiveStr);
//        saveArchiveString(archiveStr);
    }

    public void removeArchive(Context context, String archive){
        String archiveStr = FavoriteTool.getArchivePreferences(context);
//        String archiveStr = getArchiveString();

        if(archiveStr.contains(archive + ","))
            archiveStr = archiveStr.replace(archive + ",", "");
        else
            archiveStr = archiveStr.replace(archive, "");
        FavoriteTool.setArchivePreferences(context, archiveStr);
//        saveArchiveString(archiveStr);
        FavoriteItemLib.get(context).patchUpdateFavoriteItemByArchive(null, archive);
    }

    // 收藏夹相关操作
    public void renameArchive(Context context, String newArchive, String oldArchive){
        String archiveStr = FavoriteTool.getArchivePreferences(context);
        archiveStr = archiveStr.replace(oldArchive, newArchive);
        FavoriteTool.setArchivePreferences(context, archiveStr);
        FavoriteItemLib.get(context).patchUpdateFavoriteItemByArchive(newArchive, oldArchive);
//        String archiveStr = getArchiveString();
//        archiveStr = archiveStr.replace(oldArchive, newArchive);
//        saveArchiveString(archiveStr);
//        FavoriteItemLib.get(this).patchUpdateFavoriteItemByArchive(newArchive, oldArchive);
    }

    // 使用文件存储
    private void saveArchiveString(Context context, String archiveString){
        try {
            FileOutputStream outputStream = context.openFileOutput(ARCHIVE_FILE, MODE_PRIVATE);
            outputStream.write(archiveString.getBytes());
            outputStream.close();
        }catch (FileNotFoundException e) {
            Log.d(TAG, "saveArchiveString: file not found");
        }catch (IOException e2){
            Log.d(TAG, "saveArchiveString: io exception");
        }
    }
    private String getArchiveString(Context context){
        try {
            FileInputStream inputStream = context.openFileInput(ARCHIVE_FILE);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] bytes = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(bytes)) != -1){
                outputStream.write(bytes, 0, length);
            }
            inputStream.close();
            return outputStream.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public interface OnNewArchiveDialogListener{
        void onShown();
        void onArchived(String archiveTitle);
        void onCancelled();
    }
}
