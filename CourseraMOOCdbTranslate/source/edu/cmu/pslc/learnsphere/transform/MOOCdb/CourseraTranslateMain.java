package edu.cmu.pslc.learnsphere.transform.MOOCdb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jdom.Element;
import org.apache.commons.io.FileUtils;

import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.CourseraDbsRestoreDao;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.FeatureExtractionDao;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.MOOCdbDao;
import edu.cmu.pslc.learnsphere.analysis.moocdb.item.MOOCdbItem;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.DaoFactory;
import edu.cmu.pslc.datashop.problemcontent.oli.CommonXml;

public class CourseraTranslateMain extends AbstractComponent {

    public static void main(String[] args) {
            CourseraTranslateMain tool = new CourseraTranslateMain();
            tool.startComponent(args);
    }

    public CourseraTranslateMain() {
            super();
    }
    
    protected void processOptions() {
            logger.info("Processing Options");
            // If you want to add all headers from a previous component, try one of these:
            //this.addMetaDataFromInput("transaction", 0, 0, ".*");
            //this.addMetaDataFromInput("user-session-map", 1, 0, ".*");
            
        }

    private static String HASH_MAPPING_SUFFIX = "hash_mapping";
    private static String FORUM_SUFFIX = "anonymized_forum";
    private static String GENERAL_SUFFIX = "anonymized_general";
    private static String MOOCDB_CORE = "moocdb_core";
    private static String MOOCDB_CLEAN = "moocdb_clean";
    
