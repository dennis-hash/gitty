package org.scm;

import org.scm.core.*;
import org.scm.models.IndexEntry;

import java.io.*;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

public class Main {
    public static final String YELLOW_COLOR = "\033[33;1m";
    public static final String BLUE_COLOR = "\033[34;1m";
    public static final String RED_COLOR = "\033[31;1m";
    public static final String RESET = "\033[0m";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.err.println("Logs from your program will appear here!");

        // Uncomment this block to pass the first stage
        //
        if(args[0] == "gitty" && args.length == 1){
            gittyIntroduction();
            return;
        }
        final String command = args[1];

        switch (command) {
            case "--help"->{
                helpCenter();
            }
            case "init" -> {
                initRepository();
            }
            case "cat-file"->{
                String hash = args[2]; //zlib compressed data
                readBlob(hash);
            }
            //add new line
            case "add" ->{
                FileScanner scanner = new FileScanner();
                IndexManager indexManager = new IndexManager();
                scanner.scanDirectory(".");
                indexManager.addFilesToIndex(scanner.getFiles());
            }
            //addaad

            case "commit" ->{
                String commitMessage = "Second commit";
                String authorName = "John Doe";
                String authorEmail = "john.doe@example.com";
                String parentSha = null; // No parent for the first commit

                IndexManager indexManager = new IndexManager();

                List<IndexEntry> entries = indexManager.readIndex();

                CommitManager commitManager = new CommitManager();
                String result = commitManager.writeCommit(commitMessage, entries,authorName,authorEmail,parentSha);
                System.out.println(result);
            }
            case "log"->{
                CommitManager commitManager = new CommitManager();
                commitManager.viewCommitHistory();
            }

            case "branch"->{
                BranchManager branch = new BranchManager();
                branch.createBranch("branchesss");
            }

            case "checkout"->{
                BranchManager branch = new BranchManager();
                branch.switchBranch("main");
            }
            case "diffs"->{


            }
            //new line to
            case "merge"->{
                BranchMerger branchMerger = new BranchMerger();
                branchMerger.mergeBranch("branchesss");
            }
            case "status"->{
                GitStatus gitStatus = new GitStatus();
                gitStatus.checkStatus();
            }





            default -> System.out.println("Unknown command: " + command);
        }
    }

    private static void initRepository(){
        final File root = new File(".git");

        // Check if the .gitty directory already exists
        if (root.exists()) {
            throw new RuntimeException("Reinitialized existing Git repository in " + root.getAbsolutePath());
        }

        // Create the necessary directories and files
        new File(root, "objects").mkdirs();
        new File(root, "refs").mkdirs();
        final File head = new File(root, "HEAD");

        try {
            head.createNewFile();
            Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());

            File gitIgnore = new File(".gitignore");
            if (gitIgnore.createNewFile()) {
                String defaultIgnoreContent = """
                # Ignore temporary files
                *.tmp
                *.log
                *.bak
                *.swp

                # Ignore OS-specific files
                .DS_Store
                Thumbs.db

                # Ignore IDE-specific files
                .idea/
                *.iml
                *.vscode/
                """;
                Files.write(gitIgnore.toPath(), defaultIgnoreContent.getBytes());
                System.out.println("Created .gitignore file with default entries.");
            }
            System.out.println("Initialized Git directory");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void readBlob(String hash){
        String directoryHash = hash.substring(0, 2); //first two characters name of directory
        String fileHash = hash.substring(2); //rest of the characters: name of file

        File blobFile = new File("./.git/objects/" + directoryHash + "/" + fileHash);
        try {
            String blob = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(blobFile)))).readLine();
            String content = blob.substring(blob.indexOf("\0")+1);
            System.out.print(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void createHashObject(){

    }


    private static void gittyIntroduction() {
        System.out.println("  _   _      _ _\n" +
                " | | | | ___| | | ___\n" +
                " | |_| |/ _ \\ | |/ _ \\\n" +
                " |  _  |  __/ | | (_) |\n" +
                " |_| |_|\\___|_|_|\\___/\n\n" +
                "I am " + YELLOW_COLOR +"Clone" + RESET + ". A Version Control System for your projects.\n\n" +
                "\tExecute " + RED_COLOR + "clone [-h | --help]" + RESET + " To see the command list.\n");
    }

    private static void helpCenter() {
        System.out.println("\n\tAll the command list\n");
        System.out.println("\t" + RED_COLOR + "clone" + RESET + " - Welcome notice");
        System.out.println("\t" + RED_COLOR + "clone [-h | --help]" + RESET + " - To see the command list");
        System.out.println("\t" + RED_COLOR + "clone [-v | --version]" + RESET + " - To see the version");
        System.out.println();
        System.out.println("\t" + RED_COLOR + "clone start" + RESET + " - To initialize a cloning factory");
        System.out.println("\t" + RED_COLOR + "clone show" + RESET + " - To see the current status of files");
        System.out.println("\t" + RED_COLOR + "clone make" + RESET + " - To generate a new clone");
        System.out.println("\t" + RED_COLOR + "clone save" + RESET + " - To save the prepared clone permanently");
        System.out.println("\t" + RED_COLOR + "clone log" + RESET + " - To see the clone list");
        System.out.println("\t" + RED_COLOR + "clone activate <hashcode>" + RESET + " - To traverse along the saved clones");
        System.out.println();
    }
}