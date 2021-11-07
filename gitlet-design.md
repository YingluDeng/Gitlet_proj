# Gitlet Design Document

**Name**: Yinglu Deng

## Classes and Data Structures

### Main.java
java gitlet.Main init

java gitlet.Main add [filename]

java gitlet.Main commit [message]

java gitlet.Main log

java gitlet.Main checkout -- [file name]

java gitlet.Main checkout [commit id] -- [file name]

### Command.java
a Main.java helper

    if (the error cases happened) {
        print error message
    } else {
        assign to Repository method
    }

### Repository.java
* directory:


    .gitlet
        commit -- <commitId, blobId>
        splitPoint.txt -- save the split point commit id
        stage
            addition -- <fileName, blobId> save for staging addition
            removal
        blobs -- <blobId, blobContent> save the blobs for future use
        repository
        head.txt --save the head commitId
        branch -- save some branch names and write commitId as content
        activeBranch.txt -- save the avtive branch name
        

Method:

* support method:
 
  public String getActiveBranchName()
  
  public Branch getActiveBranch()
  
  public Commit getSplitCommit()


* main method:
  
    * init
  
        public void initRepo()

    * add

        public void addRepo(String fileName)

    * commit

        public void commitRepo(String msg)

    * checkout
    
        public void checkoutRepo1(String fileName)
    
        public void checkoutRepo2(String id, String fileName)
    
        public void checkoutShort(String shortId, String fileName)
    
        public void checkoutRepo3(String branchName)
    
    * log
    
        public static void logRepo()

        public static void globalLogRepo()

        public static void findRepo(String msg)
        
        public void statusRepo()
        
        public static void branchRepo(String branchName)
        
        public void rmBranchRepo(String branchName)
        
        public void resetRepo(String commitId)
        
        public void mergeRepo(String branchName)

    public static String getHead()
    
    public static void setupHead(String commitId)
 
### Blobs.java
Blobs (String fileName, String blobId, byte[] blobContent)

Method:

public static Blobs fromBlobFile(String blobId)

public void saveBlob()

public String hash()

public String getFileName()

public String getBlobId()

public byte[] getBlobContent()

### Commit.java
Commit (String message, String timestamp, String parentId, String commitId, HashMap<String, String> blobs)

Method:

public String commitSha()

public String getCommitId()

public static Commit fromCommitFile(String id)

public void saveCommits()

public String getMessage()

public String getTimestamp()

public String getParent()


public HashMap<String, String> getBlobs() -- pairs of <fileName, blobId>



public String logMessage()

public void writeLogMessage()

## Algorithms

## Persistence