    @Override
    protected void runComponent() {
            // Dao-enabled components require an applicationContext.xml in the component directory,

            String appContextPath = this.getApplicationContextPath();
            logger.info("appContextPath: " + appContextPath);

            // Do not follow symbolic links so we can prevent unwanted directory traversals if someone
            // does manage to create a symlink to somewhere dangerous (like /datashop/deploy/)
            if (Files.exists(Paths.get(appContextPath), LinkOption.NOFOLLOW_LINKS)) {
                /** Initialize the Spring Framework application context. */
                SpringContext.getApplicationContext(appContextPath);
            }
            String optionMOOCdbName = this.getOptionAsString("customMOOCdbName");
            if (optionMOOCdbName != null) {
                    optionMOOCdbName = optionMOOCdbName.replaceAll("[\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\?\\*\\+\\.\\>]", "");
                    optionMOOCdbName = optionMOOCdbName.toLowerCase();
            }
            //get the three input sql files
            String fileName1 = this.getAttachment(0, 0).getAbsolutePath();
            String fileName2 = this.getAttachment(1, 0).getAbsolutePath();
            String fileName3 = this.getAttachment(2, 0).getAbsolutePath();
            
            //make sure input files have the right suffix and prefix
            //Game_Theory_gametheory-003_SQL_anonymized_forum.sql, 
            //Game_Theory_gametheory-003_SQL_anonymized_general.sql
            //Game_Theory_gametheory-003_SQL_hash_mapping.sql
            String[] filePrefSuf1 = getPrefixSuffixFromFileName(fileName1);
            String[] filePrefSuf2 = getPrefixSuffixFromFileName(fileName2);
            String[] filePrefSuf3 = getPrefixSuffixFromFileName(fileName3);
            String escapedCourseName = null;
            String MOOCdbName = null;
            String hashMappingFile = null;
            String hashMappingDbName = null;
            String forumFile = null;
            String forumDbName = null;
            String generalFile = null;
            String generalDbName = null;
            if (filePrefSuf1 == null || filePrefSuf2 == null || filePrefSuf3 == null) {
                    //send error message
                    String err = "Coursera SQL backup file names have wrong format.";
                    addErrorMessage(err);
                    logger.info("CourseraMOOCdbTranlate aborted: " + err);
                    System.err.println(err);
                    return;
            } 
            if (!filePrefSuf1[0].equals(filePrefSuf2[0]) || !filePrefSuf1[0].equals(filePrefSuf3[0]) || !filePrefSuf2[0].equals(filePrefSuf3[0])) {
                    //send error message
                    String err = "Coursera SQL backup files are not for the same course.";
                    addErrorMessage(err);
                    logger.info("CourseraMOOCdbTranlate aborted: " + err);
                    System.err.println(err);
                    return;
            } else {
                    escapedCourseName = filePrefSuf1[0].replaceAll("[\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\?\\*\\+\\.\\>]", "");
                    escapedCourseName = escapedCourseName.toLowerCase();
                    logger.info("escaped course name: " + escapedCourseName);
                    if (optionMOOCdbName != null && !optionMOOCdbName.trim().equals("")) {
                            MOOCdbName = optionMOOCdbName;
                    } else {
                            MOOCdbName = "moocdb_" + escapedCourseName;
                    }
                    hashMappingDbName = MOOCdbName + "_hash_mapping";
                    forumDbName = MOOCdbName + "_anonymized_forum";
                    generalDbName = MOOCdbName + "_anonymized_general";
                    
            }
            if (MOOCdbName.equals(MOOCDB_CLEAN) || MOOCdbName.equals(MOOCDB_CORE)){
                    //send error message
                    String errMsg = "Wrong MOOCdb name: " + MOOCDB_CLEAN + " or " + MOOCDB_CORE + " is not allowed.";
                    addErrorMessage(errMsg);
                    logger.info("CourseraMOOCdbTranlate aborted: " + errMsg);
                    System.err.println(errMsg);
                    return;
            }
            logger.info("Working on MOOCdb: " + MOOCdbName);
            
            //find out which file is hash_mapping
            if (filePrefSuf1[1].equals(HASH_MAPPING_SUFFIX)) {
                    hashMappingFile = fileName1;
            } else if (filePrefSuf2[1].equals(HASH_MAPPING_SUFFIX)) {
                    hashMappingFile = fileName2;
            } else if (filePrefSuf3[1].equals(HASH_MAPPING_SUFFIX)) {
                    hashMappingFile = fileName3;
            }
            //find out which file is anonymized_general
            if (filePrefSuf1[1].equals(GENERAL_SUFFIX)) {
                    generalFile = fileName1;
            } else if (filePrefSuf2[1].equals(GENERAL_SUFFIX)) {
                    generalFile = fileName2;
            } else if (filePrefSuf3[1].equals(GENERAL_SUFFIX)) {
                    generalFile = fileName3;
            }
            //find out which file is anonymized_forum
            if (filePrefSuf1[1].equals(FORUM_SUFFIX)) {
                    forumFile = fileName1;
            } else if (filePrefSuf2[1].equals(FORUM_SUFFIX)) {
                    forumFile = fileName2;
            } else if (filePrefSuf3[1].equals(FORUM_SUFFIX)) {
                    forumFile = fileName3;
            }
            if (hashMappingFile == null || forumFile  == null || generalFile == null) {
                    //send error message
                    String err = "Missing one or more Coursera SQL backup file(s).";
                    addErrorMessage(err);
                    logger.info("CourseraMOOCdbTranlate aborted: " + err + " hash_mapping: " + hashMappingFile + "; forumFile: " + forumFile + "; generalFile: " + generalFile);
                    System.err.println(err);
                    return;
            }
            
            String hashMappingMd5HashValue = CommonXml.md5Hash(hashMappingFile);
            String generalMd5HashValue = CommonXml.md5Hash(generalFile);
            String forumMd5HashValue = CommonXml.md5Hash(forumFile);
            
            //check if this dataset exists in moocdb_courses table
            MOOCdbItem currMOOCdbItem = findMOOCdb(MOOCdbName);
            logger.info("currMOOCdbItem: " + currMOOCdbItem);
            if (currMOOCdbItem != null) {
                    //send error message if there is a current progress going on
                    String progress = currMOOCdbItem.getCurrentProgress();
                    String itemHashMappingMd5HashValue = currMOOCdbItem.getHashMappingFileMd5HashValue();
                    String itemGeneralMd5HashValue = currMOOCdbItem.getGeneralFileMd5HashValue();
                    String itemForumMd5HashValue = currMOOCdbItem.getForumFileMd5HashValue();
                    
                    if (progress != null && !progress.equals("") && !progress.equals(MOOCdbItem.PROGRESS_DONE)) {
                            String errMsg = "Course " + escapedCourseName + " is currently undergoing " + progress + " by another process.";
                            addErrorMessage(errMsg + " You can either wait till it's done or start process with a new custom MOOCdb name.");
                            logger.info("CourseraMOOCdbTranlate aborted: " + errMsg);
                            System.err.println(errMsg);
                            return;
                    } else {
                            boolean hashMappingFileEqual = false;
                            boolean generalFileEqual = false;
                            boolean forumFileEqual = false;
                            File fHashMapping = new File(currMOOCdbItem.getHashMappingFile());
                            File fGeneral = new File(currMOOCdbItem.getGeneralFile());
                            File fForum = new File(currMOOCdbItem.getForumFile());
                                    
                            hashMappingFileEqual = itemHashMappingMd5HashValue.equals(hashMappingMd5HashValue);
                            generalFileEqual = itemGeneralMd5HashValue.equals(generalMd5HashValue);
                            forumFileEqual = itemForumMd5HashValue.equals(forumMd5HashValue);
                            logger.info("Compared hash-mapping file: " + hashMappingFileEqual);
                            logger.info("Compared anonymized-general file: " + generalFileEqual);
                            logger.info("Compared anonymized-forum file: " + forumFileEqual);
                            if (hashMappingFileEqual && generalFileEqual && forumFileEqual) {
                                    //output MOOCdb file and feature name
                                    File dbPointerFile = this.createFile("MOOCdbPointer", ".txt");
                                    File featuresFile = this.createFile("MOOCdbFeatures", ".txt");
                                            
                                    //write to dbPointerFile
                                    try (OutputStream outputStream = new FileOutputStream(dbPointerFile)) {
                                            // Write header and course name to export
                                            byte[] cname = null;
                                            String osName = System.getProperty("os.name").toLowerCase();
                                            if (osName.indexOf("win") >= 0) {
                                                    cname = (MOOCdbItem.MOOCdb_COLUMN_HEADER_NAME + "\r\n" + MOOCdbName).getBytes("UTF-8");
                                            } else {
                                                    cname = (MOOCdbItem.MOOCdb_COLUMN_HEADER_NAME + "\n" + MOOCdbName).getBytes("UTF-8");
                                            }
                                            outputStream.write(cname);
                                    } catch (Exception e) {
                                            // This will be picked up by the workflows platform and relayed to the user.
                                            e.printStackTrace();
                                    }
                                            
                                    //write one line to featureFile
                                    try (OutputStream outputStream = new FileOutputStream(featuresFile)) {
                                            // Write features to export
                                            byte[] features = null;
                                            String osName = System.getProperty("os.name").toLowerCase();
                                            if (osName.indexOf("win") >= 0) {
                                                    features = (getAllFeatures(MOOCdbName) + "\r\n").getBytes("UTF-8");
                                            } else {
                                                    features = (getAllFeatures(MOOCdbName) + "\n").getBytes("UTF-8");
                                            }
                                            outputStream.write(features);
                                    } catch (Exception e) {
                                            // This will be picked up by the workflows platform and relayed to the user.
                                            e.printStackTrace();
                                    }
                                                    
                                    Integer nodeIndex = 0;
                                    Integer fileIndex = 0;
                                    String fileLabel = "MOOCdb";
                                    this.addOutputFile(dbPointerFile, nodeIndex, fileIndex, fileLabel);
                                    nodeIndex = 1;
                                    fileIndex = 0;
                                    fileLabel = "MOOCdb-features";
                                    this.addOutputFile(featuresFile, nodeIndex, fileIndex, fileLabel);

                                    logger.info("Output MOOCdb to previously existing MOOCdb: " + MOOCdbName);
                                    // Send the component output back to the workflow.
                                    System.out.println(this.getOutput());
                                    return;
                                            
                            } else {
                                    String errMsg = "MOOCdb with the same name already exists. MOOCdb name: " + MOOCdbName + ". ";
                                    logger.info("CourseraMOOCdbTranlate aborted: " + errMsg);
                                    if (fHashMapping.exists() && fForum.exists() && fGeneral.exists()) 
                                            errMsg += " You can start translation process with a different MOOCdb name.";
                                    else
                                            errMsg += " You can use Feature Extraction Workflow component with the existing MOOCdb.";
                                    addErrorMessage(errMsg);
                                    System.err.println(errMsg);
                                    return;
                            }
                    }  
            }
            //delete current one
            if (currMOOCdbItem != null)
                    deleteMOOCDbItem(currMOOCdbItem);
            //save a record in moocdb_courses table
            MOOCdbItem moocdbItem = new MOOCdbItem();
            moocdbItem.setMOOCdbName(MOOCdbName);
            //courseItem.setCreatedBy("");
            moocdbItem.setCurrentProgress(MOOCdbItem.PROGRESS_RESTORE);
            moocdbItem.setForumFile(forumFile);
            moocdbItem.setForumFileMd5HashValue(forumMd5HashValue);
            moocdbItem.setGeneralFile(generalFile);
            moocdbItem.setGeneralFileMd5HashValue(generalMd5HashValue);
            moocdbItem.setHashMappingFile(hashMappingFile);
            moocdbItem.setHashMappingFileMd5HashValue(hashMappingMd5HashValue);
            moocdbItem.setStartTimestamp(new Date());
            saveOrUpdateMOOCdb(moocdbItem);
            logger.info("Saved MOOCdbItem: " + moocdbItem);
            
            //restore coursera databases
            try {
                    restoreCourseraDBs(hashMappingDbName, hashMappingFile, 
                            forumDbName, forumFile, 
                            generalDbName, generalFile);
            } catch (Exception ex) {
                    //send error message
                    this.deleteMOOCDbItem(moocdbItem);
                    String errMsg = "Found error restoring Coursera backup files;  hash_mapping: " + hashMappingFile + "; forumFile: " + forumFile + "; generalFile: " + generalFile + "; Exception: " + ex.getMessage();
                    addErrorMessage(errMsg);
                    logger.info("CourseraMOOCdbTranlate aborted: " + errMsg);
                    System.err.println(errMsg);
                    return;
            }
            logger.info("Restored Coursera DBs are: " + hashMappingDbName + "; " + forumDbName + "; " + generalDbName);
            moocdbItem.setLastProgress(MOOCdbItem.PROGRESS_RESTORE);
            moocdbItem.setCurrentProgress(MOOCdbItem.PROGRESS_TRANSLATE_PREPROCESS_CURATE);
            moocdbItem.setLastProgressEndTimestamp(new Date());
            logger.info("Start translation/preprocess/curate for MOOCdbItem: " + moocdbItem);
            saveOrUpdateMOOCdb(moocdbItem);
            
            // Run the program and return its stdout to a file.
            // pass arguments
            //Map<String, String> login = HibernateDaoFactory.DEFAULT.getAnalysisDatabaseLogin();
            this.componentOptions.addContent(0, new Element("courseName").setText(escapedCourseName));
            this.componentOptions.addContent(0, new Element("MOOCdbName").setText(MOOCdbName));
            //this.componentOptions.addContent(0, new Element("userName").setText(login.get("user")));
            //this.componentOptions.addContent(0, new Element("password").setText(login.get("password")));
            //this.componentOptions.addContent(0, new Element("dbHost").setText("127.0.0.1"));
            //this.componentOptions.addContent(0, new Element("dbPort").setText("3306"));
            File outputDirectory = this.runExternalMultipleFileOuput();
            File dbPointerFile = new File(outputDirectory.getAbsolutePath() + "/MOOCdbPointer.txt");
            File featuresFile = new File(outputDirectory.getAbsolutePath() + "/MOOCdbFeatures.txt");
            

            //check if the content of the dbPointerFile is correct
            if (checkOutputContent(dbPointerFile, MOOCdbName)) {
                    //set the earliest_submission_time
                    Date earliestSubmissionTime = getEarliestSubmissionTime(MOOCdbName); 
                    moocdbItem.setEarliestSubmissionTimestamp(earliestSubmissionTime); 
                    moocdbItem.setCurrentProgress(MOOCdbItem.PROGRESS_DONE);
                    moocdbItem.setEndTimestamp(new Date());
                    moocdbItem.setLastProgress(MOOCdbItem.PROGRESS_TRANSLATE_PREPROCESS_CURATE);
                    moocdbItem.setLastProgressEndTimestamp(new Date());
                    logger.info("Completed translation and pre-process for MOOCdb: " + MOOCdbName);
                    saveOrUpdateMOOCdb(moocdbItem);
                    
                    Integer nodeIndex = 0;
                    Integer fileIndex = 0;
                    String fileLabel = "MOOCdb";
                    this.addOutputFile(dbPointerFile, nodeIndex, fileIndex, fileLabel);
                    nodeIndex = 1;
                    fileIndex = 0;
                    fileLabel = "MOOCdb-features";
                    this.addOutputFile(featuresFile, nodeIndex, fileIndex, fileLabel);
                    return;
            } else {
                    //send error message
                    deleteMOOCDbItem(moocdbItem);
                    String errMsg = "Found error in translating/curating MOOCdb: " + MOOCdbName + "; MOOCdb name doesn't match with the Python output file. ";
                    addErrorMessage(errMsg);
                    logger.info(errMsg + " Deleted MOOCdbItem: " + moocdbItem + "; and Coursera DBs and MOOCdb.");
                    try {
                            deleteMOOCdb(MOOCdbName);
                            deleteCourseraDbs(hashMappingDbName, forumDbName, generalDbName);
                            logger.info(" Deleted MOOCdb " + MOOCdbName + "; and Coursera DBs: " + hashMappingDbName + "; " + forumDbName + "; " + generalDbName);
                    } catch (Exception ex) {
                            String exErr = " Error found deleting MOOCdb " + MOOCdbName + "; or Coursera DBs: " + hashMappingDbName + "; " + forumDbName + "; " + generalDbName;
                            addErrorMessage(exErr);
                            logger.info(exErr);
                            System.err.println(exErr);
                    }
                    System.err.println(errMsg);
                    return;
            }
            
            
            
    }
    
