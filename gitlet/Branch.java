package gitlet;

import java.io.*;

import static gitlet.Repository.*;
import static gitlet.Utils.*;
import static gitlet.Commit.*;

/** blobs: The saved contents of files. Since Gitlet saves many versions of files,
 * a single file might correspond to multiple blobs: each being tracked in a
 * different commit.*/

public class Branch implements Serializable {

//    public String getBranchName;
    /** create instance variables */
    private String branchName;
    private String pointerCommitId;
//    private String parentId;
    private Commit headCommit;
//    private TreeMap<String, String> branch;
//    public boolean current;

    /** default branch constructor
     * including branchName, commitId
     * */
    //initial branch, only the master
    public Branch(Commit initialCommit) {
        this.branchName = "master";
        this.headCommit = initialCommit;
        this.pointerCommitId = initialCommit.getCommitId();

    }

    public Branch(Commit headCommit, String newBranchName) {
        this.branchName = newBranchName;
        this.pointerCommitId = headCommit.getCommitId();
        this.headCommit = headCommit;
    }

    /** Reads in and deserializes a blob from a file with name fileName in BLOBS_DIR. */
    public static Branch fromBranchFile(String branchName) {
        File inFile = Utils.join(BRANCH_DIR, branchName);
        if (!inFile.exists()) {
            return null;
        } else {
            Branch b = readObject(inFile, Branch.class);
            return b;
        }
    }

    public Commit getHeadCommit() {
        return headCommit;
    }

    public String getBranchName() {
        return this.branchName;
    }

    public String getCommitId() {
        return this.pointerCommitId;
    }

    /** save a branch for future use */
    public void saveBranch() {
        //branchName as fileName, object as content
        File outFile = Utils.join(BRANCH_DIR, this.branchName);
        writeObject(outFile, this);
    }

    public void saveActiveBranch() {
//        File outFile = Utils.join(ACTIVE_BRANCH_DIR, branchName);
        writeContents(ACTIVE_BRANCH_DIR, this.branchName);
    }

    /** create a HEAD.txt and save the commitId to HEAD for future use */
    public static void setupPointer(File file, String commitId) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Utils.writeContents(file, commitId);
    }

    /** return the head commitId */
    public static String getHead() {
        return Utils.readContentsAsString(HEAD);
    }

}
