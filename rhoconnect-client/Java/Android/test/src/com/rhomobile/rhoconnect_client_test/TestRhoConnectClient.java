package com.rhomobile.rhoconnect_client_test;

import java.util.HashMap;
import java.util.Map;

import android.content.pm.ApplicationInfo;
import android.test.AndroidTestCase;

import com.rhomobile.rhodes.RhoLogConf;
import com.rhomobile.rhodes.file.RhoFileApi;
import com.rhomobile.rhoconnect.RhoConnectClient;
import com.rhomobile.rhoconnect.RhoConnectObjectNotify;
import com.rhomobile.rhoconnect.RhomModel;
import com.rhomobile.rhoconnect.RhoConnectNotify;

public class TestRhoConnectClient extends AndroidTestCase {
    private static final String TAG = TestRhoConnectClient.class.getSimpleName();

	private final String SYNC_URL = "http://rhodes-store-server.heroku.com/application";
	
	class ObjectNotifyDelegate implements RhoConnectObjectNotify.IDelegate
	{
		RhoConnectObjectNotify mNotify;
		@Override
		public void call(RhoConnectObjectNotify notify) {
			mNotify = notify;
		}
	}

    RhoConnectClient mClient;
    RhomModel mModels[];
    RhomModel mProduct;
    
    @Override
    protected void setUp()
    {
		System.loadLibrary("rhoconnectclient");

		ApplicationInfo appInfo = this.getContext().getApplicationInfo();
		try {
			RhoFileApi.initRootPath(appInfo.dataDir, appInfo.sourceDir);
			RhoFileApi.init(this.getContext());
			
			RhoLogConf.setMinSeverity(0);
			RhoLogConf.setEnabledCategories("*");
			
			RhoConnectClient.nativeInit();
		} catch (Exception e) {
			fail(e.getMessage());
		}
		
    	mClient = new RhoConnectClient();

    	mModels = new RhomModel[]{
    			new RhomModel("Perftest", RhomModel.SYNC_TYPE_NONE),
				new RhomModel("Customer", RhomModel.SYNC_TYPE_INCREMENTAL),
				new RhomModel("Product", RhomModel.SYNC_TYPE_INCREMENTAL)
			};
    	mProduct = mModels[2];

		mClient.initialize(mModels);
        mClient.setThreadedMode(false);
        mClient.setPollInterval(0);
        mClient.setSyncServer(SYNC_URL);
        mClient.setBulkSyncState(1);
    }
    
    @Override
    protected void tearDown()
    {
        mClient.databaseFullResetAndLogout();
    	mClient.close();
    }
    
    public void testInitiallyLoggedOut()
    {
        mClient.databaseFullResetAndLogout();
    	assertFalse(mClient.isLoggedIn());
    }
    public void testLogin()
    {
        RhoConnectNotify notify = mClient.loginWithUserSync("", "");
        assertEquals(notify.getErrorCode(), 0);
        assertTrue(mClient.isLoggedIn());
    }
    public void testSyncProductByName()
    {
    	testLogin();
    	RhoConnectNotify notify = mProduct.sync();
    	assertEquals(notify.getErrorCode(), 0);
    }
    
    public void testSyncAll()
    {
    	testLogin();
    	RhoConnectNotify notify = mClient.syncAll();
    	assertEquals(notify.getErrorCode(), 0);
    }
    
    public void testCreateNewProduct()
    {
    	Map<String, String> item = new HashMap<String, String>();
    	item.put("name", "AndroidTest");
    	
    	mProduct.create(item);
    	
    	assertTrue(item.containsKey("object"));
    	assertTrue(item.containsKey("source_id"));
    	
    	Map<String, String> item2 = mModels[2].find(item.get("object"));
    	assertTrue(item2 != null);
    	assertEquals(item.get("name"), item2.get("name"));
    	
    }
    
    public void testCreateObjectNotify()
    {
    	Map<String, String> item = new HashMap<String, String>();
    	item.put("name", "AndroidTest2");
    	
    	mProduct.create(item);
    	
    	assertTrue(item.containsKey("object"));
    	assertTrue(item.containsKey("source_id"));
    	
    	Map<String, String> item2 = mModels[2].find(item.get("object"));
    	assertNotNull(item2);
    	assertEquals(item.get("name"), item2.get("name"));
    	
    	ObjectNotifyDelegate objectCallback = new ObjectNotifyDelegate();
    	mClient.setObjectNotification(objectCallback);
    	mClient.addObjectNotify(Integer.parseInt(item.get("source_id")), item.get("object"));
    	
    	testSyncProductByName();
    	
    	assertNotNull(objectCallback.mNotify);
    	
    	String[] createdObjects = objectCallback.mNotify.getCreatedObjects();
    	assertNotNull(createdObjects);
    	
    	int[] createdSourceIds = objectCallback.mNotify.getCreatedSourceIds();
    	assertNotNull(createdSourceIds);
    	
    	assertEquals(createdObjects[0], item.get("object"));
    }

}