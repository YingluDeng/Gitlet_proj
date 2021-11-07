package gitlet;

import java.io.File;
import java.io.*;
import java.util.*;

import static gitlet.Utils.*;
import static gitlet.Branch.*;

/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author Yinglu Deng
 */
public class Repository implements Serializable {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** current working directory */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** .gitlet directory */
    public static final File GITLET_DIR = Utils.join(CWD, ".gitlet");
    /** .commit directory */
    public static final File COMMIT_DIR = Utils.join(GITLET_DIR, ".commit");
    public static final File SPLIT_DIR = Utils.join(GITLET_DIR, "splitPoint.txt");

    /** .stage directory */
    public static final File STAGING_DIR = Utils.join(GITLET_DIR, ".stage");
    public static final File ADDITION_DIR = Utils.join(STAGING_DIR, ".addition");
    public static final File REMOVAL_DIR = Utils.join(STAGING_DIR, ".removal");
    /** .blobs directory */
    public static final File BLOBS_DIR = Utils.join(GITLET_DIR, ".blobs");
    /** .repository (file representing serialization) */
    public static final File REPO_DIR = Utils.join(GITLET_DIR, ".repository");
    public static final File HEAD = Utils.join(GITLET_DIR, "head.txt");
    /** .branches directory */
    public static final File BRANCH_DIR = Utils.join(GITLET_DIR, ".branch");
    public static final File ACTIVE_BRANCH_DIR = Utils.join(GITLET_DIR, "activeBranch.txt");
    /**  */
    public static final File MERGE_DIR = Utils.join(GITLET_DIR, ".merge");
    /** .remote directory */
    public static final File REMOTES_DIR = Utils.join(GITLET_DIR, ".remotes");

    public String getActiveBranchName() {
        return readContentsAsString(ACTIVE_BRANCH_DIR);
    }

    public Branch getActiveBranch() {
        return Branch.fromBranchFile(getActiveBranchName());
    }

    public Commit getSplitCommit() {
        String s = Utils.readContentsAsString(SPLIT_DIR);
        Commit c = Commit.fromCommitFile(s);
        return c;
    }

