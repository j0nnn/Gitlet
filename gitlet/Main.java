package gitlet;

import java.io.File;

import static gitlet.Utils.join;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Jonathan Sun
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {

        final File CWD = new File(System.getProperty("user.dir"));
        /** The .gitlet directory. */
        final File GITLET_DIR = join(CWD, ".gitlet");

        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        String fileName;
        String message;
        String commitID;
        String branch;
        switch(firstArg) {
            case "init":
                validateNumArgs(args, 1);
                Repository.initialize();
                break;
            case "add":
                initialDirectory(GITLET_DIR);
                validateNumArgs(args, 2);
                fileName = args[1];
                Repository.add(fileName);
                break;
            case "commit":
                initialDirectory(GITLET_DIR);
                validateNumArgs(args, 2);
                message = args[1];
                Repository.commit(message);
                break;
            case "rm":
                initialDirectory(GITLET_DIR);
                validateNumArgs(args, 2);
                fileName = args[1];
                Repository.remove(fileName);
                break;
            case "log":
                initialDirectory(GITLET_DIR);
                validateNumArgs(args, 1);
                Repository.log();
                break;
            case "global-log":
                initialDirectory(GITLET_DIR);
                validateNumArgs(args, 1);
                Repository.globalLog();
                break;
            case "find":
                initialDirectory(GITLET_DIR);
                validateNumArgs(args, 2);
                message = args[1];
                Repository.find(message);
                break;
            case "status":
                initialDirectory(GITLET_DIR);
                validateNumArgs(args, 1);
                Repository.status();
                break;
            case "checkout":
                initialDirectory(GITLET_DIR);
                switch(args.length) {
                    case 2:
                        branch = args[1];
                        Repository.checkoutBranch(branch);
                        break;
                    case 3:
                        fileName = args[2];
                        if (!args[1].equals("--")) {
                            System.out.println("Incorrect operands.");
                            System.exit(0);
                        }
                        Repository.checkoutFile(fileName);
                        break;
                    case 4:
                        commitID = args[1];
                        fileName = args[3];
                        if (!args[2].equals("--")) {
                            System.out.println("Incorrect operands.");
                            System.exit(0);
                        }
                        Repository.checkoutCommit(commitID, fileName);
                        break;
                    default:
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                }
                break;
            case "branch":
                initialDirectory(GITLET_DIR);
                validateNumArgs(args, 2);
                branch = args[1];
                Repository.branch(branch);
                break;
            case "rm-branch":
                initialDirectory(GITLET_DIR);
                validateNumArgs(args, 2);
                branch = args[1];
                Repository.removeBranch(branch);
                break;
            case "reset":
                initialDirectory(GITLET_DIR);
                validateNumArgs(args, 2);
                commitID = args[1];
                Repository.reset(commitID);
                break;
            case "merge":
                initialDirectory(GITLET_DIR);
                validateNumArgs(args, 2);
                branch = args[1];
                Repository.merge(branch);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }
    //check args length
    public static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    //check .gitlet initialized
    public static void initialDirectory(File GITLET_DIR) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
