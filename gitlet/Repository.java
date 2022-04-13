package gitlet;

import java.io.File;
import static gitlet.Utils.*;
import java.util.*;



/** Represents a gitlet repository.
 *
 *  does at a high level.
 *
 *  @author Jonathan Sun
 */
public class Repository {

    /**
     *
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The staging area. */
    public static final File STAGING_AREA = join(GITLET_DIR, "staging area");
    /** The blob folder. */
    public static final File BLOBS = join(GITLET_DIR, "blobs");
    /** The commit folder. */
    public static final File COMMITS = join(GITLET_DIR, "commits");
    /** The pointer folder. */
    public static final File POINTERS = join(GITLET_DIR, "pointers");


    public static void setUpConsistent() {
        //directory setup
        GITLET_DIR.mkdir();
        STAGING_AREA.mkdir();
        join(STAGING_AREA, "addition").mkdir();
        join(STAGING_AREA, "removal").mkdir();
        COMMITS.mkdir();
        BLOBS.mkdir();
        POINTERS.mkdir();

        //Commit setup
        Commit currentCommit = new Commit("initial commit", null, null);
        writeObject(join(COMMITS, "0"), currentCommit);

        //HashMap setup
        HashMap<String, String> stageHash = new HashMap<String, String>();
        writeObject(join(GITLET_DIR, "blobmap"), stageHash);
        HashMap<String, Commit> commitHash = new HashMap<String, Commit>();
        commitHash.put(sha1(serialize(currentCommit)), currentCommit);
        writeObject(join(GITLET_DIR, "commitmap"), commitHash);

        //Pointer setup
        String headPointer = sha1(serialize(currentCommit));
        String masterPointer = sha1(serialize(currentCommit));
        writeObject(join(POINTERS, "headpointer"), headPointer);
        writeObject(join(POINTERS, "master"), masterPointer);
        writeObject(join(GITLET_DIR, "checkout"), "master");
    }

