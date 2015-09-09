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
package com.ustadmobile.port.j2me.impl;

import com.sun.lwuit.Form;
import com.ustadmobile.port.j2me.app.AppPref;
import com.ustadmobile.port.j2me.app.DeviceRoots;
import com.ustadmobile.port.j2me.app.FileUtils;
import com.ustadmobile.port.j2me.app.HTTPUtils;
import com.ustadmobile.core.impl.UMTransferJob;
import java.io.IOException;
import java.util.Hashtable;
import javax.microedition.io.Connector;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.port.j2me.app.UserPref;
import com.ustadmobile.core.impl.HTTPResult;
import com.ustadmobile.core.impl.UMLog;
import com.ustadmobile.core.impl.UMProgressEvent;
import com.ustadmobile.core.impl.UMProgressListener;
import com.ustadmobile.core.impl.ZipFileHandle;
import com.ustadmobile.core.opds.UstadJSOPDSEntry;
import com.ustadmobile.core.util.UMFileUtil;
import com.ustadmobile.core.util.UMIOUtils;
import com.ustadmobile.core.util.UMUtil;
import com.ustadmobile.core.util.URLTextUtil;
import com.ustadmobile.core.view.AppView;
import com.ustadmobile.port.j2me.impl.zip.ZipFileHandleJ2ME;
import com.ustadmobile.port.j2me.util.J2MEIOUtils;
import com.ustadmobile.port.j2me.view.AppViewJ2ME;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connection;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * @author varuna
 */
public class UstadMobileSystemImplJ2ME  extends UstadMobileSystemImpl {

    private UMLog umLogger;
    
    private AppViewJ2ME appView;
    
    private Form currentForm;
    
    private ZipFileHandle openZip;
    
    private String openZipURI;
    
    public static final String OPENZIP_PROTO = "zip:///";
    
    private Player player;
    public int volumeLevel=70;
    
    /**
     * The shared content dir according to 
     */
    private String sharedContentDir = null;
    
    public String getImplementationName() {
        return "J2ME";
    }

    public UstadMobileSystemImplJ2ME() {
        umLogger = new UMLogJ2ME();
        appView = new AppViewJ2ME(this);
    }
    
    /**
     * {@inheritDoc}
     */
    public void init() {
        super.init();
    }
    
    public static UstadMobileSystemImplJ2ME getInstanceJ2ME() {
        return (UstadMobileSystemImplJ2ME)mainInstance;
    }
    
    /**
     * This needs to be called so the system knows the current form
     * @param frm 
     */
    public void handleFormShow(Form frm) {
        l(UMLog.DEBUG, 525, frm.getTitle());
        if(this.currentForm != frm) {
            appView.dismissAll();
        }
        this.currentForm = frm;
    }
    
    public Form getCurrentForm() {
        return currentForm;
    }

    /**
     * {@inheritDoc} 
     */
    public boolean dirExists(String dirURI) throws IOException {
        l(UMLog.DEBUG, 527, dirURI);
        
        dirURI = dirURI.trim();
        if (!dirURI.endsWith("/")){
            dirURI += '/';
            l(UMLog.DEBUG, 529, dirURI);
        }
        
        boolean exists = false;
        FileConnection fc = null;
        IOException ioe = null;
        try {
            fc = (FileConnection) Connector.open(dirURI, 
                Connector.READ_WRITE);
            exists = fc.exists() && fc.isDirectory();
        }catch(IOException e) {
            ioe = e;
            l(UMLog.ERROR, 126, dirURI, e);
        }catch(SecurityException se) {
            ioe = new IOException(PREFIX_SECURITY_EXCEPTION +se.toString());
            l(UMLog.ERROR, 126, dirURI, se);
        }finally {
            J2MEIOUtils.closeConnection(fc);
        }
        
        UMIOUtils.throwIfNotNullIO(ioe);
        return exists;
    }

    /**
     * {@inheritDoc} 
     */
    public UMTransferJob downloadURLToFile(String url, String fileURI, 
            Hashtable headers) {
        return new DownloadJob(url, fileURI, this);
    }

