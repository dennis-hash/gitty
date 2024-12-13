package org.scm.core;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.InflaterInputStream;

public class Initialize {
    public static final String YELLOW_COLOR = "\033[33;1m";
    public static final String BLUE_COLOR = "\033[34;1m";
    public static final String RED_COLOR = "\033[31;1m";
    public static final String RESET = "\033[0m";

    public static void initRepository(){
        final File root = new File(".gitty");

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

            File gitIgnore = new File(".gittyignore");
            if (gitIgnore.createNewFile()) {
                String defaultIgnoreContent = """
                # Ignore IDE-specific files
                .idea/
                *.iml
                *.vscode/
                /scm/target
                .git/
                .gitty/
                .gittyignore
                """;
                Files.write(gitIgnore.toPath(), defaultIgnoreContent.getBytes());
                System.out.println("Created .gittyignore file with default entries.");
            }
            System.out.println("Initialized Gitty Repository ");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void readBlob(String hash){
        String directoryHash = hash.substring(0, 2); //first two characters name of directory
        String fileHash = hash.substring(2); //rest of the characters: name of file

        File blobFile = new File("./.gitty/objects/" + directoryHash + "/" + fileHash);
        try {
            String blob = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(blobFile)))).readLine();
            String content = blob.substring(blob.indexOf("\0")+1);
            System.out.print(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public static void gittyIntroduction() {
        System.out.println("" + BLUE_COLOR +
                "   ____ _ _ _   _\n" +
                "  / ___(_) |_| |_ _   _ \n" +
                " | |  _| | __| __| | | |\n" +
                " | |_| | | |_| |_| |_| |\n" +
                "  \\____|_|\\__|\\__|\\__, |\n" +
                "                  |___/\n" + RESET +
                "I am " + YELLOW_COLOR + "Gitty" + RESET +
                ". A Version Control System for your projects.\n\n" +
                "\tRun " + RED_COLOR + "gitty [--help]" + RESET +
                " to see the list of commands.\n");
    }


    public static void helpCenter() {
        System.out.println("\n\tAll the command list\n");
        System.out.println("\t" + "\u001B[31m" + "init" + "\u001B[0m" + " - Initialize a new Gitty repository.");
        System.out.println("\t" + "\u001B[31m" + "cat-file <hash>" + "\u001B[0m" + " - View the contents of a blob by its hash.");
        System.out.println("\t" + "\u001B[31m" + "add" + "\u001B[0m" + " - Add files to the staging area.");
        System.out.println("\t" + "\u001B[31m" + "commit -m <message> [authorName] [authorEmail]" + "\u001B[0m" + " - Commit staged changes with a message and optional author info.");
        System.out.println("\t" + "\u001B[31m" + "log" + "\u001B[0m" + " - View the commit history.");
        System.out.println("\t" + "\u001B[31m" + "branch <branchName>" + "\u001B[0m" + " - Create a new branch.");
        System.out.println("\t" + "\u001B[31m" + "checkout <branchName>" + "\u001B[0m" + " - Switch to a specified branch.");
        System.out.println("\t" + "\u001B[31m" + "merge <branchName>" + "\u001B[0m" + " - Merge the specified branch into the current branch.");
        System.out.println("\t" + "\u001B[31m" + "status" + "\u001B[0m" + " - Display the status of the working directory.");
        System.out.println("\t" + "\u001B[31m" + "diffs" + "\u001B[0m" + " - View differences between the working directory and the index.");
        System.out.println("\n\tUse 'gitty --help' to view this list again.");
    }
}
