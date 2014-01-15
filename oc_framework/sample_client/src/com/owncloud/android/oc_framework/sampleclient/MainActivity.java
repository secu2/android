package com.owncloud.android.oc_framework.sampleclient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import com.owncloud.android.oc_framework.accounts.AccountUtils;
import com.owncloud.android.oc_framework.network.webdav.OnDatatransferProgressListener;
import com.owncloud.android.oc_framework.network.webdav.OwnCloudClientFactory;
import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.operations.OnRemoteOperationListener;
import com.owncloud.android.oc_framework.operations.RemoteFile;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.remote.DownloadRemoteFileOperation;
import com.owncloud.android.oc_framework.operations.remote.ReadRemoteFolderOperation;
import com.owncloud.android.oc_framework.operations.remote.RemoveRemoteFileOperation;
import com.owncloud.android.oc_framework.operations.remote.UploadRemoteFileOperation;
import com.owncloud.android.oc_framework.utils.FileUtils;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnRemoteOperationListener, OnDatatransferProgressListener {
	
	private static String LOG_TAG = MainActivity.class.getCanonicalName();  
	
	private Handler mHandler;
	
	private WebdavClient mClient; 
	
	private FilesArrayAdapter mFilesAdapter;
	
	private View mFrame;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mHandler = new Handler();
        
    	Uri serverUri = Uri.parse(getString(R.string.server_base_url) + AccountUtils.WEBDAV_PATH_4_0);
    	mClient = OwnCloudClientFactory.createOwnCloudClient(serverUri, this, true);
    	mClient.setBasicCredentials(getString(R.string.username), getString(R.string.password));
    	
    	mFilesAdapter = new FilesArrayAdapter(this, R.layout.file_in_list);
    	((ListView)findViewById(R.id.list_view)).setAdapter(mFilesAdapter);
    	
    	// TODO move to background thread or task
    	AssetManager assets = getAssets();
		try {
			String sampleFileName = getString(R.string.sample_file_name); 
	    	File upFolder = new File(getCacheDir(), getString(R.string.upload_folder_path));
	    	upFolder.mkdir();
	    	File upFile = new File(upFolder, sampleFileName);
	    	FileOutputStream fos = new FileOutputStream(upFile);
	    	InputStream is = assets.open(sampleFileName);
	    	int count = 0;
	    	byte[] buffer = new byte[1024];
	    	while ((count = is.read(buffer, 0, buffer.length)) >= 0) {
	    		fos.write(buffer, 0, count);
	    	}
	    	is.close();
	    	fos.close();
		} catch (IOException e) {
			Toast.makeText(this, R.string.error_copying_sample_file, Toast.LENGTH_SHORT).show();
			Log.e(LOG_TAG, getString(R.string.error_copying_sample_file), e);
		}
		
		mFrame = findViewById(R.id.frame);
    }
    
    
    @Override
    public void onDestroy() {
    	File upFolder = new File(getCacheDir(), getString(R.string.upload_folder_path));
    	File upFile = upFolder.listFiles()[0];
    	upFile.delete();
    	upFolder.delete();
    	super.onDestroy();
    }
    
    
    public void onClickHandler(View button) {
    	switch (button.getId())	{
	    	case R.id.button_refresh:
	    		startRefresh();
	    		break;
	    	case R.id.button_upload:
	    		startUpload();
	    		break;
	    	case R.id.button_delete_remote:
	    		startRemoteDeletion();
	    		break;
	    	case R.id.button_download:
	    		startDownload();
	    		break;
	    	case R.id.button_delete_local:
	    		startLocalDeletion();
	    		break;
			default:
	    		Toast.makeText(this, R.string.youre_doing_it_wrong, Toast.LENGTH_SHORT).show();
    	}
    }
    
    private void startRefresh() {
    	ReadRemoteFolderOperation refreshOperation = new ReadRemoteFolderOperation(FileUtils.PATH_SEPARATOR);
    	refreshOperation.execute(mClient, this, mHandler);
    }
    
    private void startUpload() {
    	File upFolder = new File(getCacheDir(), getString(R.string.upload_folder_path));
    	File fileToUpload = upFolder.listFiles()[0]; 
    	String remotePath = FileUtils.PATH_SEPARATOR + fileToUpload.getName(); 
    	String mimeType = getString(R.string.sample_file_mimetype);
    	UploadRemoteFileOperation uploadOperation = new UploadRemoteFileOperation(fileToUpload.getAbsolutePath(), remotePath, mimeType);
    	uploadOperation.addDatatransferProgressListener(this);
    	uploadOperation.execute(mClient, this, mHandler);
    }
    
    private void startRemoteDeletion() {
    	File upFolder = new File(getCacheDir(), getString(R.string.upload_folder_path));
    	File fileToUpload = upFolder.listFiles()[0]; 
    	String remotePath = FileUtils.PATH_SEPARATOR + fileToUpload.getName();
    	RemoveRemoteFileOperation removeOperation = new RemoveRemoteFileOperation(remotePath);
    	removeOperation.execute(mClient, this, mHandler);
    }
    
    private void startDownload() {
    	File downFolder = new File(getCacheDir(), getString(R.string.download_folder_path));
    	downFolder.mkdir();
    	File upFolder = new File(getCacheDir(), getString(R.string.upload_folder_path));
    	File fileToUpload = upFolder.listFiles()[0];
    	String remotePath = FileUtils.PATH_SEPARATOR + fileToUpload.getName();
    	RemoteFile rFileToDownload = new RemoteFile(remotePath);
    	rFileToDownload.setLength(fileToUpload.length());
    	DownloadRemoteFileOperation downloadOperation = new DownloadRemoteFileOperation(rFileToDownload, downFolder.getAbsolutePath());
    	downloadOperation.addDatatransferProgressListener(this);
    	downloadOperation.execute(mClient, this, mHandler);
    }
    
    @SuppressWarnings("deprecation")
	private void startLocalDeletion() {
    	File downFolder = new File(getCacheDir(), getString(R.string.download_folder_path));
    	File downloadedFile = downFolder.listFiles()[0];
    	if (!downloadedFile.delete() && downloadedFile.exists()) {
    		Toast.makeText(this, R.string.error_deleting_local_file, Toast.LENGTH_SHORT).show();
    	} else {
    		((TextView) findViewById(R.id.download_progress)).setText("0%");
    		findViewById(R.id.frame).setBackgroundDrawable(null);
    	}
    }

	@Override
	public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
		if (!result.isSuccess()) {
			Toast.makeText(this, R.string.todo_operation_finished_in_fail, Toast.LENGTH_SHORT).show();
			Log.e(LOG_TAG, result.getLogMessage(), result.getException());
			
		} else if (operation instanceof ReadRemoteFolderOperation) {
			onSuccessfulRefresh((ReadRemoteFolderOperation)operation, result);
			
		} else if (operation instanceof UploadRemoteFileOperation ) {
			onSuccessfulUpload((UploadRemoteFileOperation)operation, result);
			
		} else if (operation instanceof RemoveRemoteFileOperation ) {
			onSuccessfulRemoteDeletion((RemoveRemoteFileOperation)operation, result);
			
		} else if (operation instanceof DownloadRemoteFileOperation ) {
			onSuccessfulDownload((DownloadRemoteFileOperation)operation, result);
			
		} else {
			Toast.makeText(this, R.string.todo_operation_finished_in_success, Toast.LENGTH_SHORT).show();
		}
	}

	private void onSuccessfulRefresh(ReadRemoteFolderOperation operation, RemoteOperationResult result) {
		mFilesAdapter.clear();
		List<RemoteFile> files = result.getData();
		if (files != null) {
			Iterator<RemoteFile> it = files.iterator();
			while (it.hasNext()) {
				mFilesAdapter.add(it.next());
			}
			mFilesAdapter.remove(mFilesAdapter.getItem(0));
		}
		mFilesAdapter.notifyDataSetChanged();
	}

	private void onSuccessfulUpload(UploadRemoteFileOperation operation, RemoteOperationResult result) {
		startRefresh();
	}

	private void onSuccessfulRemoteDeletion(RemoveRemoteFileOperation operation, RemoteOperationResult result) {
		startRefresh();
		TextView progressView = (TextView) findViewById(R.id.upload_progress);
		if (progressView != null) {
			progressView.setText("0%");
		}
	}

	@SuppressWarnings("deprecation")
	private void onSuccessfulDownload(DownloadRemoteFileOperation operation, RemoteOperationResult result) {
    	File downFolder = new File(getCacheDir(), getString(R.string.download_folder_path));
    	File downloadedFile = downFolder.listFiles()[0];
    	BitmapDrawable bDraw = new BitmapDrawable(getResources(), downloadedFile.getAbsolutePath());
    	mFrame.setBackgroundDrawable(bDraw);
	}

	@Override
	public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String fileName) {
		final long percentage = (totalToTransfer > 0 ? totalTransferredSoFar * 100 / totalToTransfer : 0);
		final boolean upload = fileName.contains(getString(R.string.upload_folder_path));
		Log.d(LOG_TAG, "progressRate " + percentage);
    	mHandler.post(new Runnable() {
            @Override
            public void run() {
				TextView progressView = null;
				if (upload) {
					progressView = (TextView) findViewById(R.id.upload_progress);
				} else {
					progressView = (TextView) findViewById(R.id.download_progress);
				}
				if (progressView != null) {
	    			progressView.setText(Long.toString(percentage) + "%");
				}
            }
        });
	}


	@Override
	public void onTransferProgress(long arg0) {
		// TODO Remove from library
	}
	
}