    //get prefix and suffix from input file name.
    //e.g. Game_Theory_gametheory-003_SQL_anonymized_forum.sql will return Game_Theory_gametheory-003 and anonymized_forum
    private String[] getPrefixSuffixFromFileName(String absoluteFileName) {
            File file = new File(absoluteFileName);
            String fileName = file.getName();
            if (fileName.indexOf(".sql") == -1)
                    return null;
            int posSQL = fileName.indexOf("_SQL_");
            int posHashMapping = fileName.indexOf("_" + HASH_MAPPING_SUFFIX);
            int posForum = fileName.indexOf("_" + FORUM_SUFFIX);
            int posGeneral = fileName.indexOf("_" + GENERAL_SUFFIX);
            if (posHashMapping == -1 && posForum == -1 && posGeneral == -1)
                    return null;
            else {
                    String[] prefSuf = new String[3];
                    if (posHashMapping != -1) {
                            if (posSQL != -1) {
                                    prefSuf[0] = fileName.substring(0, posSQL);
                            } else {
                                    prefSuf[0] = fileName.substring(0, posHashMapping);
                            }
                            prefSuf[1] = HASH_MAPPING_SUFFIX;
                    } else if (posForum != -1) {
                            if (posSQL != -1) {
                                    prefSuf[0] = fileName.substring(0, posSQL);
                            } else {
                                    prefSuf[0] = fileName.substring(0, posForum);
                            }
                            prefSuf[1] = FORUM_SUFFIX;
                    } else if (posGeneral != -1) {
                            if (posSQL != -1) {
                                    prefSuf[0] = fileName.substring(0, posSQL);
                            } else {
                                    prefSuf[0] = fileName.substring(0, posGeneral);
                            }
                            prefSuf[1] = GENERAL_SUFFIX;
                    } else
                            return null;
                    prefSuf[2] = fileName;
                    return prefSuf;
            }
    }
    
