/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.storage;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.ws.Storage;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.util.StorageProperties;
import edu.ucsb.eucalyptus.keys.Hashes;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.nio.channels.FileChannel;
import java.net.InetAddress;

public class LVM2Manager implements ElasticBlockManager {

    //Location where LVM stores VGs and LVs
    //TODO: make these configurable
    public static String volumeRootDirectory;
    public static String snapshotRootDirectory;
    public static final String lvmRootDirectory = "/dev";
    public static final String PATH_SEPARATOR = "/";
    public static final String iface = "wlan0";
    public static boolean initialized = false;
    public static String hostName = "localhost";
    public static final int MAX_LOOP_DEVICES = 256;

    public void initVolumeManager(String volumeRoot, String snapshotRoot) {
        volumeRootDirectory = volumeRoot;
        snapshotRootDirectory = snapshotRoot;                                                            
        if(!initialized) {
          //  System.loadLibrary("lvm2control");
            try {
                hostName = InetAddress.getLocalHost().getHostName();
                EntityWrapper<LVMMetaInfo> db = new EntityWrapper<LVMMetaInfo>();
                LVMMetaInfo metaInfo = new LVMMetaInfo(hostName);
                List<LVMMetaInfo> metaInfoList = db.query(metaInfo);
                if(metaInfoList.size() <= 0) {
                    metaInfo.setMajorNumber(-1);
                    metaInfo.setMinorNumber(-1);
                    db.add(metaInfo);
                    initialized = true;
                }
                db.commit();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public native String losetup(String fileName);

    public native String createEmptyFile(String fileName, int size);

    public native String createPhysicalVolume(String loDevName);

    public native String createVolumeGroup(String pvName, String vgName);

    public native String extendVolumeGroup(String pvName, String vgName);

    public native String createLogicalVolume(String vgName, String lvName);

    public native String createSnapshotLogicalVolume(String lvName, String snapLvName);

    public native int aoeExport(String iface, String lvName, int major, int minor);

    public native void aoeUnexport(int vbladePid);

    public native String removeLogicalVolume(String lvName);

    public native String removeVolumeGroup(String vgName);

    public native String removePhysicalVolume(String loDevName);

    public native String removeLoopback(String loDevName);

    public native String reduceVolumeGroup(String vgName, String pvName);

    public native String suspendDevice(String deviceName);

    public native String resumeDevice(String deviceName);

    public native String duplicateLogicalVolume(String oldLvName, String newLvName);
    
    public int exportVolume(LVMVolumeInfo lvmVolumeInfo, String vgName, String lvName) {
        int majorNumber = -1;
        int minorNumber = -1;
        LVMMetaInfo metaInfo = new LVMMetaInfo(hostName);
        EntityWrapper<LVMMetaInfo> db = new EntityWrapper<LVMMetaInfo>();
        try {
            LVMMetaInfo foundMetaInfo = db.getUnique(metaInfo);
            if(foundMetaInfo != null) {
                majorNumber = foundMetaInfo.getMajorNumber();
                minorNumber = foundMetaInfo.getMinorNumber();
                if(((++minorNumber) % MAX_LOOP_DEVICES) == 0) {
                    ++majorNumber;
                }
                foundMetaInfo.setMajorNumber(majorNumber);
                foundMetaInfo.setMinorNumber(minorNumber);
            }
            db.commit();
        } catch (Exception ex) {
            db.rollback();
            ex.printStackTrace();
        }
        String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
        int pid = aoeExport(iface, absoluteLVName, majorNumber, minorNumber);
        //TODO: do error checking
        lvmVolumeInfo.setVbladePid(pid);
        lvmVolumeInfo.setMajorNumber(majorNumber);
        lvmVolumeInfo.setMinorNumber(minorNumber);
        return pid;
    }

    public void dupFile(String oldFileName, String newFileName) {
        try {
            FileChannel out = new FileOutputStream(new File(newFileName)).getChannel();
            FileChannel in = new FileInputStream(new File(oldFileName)).getChannel();
            in.transferTo(0, in.size(), out);
            in.close();
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public String createDuplicateLoopback(String oldRawFileName, String rawFileName) {
        dupFile(oldRawFileName, rawFileName);
        //TODO: do error checking
        String loDevName = losetup(rawFileName);
        //TODO: do error checking
        return loDevName;
    }

    public String createLoopback(String fileName, int size) {
        String returnValue = createEmptyFile(fileName, size);
        //TODO: do error checking
        String loDevName = losetup(fileName);
        //TODO: do error checking
        return loDevName;
    }

    public String createLoopback(String fileName) {
        String loDevName = losetup(fileName);
        return loDevName;
    }

    //creates a logical volume (and a new physical volume and volume group)
    public void createLogicalVolume(String loDevName, String vgName, String lvName) {
        String returnValue = createPhysicalVolume(loDevName);
        //TODO: do error checking
        returnValue = createVolumeGroup(loDevName, vgName);
        //TODO: do error checking
        returnValue = createLogicalVolume(vgName, lvName);
        //TODO: do error checking
    }

    public  void createSnapshotLogicalVolume(String loDevName, String vgName, String lvName, String snapLvName) {
        String returnValue = createPhysicalVolume(loDevName);
        //TODO: do error checking
        returnValue = extendVolumeGroup(loDevName, vgName);
        //TODO: do error checking
        returnValue = createSnapshotLogicalVolume(lvName, snapLvName);
        //TODO: do error checking
    }

    public void createVolume(String volumeId, int size) throws EucalyptusCloudException {
        File volumeDir = new File(StorageProperties.volumeRootDirectory);
        volumeDir.mkdirs();

        String vgName = "vg-" + Hashes.getRandom(4);
        String lvName = "lv-" + Hashes.getRandom(4);
        LVMVolumeInfo lvmVolumeInfo = new LVMVolumeInfo();

        String rawFileName = StorageProperties.volumeRootDirectory + "/" + volumeId;
        //create file and attach to loopback device
        String loDevName = createLoopback(rawFileName, size);
        //create physical volume, volume group and logical volume
        createLogicalVolume(loDevName, vgName, lvName);
        //export logical volume
        int vbladePid = exportVolume(lvmVolumeInfo, vgName, lvName);
        if(vbladePid < 0) {
            throw new EucalyptusCloudException();
        }
        lvmVolumeInfo.setVolumeId(volumeId);
        lvmVolumeInfo.setLoDevName(loDevName);
        lvmVolumeInfo.setLoFileName(rawFileName);
        lvmVolumeInfo.setPvName(loDevName);
        lvmVolumeInfo.setVgName(vgName);
        lvmVolumeInfo.setLvName(lvName);
        lvmVolumeInfo.setStatus(Storage.Status.available.toString());
        lvmVolumeInfo.setSize(size);

        EntityWrapper<LVMVolumeInfo> db = new EntityWrapper<LVMVolumeInfo>();
        db.add(lvmVolumeInfo);
        db.commit();
    }

    public int createVolume(String volumeId, String snapshotId, int size) throws EucalyptusCloudException {
        EntityWrapper<LVMVolumeInfo> db = new EntityWrapper<LVMVolumeInfo>();
        LVMVolumeInfo lvmVolumeInfo = new LVMVolumeInfo(snapshotId);
        LVMVolumeInfo foundSnapshotInfo = db.getUnique(lvmVolumeInfo);
        if(foundSnapshotInfo != null) {
            String status = foundSnapshotInfo.getStatus();
            if(status.equals(Storage.Status.available.toString())) {
                String vgName = "vg-" + Hashes.getRandom(4);
                String lvName = "lv-" + Hashes.getRandom(4);
                lvmVolumeInfo = new LVMVolumeInfo();

                String rawFileName = StorageProperties.volumeRootDirectory + "/" + volumeId;
                //create file and attach to loopback device
                File snapshotFile = new File(foundSnapshotInfo.getLoFileName());
                assert(snapshotFile.exists());
                size = (int)(snapshotFile.length() / StorageProperties.GB);
                String loDevName = createLoopback(rawFileName, size);
                //create physical volume, volume group and logical volume
                createLogicalVolume(loDevName, vgName, lvName);
                //duplicate snapshot volume
                String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
                String absoluteSnapshotLVName = lvmRootDirectory + PATH_SEPARATOR + foundSnapshotInfo.getVgName() +
                        PATH_SEPARATOR + foundSnapshotInfo.getLvName();
                duplicateLogicalVolume(absoluteSnapshotLVName, absoluteLVName);
                //export logical volume
                int vbladePid = exportVolume(lvmVolumeInfo, vgName, lvName);
                if(vbladePid < 0) {
                    throw new EucalyptusCloudException();
                }
                lvmVolumeInfo.setVolumeId(volumeId);
                lvmVolumeInfo.setLoDevName(loDevName);
                lvmVolumeInfo.setLoFileName(rawFileName);
                lvmVolumeInfo.setPvName(loDevName);
                lvmVolumeInfo.setVgName(vgName);
                lvmVolumeInfo.setLvName(lvName);
                lvmVolumeInfo.setStatus(Storage.Status.available.toString());
                lvmVolumeInfo.setSize(size);
                db.add(lvmVolumeInfo);
                db.commit();
            }
        } else {
            db.rollback();
            throw new EucalyptusCloudException();
        }
        return size;
    }

    public List<String> getStatus(List<String> volumeSet) throws EucalyptusCloudException {
        EntityWrapper<LVMVolumeInfo> db = new EntityWrapper<LVMVolumeInfo>();
        ArrayList<String> status = new ArrayList<String>();
        for(String volumeSetEntry: volumeSet) {
            LVMVolumeInfo lvmVolumeInfo = new LVMVolumeInfo();
            lvmVolumeInfo.setVolumeId(volumeSetEntry);
            LVMVolumeInfo foundLvmVolumeInfo = db.getUnique(lvmVolumeInfo);
            if(foundLvmVolumeInfo != null) {
                status.add(foundLvmVolumeInfo.getStatus());
            } else {
                db.rollback();
                throw new EucalyptusCloudException();
            }
        }
        db.commit();
        return status;
    }

    public void deleteVolume(String volumeId) throws EucalyptusCloudException {
        EntityWrapper<LVMVolumeInfo> db = new EntityWrapper<LVMVolumeInfo>();
        LVMVolumeInfo lvmVolumeInfo = new LVMVolumeInfo(volumeId);
        LVMVolumeInfo foundLVMVolumeInfo = db.getUnique(lvmVolumeInfo);

        if(foundLVMVolumeInfo != null) {
            //remove aoe export
            String loDevName = foundLVMVolumeInfo.getLoDevName();
            String vgName = foundLVMVolumeInfo.getVgName();
            String lvName = foundLVMVolumeInfo.getLvName();
            String fileName = foundLVMVolumeInfo.getLoFileName();
            String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;

            aoeUnexport(foundLVMVolumeInfo.getVbladePid());
            String returnValue = removeLogicalVolume(absoluteLVName);
            //TODO: error checking
            returnValue = removeVolumeGroup(vgName);
            //TODO: error checking
            returnValue = removePhysicalVolume(loDevName);
            //TODO: error checking
            returnValue = removeLoopback(loDevName);
            //TODO: error checking
            db.delete(foundLVMVolumeInfo);
            db.commit();
        }  else {
            db.rollback();
            throw new EucalyptusCloudException();
        }
    }


    public List<String> createSnapshot(String volumeId, String snapshotId) throws EucalyptusCloudException {
        EntityWrapper<LVMVolumeInfo> db = new EntityWrapper<LVMVolumeInfo>();
        LVMVolumeInfo lvmVolumeInfo = new LVMVolumeInfo(volumeId);
        LVMVolumeInfo foundLVMVolumeInfo = db.getUnique(lvmVolumeInfo);
        ArrayList<String> returnValues = new ArrayList<String>();
        if(foundLVMVolumeInfo != null) {
            LVMVolumeInfo snapshotInfo = new LVMVolumeInfo(snapshotId);
            snapshotInfo.setSnapshotOf(volumeId);
            File snapshotDir = new File(StorageProperties.snapshotRootDirectory);
            snapshotDir.mkdirs();

            String vgName = foundLVMVolumeInfo.getVgName();
            String lvName = "lv-snap-" + Hashes.getRandom(4);
            String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + foundLVMVolumeInfo.getLvName();

            int size = foundLVMVolumeInfo.getSize();
            String rawFileName = StorageProperties.snapshotRootDirectory + "/" + snapshotId;
            //create file and attach to loopback device
            String loDevName = createLoopback(rawFileName, size);
            //create physical volume, volume group and logical volume
            createSnapshotLogicalVolume(loDevName, vgName, absoluteLVName, lvName);

            snapshotInfo.setLoDevName(loDevName);
            snapshotInfo.setLoFileName(rawFileName);
            snapshotInfo.setPvName(loDevName);
            snapshotInfo.setVgName(vgName);
            snapshotInfo.setLvName(lvName);
            snapshotInfo.setStatus(Storage.Status.available.toString());
            snapshotInfo.setVbladePid(-1);
            snapshotInfo.setSize(size);
            returnValues.add(vgName);
            returnValues.add(lvName);
            db.add(snapshotInfo);
        }
        db.commit();
        return returnValues;
    }

    public List<String> prepareForTransfer(String volumeId, String snapshotId) throws EucalyptusCloudException {
        EntityWrapper<LVMVolumeInfo> db = new EntityWrapper<LVMVolumeInfo>();
        LVMVolumeInfo lvmVolumeInfo = new LVMVolumeInfo(volumeId);
        LVMVolumeInfo foundLVMVolumeInfo = db.getUnique(lvmVolumeInfo);
        ArrayList<String> returnValues = new ArrayList<String>();

        if(foundLVMVolumeInfo != null) {

            returnValues.add(foundLVMVolumeInfo.getLoFileName());
            String dmDeviceName = foundLVMVolumeInfo.getVgName().replaceAll("-", "--") + "-" + foundLVMVolumeInfo.getLvName().replaceAll("-", "--");
            lvmVolumeInfo = new LVMVolumeInfo(snapshotId);
            foundLVMVolumeInfo = db.getUnique(lvmVolumeInfo);
            if(foundLVMVolumeInfo != null) {
                String snapshotRawFileName = foundLVMVolumeInfo.getLoFileName();
                String dupSnapshotDeltaFileName = snapshotRawFileName + "." + Hashes.getRandom(4);
                String returnValue = suspendDevice(dmDeviceName);
                dupFile(snapshotRawFileName, dupSnapshotDeltaFileName);
                returnValue = resumeDevice(dmDeviceName);
                returnValues.add(dupSnapshotDeltaFileName);
            }
        } else {
            db.rollback();
            throw new EucalyptusCloudException();
        }
        return returnValues;
    }

    public void deleteSnapshot(String snapshotId) throws EucalyptusCloudException {
        EntityWrapper<LVMVolumeInfo> db = new EntityWrapper<LVMVolumeInfo>();
        LVMVolumeInfo lvmVolumeInfo = new LVMVolumeInfo(snapshotId);
        LVMVolumeInfo foundLVMVolumeInfo = db.getUnique(lvmVolumeInfo);

        if(foundLVMVolumeInfo != null) {
            String loDevName = foundLVMVolumeInfo.getLoDevName();
            String vgName = foundLVMVolumeInfo.getVgName();
            String lvName = foundLVMVolumeInfo.getLvName();
            String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;

            String returnValue = removeLogicalVolume(absoluteLVName);
            //TODO: error checking
            returnValue = reduceVolumeGroup(vgName, loDevName);
            //TODO: error checking
            returnValue = removePhysicalVolume(loDevName);
            //TODO: error checking
            returnValue = removeLoopback(loDevName);
            //TODO: error checking
            db.delete(foundLVMVolumeInfo);
            db.commit();
        }  else {
            db.rollback();
            throw new EucalyptusCloudException();
        }
    }

    public List<String> getVolume(String volumeId) throws EucalyptusCloudException {
        ArrayList<String> returnValues = new ArrayList<String>();

        EntityWrapper<LVMVolumeInfo> db = new EntityWrapper<LVMVolumeInfo>();
        LVMVolumeInfo lvmVolumeInfo = new LVMVolumeInfo(volumeId);
        LVMVolumeInfo foundLvmVolumeInfo = db.getUnique(lvmVolumeInfo);
        if(foundLvmVolumeInfo != null) {
            returnValues.add(String.valueOf(foundLvmVolumeInfo.getMajorNumber()));
            returnValues.add(String.valueOf(foundLvmVolumeInfo.getMinorNumber()));
        } else {
            db.rollback();
            throw new EucalyptusCloudException();
        }
        db.commit();
        return returnValues;
    }

    public void loadSnapshots(List<String> snapshotSet, List<String> snapshotFileNames) {
        EntityWrapper<LVMVolumeInfo> db = new EntityWrapper<LVMVolumeInfo>();
        assert(snapshotSet.size() == snapshotFileNames.size());
        int i = 0;
        for(String snapshotFileName: snapshotFileNames) {
            String loDevName = createLoopback(snapshotFileName);
            LVMVolumeInfo lvmVolumeInfo = new LVMVolumeInfo(snapshotSet.get(i++));
            lvmVolumeInfo.setLoDevName(loDevName);
            lvmVolumeInfo.setLoFileName(snapshotFileName);
            lvmVolumeInfo.setMajorNumber(-1);
            lvmVolumeInfo.setMinorNumber(-1);
            lvmVolumeInfo.setStatus(Storage.Status.available.toString());
            db.add(lvmVolumeInfo);
        }
        db.commit();
    }
}