    public void setActiveUser(String username) {
        super.setActiveUser(username);
        l(UMLog.DEBUG, 531, username);
        AppPref.addSetting("CURRENTUSER", username);
        UserPref.setActiveUser(username);
        
    }

    public void setUserPref(String key, String value) {
        l(UMLog.DEBUG, 533, key + '=' + value);
        UserPref.addSetting(key, value);
    }

    public void saveUserPrefs() {
        
    }
    
    /**
     * {@inheritDoc} 
     */
    public String getSharedContentDir(){ 
        //This will be in something like ustadmobileContent
        //appData is different
        try{
            DeviceRoots dt = FileUtils.getBestRoot();
            String sharedContentDir = FileUtils.joinPath(dt.path, 
                    FileUtils.USTAD_CONTENT_DIR);
            
            //Check if it is created. If it isnt, create it.       
            if(FileUtils.createFileOrDir(sharedContentDir, 
                    Connector.READ_WRITE, true)){
                return sharedContentDir;
            }
            
            //Return null if it doens't exist.
            if (!FileUtils.checkDir(sharedContentDir)){
                return null;
            }
        }catch (Exception e){}
        return null;
    }
    
    public String getUserContentDirectory(String username){
        try{
            getLogger().l(UMLog.DEBUG, 507, username);
            DeviceRoots dt = FileUtils.getBestRoot();
            String toSlashOrNot="";
            if (dt.path.endsWith("//")){
                toSlashOrNot="";
            }else if(dt.path.endsWith("/")){
                toSlashOrNot="";
            }else{
                toSlashOrNot = "/";
            }
            String sharedUserContentDir = dt.path + toSlashOrNot + 
                    FileUtils.USTAD_CONTENT_DIR + FileUtils.FILE_SEP + username;
            
            getLogger().l(UMLog.DEBUG, 507, "dir: " + sharedUserContentDir);
            
            
            //Check if it is created. If it isn't, create it.
            /*
            if(FileUtils.createFileOrDir(sharedUserContentDir, 
                    Connector.READ_WRITE, true)){
                getLogger().l(UMLog.DEBUG, 507, "dir made ok");
                return sharedUserContentDir;
            }
            */
            return sharedUserContentDir;
        }catch (Exception e){
            getLogger().l(UMLog.DEBUG, 507, "exception in getUserContentDirectory", e);
        }
        return null;
    }
    
    public String getSystemLocale(){
        String locale = System.getProperty("microedition.locale");
        l(UMLog.DEBUG, 535, locale);
        return locale;
    }
    
    public Hashtable getSystemInfo(){
        Hashtable systemInfo = new Hashtable();
        
        systemInfo.put("platform", 
                System.getProperty("microedition.platform"));
        systemInfo.put("encoding", 
                System.getProperty("microedition.encoding"));
        systemInfo.put("configuration", 
                System.getProperty("microedition.configuration"));
        systemInfo.put("profiles", 
                System.getProperty("microedition.profiles"));
        systemInfo.put("locale", 
                System.getProperty("microedition.locale"));
        systemInfo.put("memorytotal", 
                Long.toString(Runtime.getRuntime().totalMemory()));
        systemInfo.put("memoryfree", 
                Long.toString(Runtime.getRuntime().freeMemory()));
        return systemInfo;
    }
    
    public long modTimeDifference(String fileURI1, String fileURI2){
        try{
            long file1LastModified = FileUtils.getLastModified(fileURI1);
            long file2LastModified = FileUtils.getLastModified(fileURI2);
            if (file1LastModified != -1 || file2LastModified != -1 ){
                long difference = file1LastModified - file2LastModified;
                return difference;
            }
        }catch(Exception e){}
        return -1;
    }
        