    /** Creates a new Gitlet version-control system in the current directory. */
    public static void initRepo() {
        if (GITLET_DIR.exists()) {
            Utils.message("A Gitlet version-control system already "
                    + "exists in the current directory.");
            System.exit(0);
        } else {
            GITLET_DIR.mkdir();
            COMMIT_DIR.mkdir();
            STAGING_DIR.mkdir();
            ADDITION_DIR.mkdir();
            REMOVAL_DIR.mkdir();
            BLOBS_DIR.mkdir();
            REPO_DIR.mkdir();
            BRANCH_DIR.mkdir();
            MERGE_DIR.mkdir();
//            SPLIT_DIR.mkdir();
            try {
                SPLIT_DIR.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                ACTIVE_BRANCH_DIR.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                HEAD.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // start with one commit
            Commit initialCommit = new Commit();
            initialCommit.saveCommits();
            String commitId = initialCommit.getCommitId();

            // update head
            setupPointer(HEAD, commitId);

            // initialize a master branch and have it point to initial commit
            Branch master = new Branch(initialCommit);
            master.saveBranch();
            master.saveActiveBranch();
        }
    }


    /** staging the file for addition */
    public static void addRepo(String fileName) {
        File f = Utils.join(CWD, fileName);
        if (!f.exists()) {
            Utils.message("File does not exist.");
            System.exit(0);
        } else {
            // make a new blob
            Blobs newBlob = new Blobs(fileName);
            newBlob.saveBlob();

            //find the commit object based on HEAD pointer
            Commit trackHead = Commit.fromCommitFile(getHead());
            TreeMap<String, String> headBlobs = trackHead.getBlobs();


            // If the current working version of the file is identical to the
            // version in the current commit, do not stage it to be added,
            //  and remove it from the staging area if it is already there
            if (headBlobs.containsKey(fileName)
                    && (headBlobs.get(fileName).equals(newBlob.getBlobId()))) {
                for (File file : ADDITION_DIR.listFiles()) {
                    if (file.getName() == fileName) {
                        file.delete();
                    }
                }
            } else {
                //Staging an already-staged file overwrites the previous
                // entry in the staging area with the new contents.
                File outFile = Utils.join(ADDITION_DIR, fileName);
                for (File file : ADDITION_DIR.listFiles()) {
                    if (file.getName().equals(fileName)) {
                        file.delete();
                    }
                }

                try {
                    outFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                writeContents(outFile, newBlob.getBlobId());
            }

            //if you add a file that is staged for removal, it is no longer staged for removal
            for (File f2 : REMOVAL_DIR.listFiles()) {
                if (f2.getName().equals(fileName)) {
                    f2.delete();
                }
            }
        }
    }

    /**
     * Unstage the file if it is currently staged for addition. If the file is tracked
     * in the current commit, stage it for removal and remove the file from the working
     * directory if the user has not already done so (do not remove it unless it is
     * tracked in the current commit).
     * */
    public static void rmRepo(String fileName) {
        //If the file is neither staged nor tracked by the head commit
        File f1 = Utils.join(ADDITION_DIR, fileName);
//        File f2 = Utils.join(REMOVAL_DIR, fileName);
        //tracked by the head commit
        Commit headCommit = Commit.fromCommitFile(getHead());

        if (!f1.exists() && !headCommit.getBlobs().containsKey(fileName)) {
            Utils.message("No reason to remove the file.");
            System.exit(0);
        } else {
            //Unstage the file if it is currently staged for addition.
            for (File f : ADDITION_DIR.listFiles()) {
                if (f.getName().equals(fileName)) {
                    f.delete();
                }
            }

            //If the file is tracked in the current commit, stage it for removal and remove the
            // file from the working directory if the user has not already done so
//            Commit headCommit = Commit.fromCommitFile(getHead());
            if (headCommit.getBlobs().containsKey(fileName)) {
//            // make a new blob
//            Blobs newBlob = new Blobs(fileName);
//            newBlob.saveBlob();

                //stage it for removal
                File outFile = Utils.join(REMOVAL_DIR, fileName);
                try {
                    outFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //write blobs id as content
                writeContents(outFile, headCommit.getBlobs().get(fileName));

                //remove file from the working directory
                File f3 = Utils.join(CWD, fileName);
                if (f3.exists()) {
                    f3.delete();
                }
            }
        }
    }

    public void commitRepo(String msg) {
//        if (ADDITION_DIR.list().length == 0 && REMOVAL_DIR.list().length == 0) {
        if ((plainFilenamesIn(ADDITION_DIR).size() == 0)
                && (plainFilenamesIn(REMOVAL_DIR).size() == 0)) {
            Utils.message("No changes added to the commit.");
            System.exit(0);
        } else {

            //save splitCommit
            for (String bName : plainFilenamesIn(BRANCH_DIR)) {
                //if other branch's parentId = activeBranch's id, then it is the split point
                Commit temp = Branch.fromBranchFile(bName).getHeadCommit();
                String pID = temp.getParent();
                String pID2 = temp.getParentIdTwo();
                String activeId = getActiveBranch().getCommitId();
                if (activeId.equals(pID) || activeId.equals(pID2)) {
                    //write commit id into splitPoint.txt
                    writeContents(SPLIT_DIR, activeId);
                }
            }

            // clone the HEAD commit
            String parentId = getHead();

            // modify its message and timestamp according to user input
            Commit newCommit = new Commit(parentId, msg);
            newCommit.saveCommits();

            //write back any new object made by any modified objects read earlier
            String currentHead = newCommit.getCommitId();
            Utils.writeContents(HEAD, currentHead);

            //update branch location
            Branch activeB = new Branch(newCommit, getActiveBranchName());
            activeB.saveBranch();
            activeB.saveActiveBranch();

            //clear staging area
            clearStage();

        }
    }



    //clear the staging place
    public void clearStage() {
        for (File file : ADDITION_DIR.listFiles()) {
            file.delete();
        }
        for (File file : REMOVAL_DIR.listFiles()) {
            file.delete();
        }
    }

    // java gitlet.Main checkout -- [file name]
    public static void checkoutRepo1(String fileName) {
        Commit headCommit = Commit.fromCommitFile(getHead());
        if (headCommit == null) {
            Utils.message("File does not exist in that commit.");
            System.exit(0);
        } else {
            // read the previous commit
//            Commit headCommit = Commit.fromCommitFile(getHead());

            // read from blob content
            Blobs b = Blobs.fromBlobFile(headCommit.getBlobs().get(fileName));

            byte[] content = b.getBlobContent();
            b.saveBlob();

            // puts it in the working directory
            File outFile = Utils.join(CWD, fileName);
            if (!outFile.exists()) {
                try {
                    outFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // overwriting the version of the file that’s already there if there is one
            writeContents(outFile, content);

        }

    }

    /**
     * java gitlet.Main checkout [commit id] -- [file name]
     * <Key : fileName, value : blobId>
     * */
    public static void checkoutRepo2(String id, String fileName) {
        //read the checkout commit
        Commit headCommit = Commit.fromCommitFile(id);

        // If no commit with the given id exists
        if (headCommit == null) {
            Utils.message("No commit with that id exists.");
            System.exit(0);
        } else if (!headCommit.getBlobs().containsKey(fileName)) {
            //if the file does not exist in the given commit
            Utils.message("File does not exist in that commit.");
            System.exit(0);
        } else {
            // Takes the version of the file as it exists in the commit with the given id
            Commit prevVersion = Commit.fromCommitFile(id);

            Blobs b = Blobs.fromBlobFile(prevVersion.getBlobs().get(fileName));

            byte[] content = b.getBlobContent();

            // puts it in the working directory
            File outFile = Utils.join(CWD, fileName);
            if (!outFile.exists()) {
                try {
                    outFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // overwriting the version of the file that’s already there if there is one
            writeContents(outFile, content);
        }
    }

    public void checkoutShort(String shortId, String fileName) {
        String longId = "";
        for (String id : plainFilenamesIn(COMMIT_DIR)) {
            if (id.contains(shortId)) {
                longId = id;
            }
        }
        checkoutRepo2(longId, fileName);
    }

    /**
     * 1. overwriting the versions of the files that are already there if they exist.
     * 2. Any files that are tracked in the current branch but are not present in the
     * checked-out branch are deleted. The staging area is cleared
     * 3. given branch will now be considered the current branch (HEAD).*/
    public void checkoutRepo3(String branchName) {
        //Case 1: If no branch with that name exists.
        File checkIfExist = Utils.join(BRANCH_DIR, branchName);
        if (!checkIfExist.exists()) {
            Utils.message("No such branch exists.");
            System.exit(0);
        }

        //Case 2: If that branch is the current branch.
        String activeBranchName = readContentsAsString(ACTIVE_BRANCH_DIR);
        if (branchName.equals(activeBranchName)) {
            Utils.message("No need to checkout the current branch.");
            System.exit(0);
        }

        Branch checkoutBranch = Branch.fromBranchFile(branchName);
        TreeMap<String, String> currentBlob = getActiveBranch().getHeadCommit().getBlobs();
        TreeMap<String, String> checkoutBlob = checkoutBranch.getHeadCommit().getBlobs();
        //Case 3: If a working file is untracked in the current branch and would be
        //overwritten by the checkout. see if every files in CWD existed in current branch
        for (String fName : Utils.plainFilenamesIn(CWD)) {
            Blobs b = new Blobs(fName);
            String bId = b.getBlobId();
            //1. cwd version is different from checkout version (same file name)
            if ((checkoutBlob.containsKey(fName))
                    && (!currentBlob.containsKey(fName))) {
                if (!checkoutBlob.get(fName).equals(bId)) {
                    Utils.message("There is an untracked file in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                }
            }

            //2. same file name in cwd, current, checkout (all have different content)
            if (checkoutBlob.containsKey(fName) && currentBlob.containsKey(fName)) {
                if ((!checkoutBlob.get(fName).equals(bId))
                        && (!currentBlob.get(fName).equals(bId))) {
//                    System.out.println(bId + " " + fName);
                    Utils.message("There is an untracked file in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                }
            }
        }

        //put all given branch file to CWD
        if (!checkoutBlob.isEmpty()) {
            for (String fileName : checkoutBlob.keySet()) {
                Blobs putBlob = Blobs.fromBlobFile(checkoutBlob.get(fileName));
                File file = Utils.join(CWD, fileName);
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                writeContents(file, putBlob.getBlobString());
            }
        }

        //Any files that are tracked in the current branch but are not
        // present in the checked-out branch are deleted.
        for (String fName : plainFilenamesIn(CWD)) {
            File file = Utils.join(CWD, fName);
            if (!checkoutBlob.containsKey(file.getName())) {
                if (currentBlob.containsKey(file.getName())) {
                    file.delete();
                }
            }
        }

        //change head
        writeContents(HEAD, checkoutBranch.getCommitId());
        //update the commit
        Commit headCommit = Commit.fromCommitFile(getHead());
        headCommit.saveCommits();
        //update and save branch
        Branch newBranch = new Branch(headCommit, branchName);
        newBranch.saveBranch();
        newBranch.saveActiveBranch();
    }




    /** Starting at the current head commit, display information about each commit backwards along
     * the commit tree until the initial commit, following the first parent commit links, ignoring
     * any second parents found in merge commits.*/
    public void logRepo() {
//        if (getActiveBranchName().equals("master")) {
//            System.out.println(getActiveBranchName() + " " + getActiveBranch().getCommitId());
//        }

        //keep track from the head commit
        Commit temp = Commit.fromCommitFile(getHead());
        String pointer = getHead();

        //if the pointer has parent
        while (temp.getParent() != null && temp != null) {
            Utils.message(temp.printLogMsg());
            pointer = temp.getParent();
            temp = Commit.fromCommitFile(temp.getParent());
        }
        Utils.message(temp.printLogMsg(pointer));
    }

    /** Like log, except displays information about all commits ever made.
     * The order of the commits does not matter.
     *
     *
     * There is a difference when it comes to branches. log will follow the start commit back up
     * through its parents until the initial commit. However it will not display commits of branches
     * that are not it's parents. Global log however will show all commits (this is also why the
     * order doesn't matter)
     *
     *
     * gitlet.Utils that will help you iterate over files within a directory.
     * */
    public static void globalLogRepo() {
        //iterate each commit id save in the commit_dir
        for (String commitId : plainFilenamesIn(COMMIT_DIR)) {
            Commit temp = Commit.fromCommitFile(commitId);
            Utils.message(temp.printLogMsg(commitId));
        }
    }

    public static void findRepo(String msg) {
        int count = plainFilenamesIn(COMMIT_DIR).size();
        for (String commitId : plainFilenamesIn(COMMIT_DIR)) {
            if (Commit.fromCommitFile(commitId).getMessage().equals(msg)) {
                System.out.println(commitId);
            } else {
                count--;
            }
        }
        if (count == 0) {
            Utils.message("Found no commit with that message.");
            System.exit(0);
        }

    }



//    /** Displays what branches currently exist, and marks the current branch with a *.
//     * Also displays what files have been staged for addition or removal.*/
    public void statusRepo() {
        if (!REPO_DIR.exists()) {
            Utils.message("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        //for files present in the working directory but neither staged for addition nor tracked.
        System.out.println("=== Branches ===");
        for (String n : plainFilenamesIn(BRANCH_DIR)) {
            Branch b = Branch.fromBranchFile(n);
            if (b.getBranchName().equals(readContentsAsString(ACTIVE_BRANCH_DIR))) {
                System.out.println("*" + n);
            } else {
                System.out.println(n);
            }
        }

        System.out.println("\n=== Staged Files ===");
        for (String f : plainFilenamesIn(ADDITION_DIR)) {
            System.out.println(f);
        }

        System.out.println("\n=== Removed Files ===");
        for (String f : plainFilenamesIn(REMOVAL_DIR)) {
            System.out.println(f);
        }

        System.out.println("\n=== Modifications Not Staged For Commit ===");

        TreeMap<String, String> currentFile = getActiveBranch().getHeadCommit().getBlobs();
        System.out.println("\n=== Untracked Files ===");
        for (String f: Utils.plainFilenamesIn(CWD)) {
            if (!f.equals("head.txt") && !f.equals("activeBranch.txt")
                    && !f.equals("splitPoint.txt") && !f.equals(".gitlet")
                    && !currentFile.containsKey(f)
                    && !Utils.plainFilenamesIn(ADDITION_DIR).contains(f)
                    && !Utils.plainFilenamesIn(REMOVAL_DIR).contains(f))  {
                System.out.println(f);
            }
        }

    }



    /**
     * Creates a new branch with the given name, and points it at the current head commit.
     * branchName --> commitId
     *
     * */
    public static void branchRepo(String branchName) {
        File checkBranch = Utils.join(BRANCH_DIR, branchName);
        if (checkBranch.exists()) {
            Utils.message("A branch with that name already exists.");
            System.exit(0);
        } else {
            //head commit
            Commit headCommit = Commit.fromCommitFile(getHead());
            Branch newBranch = new Branch(headCommit, branchName);
            newBranch.saveBranch();
        }
    }

    /** Deletes the branch with the given name. (only delete the branch pointer) */
    public void rmBranchRepo(String branchName) {
        File checkIfExist = Utils.join(BRANCH_DIR, branchName);
        if (!checkIfExist.exists()) {
            Utils.message("A branch with that name does not exist.");
            System.exit(0);
//        } else if (getHead().equals(fromBranchFile(branchName).getCommitId())) {
        } else if (getActiveBranchName().equals(branchName)) {
            Utils.message("Cannot remove the current branch.");
            System.exit(0);
        } else {
            File f = Utils.join(BRANCH_DIR, branchName);
            f.delete();
        }
    }

    /** Checks out all the files tracked by the given commit. Removes tracked files that are
     * not present in that commit. Also moves the current branch’s head to that commit node.
     * See the intro for an example of what happens to the head pointer after using reset.
     * The [commit id] may be abbreviated as for checkout. The staging area is cleared.
     * The command is essentially checkout of an arbitrary commit that also changes the
     * current branch head.*/
    //checkout + rm
    public void resetRepo(String commitId) {
        String longId = Commit.getLongId(commitId);
        if (longId == null) {
            Utils.message("No commit with that id exists.");
            System.exit(0);
        }

//        Branch checkoutBranch = Branch.fromBranchFile(branchName);
        TreeMap<String, String> currentBlob = getActiveBranch().getHeadCommit().getBlobs();
        TreeMap<String, String> checkoutBlob = Commit.fromCommitFile(commitId).getBlobs();
        //Case 3: If a working file is untracked in the current branch and would be
        //overwritten by the checkout. see if every files in CWD existed in current branch
        for (String fName : Utils.plainFilenamesIn(CWD)) {
            Blobs b = new Blobs(fName);
            String bId = b.getBlobId();
            //1. cwd version is different from checkout version (same file name)
            if ((checkoutBlob.containsKey(fName))
                    && (!currentBlob.containsKey(fName))) {
                if (!checkoutBlob.get(fName).equals(bId)) {
                    Utils.message("There is an untracked file in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                }
            }

            //2. same file name in cwd, current, checkout (all have different content)
            if (checkoutBlob.containsKey(fName) && currentBlob.containsKey(fName)) {
                if ((!checkoutBlob.get(fName).equals(bId))
                        && (!currentBlob.get(fName).equals(bId))) {
                    Utils.message("There is an untracked file in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                }
            }
        }

//        //same file name but different content
//        TreeMap<String, String> resetCommitBlob = Commit.fromCommitFile(commitId).getBlobs();
//        for (String f : plainFilenamesIn(CWD)) {  //find blob based on fileName
//            Blobs temp = new Blobs(f);
//            if (resetCommitBlob.keySet().contains(f)
//                    && !resetCommitBlob.get(f).equals(temp.getBlobId())) {
//                Utils.message("There is an untracked file in the way; delete it, "
//                     + "or add and commit it first.");
//                System.exit(0);
//            }
//        }

        //Remove all the files in the CWD
        for (String f : plainFilenamesIn(CWD)) {
            File file = Utils.join(CWD, f);
            file.delete();
        }

        //Checks out all the files tracked by the given commit
        Commit temp = Commit.fromCommitFile(Commit.getLongId(commitId));
        for (String fileName : temp.getBlobs().keySet()) {
            checkoutShort(commitId, fileName);
        }

        //clearing the staging area
        clearStage();

        //moves the current branch’s head  and head to that commit node
        String activeBranchName = readContentsAsString(ACTIVE_BRANCH_DIR);
        Branch moveBranch = new Branch(temp, activeBranchName);
        //save the branch
        moveBranch.saveBranch();
        moveBranch.saveActiveBranch();
        setupPointer(HEAD, temp.getCommitId());
    }

    public void mergeRepo(String branchName) {
        //staged additions or removals present
        if ((plainFilenamesIn(ADDITION_DIR).size() != 0)
                || (plainFilenamesIn(REMOVAL_DIR).size() != 0)) {
            Utils.message("You have uncommitted changes.");
            System.exit(0);
        }
        //branch doesn't exist
        if (!plainFilenamesIn(BRANCH_DIR).contains(branchName)) {
            Utils.message("A branch with that name does not exist.");
            System.exit(0);
        }
        //merge itself
        if (getActiveBranchName().equals(branchName)) {
            Utils.message("Cannot merge a branch with itself.");
            System.exit(0);
        }

        //an untracked file in the current commit would be overwritten or deleted by the merge
        Branch givenBranch = Branch.fromBranchFile(branchName);
        TreeMap<String, String> currentBlob = getActiveBranch().getHeadCommit().getBlobs();
        TreeMap<String, String> checkoutBlob = givenBranch.getHeadCommit().getBlobs();
        //Case 3: If a working file is untracked in the current branch and would be
        //overwritten by the checkout. see if every files in CWD existed in current branch
        for (String fName : Utils.plainFilenamesIn(CWD)) {
            Blobs b = new Blobs(fName);
            String bId = b.getBlobId();
            //1. cwd version is different from checkout version (same file name)
            if ((checkoutBlob.containsKey(fName))
                    && (!currentBlob.containsKey(fName))) {
                if (!checkoutBlob.get(fName).equals(bId)) {
                    Utils.message("There is an untracked file in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                }
            }
            //2. same file name in cwd, current, checkout (all have different content)
            if (checkoutBlob.containsKey(fName) && currentBlob.containsKey(fName)) {
                if ((!checkoutBlob.get(fName).equals(bId))
                        && (!currentBlob.get(fName).equals(bId))) {
                    Utils.message("There is an untracked file in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                }
            }
        }
        //split point's commit id = given branch's commit id -> done!
        if (findPrev(getActiveBranch().getCommitId()).contains(givenBranch.getCommitId())) {
            Utils.message("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        //split point = activeBranch -- checkout activeBranch
        if (findPrev(givenBranch.getCommitId()).contains(getActiveBranch().getCommitId())) {
            checkoutRepo3(branchName);
            Utils.message("Current branch fast-forwarded.");
            System.exit(0);
        }
        //if no error case, start merge
        Commit givenCommit = Branch.fromBranchFile(branchName).getHeadCommit();
        Commit currentCommit = Commit.fromCommitFile(getHead());
        Commit splitCommit = getSplitPoint(currentCommit, givenCommit);
        boolean conflictExist = mergeHelper(splitCommit,
                getActiveBranch().getHeadCommit(), givenCommit);

        //update the commit
        Commit newCommit = new Commit(givenBranch, getActiveBranch());
        newCommit.saveCommits();

        //update head and branch location
        writeContents(HEAD, newCommit.getCommitId());
        //update and save branch
        Branch newBranch = new Branch(newCommit, getActiveBranchName());
        newBranch.saveBranch();
//        newBranch.saveActiveBranch();

        if (conflictExist) {
            Utils.message("Encountered a merge conflict.");
//            System.exit(0);
        }
    }

    /** find if they are on one line */
    public ArrayList<String> findPrev(String commitId) {
        Commit c = Commit.fromCommitFile(commitId);
        ArrayList<String> parents = new ArrayList<>();
        while (c != null) {
            String pID = c.getParent();
            if (pID != null) {
                parents.add(pID);
                c = Commit.fromCommitFile(pID);
            } else {
                break;
            }
        }
        return parents;
    }

    /** return the near ancestor of current branch and given branch. */
    public Commit getSplitPoint(Commit currentCommit, Commit givenCommit) {
        TreeMap<String, String> curr = new TreeMap<>();
        TreeMap<String, String> visited = new TreeMap<>();
        Queue<Commit> findSplit = new ArrayDeque<>();

        //store the <id, msg>
        visited.put(givenCommit.getCommitId(), givenCommit.getMessage());

        splitHelper(givenCommit, visited);
        findSplit.add(currentCommit);
        while (!findSplit.isEmpty()) {
            //returns and removes the element at the front the container
            Commit f = findSplit.poll();  //the latest one
            if (f.getParent() != null && !curr.containsKey(f.getCommitId())) {
                curr.put(f.getCommitId(), f.getMessage());
                if (visited.containsKey(f.getCommitId())) {
                    return f;
                }

                if (f.getParentIdTwo() != null) {
                    //two parents
                    Commit parent2 = Commit.fromCommitFile(f.getParentIdTwo());
                    if (visited.containsKey(parent2.getCommitId())) {
                        return parent2;
                    }
                    if (!curr.containsKey(parent2.getCommitId())) {
                        findSplit.add(parent2);
                    }
                }
                if (f.getParent() != null) {
                    //one parents
                    Commit parent1 = Commit.fromCommitFile(f.getParent());
                    if (visited.containsKey(parent1.getCommitId())) {
                        return parent1;
                    }
                    if (!curr.containsKey(parent1.getCommitId())) {
                        findSplit.add(parent1);
                    }
                }
            }
        }
        return null;
    }


    public void splitHelper(Commit curr, TreeMap<String, String> checkBox) {
        if (curr.getParent() == null) {
            checkBox.put(curr.getCommitId(), curr.getMessage());
            return;
        }

        //check the first parent
        if (curr.getParent() != null) {
            String first = curr.getParent();
            Commit firstP = Commit.fromCommitFile(first);
            if (!checkBox.containsKey(firstP.getCommitId())) {
                checkBox.put(firstP.getCommitId(), firstP.getMessage());
            }
            splitHelper(firstP, checkBox);
        }

        //check the second parent
        if (curr.getParentIdTwo() != null) {
            String second = curr.getParentIdTwo();
            Commit secondP = Commit.fromCommitFile(second);
            if (!checkBox.containsKey(secondP.getCommitId())) {
                checkBox.put(secondP.getCommitId(), secondP.getMessage());
            }
            splitHelper(secondP, checkBox);
        }
    }

    public boolean mergeHelper(Commit splitPoint, Commit headCommit, Commit givenCommit) {
        TreeMap<String, String> split = splitPoint.getBlobs();
        TreeMap<String, String> currentBlob = headCommit.getBlobs(); //<fileName, blobId>
        TreeMap<String, String> givenBlobs = givenCommit.getBlobs();
        boolean conflict = false;
        //Create a HashSet of all file names in other, split, and current
        HashSet<String> result = new HashSet<>();
        for (String a : split.keySet()) {
            result.add(a);
        }
        for (String b : givenBlobs.keySet()) {
            result.add(b);
        }
        for (String c : currentBlob.keySet()) {
            result.add(c);
        }
        for (String f : result) {
            //get the blob id
            String currBlobId = currentBlob.get(f);
            String splitBlobId = split.get(f);
            String givenBlobId = givenBlobs.get(f);
            if (split.containsKey(f)) {  // same file at split
                if ((!currentBlob.containsKey(f)) && (!givenBlobs.containsKey(f))) {
                    continue; //modified same way
                } else if ((!currentBlob.containsKey(f))
                        && givenBlobs.containsKey(f)) { // curr none, given has
                    if (givenBlobs.get(f).equals(split.get(f))) {
                        continue; // equal content, do nothing
                    } else {  // conflict
                        conflict = true;
                        mergeConflict(currBlobId, givenBlobId, f, false, false);
                        addRepo(f);
                    }
                } else if (currentBlob.containsKey(f)
                        && !givenBlobs.containsKey(f)) { // curr has, given none
                    if (currentBlob.get(f).equals(split.get(f))) {
                        rmRepo(f); // remove curr
                    } else {  // conflict (content different)
                        conflict = true;
                        mergeConflict(currBlobId, givenBlobId, f, true, false);
                        addRepo(f);
                    }
                } else { // both have file
                    if (currBlobId.equals(givenBlobId)) {
                        continue; // same case
                    } else if (splitBlobId.equals(givenBlobId) && !splitBlobId.equals(currBlobId)) {
                        continue;
                    } else if (splitBlobId.equals(currBlobId) && !splitBlobId.equals(givenBlobId)) {
                        checkoutRepo2(givenCommit.getCommitId(), f);
                        addRepo(f);
                    } else {  // conflict;
                        conflict = true;
                        mergeConflict(currBlobId, givenBlobId, f, true, true);
                        addRepo(f);
                    }
                }
            } else {  // file is not in split point
                if (currentBlob.containsKey(f) && !givenBlobs.containsKey(f)) {
                    continue; //case 4
                } else if (currentBlob.containsKey(f) && givenBlobs.containsKey(f)) {
                    if (currentBlob.get(f).equals(givenBlobs.get(f))) {
                        continue; // content same
                    } else { // conflict
                        conflict = true;
                        mergeConflict(currBlobId, givenBlobId, f, true, true);
                        addRepo(f);
                    }
                } else if (!currentBlob.containsKey(f) && givenBlobs.containsKey(f)) {
                    checkoutRepo2(givenCommit.getCommitId(), f);  // case 5
                    addRepo(f);

                } else { //both none
                    continue;
                }
            }
        }
        return conflict;
    }

    //conflict file content
    public void mergeConflict(String currBlobId, String givenBlobId,
                              String fileName, boolean curr, boolean given) {

        String blobContent = "";
        if (currBlobId != null) {
            blobContent = Blobs.fromBlobFile(currBlobId).getBlobString();
        }

        String givenContent = "";
        if (givenBlobId != null) {
            givenContent = Blobs.fromBlobFile(givenBlobId).getBlobString();
        }

        String msg = "";
        msg += "<<<<<<< HEAD\n";
        if (curr) {
            msg += blobContent;
            msg += "\n";
        }
        msg += "=======\n";
        if (given) {
            msg += givenContent;
            msg += "\n";
        }
        msg += ">>>>>>>";
        if (curr) {
            File inFile = Utils.join(CWD, fileName);
            Utils.writeContents(inFile, msg);
        }
    }
}