    //check if a MOOCdb already exists
    private MOOCdbItem findMOOCdb(String MOOCdbName) {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            MOOCdbItem item = dbDao.getMOOCdbByName(MOOCdbName);
            return item;
    }
    
    //save or update a MOOCdbITem and return the item
    private void saveOrUpdateMOOCdb(MOOCdbItem dbItem) {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            dbDao.saveOrUpdate(dbItem);
    }
    
    //restore coursera databases
    private void restoreCourseraDBs(String hashMappingDbName, String hashMappingFile, 
                    String forumDbName, String forumFile, 
                    String generalDbName, String generalFile) throws SQLException, Exception {
            CourseraDbsRestoreDao restoreDao = DaoFactory.DEFAULT.getCourseraDbsRestoreDao();
            restoreDao.restoreCourseraDBs(hashMappingDbName, hashMappingFile, 
                     forumDbName, forumFile, 
                     generalDbName, generalFile);
    }
    
    //delete MOOCdbItem
    private void deleteMOOCDbItem(MOOCdbItem dbItem) {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            dbDao.delete(dbItem);
    }
    
    //compare the content of the output file of python translation script matches the MOOCdbName
    private boolean checkOutputContent(File dbPointerFile, String MOOCdbName) {
            String[][] fileContent = IOUtil.read2DStringArray(dbPointerFile.getAbsolutePath());
            String MOOCdbNameInFile = null;
            //use the first meaningful name 
            for (String[] row : fileContent) {
                    for (String name : row) {
                            if (name != null && !name.equals("") && !name.equals(MOOCdbItem.MOOCdb_COLUMN_HEADER_NAME)) {
                                    MOOCdbNameInFile = name;
                                    break;
                            }
                    }
            }
                    
            return MOOCdbNameInFile.equals(MOOCdbName);
    }
    
    private Date getEarliestSubmissionTime (String MOOCdbName) {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            return dbDao.getEarliestSubmissionTime(MOOCdbName);
    }
    
    private void deleteCourseraDbs(String hashMappingDbName, String forumDbName, String generalDbName) 
                    throws SQLException, Exception {
            CourseraDbsRestoreDao restoreDao = DaoFactory.DEFAULT.getCourseraDbsRestoreDao();
            restoreDao.deleteCourseraDBs(hashMappingDbName, forumDbName, generalDbName);
    }
    
    private void deleteMOOCdb(String MOOCdbName) throws SQLException, Exception {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            dbDao.deleteMOOCdb(MOOCdbName);
    }
    
    private String getAllFeatures (String MOOCdbName) {
            FeatureExtractionDao feDao = DaoFactory.DEFAULT.getFeatureExtractionDao();
            Map<Integer, String> featureMap = feDao.getAllFeatures(MOOCdbName);
            Iterator it = featureMap.entrySet().iterator();
            String features = "";
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                features += pair.getValue() + "\t";
            }
            return features.trim();
    }
}