    public static void initialize() {
        //check directory existence
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists"
                    + " in the current directory.");
            System.exit(0);
        } else {
            setUpConsistent();
        }
    }

    public static void add(String file) {
        String currentCommitSHA = readObject(join(POINTERS, "headpointer"), String.class);
        File commitmap = join(GITLET_DIR, "commitmap");
        HashMap<String, Commit> commitMap = readObject(commitmap, HashMap.class);
        Commit currentCommit = commitMap.get(currentCommitSHA);

        //check file existence
        if (!join(CWD, file).exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        //fringe case condition
        if (currentCommit.getParent() != null) {
            boolean fileExist = currentCommit.getMetadata().containsKey(file);
            if (fileExist) {
                File blob = join(BLOBS, currentCommit.getMetadata().get(file));
                byte[] previousContent = readObject(blob, byte[].class);
                writeContents(join(GITLET_DIR, "temp.txt"), previousContent);
                String previousContentString = readContentsAsString(join(GITLET_DIR, "temp.txt"));
                if (previousContentString.equals(readContentsAsString(join(CWD, file)))) {
                    if (join(STAGING_AREA, "addition", file).exists()) {
                        join(STAGING_AREA, "addition", file).delete();
                    }
                    if (join(STAGING_AREA, "removal", file).exists()) {
                        join(STAGING_AREA, "removal", file).delete();
                    }
                    System.exit(0);
                }
            }
        }
        join(GITLET_DIR, "temp.txt").delete();

        //add file in addition staging area and delete from removal
        File newFile = join(STAGING_AREA, "addition", file);
        byte[] content = readContents(join(CWD, file));
        if (join(STAGING_AREA, "removal", file).exists()) {
            join(STAGING_AREA, "removal", file).delete();
        } else {
            writeContents(newFile, content);
        }

        //add new blob
        File tempFile = join(GITLET_DIR, "temp.txt");
        List<String> blobFile = plainFilenamesIn(BLOBS);
        boolean action = false;
        String blobName = "";
        for (int i = 0; i < blobFile.size(); i++) {
            byte[] tempContent = readObject(join(BLOBS, blobFile.get(i)), byte[].class);
            writeContents(join(GITLET_DIR, "temp.txt"), tempContent);
            String tempString = readContentsAsString(join(GITLET_DIR, "temp.txt"));
            if (tempString.equals(readContentsAsString(join(CWD, file)))) {
                blobName = Integer.toString(i);
                action = true;
            }
        }
        if (!action) {
            blobName = Integer.toString(plainFilenamesIn(BLOBS).size());
        }
        writeObject(join(BLOBS, blobName), content);

        //update HashMap
        HashMap<String, String> blobMap = readObject(join(GITLET_DIR, "blobmap"), HashMap.class);
        blobMap.put(file, blobName);
        writeObject(join(GITLET_DIR, "blobmap"), blobMap);
    }

    public static void commit(String message) {
        String currentCommitSHA = readObject(join(POINTERS, "headpointer"), String.class);
        List<String> parentList = new ArrayList<String>();
        parentList.add(currentCommitSHA);
        mergeCommit(message, parentList);
    }

    public static void remove(String file) {
        String currentCommitSHA = readObject(join(POINTERS, "headpointer"), String.class);
        File commitmap = join(GITLET_DIR, "commitmap");
        HashMap<String, Commit> commitMap = readObject(commitmap, HashMap.class);
        Commit currentCommit = commitMap.get(currentCommitSHA);
        boolean action = false;

        //remove file from staging area if exist
        if (join(STAGING_AREA, "addition", file).exists()) {
            join(STAGING_AREA, "addition", file).delete();
            action = true;
        }

        //check if file is in current Commit
        if (currentCommit.getMetadata() != null && currentCommit.getMetadata().containsKey(file)) {
            //stage for removal
            File newFile = join(STAGING_AREA, "removal", file);
            if (join(CWD, file).exists()) {
                byte[] content = readContents(join(CWD, file));
                writeContents(newFile, content);
            } else {
                writeContents(newFile, "placeholder for deletion");
            }
            join(CWD, file).delete();
            action = true;
        }

        //check if action is done
        if (!action) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        } else {
            //update HashMap
            File blobmap = join(GITLET_DIR, "blobmap");
            HashMap<String, String> newBlobMap = readObject(blobmap, HashMap.class);
            newBlobMap.remove(file);
            writeObject(join(GITLET_DIR, "blobmap"), newBlobMap);
        }
    }

    public static void log() {
        String headCommitSHA = readObject(join(POINTERS, "headpointer"), String.class);
        File commitmap = join(GITLET_DIR, "commitmap");
        HashMap<String, Commit> commitMap = readObject(commitmap, HashMap.class);
        Commit headCommit = commitMap.get(headCommitSHA);

        while (headCommit.getParent() != null) {
            if (headCommit.getParent().size() == 1) {
                System.out.println("===");
                System.out.println("commit " + sha1(serialize(headCommit)));
                System.out.println("Date: " + headCommit.getTimestamp());
                System.out.println(headCommit.getMessage());
                System.out.println("");
            } else {
                List<String> parentList = headCommit.getParent();
                System.out.println("===");
                System.out.println("commit " + sha1(serialize(headCommit)));
                System.out.println("Merge: " + parentList.get(0).substring(0, 7) + " "
                        + parentList.get(1).substring(0, 7));
                System.out.println("Date: " + headCommit.getTimestamp());
                System.out.println(headCommit.getMessage());
                System.out.println("");
            }
            headCommit = commitMap.get(headCommit.getParent().get(0));
        }
        System.out.println("===");
        System.out.println("commit " + sha1(serialize(headCommit)));
        System.out.println("Date: " + headCommit.getTimestamp());
        System.out.println(headCommit.getMessage());
        System.out.println("");
    }

    public static void globalLog() {
        List<String> commitFiles = plainFilenamesIn(COMMITS);
        for (int i = 0; i < commitFiles.size(); i++) {
            Commit headCommit = readObject(join(COMMITS, commitFiles.get(i)), Commit.class);
            if (headCommit.getParent() == null || headCommit.getParent().size() == 1) {
                System.out.println("===");
                System.out.println("commit " + sha1(serialize(headCommit)));
                System.out.println("Date: " + headCommit.getTimestamp());
                System.out.println(headCommit.getMessage());
                System.out.println("");
            } else {
                List<String> parentList = headCommit.getParent();
                System.out.println("===");
                System.out.println("commit " + sha1(serialize(headCommit)));
                System.out.println("Merge: " + parentList.get(0).substring(0, 7) + " "
                        + parentList.get(1).substring(0, 7));
                System.out.println("Date: " + headCommit.getTimestamp());
                System.out.println(headCommit.getMessage());
                System.out.println("");
            }
        }
    }

    public static void find(String message) {
        List<String> commitFiles = plainFilenamesIn(COMMITS);
        boolean action = false;
        for (int i = 0; i < commitFiles.size(); i++) {
            Commit commit = readObject(join(COMMITS, commitFiles.get(i)), Commit.class);
            if (commit.getMessage().equals(message)) {
                System.out.println(sha1(serialize(commit)));
                action = true;
            }
        }
        if (!action) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    public static void status() {
        String checkout = readObject(join(GITLET_DIR, "checkout"), String.class);
        List<String> pointerList = plainFilenamesIn(POINTERS);
        List<String> additionList = plainFilenamesIn(join(STAGING_AREA, "addition"));
        List<String> removeList = plainFilenamesIn(join(STAGING_AREA, "removal"));
        Collections.sort(pointerList);
        Collections.sort(additionList);
        Collections.sort(removeList);

        //print branches
        System.out.println("=== Branches ===");
        for (int i = 0; i < pointerList.size(); i++) {
            if (!pointerList.get(i).equals("headpointer")) {
                if (checkout.equals(pointerList.get(i))) {
                    System.out.print("*");
                }
                System.out.println(pointerList.get(i));
            }
        }
        System.out.println("");

        //print staged files
        System.out.println("=== Staged Files ===");
        for (int j = 0; j < additionList.size(); j++) {
            System.out.println(additionList.get(j));
        }
        System.out.println("");

        //print removed files
        System.out.println("=== Removed Files ===");
        for (int k = 0; k < removeList.size(); k++) {
            System.out.println(removeList.get(k));
        }
        System.out.println("");

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println("");
        System.out.println("=== Untracked Files ===");
        System.out.println("");
    }

    public static void checkoutFile(String file) {
        String headCommitSHA = readObject(join(POINTERS, "headpointer"), String.class);
        File commitmap = join(GITLET_DIR, "commitmap");
        HashMap<String, Commit> commitMap = readObject(commitmap, HashMap.class);
        Commit headCommit = commitMap.get(headCommitSHA);
        HashMap<String, String> metadata = headCommit.getMetadata();

        //check existence of file in Head Commit
        if (!metadata.containsKey(file)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        writeContents(join(CWD, file), readObject(join(BLOBS, metadata.get(file)), byte[].class));
    }

    public static void checkoutCommit(String commitID, String file) {
        File commitmap = join(GITLET_DIR, "commitmap");
        HashMap<String, Commit> commitMap = readObject(commitmap, HashMap.class);

        //check existence of commit
        if (!commitMap.containsKey(commitID)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit commit = commitMap.get(commitID);
        HashMap<String, String> metadata = commit.getMetadata();

        //check existence of file in commit
        if (!metadata.containsKey(file)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        byte[] content = readObject(join(BLOBS, metadata.get(file)), byte[].class);
        writeContents(join(CWD, file), content);
    }

    public static void checkoutBranch(String branch) {
        //check existence of branch
        if (!join(POINTERS, branch).exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        String branchHeadSHA = readObject(join(POINTERS, branch), String.class);
        String currentBranch = readObject(join(GITLET_DIR, "checkout"), String.class);
        String headCommitSHA = readObject(join(POINTERS, currentBranch), String.class);

        //check if head is same as branchHead
        if (branch.equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        //check for untracked files
        File commitmap = join(GITLET_DIR, "commitmap");
        HashMap<String, Commit> commitMap = readObject(commitmap, HashMap.class);
        Commit branchHead = commitMap.get(branchHeadSHA);
        Commit headCommit = commitMap.get(headCommitSHA);
        if (checkUntrackedFiles(headCommit, branchHead)) {
            System.out.println("There is an untracked file in the way; delete it, "
                    + "or add and commit it first.");
            System.exit(0);
        }

        //overwrite cwd with branchHead data and delete tracked files not present
        HashMap<String, String> branchBlobMap = branchHead.getMetadata();
        HashMap<String, String> currentBlobMap = headCommit.getMetadata();

        if (branchBlobMap != null && currentBlobMap != null) {
            String[] metadata = branchBlobMap.keySet().toArray(new String[0]);
            for (int i = 0; i < metadata.length; i++) {
                File blob = join(BLOBS, branchBlobMap.get(metadata[i]));
                byte[] content = readObject(blob, byte[].class);
                writeContents(join(CWD, metadata[i]), content);
            }
            String[] currentFiles = currentBlobMap.keySet().toArray(new String[0]);
            List<String> branchFileList = Arrays.asList(metadata);
            for (int i = 0; i < currentFiles.length; i++) {
                if (!branchFileList.contains(currentFiles[i])) {
                    join(CWD, currentFiles[i]).delete();
                }
            }
        } else if (branchBlobMap != null && currentBlobMap == null) {
            String[] metadata = branchBlobMap.keySet().toArray(new String[0]);
            for (int i = 0; i < metadata.length; i++) {
                File blob = join(BLOBS, branchBlobMap.get(metadata[i]));
                byte[] content = readObject(blob, byte[].class);
                writeContents(join(CWD, metadata[i]), content);
            }
        } else if (branchBlobMap == null && currentBlobMap != null) {
            String[] currentFiles = currentBlobMap.keySet().toArray(new String[0]);
            for (int i = 0; i < currentFiles.length; i++) {
                String fileName = currentFiles[i];
                if (join(CWD, fileName).exists()) {
                    join(CWD, fileName).delete();
                }
            }
        }

        writeObject(join(POINTERS, "headpointer"), branchHeadSHA);
        writeObject(join(GITLET_DIR, "checkout"), branch);
        if (branchBlobMap != null) {
            writeObject(join(GITLET_DIR, "blobmap"), branchBlobMap);
        } else {
            writeObject(join(GITLET_DIR, "blobmap"), new HashMap<String, String>());
        }

        List<String> additionFiles = plainFilenamesIn(join(STAGING_AREA, "addition"));
        List<String> removalFiles = plainFilenamesIn(join(STAGING_AREA, "removal"));
        for (int l = 0; l < additionFiles.size(); l++) {
            join(STAGING_AREA, "addition", additionFiles.get(l)).delete();
        }
        for (int m = 0; m < removalFiles.size(); m++) {
            join(STAGING_AREA, "removal", removalFiles.get(m)).delete();
        }
    }

    public static void branch(String branch) {
        //check branch existence
        if (join(POINTERS, branch).exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        String headCommitSHA = readObject(join(POINTERS, "headpointer"), String.class);
        writeObject(join(POINTERS, branch), headCommitSHA);
    }

    public static void removeBranch(String branch) {
        //check branch existence
        if (!join(POINTERS, branch).exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        //check if it is current branch
        String branchSHA = readObject(join(POINTERS, branch), String.class);
        String currentSHA = readObject(join(POINTERS, "headpointer"), String.class);
        if (currentSHA.equals(branchSHA)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        join(POINTERS, branch).delete();
    }

    public static void reset(String commitID) {
        //check commit existence
        File commitmap = join(GITLET_DIR, "commitmap");
        HashMap<String, Commit> commitMap = readObject(commitmap, HashMap.class);
        if (!commitMap.containsKey(commitID)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        //check untracked files
        Commit targetCommit = commitMap.get(commitID);
        String currentBranch = readObject(join(GITLET_DIR, "checkout"), String.class);
        String headpointerSHA = readObject(join(POINTERS, currentBranch), String.class);
        Commit headCommit = commitMap.get(headpointerSHA);
        if (checkUntrackedFiles(headCommit, targetCommit)) {
            System.out.println("There is an untracked file in the way; delete it, "
                    + "or add and commit it first.");
            System.exit(0);
        }

        //write files in commit
        HashMap<String, String> blobMap = targetCommit.getMetadata();
        String[] metadata = blobMap.keySet().toArray(new String[0]);
        for (int i = 0; i < metadata.length; i++) {
            byte[] content = readObject(join(BLOBS, blobMap.get(metadata[i])), byte[].class);
            writeContents(join(CWD, metadata[i]), content);
        }

        //delete files not present in commit
        List<String> cwdFiles = plainFilenamesIn(CWD);
        for (int i = 0; i < cwdFiles.size(); i++) {
            if (!targetCommit.getMetadata().containsKey(cwdFiles.get(i))) {
                join(CWD, cwdFiles.get(i)).delete();
            }
        }

        //clear staging area
        List<String> additionFiles = plainFilenamesIn(join(STAGING_AREA, "addition"));
        List<String> removalFiles = plainFilenamesIn(join(STAGING_AREA, "removal"));
        for (int l = 0; l < additionFiles.size(); l++) {
            join(STAGING_AREA, "addition", additionFiles.get(l)).delete();
        }
        for (int m = 0; m < removalFiles.size(); m++) {
            join(STAGING_AREA, "removal", removalFiles.get(m)).delete();
        }

        //UPDATE POINTER
        writeObject(join(POINTERS, currentBranch), commitID);
        writeObject(join(POINTERS, "headpointer"), commitID);
    }

    public static void merge(String branch) {
        mergeCondition(branch);
        File commitmap = join(GITLET_DIR, "commitmap");
        HashMap<String, Commit> commitMap = readObject(commitmap, HashMap.class);
        String branchHeadSHA = readObject(join(POINTERS, branch), String.class);
        String currentHeadSHA = readObject(join(POINTERS, "headpointer"), String.class);
        Commit branchCommit = commitMap.get(branchHeadSHA);
        Commit currentCommit = commitMap.get(currentHeadSHA);
        Commit iterationCommit = currentCommit;
        String checkout = readObject(join(GITLET_DIR, "checkout"), String.class);

        //find split point
        Commit splitPointCommit = findSplit(currentCommit, branchCommit, commitMap);

        //cases
        HashMap<String, String> splitPointBM = new HashMap<String, String>();
        HashMap<String, String> currentCBM = new HashMap<String, String>();
        HashMap<String, String> branchCommitBM = new HashMap<String, String>();

        if (splitPointCommit.getMetadata() != null) {
            splitPointBM = splitPointCommit.getMetadata();
        }
        if (currentCommit.getMetadata() != null) {
            currentCBM = currentCommit.getMetadata();
        }   
        if (branchCommit.getMetadata() != null) {
            branchCommitBM = branchCommit.getMetadata();
        }

        String[] splitPointFiles = splitPointBM.keySet().toArray(new String[0]);
        boolean con = mergeCases(splitPointFiles, currentCBM, splitPointBM, branchCommitBM);
        //case 5
        String[] branchCommitFiles = branchCommitBM.keySet().toArray(new String[0]);
        List<String> splitCommitArray = Arrays.asList(splitPointFiles);

        for (int j = 0; j < branchCommitFiles.length; j++) {
            String fileName = branchCommitFiles[j];
            if (!splitCommitArray.contains(fileName)) {
                if (currentCBM.containsKey(fileName)) {
                    if (!currentCBM.get(fileName).equals(branchCommitBM.get(fileName))) {
                        con = writeMergeFile(currentCBM, branchCommitBM, fileName);
                    }
                } else {
                    File blob = join(BLOBS, branchCommitBM.get(branchCommitFiles[j]));
                    byte[] content = readObject(blob, byte[].class);
                    writeContents(join(CWD, branchCommitFiles[j]), content);
                    add(branchCommitFiles[j]);
                }
            }
        }

        List<String> newParentList = new ArrayList<String>();
        newParentList.add(currentHeadSHA);
        newParentList.add(branchHeadSHA);
        mergeCommit("Merged " + branch + " into " + checkout + ".", newParentList);
        if (con) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    public static boolean checkUntrackedFiles(Commit headCommit, Commit branchCommit) {
        if (branchCommit.getParent() == null) {
            return false;
        } else if (headCommit.getParent() == null) {
            String[] branchFiles = branchCommit.getMetadata().keySet().toArray(new String[0]);
            for (int i = 0; i < branchFiles.length; i++) {
                if (join(CWD, branchFiles[i]).exists()) {
                    File blob = join(BLOBS, branchCommit.getMetadata().get(branchFiles[i]));
                    byte[] content = readObject(blob, byte[].class);
                    File tempFile = join(GITLET_DIR, "temp.txt");
                    writeContents(tempFile, content);
                    String currentContent = readContentsAsString(join(CWD, branchFiles[i]));
                    if (!readContentsAsString(tempFile).equals(currentContent)) {
                        return true;
                    }
                }
            }
            return false;
        }
        List<String> cwdFiles = plainFilenamesIn(CWD);
        for (int i = 0; i < cwdFiles.size(); i++) {
            boolean headCommitContains = headCommit.getMetadata().containsKey(cwdFiles.get(i));
            boolean branchCommitContains = branchCommit.getMetadata().containsKey(cwdFiles.get(i));
            if (!headCommitContains && branchCommitContains) {
                File blob = join(BLOBS, branchCommit.getMetadata().get(cwdFiles.get(i)));
                byte[] content = readObject(blob, byte[].class);
                writeContents(join(GITLET_DIR, "temp.txt"), content);
                String currentContent = readContentsAsString(join(CWD, cwdFiles.get(i)));
                if (readContentsAsString(join(GITLET_DIR, "temp.txt")).equals(currentContent)) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public static boolean writeMergeFile(HashMap<String, String> currentCBM,
                                         HashMap<String, String> branchCommitBM,
                                         String fileName) {

        String headContentString = "";
        String branchContentString = "";
        File tempFile = join(GITLET_DIR, "temp");
        if (currentCBM.containsKey(fileName)) {
            File blob = join(BLOBS, currentCBM.get(fileName));
            byte[] headContent = readObject(blob, byte[].class);
            writeContents(tempFile, headContent);
            headContentString = readContentsAsString(tempFile);
        }
        if (branchCommitBM.containsKey(fileName)) {
            File blob = join(BLOBS, branchCommitBM.get(fileName));
            byte[] branchContent = readObject(blob, byte[].class);
            writeContents(tempFile, branchContent);
            branchContentString = readContentsAsString(tempFile);
        }
        String finalContent = "<<<<<<< HEAD\n" + headContentString
                + "=======\n" + branchContentString + ">>>>>>>\n";
        writeContents(join(CWD, fileName), finalContent);
        tempFile.delete();
        add(fileName);
        return true;
    }

    public static void mergeCommit(String message, List<String> parentList) {
        //check if it is a non-blank message
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        //check if files exist in staging area
        boolean fileInAddition = plainFilenamesIn(join(STAGING_AREA, "addition")).size() == 0;
        boolean fileInRemoval = plainFilenamesIn(join(STAGING_AREA, "removal")).size() == 0;
        if (fileInAddition && fileInRemoval) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        //transfer HashMap to Commit object
        HashMap<String, String> blobMap = readObject(join(GITLET_DIR, "blobmap"), HashMap.class);
        Commit newCommit = new Commit(message, parentList, blobMap);

        //update pointers
        String currentHeadName = readObject(join(GITLET_DIR, "checkout"), String.class);
        writeObject(join(POINTERS, currentHeadName), sha1(serialize(newCommit)));
        writeObject(join(POINTERS, "headpointer"), sha1(serialize(newCommit)));

        //clear staging area
        List<String> additionStageFile = plainFilenamesIn(join(STAGING_AREA, "addition"));
        for (int i = 0; i < additionStageFile.size(); i++) {
            join(STAGING_AREA, "addition", additionStageFile.get(i)).delete();
        }
        List<String> removalStageFile = plainFilenamesIn(join(STAGING_AREA, "removal"));
        for (int j = 0; j < removalStageFile.size(); j++) {
            join(STAGING_AREA, "removal", removalStageFile.get(j)).delete();
        }

        //write Commit object
        String commitName = Integer.toString(plainFilenamesIn(COMMITS).size());
        writeObject(join(COMMITS, commitName), newCommit);

        //update Commit HashMap
        File commitmap = join(GITLET_DIR, "commitmap");
        HashMap<String, Commit> newCommitMap = readObject(commitmap, HashMap.class);
        newCommitMap.put(sha1(serialize(newCommit)), newCommit);
        newCommitMap.put(sha1(serialize(newCommit)).substring(0, 8), newCommit);
        writeObject(join(GITLET_DIR, "commitmap"), newCommitMap);
    }

    public static void parseCommit(Commit iterationCommit, List<String> parentList) {
        File commitmap = join(GITLET_DIR, "commitmap");
        HashMap<String, Commit> commitMap = readObject(commitmap, HashMap.class);
        if (iterationCommit == null || iterationCommit.getParent() == null) {
            return;
        }
        if (iterationCommit.getParent().size() != 1) {
            for (int i = 0; i < iterationCommit.getParent().size(); i++) {
                parentList.add(iterationCommit.getParent().get(i));
                parseCommit(commitMap.get(iterationCommit.getParent().get(i)), parentList);
            }
        } else {
            parentList.add(iterationCommit.getParent().get(0));
            parseCommit(commitMap.get(iterationCommit.getParent().get(0)), parentList);
        }
    }

    public static void mergeCondition(String branch) {
        //check uncommited changes
        List<String> additionFiles = plainFilenamesIn(join(STAGING_AREA, "addition"));
        List<String> removalFiles = plainFilenamesIn(join(STAGING_AREA, "removal"));
        if (additionFiles.size() != 0 || removalFiles.size() != 0) {
            System.out.println("You have uncommited changes.");
            System.exit(0);
        }

        //check branch existence
        if (!join(POINTERS, branch).exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        //check if merging with self
        String checkout = readObject(join(GITLET_DIR, "checkout"), String.class);
        if (checkout.equals(branch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        File commitmap = join(GITLET_DIR, "commitmap");
        HashMap<String, Commit> commitMap = readObject(commitmap, HashMap.class);
        String branchHeadSHA = readObject(join(POINTERS, branch), String.class);
        String currentHeadSHA = readObject(join(POINTERS, "headpointer"), String.class);
        Commit branchCommit = commitMap.get(branchHeadSHA);
        Commit currentCommit = commitMap.get(currentHeadSHA);
        Commit iterationCommit = currentCommit;

        //check untracked file
        if (checkUntrackedFiles(currentCommit, branchCommit)) {
            System.out.println("There is an untracked file in the way; delete it, "
                    + "or add and commit it first.");
            System.exit(0);
        }

        //check if branch is ancestor
        List<String> parentList = new ArrayList<String>();
        parseCommit(iterationCommit, parentList);
        if (parentList.contains(branchHeadSHA)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }

        //check if current is ancestor of branch
        iterationCommit = branchCommit;
        parentList.clear();
        parseCommit(iterationCommit, parentList);
        if (parentList.contains(currentHeadSHA)) {
            checkoutBranch(branch);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
    }

    public static Commit findSplit(Commit currentCommit, Commit branchCommit,
                                   HashMap<String, Commit> commitMap) {
        Commit iterationCommit = currentCommit;
        Commit splitPointCommit = null;
        List<String> parentList = new ArrayList<String>();
        parseCommit(iterationCommit, parentList);

        iterationCommit = branchCommit;
        List<String> otherParentList = new ArrayList<String>();
        List<String> splitPointList = new ArrayList<String>();
        parseCommit(iterationCommit, otherParentList);
        for (int i = 0; i < otherParentList.size(); i++) {
            if (parentList.contains(otherParentList.get(i))
                    && !splitPointList.contains(otherParentList.get(i))) {
                splitPointList.add(otherParentList.get(i));
            }
        }
        List<String> tempList = new ArrayList<String>();
        for (int j = 0; j < splitPointList.size(); j++) {
            parseCommit(commitMap.get(splitPointList.get(j)), tempList);
        }
        for (int k = 0; k < splitPointList.size(); k++) {
            if (!tempList.contains(splitPointList.get(k))) {
                splitPointCommit = commitMap.get(splitPointList.get(k));
                break;
            }
        }
        return splitPointCommit;
    }

    public static boolean mergeCases(String[] splitPointFiles,
                                     HashMap<String, String> currentCBM,
                                     HashMap<String, String> splitPointBM,
                                     HashMap<String, String> branchCommitBM) {
        boolean con = false;
        for (int i = 0; i < splitPointFiles.length; i++) {
            String fileName = splitPointFiles[i];
            //present in head
            if (currentCBM.containsKey(fileName)) {
                boolean currentSame = splitPointBM.get(fileName).equals(currentCBM.get(fileName));
                //unmodified in head
                if (currentSame) {
                    //present in branch
                    if (branchCommitBM.containsKey(fileName)) {
                        //modified in branch
                        if (!branchCommitBM.get(fileName).equals(splitPointBM.get(fileName))) {
                            File blob = join(BLOBS, branchCommitBM.get(fileName));
                            byte[] content = readObject(blob, byte[].class);
                            writeContents(join(CWD, fileName), content);
                            add(fileName);
                        }
                    } else {
                        //remove from cwd if not present in branch
                        remove(fileName);
                    }
                } else if (branchCommitBM.containsKey(fileName)) {
                    boolean branchSame;
                    branchSame = splitPointBM.get(fileName).equals(branchCommitBM.get(fileName));
                    //modified in branch
                    if (!branchSame) {
                        //branch and head modified in different way
                        if (!branchCommitBM.get(fileName).equals(currentCBM.get(fileName))) {
                            con = writeMergeFile(currentCBM, branchCommitBM, fileName);
                        }
                    }
                } else {
                    con = writeMergeFile(currentCBM, branchCommitBM, fileName);
                }
            } else if (branchCommitBM.containsKey(fileName)) {
                boolean branchSame;
                branchSame = splitPointBM.get(fileName).equals(branchCommitBM.get(fileName));
                //modified in branch
                if (!branchSame) {
                    con = writeMergeFile(currentCBM, branchCommitBM, fileName);
                } else {
                    if (join(CWD, fileName).exists()) {
                        join(CWD, fileName).delete();
                    }
                }
            } else {
                if (join(CWD, fileName).exists()) {
                    join(CWD, fileName).delete();
                }
            }
        }
        return con;
    }
}
