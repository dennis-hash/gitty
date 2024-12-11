package org.scm;

import org.scm.core.*;
import org.scm.models.IndexEntry;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.List;



public class Main {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        if (args.length == 0) {
            Initialize.gittyIntroduction();
            return;
        }

        final String command = args[0];

        switch (command) {
            case "--help" -> Initialize.helpCenter();

            case "init" -> Initialize.initRepository();

            case "cat-file" -> {
                if (args.length < 2) {
                    System.out.println("Error: Missing hash for 'cat-file' command.");
                    return;
                }
                String hash = args[1];
                Initialize.readBlob(hash);
            }

            case "add" -> {
                FileScanner scanner = new FileScanner();
                IndexManager indexManager = new IndexManager();
                scanner.scanDirectory(".");
                indexManager.addFilesToIndex(scanner.getFiles());
            }

            case "commit" -> {
                if (args.length < 2) {
                    System.out.println("Error: Missing commit message for 'commit' command.");
                    return;
                }
                String commitMessage = args[1];
                String authorName = args.length > 2 ? args[2] : "Default User";
                String authorEmail = args.length > 3 ? args[3] : "default@example.com";

                IndexManager indexManager = new IndexManager();
                List<IndexEntry> entries = indexManager.readIndex();

                CommitManager commitManager = new CommitManager();
                String parentSha = commitManager.getLatestCommitSha();

                String result = commitManager.writeCommit(commitMessage, entries, authorName, authorEmail, parentSha);
                System.out.println(result);
            }

            case "log" -> {
                CommitManager commitManager = new CommitManager();
                commitManager.viewCommitHistory();
            }

            case "branch" -> {
                if (args.length < 2) {
                    System.out.println("Error: Missing branch name for 'branch' command.");
                    return;
                }
                String branchName = args[1];
                BranchManager branch = new BranchManager();
                branch.createBranch(branchName);
            }

            case "checkout" -> {
                if (args.length < 2) {
                    System.out.println("Error: Missing branch name for 'checkout' command.");
                    return;
                }
                String branchName = args[1];
                BranchManager branch = new BranchManager();
                branch.switchBranch(branchName);
            }

            //new line

            case "diffs" -> {
                if (args.length < 2) {
                    System.out.println("Error: Missing branch name for 'diffs' command.");
                    return;
                }
                String branchName = args[1];
                Diffs diffs = new Diffs();
                diffs.diffBranches(branchName);
            }

            case "merge" -> {
                if (args.length < 2) {
                    System.out.println("Error: Missing branch name for 'merge' command.");
                    return;
                }
                String branchName = args[1];
                BranchMerger branchMerger = new BranchMerger();
                branchMerger.mergeBranch(branchName);
            }

            case "status" -> {
                GitStatus gitStatus = new GitStatus();
                gitStatus.checkStatus();
            }
            case "clone" -> {
                if (args.length < 2) {
                    System.out.println("Error: Missing target path");
                    return;
                }
                String path = args[1];
                CloneManager cloneManager = new CloneManager();
                cloneManager.cloneRepo(path);
            }

            default -> System.out.println("Unknown command: " + command + ". Use 'gitty --help' for the command list.");
        }
    }
}
