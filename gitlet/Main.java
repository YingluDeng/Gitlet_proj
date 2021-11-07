package gitlet;

import java.io.File;
import static gitlet.Utils.*;
import static gitlet.Repository.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Yinglu Deng
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     * java gitlet.Main add hello.txt */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        /** check all the argument*/
        String firstArg = args[0];
        Repository repo = new Repository();

        switch(firstArg) {
            case "init":
                repo.initRepo();
                break;
            case "add":
                repo.addRepo(args[1]);
                break;
            case "commit":
                if (args[1].equals("") || args[1].isEmpty()) {
                    System.out.println("Please enter a commit message.");
//                } else if ((ADDITION_DIR.listFiles()) && (REMOVAL_DIR.listFiles() == null)) {
//                    System.out.println("No changes added to the commit.");
                } else {
                    repo.commitRepo(args[1]);
                }
                break;
            case "log":
                repo.logRepo();
                break;
            case "checkout":
                //java gitlet.Main checkout -- [file name]
                if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        message("Incorrect operands.");
                        System.exit(0);
                    } else if (args[2].equals("") || args[2].isEmpty()) {
                        message("Incorrect operands.");
                        System.exit(0);
                    } else {
                        repo.checkoutRepo1(args[2]);
                    }
                }
                //java gitlet.Main checkout [commit id] -- [file name]
                if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        message("Incorrect operands.");
                        System.exit(0);   //TODO: short abbrv commit id
                    } else if (args[1].length() != UID_LENGTH) {
                        repo.checkoutShort(args[1], args[3]);
                    } else {
                        repo.checkoutRepo2(args[1], args[3]);
                    }
                }
                //java gitlet.Main checkout [branch name]
                if (args.length == 2) {
                repo.checkoutRepo3(args[1]);
                }
                // TODO: java gitlet.Main checkout [branch name]
                break;
            case "rm":
                repo.rmRepo(args[1]);
                break;
            case "global-log":
                repo.globalLogRepo();
                break;
            case "find":
                repo.findRepo(args[1]);
                break;
            case "status":
                repo.statusRepo();
                break;
            case "branch":
                repo.branchRepo(args[1]);
                break;
            case "rm-branch":
                repo.rmBranchRepo(args[1]);
                break;
            case "reset":
                // java gitlet.Main reset [commit id]
                repo.resetRepo(args[1]);
                break;
            case "merge":
                // java gitlet.Main merge [branch name]
                repo.mergeRepo(args[1]);
                break;
            default:
                message("No command with that name exists.");
                System.exit(0);
        }


    }

}
