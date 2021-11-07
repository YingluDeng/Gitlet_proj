package gitlet;

import java.io.*;
import static gitlet.Repository.*;
import static gitlet.Utils.*;

/** blobs: The saved contents of files. Since Gitlet saves many versions of files,
 * a single file might correspond to multiple blobs: each being tracked in a
 * different commit.*/

public class Blobs implements Serializable {

    /** create instance variables */
    private String fileName;
    private String blobId;
    private byte[] blobContent;
    private String blobString;

    /** blobs constructor
     * including version; fileName; UID; abbrvID
     * */
    public Blobs(String file) {
        this.fileName = file;
        File f = Utils.join(CWD, file);
        this.blobContent = readContents(f);
        this.blobString = readContentsAsString(f);
        this.blobId = hash();
    }

    /** Reads in and deserializes a blob from a file with name fileName in BLOBS_DIR. */
    public static Blobs fromBlobFile(String blobId) {
        File inFile = Utils.join(BLOBS_DIR, blobId);
        if (!inFile.exists()) {
            return null;
        } else {
            Blobs b = readObject(inFile, Blobs.class);
            return b;
        }
    }

    /** save a blob for future use */
    public void saveBlob() {
        //blobId is unique
        File outFile = Utils.join(BLOBS_DIR, blobId);
        writeObject(outFile, this);
    }

    /** create blobId */
    public String hash() {
        return Utils.sha1(Utils.serialize(this));
    }

    /** get method for fileName, hashCode, blobContent */
    public String getFileName() {
        return this.fileName;
    }

    public String getBlobId() {
        return this.blobId;
    }

    public byte[] getBlobContent() {
        return this.blobContent;
    }

    public String getBlobString() {
        return this.blobString;
    }


}
