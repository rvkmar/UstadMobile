/*
    This file is part of Ustad Mobile.

    Ustad Mobile Copyright (C) 2011-2014 UstadMobile Inc.

    Ustad Mobile is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version with the following additional terms:

    All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
    LLC must be kept as they are in the original distribution.  If any new
    screens are added you must include the Ustad Mobile logo as it has been
    used in the original distribution.  You may not create any new
    functionality whose purpose is to diminish or remove the Ustad Mobile
    Logo.  You must leave the Ustad Mobile logo as the logo for the
    application to be used with any launcher (e.g. the mobile app launcher).

    If you want a commercial license to remove the above restriction you must
    contact us.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Ustad Mobile is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

 */

package com.ustadmobile.port.android.view;

import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.toughra.ustadmobile.R;

import com.ustadmobile.core.controller.CatalogController;
import com.ustadmobile.core.controller.CatalogEntryInfo;
import com.ustadmobile.core.impl.UMTransferJob;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.opds.UstadJSOPDSFeed;
import com.ustadmobile.port.android.impl.UstadMobileSystemImplAndroid;


public class SplashScreenActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();
        ((UstadMobileSystemImplAndroid) impl).setCurrentContext(this);

        String httpRoot = "http://192.168.0.49:5062";
        String acquireURL = httpRoot + "/acquire.opds";

        long totalSize = -1;

        /*
        try {
            UstadJSOPDSFeed feed = CatalogController.getCatalogByURL(acquireURL,
                CatalogController.SHARED_RESOURCE, null, null, CatalogController.CACHE_ENABLED);
            UMTransferJob job = CatalogController.acquireCatalogEntries(feed.entries, null, null,
                    CatalogController.SHARED_RESOURCE, CatalogController.CACHE_ENABLED);
            totalSize = job.getTotalSize();
        }catch(Exception e) {
            e.printStackTrace();
        }
        */

        CatalogEntryInfo testInfo = new CatalogEntryInfo();
        testInfo.acquisitionStatus = CatalogEntryInfo.ACQUISITION_STATUS_ACQUIRED;
        testInfo.srcURLs = new String[]{"http://www.server1.com/file.epub",
                "http://www.server2.com/file.epub"};
        testInfo.fileURI = "/some/file/path/file.epub";
        testInfo.mimeType = "application/epub+zip";

        String infoStr = testInfo.toString();
        CatalogEntryInfo restoreEntry = CatalogEntryInfo.fromString(infoStr);


        impl.startUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
