package gitlet;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import static gitlet.Utils.*;
import static gitlet.Repository.*;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author Yinglu Deng
 */
public class Commit implements Serializable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    /** The timestamp. */
    private String timestamp;
    /** The parent set. */
    private String parentId;
    private String parentIdTwo;
    /** Commit id */
    private String commitId;
    /** Commit id */
    private String shortId;
    /** List of Blobs <fileName, blobID> */
    private TreeMap<String, String> blobs;
    /** merge or not */
    private boolean isMerge;

    /** default commit constructor*/
    public Commit() {
        this.message = "initial commit";
        Date now = new Date(0);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
//        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.timestamp = sdf.format(now);

//        Date now = new Date(0);
//        this.timestamp = String.format("%ta %tb %td %tT %tY %tz", now, now, now, now, now, now);

        this.parentId = null;
        this.parentIdTwo = null;
        this.blobs = new TreeMap<>();
        this.commitId = commitSha();
        this.shortId = commitSha().substring(0, 6);
        this.isMerge = false;
    }

    /** commit constructor with parameters */
    public Commit(String parent, String msg) {
        // instance variables
        this.parentId = parent;
        this.parentIdTwo = null;

//        Date now = new Date();
//        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
////        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
//        this.timestamp = sdf.format(now);

        Date now = new Date();
        this.timestamp = String.format("%ta %tb %td %tT %tY %tz", now, now, now, now, now, now);

        this.commitId = commitSha();
        this.shortId = commitSha().substring(0, 6);
        this.message = msg;
        this.blobs = new TreeMap<>();
        this.isMerge = false;

        // copy the blobs from parent head
        TreeMap<String, String> parentBlobs = fromCommitFile(parentId).getBlobs();
        for (String i : parentBlobs.keySet()) {
            this.blobs.put(i, parentBlobs.get(i));
        }

        // update the blobs TreeMap if something changed
        for (File f : ADDITION_DIR.listFiles()) {
            this.blobs.put(f.getName(), readContentsAsString(f));

            //clear the staging area after a commit
            f.delete();
        }
        // update the blobs TreeMap if something changed
        for (File f : REMOVAL_DIR.listFiles()) {
            this.blobs.remove(f.getName());

            //clear the staging area after a commit
            f.delete();
        }
    }

    //merge commit constructor
    public Commit(Branch givenBranch, Branch activeBranch) {
        // instance variables
        this.parentId = activeBranch.getCommitId();
        this.parentIdTwo = givenBranch.getCommitId();
        Date now = new Date();
//        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
//        this.timestamp = sdf.format(now);
        this.timestamp = String.format("%ta %tb %td %tT %tY %tz", now, now, now, now, now, now);
        this.commitId = commitSha();
        this.shortId = commitSha().substring(0, 6);
        this.message = "Merged " + givenBranch.getBranchName() + " into "
                + activeBranch.getBranchName() + ".";
        this.blobs = new TreeMap<>();
        this.isMerge = true;

        // copy the blobs from parent head
        TreeMap<String, String> parentBlobs = fromCommitFile(parentId).getBlobs();
        for (String i : parentBlobs.keySet()) {
            this.blobs.put(i, parentBlobs.get(i));
        }
//        TreeMap<String, String> parentBlobsTwo = fromCommitFile(parentIdTwo).getBlobs();
//        for (String i : parentBlobs.keySet()) {
//            this.blobs.put(i, parentBlobsTwo.get(i));
//        }

        // update the blobs TreeMap if something changed
        for (File f : ADDITION_DIR.listFiles()) {
            this.blobs.put(f.getName(), readContentsAsString(f));

            //clear the staging area after a commit
            f.delete();
        }

        // update the blobs TreeMap if something changed
        for (File f : REMOVAL_DIR.listFiles()) {
            this.blobs.remove(f.getName());

            //clear the staging area after a commit
            f.delete();
        }
    }

    /** get the long id based on the short id */
    public static String getLongId(String abbrId) {
        for (String id : plainFilenamesIn(COMMIT_DIR)) {
            if (id.contains(abbrId)) {
                return id;
            }
        }
        return null;
    }

    /** create the commitId */
    public String commitSha() {
        return Utils.sha1(Utils.serialize(this));
    }

    /** get the commitId */
    public String getCommitId() {
        return this.commitId;
    }

    /** Reads in and deserializes a commit from a file with id in COMMIT_DIR. */
    public static Commit fromCommitFile(String id) {
        File inFile = Utils.join(COMMIT_DIR, id);
        if (!inFile.exists()) {
            return null;
        } else {
            Commit c = readObject(inFile, Commit.class);
            return c;
        }
    }

    /** Saves a commit to a file for future use. */
    public void saveCommits() {
        //commitId is unique
        File outfile = Utils.join(COMMIT_DIR, commitId);
        writeObject(outfile, this);
    }

    /** Saves split point */
    public void saveSplitPoint() {
        writeContents(SPLIT_DIR, this.commitId);
    }

    /** get mothod
     *
     * 1. message
     * 2. timestamp
     * 3. parent
     * 4. blobs
     * */
    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getParent() {
        return parentId;
    }

    public String getParentIdTwo() {
        return parentIdTwo;
    }

    /** storing the pairs of <fileName, blobId> */
    public TreeMap<String, String> getBlobs() {
        return blobs;
    }

    //print log message based on current commit
    public String printLogMsg() {
        String log = "===\n"
                + "commit " + this.getCommitId() + "\n"
                + "Date: " + this.getTimestamp() + "\n"
                + this.getMessage() + "\n";
        return log;
    }

    //print log message based on commit id
    public String printLogMsg(String c) {
        Commit temp = Commit.fromCommitFile(c);
        String log = "===\n"
                + "commit " + temp.getCommitId() + "\n"
                + "Date: " + temp.getTimestamp() + "\n"
                + temp.getMessage() + "\n";
        return log;
    }

}