    /**
     * {@inheritDoc }
     */
    public boolean fileExists(String fileURI) throws IOException{
        l(UMLog.DEBUG, 537, fileURI);
        boolean fileExists = false;
        IOException e = null;
        try {
            FileConnection fc = (FileConnection)Connector.open(fileURI,
                Connector.READ);
            fileExists = fc.exists();
        }catch(IOException ioe) {
            e = ioe;
        }catch(SecurityException se) {
            e = new IOException(PREFIX_SECURITY_EXCEPTION  + se.toString());
        }
        
        UMIOUtils.logAndThrowIfNotNullIO(e, UMLog.ERROR, volumeLevel, fileURI);
        
        return fileExists;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean removeFile(String fileURI) {
        l(UMLog.DEBUG, 537, fileURI);
        boolean success = false;
        FileConnection con = null;
        
        try {
            con = (FileConnection)Connector.open(fileURI, Connector.READ_WRITE);
            if(con.exists()) {
                con.delete();
            }
            success = true;
        } catch (IOException ioe) {
            l(UMLog.ERROR, 129, fileURI, ioe);
        }catch(SecurityException se) {
            l(UMLog.ERROR, 129, fileURI, se);
        }finally {
            J2MEIOUtils.closeConnection(con);
        }
        
        return success;
    }

    /** 
     * {@inheritDoc}
     */
    public String[] listDirectory(String dirURI) throws IOException{
        l(UMLog.DEBUG, 539, dirURI);
        IOException e = null;
        
        if (!dirURI.endsWith("/")){
            dirURI += '/';
            l(UMLog.DEBUG, 541, dirURI);
        }
        
        FileConnection fc = null;
        String dirList[] = null;
        
        try {
            fc = (FileConnection) Connector.open(dirURI, 
                Connector.READ);
            Enumeration dirListEnu = fc.list();
            dirList = UMUtil.enumerationToStringArray(dirListEnu);
        }catch(IOException ioe) {
            e = ioe;
        }catch(SecurityException se) {
            e = new IOException(PREFIX_SECURITY_EXCEPTION+se.toString());
        }finally {
            J2MEIOUtils.closeConnection(fc);
        }
        
        UMIOUtils.logAndThrowIfNotNullIO(e, UMLog.ERROR, 131, dirURI);
        
        return dirList;
    }
    
    /**
     * 
     * @param fromFileURI
     * @param toFileURI
     * @return 
     */
    public boolean renameFile(String fromFileURI, String toFileURI){
        l(UMLog.DEBUG, 543, fromFileURI + "->" + toFileURI);
        boolean success = false;
        fromFileURI = fromFileURI.trim();
        toFileURI = toFileURI.trim();
        
        String fromParent = UMFileUtil.getParentFilename(fromFileURI);
        String toParent = UMFileUtil.getParentFilename(toFileURI);
        if((fromParent == null && toParent == null) || fromParent != null && fromParent.equals(toParent)) {
            //is the same parent directory - OK to move
            String newFilename = UMFileUtil.getFilename(toFileURI);
            FileConnection fc = null;
            try {
                fc = (FileConnection)Connector.open(fromFileURI, 
                        Connector.READ_WRITE);
                fc.rename(newFilename);
                success = true;
            }catch(IOException e) {
                l(UMLog.ERROR, 133, fromFileURI + "->" + toFileURI, e);
            }catch(SecurityException se) {
                l(UMLog.ERROR, 133, fromFileURI + "->" + toFileURI, se);
            }finally {
                J2MEIOUtils.closeConnection(fc);
            }
        }else {
            l(UMLog.ERROR, 135, fromFileURI + "->" + toFileURI);
        }
        
        return success;
       
    }
    
    public long fileSize(String fileURI){
        FileConnection fc = null;
        long size = -1;
        try {
            fc = (FileConnection) Connector.open(fileURI, Connector.READ);
            size = fc.fileSize();
        }catch(Exception e) {
            l(UMLog.ERROR, 137, fileURI, e);
        }finally {
            J2MEIOUtils.closeConnection(fc);
        }
        l(UMLog.VERBOSE, 545, fileURI + " (" + size + "bytes");
        return size;
    } 
    
    public boolean makeDirectory(String dirURI) throws IOException{
        getLogger().l(UMLog.VERBOSE, 401, dirURI);
        FileConnection fc = null;
        
        if(dirURI.charAt(dirURI.length()-1) != '/') {
            //dirURI = dirURI.substring(0, dirURI.length()-1);
            dirURI += '/'; 
            getLogger().l(UMLog.DEBUG, 504, dirURI);
        }
        
        
        IOException e = null;
        boolean dirOK = false;
        try {
            fc = (FileConnection)Connector.open(dirURI);
            getLogger().l(UMLog.DEBUG, 506, dirURI);
            if(!(fc.isDirectory() && fc.exists())) {
                fc.mkdir();
                dirOK = true;
                getLogger().l(UMLog.DEBUG, 503, dirURI);
            }else {
                getLogger().l(UMLog.DEBUG, 502, dirURI);
                dirOK = true;
            }
        }catch(IOException e2) {
            e = e2;
            getLogger().l(UMLog.ERROR, 104, dirURI, e);
        }finally {
            J2MEIOUtils.closeConnection(fc);
        }
        
        if(e != null) {
            throw e;
        }
        
        return dirOK;

    }
    
    public boolean removeRecursively(String dirURI){
        l(UMLog.DEBUG, 547, dirURI);
        
        if (!dirURI.endsWith("/")){
            dirURI += "/";
        }
        
        try {
            String[] fileNames = listDirectory(dirURI);
            String fullPath;
            for(int i = 0; i < fileNames.length; i++) {
                fullPath = UMFileUtil.joinPaths(new String[]{dirURI, fileNames[i]});
                if(fileNames[i].endsWith("/")) {
                    if(!removeRecursively(fullPath)) {
                        l(UMLog.ERROR, 139, fullPath);
                        return false;
                    }
                }else {
                    if(!removeFile(fullPath)) {
                        l(UMLog.ERROR, 141, fullPath);
                        return false;
                    }
                }
            }
            return removeFile(dirURI);
        }catch(Exception e) {
            l(UMLog.ERROR, 143, dirURI, e);
        }
        return false;
    }
    
    

    public String getActiveUser() {
        //Code here:
        return AppPref.getSetting("CURRENTUSER");
    }

    public void setActiveUserAuth(String password) {
        l(UMLog.DEBUG, 549, null);
        AppPref.addSetting("CURRENTUSERAUTH", password);
    }

    public String getActiveUserAuth() {
        return AppPref.getSetting("CURRENTUSERAUTH");
    }

    public String getAppPref(String key) {   
        String value;
        value = AppPref.getSetting(key);
        l(UMLog.DEBUG, 551, key + '=' + value);
        return value;
    }

    /**
     * @inheritDoc
     */
    public HTTPResult makeRequest(final String url, final Hashtable headers, final Hashtable postParameters, final String type) throws IOException{
        getLogger().l(UMLog.VERBOSE, 305, "HTTP (" + type + ")" + url);
        HTTPResult httpResult = null;
        HttpConnection httpConn = null;
        InputStream is = null;
        OutputStream os = null;
        IOException e = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        
        try {
            
            // Open an HTTP Connection object
            httpConn = (HttpConnection)Connector.open(url);
            httpConn.setRequestMethod(type);
            // Setup HTTP Request to GET/POST
            if(type.equals("POST")){
                httpConn.setRequestProperty("User-Agent",
                    "Profile/MIDP-1.0 Confirguration/CLDC-1.0");
                httpConn.setRequestProperty("Accept_Language","en-US");
                //Content-Type is must to pass parameters in POST Request
                httpConn.setRequestProperty("Content-Type", 
                        "application/x-www-form-urlencoded");
            }
            
            //Add Parameters
            String params = null;
            if (postParameters != null){
                Enumeration keys = postParameters.keys();
                String key, value;
                boolean firstAmp = true;
                while(keys.hasMoreElements()) {
                    key = keys.nextElement().toString();
                    value = postParameters.get(key).toString();
                    value = URLTextUtil.urlEncodeUTF8(value);
                    if (firstAmp){
                        params = key + "=" + value;
                        firstAmp=false;
                    }else{
                        params = params + "&"+ key + "=" + value;
                    }
                }
            }
                        
            //Add Headers
            if (headers != null){
                Enumeration headerKeys = headers.keys();
                String hKey, hValue;
                while(headerKeys.hasMoreElements()) {
                    hKey = headerKeys.nextElement().toString();
                    hValue = headers.get(hKey).toString();
                    if(!hKey.equals("") && !hValue.equals("")){
                        httpConn.setRequestProperty(hKey, hValue);
                    }
                }
            }
            
            if(type.equals("POST")){
                //Content-Length to be set
                httpConn.setRequestProperty("Content-length", 
                        String.valueOf(params.getBytes().length));
                httpConn.setRequestProperty(url, type);
                os = httpConn.openOutputStream();
                os.write(params.getBytes());
                //os.flush?
            } 
            
            // Read Response from the Server
            int response_code=httpConn.getResponseCode();
            is = httpConn.openInputStream();
            
            UMIOUtils.readFully(is, bout, 1024);
            
            byte[] response = null;
            response = bout.toByteArray();
            Hashtable responseHeaders = null;
            httpResult = new HTTPResult(response, response_code, responseHeaders);
        }catch(IOException ioe){
            l(UMLog.ERROR, 124, url, ioe);
            e = ioe;
        }catch(SecurityException se) {
            e = new IOException(UstadMobileSystemImpl.PREFIX_SECURITY_EXCEPTION 
                + se.toString());
        }finally{
            UMIOUtils.closeInputStream(is);
            UMIOUtils.closeOutputStream(bout);
            J2MEIOUtils.closeConnection(httpConn);
        }
        
        UMIOUtils.throwIfNotNullIO(e);
        return httpResult;
    }

    public void setAppPref(String key, String value) {
        l(UMLog.DEBUG, 553, key + '=' + value);
        AppPref.addSetting(key, value);
    }

    public XmlPullParser newPullParser() throws XmlPullParserException { 
        KXmlParser parser = new KXmlParser();
        return parser;
    }

    public String getUserPref(String key) {
        String value = UserPref.getSetting(key);
        l(UMLog.DEBUG, 555, key + '=' + value);
        return value;
    }

    public AppView getAppView() {
        return appView;
    }

    public UMLog getLogger() {
        return umLogger;
    }

    /**
     * @inheritDoc
     */
    public String openContainer(UstadJSOPDSEntry entry, String containerURI, 
            String mimeType) {
        l(UMLog.DEBUG, 557, containerURI + " : " + mimeType);
        if(openZip != null) {
            throw new IllegalStateException("J2ME: Open one thing at a time please");
        }
        
        try {
            openZip = openZip(containerURI);
            openZipURI = containerURI;
        }catch(IOException e) {
            getLogger().l(UMLog.CRITICAL, 400, containerURI, e);
        }

        return OPENZIP_PROTO;
    }
    
    public ZipFileHandle getOpenZip() {
        return openZip;
    }

    public HTTPResult readURLToString(String url, Hashtable headers) throws IOException {
        if(url.startsWith(OPENZIP_PROTO)) {
            l(UMLog.DEBUG, 557, url);
            InputStream in = null;
            ByteArrayOutputStream bout = null;
            IOException ioe = null;
            try {
                in = openZip.openInputStream(url.substring(
                    OPENZIP_PROTO.length()));
                bout = new ByteArrayOutputStream();
                UMIOUtils.readFully(in, bout, 1024);
            }catch(IOException e) {
                getLogger().l(UMLog.INFO, 320, url, e);
                ioe = e;
            }finally {
                UMIOUtils.closeInputStream(in);
            }
            
            if(ioe == null) {
                return new HTTPResult(bout.toByteArray(), 200, new Hashtable());
            }else {
                throw ioe;
            }
        }else {
            return super.readURLToString(url, headers); 
        }
    }
    
    
    public void closeContainer(String openURI) {
        l(UMLog.DEBUG, 559, openURI);
        openZip = null;
    }

    public String[] getUserPrefKeyList() {
        return UserPref.getAllKeys();
    }

    public String[] getAppPrefKeyList() {
        return AppPref.getAllKeys();
    }

    public ZipFileHandle openZip(String name) throws IOException {
        l(UMLog.DEBUG, 559, name);
        return new ZipFileHandleJ2ME(name);
    }

    public OutputStream openFileOutputStream(String fileURI, int flags) throws IOException{
        l(UMLog.DEBUG, 561, fileURI);
        boolean append = (flags & FILE_APPEND) == FILE_APPEND;
        
        FileConnection con = null;
        IOException e = null;
        OutputStream out = null;
        
        try {
            con = (FileConnection)Connector.open(fileURI, Connector.READ_WRITE);
            
            if(con.exists()) {
                if(!append) {
                    con.delete();
                    con.create();
                }
            }else {
                con.create();
            }
        }catch(IOException e2) {
            e = e2;
        }catch(SecurityException se) {
            e = new IOException(PREFIX_SECURITY_EXCEPTION + se.toString());
        }finally {
            J2MEIOUtils.closeConnection(con);
            if(e != null) throw e;
        }

        if(!append) {
            out = Connector.openOutputStream(fileURI);
            
        }else {
            con = (FileConnection)Connector.open(fileURI, Connector.READ_WRITE);
            out = new ConnectorCloseOutputStream(
                con.openOutputStream(con.fileSize()), con);
        }
        
        return out;
        
    }

    /**
     * {@inheritDoc}
     */
    public InputStream openFileInputStream(String fileURI) throws IOException {
        l(UMLog.DEBUG, 563, fileURI);
        InputStream in = null;
        IOException e = null;
        try {
            in = Connector.openInputStream(fileURI);
        }catch(SecurityException se) {
            e = new IOException(PREFIX_SECURITY_EXCEPTION + se.toString());
        }
        UMIOUtils.throwIfNotNullIO(e);
        return in;
    }
    
    public String[] getPrefKeyList() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    /**
     * Plays the media's inputstream. Can be audio or video.
     * @param mediaURLInputStream the InputStream to be played.
     * @param encoding The encoding by which the player will get generated. 
     * @return 
     */
    public boolean playMedia(InputStream mediaURLInputStream, String encoding) {
        l(UMLog.DEBUG, 563, encoding);
        boolean status = false;
        stopMedia();
        VolumeControl vc = null;
        
        try{
            player = Manager.createPlayer(mediaURLInputStream, encoding);
            player.realize();
            vc = (VolumeControl) player.getControl("VolumeControl");
            if (vc != null){
                vc.setLevel(volumeLevel);
            }
            player.start();
            long playerTime = player.getDuration();
            l(UMLog.DEBUG, 565, ""+playerTime);
            status = true;            
        }catch(Exception e){
            l(UMLog.ERROR, 145, encoding, e);
            status = false;
            stopMedia();
        }finally{
            if (mediaURLInputStream != null){
                try{
                    mediaURLInputStream.close();
                }catch(Exception e){
                    HTTPUtils.httpDebug("eror nulling mediaURLInputStream.");
                }
            }
        }
        
        return status;
    }

    /**
     * Stops the media playing. 
     * @return 
     */
    public boolean stopMedia() {
        l(UMLog.DEBUG, 567, null);
        boolean status = false;
        if (player != null){
            try{
                player.stop();
                player.close();
                player.deallocate();
                player = null;
                l(UMLog.DEBUG, 571, null);
                //Garbage collect too?
                System.gc();
                status = true;
            }catch(Exception e){
                l(UMLog.ERROR, 147, null, e);
                status = false;
            }
        }else {
            l(UMLog.DEBUG, 569, null);
        }
        
        return status;
    }
    
    /**
     * Use when an output stream is bound to a connector, and we want to make sure
     * the connector gets closed when the outputstream is closed.
     * 
     * Simply calls Connection.close when the streams close method is called
     */
    public class ConnectorCloseOutputStream extends OutputStream {

        private OutputStream dst;
        
        private Connection con;
        
        public ConnectorCloseOutputStream(OutputStream dst, Connection con) {
            UstadMobileSystemImpl.l(UMLog.DEBUG, 571, null);
            this.dst = dst;
        }
        
        public void write(int b) throws IOException {
            dst.write(b);
        }

        public void close() throws IOException {
            IOException ioe = null;
            try {
                dst.close();
            }catch(IOException e) {
                ioe = e;
            }finally {
                J2MEIOUtils.closeConnection(con);
            }
            UstadMobileSystemImpl.l(UMLog.DEBUG, 573, null);
            UMIOUtils.throwIfNotNullIO(ioe);
        }

        public void flush() throws IOException {
            dst.flush(); 
        }

        public void write(byte[] b, int off, int len) throws IOException {
            dst.write(b, off, len); 
        }

        public void write(byte[] b) throws IOException {
            dst.write(b); 
        }
    }
    
    /**
     * J2ME implementation of the DownloadJob interface 
     */
    public class DownloadJob extends Thread implements UMTransferJob {

        final private String srcURL;
        
        final private String destFileURI;
        
        private long bytesDownloaded;
        
        private int totalSize;
        
        private UstadMobileSystemImplJ2ME myImpl;
        
        private boolean finished;
        
        private final Vector progressListeners;
        
        private final UMProgressEvent evt;
        
        public static final int RETRY_LIMIT_DEFAULT = 10;
        
        /**
         * The maximum number of retries allowed for this job until it fails
         */
        private int maxRetries;
        
        /**
         * The current number of tries
         */
        private int tryCount;
        
        /**
         * The delay (in ms) minimum between progress updates; this is used to
         * ensure that we don't fire out too many updates
         */
        public static final int UPDATE_MIN_INTERVAL = 1000;
        
        /**
         * The default time to wait in between download attemtps
         */
        public static final int RETRY_WAIT_DEFAULT = 1000;
        
        /**
         * The time to wait in between download attempts
         */
        private int retryWait;
        
        /**
         * Create a new download job
         * 
         * @param srcURL The HTTP source URL to download from
         * @param destFileURI The file path to save to 
         * @param myImpl our parent SystemImplementation
         */
        public DownloadJob(String srcURL, String destFileURI, UstadMobileSystemImplJ2ME myImpl) {
            UstadMobileSystemImpl.l(UMLog.DEBUG, 575, srcURL + "->" + destFileURI);
            this.srcURL = srcURL;
            this.destFileURI = destFileURI;
            
            bytesDownloaded = -1;
            totalSize = -1;
            this.myImpl =  myImpl;
            finished = false;
            progressListeners = new Vector();
            evt = new UMProgressEvent(this, UMProgressEvent.TYPE_PROGRESS, 0, 0, 0);
            maxRetries = RETRY_LIMIT_DEFAULT;
            retryWait = RETRY_WAIT_DEFAULT;
        }
        
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
        
        /**
         * Send update event to all registered listeners
         */
        protected void fireProgressEvent() {
            int i;
            int numListeners = progressListeners.size();
            for(i = 0; i < numListeners; i++) {
                ((UMProgressListener)progressListeners.elementAt(i)).progressUpdated(evt);
            }
        }
        
        public void start() {
            super.start();
        }
        
        public void continueDownload() throws IOException{
            final UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();
            OutputStream fOut = null;
            HttpConnection con = null;
            InputStream httpIn = null;
            IOException ioe = null;
            
            try {
                impl.l(UMLog.INFO, 332, srcURL + "->" + destFileURI);
                bytesDownloaded = 0;
                if(impl.fileExists(destFileURI)) {
                    bytesDownloaded = impl.fileSize(destFileURI);
                }


                fOut = myImpl.openFileOutputStream(destFileURI, FILE_APPEND);
                con = (HttpConnection)Connector.open(srcURL);
                if(bytesDownloaded > 0) {
                    myImpl.l(UMLog.VERBOSE, 410, srcURL + ':' + bytesDownloaded);
                    con.setRequestProperty("Range", "bytes=" + bytesDownloaded + '-');
                }
                con.setRequestProperty("Connection", "close");

                httpIn = con.openInputStream();
                
                myImpl.getLogger().l(UMLog.VERBOSE, 314, srcURL);

                byte[] buf = new byte[1024];
                int bytesRead = 0;
                int totalRead = 0;
                long lastUpdate = 0;

                long timeNow;
                while((bytesRead = httpIn.read(buf)) != -1) {
                    fOut.write(buf, 0, bytesRead);
                    totalRead += bytesRead;
                    timeNow = System.currentTimeMillis();
                    if(timeNow - lastUpdate > UPDATE_MIN_INTERVAL) {
                        evt.setProgress((int)(totalRead + bytesDownloaded));
                        System.out.println("Firing progress evt: " + totalRead);
                        fireProgressEvent();
                        lastUpdate = timeNow;
                    }
                }
                
                finished = true;
                this.bytesDownloaded = totalSize;
                evt.setEvtType(UMProgressEvent.TYPE_COMPLETE);
                evt.setJobLength(totalSize);
                evt.setProgress(totalSize);
                evt.setStatusCode(200);
                StringBuffer sbMsg = new StringBuffer();
                sbMsg.append(srcURL).append("->").append(destFileURI).append(" (");
                sbMsg.append(totalSize).append(" bytes)");
                impl.getLogger().l(UMLog.INFO, 333, sbMsg.toString());
                fireProgressEvent();
            }catch(IOException e) {
                ioe = e;
                impl.l(UMLog.ERROR, 115, srcURL + "->" +  destFileURI, e);
            }finally {
                UMIOUtils.closeInputStream(httpIn);
                J2MEIOUtils.closeConnection(con);
                UMIOUtils.closeOutputStream(fOut);
            }
            
            UMIOUtils.throwIfNotNullIO(ioe);
        }
        
        
        /**
         * Run as a thread the actual download (in the background)
         */
        public void run() {
            final UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();
            impl.l(UMLog.DEBUG, 577, srcURL + "->" + destFileURI);
            
            //see if we need to delete the destination beforehand
            try {
                if(impl.fileExists(destFileURI)) {
                    impl.l(UMLog.DEBUG, 579, destFileURI);
                    impl.removeFile(destFileURI);
                }
            }catch(IOException e) {
                impl.l(UMLog.ERROR, 116 , destFileURI, e);
            }
            
            while(!this.isFinished() && tryCount < maxRetries) {
                try {
                    if(totalSize == -1) {
                        getTotalSize();
                    }
                    tryCount++;
                    continueDownload();
                }catch(IOException e) {
                    StringBuffer sb = new StringBuffer();
                    sb.append(srcURL).append("->").append(destFileURI);
                    sb.append(" try : ").append(tryCount);
                    impl.l(UMLog.ERROR, 117, sb.toString() , e);
                }
                try { Thread.sleep(retryWait); }
                catch(InterruptedException e) {}
            }
        }

        /**
         * @inheritDoc
         */
        public void addProgressListener(UMProgressListener listener) {
            progressListeners.addElement(listener);
        }

        /**
         * @inheritDoc
         */
        public long getBytesDownloadedCount() {
            return bytesDownloaded;
        }

        /**
         * @inheritDoc
         */
        public int getTotalSize() {
            HttpConnection con = null;
            try {
                con = (HttpConnection)Connector.open(srcURL);
                con.setRequestMethod(HttpConnection.HEAD);
                String contentLen = con.getHeaderField("Content-Length");
                if(contentLen != null) {
                    totalSize = Integer.parseInt(contentLen);
                }
                UstadMobileSystemImpl.l(UMLog.DEBUG, 581, contentLen);
            }catch(Exception e) {
                UstadMobileSystemImpl.getInstance().getLogger().l(UMLog.INFO, 102, null, e);
            }finally {
                J2MEIOUtils.closeConnection(con);
                con = null;
            }
            
            return totalSize;
        }

        /**
         * @inheritDoc
         */
        public boolean isFinished() {
            return finished;
        }

        /**
         * @inheritDoc
         */
        public String getSource() {
            return srcURL;
        }

        /**
         * @inheritDoc
         */
        public String getDestination() {
            return destFileURI;
        }
        
    }
    

}
