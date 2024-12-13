### Version
0.1

## Gitty installation guide
The `.deb` file is available for Ubuntu users in this repository.

`sudo apt install gitty.deb`

# Introduction

### Problem Statement
Modern software development relies heavily on version control systems to manage and track changes in code. Inspired by Git, this project implements a simplified distributed source control system called Gitty, which provides foundational features like initializing a repository, staging files, making commits, viewing commit history, creating branches, and merging changes.

### Objective
The goal of this project is to build a functional version control system that emulates Git’s core features. It offers developers the ability to manage their code base locally while gaining insights into the underlying mechanics of version control systems. 

### Scope
This project focuses on essential functionalities:
- **Initializing a repository** (`init`).
- **Staging and committing files** (`add`, `commit`).
- **Navigating the commit history** (`log`).
- **Creating and managing branches** (`branch`, `checkout`).
- **Merging changes** (`merge`).
- **Viewing repository status** (`status`) and **differences** (`diffs`).
- **Inspecting file content by hash** (`cat-file`).

Additional features like conflict resolution and network-based repository management are outside the current scope.

# Design
## System Architecture 
Gitty adopts a three-stage architecture similar to Git, comprising the Working Directory, Staging Area, and the Gitty Repository. This design optimizes change tracking and ensures efficient version control for your projects.

### Components
#### 1 Working Directory
- This is the actual directory where your project files reside.
- Modifications in the working directory are considered 'untracked' until explicitly staged using the gitty `add .` command.

#### 2 Staging Area (Index)
- The staging area serves as an intermediary between the working directory and the Gitty repository.
- Files added using the `gitty add .` command are staged here, preparing them for the next commit.
- Staged files are represented by blob objects, which store their content. These blobs are referenced in the staging area, indicating readiness for a commit.

#### 3 Gitty Repository (.gitty Directory)
- The Gitty repository serves as the core version control container, storing all metadata and objects.
- When you execute the `gitty commit -m <message>` command:
- a. A commit object is created, referencing the blob objects (and potentially tree objects) that represent the current state of the project.
- b. These commit trees establish a timeline of changes for effective version tracking.

##### Key Commands and Data Flow
`gitty add`
- Files are moved from the working directory to the staging area. A blob object is created for the file content, which is then referenced in the index.

`gitty commit`
- Staged changes are packaged into a commit object, which is added to the Gitty repository as a snapshot of the project state.

`gitty diff <branch>`
- Compares the latest commit of the specified branch with the current branch, highlighting differences between the two.

`gitty clone <target path>`
- Copies the repository (locally) from the most recent commit into the specified target path.

#### Diagram
The following diagram illustrates the architecture and data flow between the three stages:

![Screenshot from 2024-12-13 13-31-56](https://github.com/user-attachments/assets/9636550b-e51c-4880-a035-7c60faa524a7)


# Implementation
## Technologies 
- **Java programming language**
- **Java SE File I/O operations**
- **Java SE Utils hashing algorithms**

## Core Components

### Main class
The `Main` class serves as the entry point for our application. It uses a command pattern to execute different Git-like operations based on user input:

```java
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

            case "commit -m" -> {
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

```

### Create Git Object (Tree, Blob, Commit)

Blob: Represents the content of a file.
Tree: Represents a directory structure and references blobs or other trees.
Commit: Captures a snapshot of the repository and metadata like the parent commit, message, and author.

The createObject method generates and stores blob, tree, and commit objects in the .gitty directory. 

```java
    public static String createObject(byte[] data, String objType, boolean write) throws IOException, NoSuchAlgorithmException {
        String header = objType + " " + data.length;
        byte[] fullData = (header + "\0").getBytes(StandardCharsets.UTF_8);
        fullData = concatenate(fullData, data);

        String sha1 = HashUtils.computeSHA1(fullData);

        if (write) {
            String path = ".gitty/objects/" + sha1.substring(0, 2) + "/" + sha1.substring(2);
            File file = new File(path);
            file.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(file)) {
                try (DeflaterOutputStream dos = new DeflaterOutputStream(fos, new Deflater(Deflater.DEFAULT_COMPRESSION))) {
                    dos.write(fullData);
                }
            }
        }

        return sha1;
    }
```


### Staging  

Staging acts as an intermediate area where changes are prepared before committing them to the repository's history.

The staging area (or index) tracks changes (added, modified, or deleted files) ready for commit, allowing users to organize and review updates before finalizing them. It is needed to manage partial commits, ensure only intended changes are committed, and reduce errors by providing a review step before changes are permanently added.

---

#### 1. `addFilesToIndex(List<File> files)`
- Adds specified files to the staging area by creating blob objects for them and updating their metadata in the index.

```java
public void addFilesToIndex(List<File> files) throws IOException, NoSuchAlgorithmException {
        List<IndexEntry> entries = readIndex();

        // Map existing entries for quick lookup by path
        Map<String, IndexEntry> entriesByPath = new HashMap<>();
        for (IndexEntry entry : entries) {
            entriesByPath.put(entry.getPath(), entry);
        }

        for (File file : files) {
            byte[] data = FileUtils.readFile(file);
            long currentTime = System.currentTimeMillis();

            String sha1 = createObject(data, "blob", true);
            // Check if the file is already in the index
            IndexEntry existingEntry = entriesByPath.get(file.getPath());
            if (existingEntry != null) {
                existingEntry.setSha1(sha1);
                existingEntry.setModifiedTime(currentTime);
                existingEntry.setSize(file.length());
            } else {
                IndexEntry newEntry = new IndexEntry(
                        file.getPath(),
                        sha1,
                        currentTime,
                        file.length()
                );
                entries.add(newEntry);
                entriesByPath.put(file.getPath(), newEntry);
            }
        }

        writeIndex(entries);
    }

```

---

#### 2. `readIndex()`
- Reads and parses the `.gitty/index` file, returning a list of `IndexEntry` objects representing the staged files.

```java
public List<IndexEntry> readIndex() throws IOException {
        File indexFile = new File(".gitty/index");
        if (!indexFile.exists()) {
            return new ArrayList<>(); // Return an empty list if the index file doesn't exist
        }

        try (FileInputStream fis = new FileInputStream(indexFile)) {
            byte[] data = fis.readAllBytes();

            // Validate checksum (last 20 bytes are SHA-1 hash of the rest)
            byte[] content = Arrays.copyOf(data, data.length - 20);
            byte[] expectedChecksum = Arrays.copyOfRange(data, data.length - 20, data.length);
            byte[] actualChecksum = HashUtils.computeSHA1Bytes(content);

            if (!Arrays.equals(expectedChecksum, actualChecksum)) {
                throw new IOException("Invalid index checksum");
            }

            // Parse header
            ByteBuffer buffer = ByteBuffer.wrap(content);
            buffer.order(ByteOrder.BIG_ENDIAN);

            byte[] signature = new byte[4];
            buffer.get(signature);
            if (!Arrays.equals(signature, "DIRC".getBytes(StandardCharsets.UTF_8))) {
                throw new IOException("Invalid index signature");
            }

            int version = buffer.getInt();
            if (version != 2) {
                throw new IOException("Unsupported index version: " + version);
            }

            int numEntries = buffer.getInt();
            List<IndexEntry> entries = new ArrayList<>();

            // Parse entries
            for (int i = 0; i < numEntries; i++) {
                long mtimeSec = buffer.getInt() & 0xFFFFFFFFL;
                long mtimeNano = buffer.getInt() & 0xFFFFFFFFL; // Unused, but aligned with written structure
                buffer.getInt(); // Skip dev (placeholder)
                buffer.getInt(); // Skip ino (placeholder)
                buffer.getInt(); // Skip mode (placeholder)
                buffer.getInt(); // Skip uid (placeholder)
                buffer.getInt(); // Skip gid (placeholder)
                long size = buffer.getInt() & 0xFFFFFFFFL;
                byte[] sha1 = new byte[20];
                buffer.get(sha1);
                buffer.getShort(); // Skip flags (placeholder)

                // Read variable-length path
                StringBuilder pathBuilder = new StringBuilder();
                while (true) {
                    byte b = buffer.get();
                    if (b == 0) break; // Null-terminator
                    pathBuilder.append((char) b);
                }
                String path = pathBuilder.toString();

                // Skip padding
                int entryLength = ((62 + path.length() + 8) / 8) * 8;
                int paddingLength = entryLength - (62 + path.length());
                buffer.position(buffer.position() + paddingLength);

                entries.add(new IndexEntry(path, bytesToHex(sha1), mtimeSec, size));
            }

            return entries;
        }
    }
```

---

#### 3. `writeIndex(List<IndexEntry> entries)`
- Writes the current state of the index to the `.gitty/index` file.

```java
    private void writeIndex(List<IndexEntry> entries) throws IOException {
        File indexFile = new File(".gitty/index");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             FileOutputStream fos = new FileOutputStream(indexFile)) {

            // Write header
            baos.write("DIRC".getBytes(StandardCharsets.UTF_8)); // Signature
            baos.write(intToBytes(2)); 
            baos.write(intToBytes(entries.size())); 

            // Write entries
            for (IndexEntry entry : entries) {
                baos.write(intToBytes((int) (entry.getModifiedTime() / 1000)));
                baos.write(intToBytes((int) (entry.getModifiedTime() % 1000))); 
                baos.write(intToBytes(0)); // dev 
                baos.write(intToBytes(0)); // ino 
                baos.write(intToBytes(0)); // mode 
                baos.write(intToBytes(0)); // uid 
                baos.write(intToBytes(0)); // gid 
                baos.write(intToBytes((int) entry.getSize())); 
                baos.write(hexToBytes(entry.getSha1())); 
                baos.write(shortToBytes(0)); 

                // Write path
                baos.write(entry.getPath().getBytes(StandardCharsets.UTF_8));
                baos.write(0); // Null-terminator for path

                // Add padding
                int entryLength = ((62 + entry.getPath().length() + 8) / 8) * 8;
                int paddingLength = entryLength - (62 + entry.getPath().length());
                baos.write(new byte[paddingLength]); // Write padding as zeros
            }

            // Compute checksum
            byte[] content = baos.toByteArray();
            byte[] checksum = HashUtils.computeSHA1Bytes(content);
            baos.write(checksum);

            // Write all data to the file
            fos.write(baos.toByteArray());
        }
    }

```
## Tree object

A **tree** represents the hierarchical structure of files and directories in a repository. Each entry in a tree is either:
- A **blob** (file), identified by its SHA1 hash and file mode (`100644`).
- Another **tree** (subdirectory), identified by its SHA1 hash and tree mode (`040000`).

#### `create Tree Object`
Creates a tree object from a list of index entries (representing files and directories).

1. Groups files by their directory structure.
2. Recursively creates tree objects starting from the root directory.
3. Returns the SHA1 hash of the created tree object.

```java
  public String createTreeObject(List<IndexEntry> entries) throws IOException, NoSuchAlgorithmException {
        Map<String, List<IndexEntry>> directoryMap = new HashMap<>();
        for (IndexEntry entry : entries) {
            String path = entry.getPath();
            int lastSlash = path.lastIndexOf('/');
            String directory = lastSlash == -1 ? "" : path.substring(0, lastSlash);
            directoryMap.computeIfAbsent(directory, k -> new ArrayList<>()).add(entry);
        }

        // Recursively create tree objects starting from the root
        return writeTree(directoryMap, "");
    }
```

#### `writeTree(Map<String, List<IndexEntry>> directoryMap, String currentDir)`
Handles the recursive creation of tree objects:
- Writes blobs (files) and subtrees (directories) to a tree content stream.
- Calls `createObject()` to store the tree and compute its SHA1 hash.

```java
 private String writeTree(Map<String, List<IndexEntry>> directoryMap, String currentDir) throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream treeContent = new ByteArrayOutputStream();
        List<IndexEntry> entries = directoryMap.getOrDefault(currentDir, new ArrayList<>());

        // Add blobs (files) to the tree
        for (IndexEntry entry : entries) {
            String fileName = entry.getPath().substring(currentDir.length() + (currentDir.isEmpty() ? 0 : 1));
            treeContent.write(String.format("100644 %s\0", fileName).getBytes(StandardCharsets.UTF_8));
            treeContent.write(hexToRawBytes(entry.getSha1())); // Convert hex SHA1 to raw bytes
        }

        // Add subdirectories to the tree
        for (String dir : directoryMap.keySet()) {
            if (dir.startsWith(currentDir) && !dir.equals(currentDir)) {
                // Process subdirectories
                String subDir = dir.substring(currentDir.length() + (currentDir.isEmpty() ? 0 : 1));
                if (subDir.contains("/")) {
                    String directSubDir = subDir.split("/")[0]; // Get the first level of the subdirectory
                    String subTreeHash = writeTree(directoryMap, dir); // Recursively process the subdirectory
                    treeContent.write(String.format("040000 %s\0", dir).getBytes(StandardCharsets.UTF_8));
                    treeContent.write(hexToRawBytes(subTreeHash)); // Convert hex SHA1 to raw bytes
                }
            }
        }

        // Create the tree object
        byte[] treeData = treeContent.toByteArray();
        return createObject(treeData, "tree", true);
    }
```

#### `readTree(String treeSha)`
Reads and parses a tree object by:
1. Decompressing the object.
2. Parsing the file/directory entries from the tree's content.
3. Recursively processing nested trees for directories.
4. Returns a map of file paths to their SHA1 hashes.

```java
    public Map<String, String> readTree(String treeSha) throws IOException {
        Map<String, String> treeEntries = new HashMap<>();

        // Locate and decompress the tree object
        File objectFile = getObjectFile(treeSha);
        byte[] objectData = decompressObject(objectFile);

        // Skip the header (e.g., "tree <size>\0")
        int headerEnd = 0;
        while (objectData[headerEnd] != 0) {
            headerEnd++;
        }
        headerEnd++; // Move past the null terminator

        ByteArrayInputStream inputStream = new ByteArrayInputStream(objectData, headerEnd, objectData.length - headerEnd);

        // Parse tree entries
        while (inputStream.available() > 0) {
            // Read the mode (e.g., 100644 or 040000)
            StringBuilder mode = new StringBuilder();
            while (true) {
                int b = inputStream.read();
                if (b == ' ') break;
                mode.append((char) b);
            }

            // Read the file or directory name
            StringBuilder name = new StringBuilder();
            while (true) {
                int b = inputStream.read();
                if (b == 0) break; // Null terminator signals end of name
                name.append((char) b);
            }

            // Read the raw SHA1 (20 bytes)
            byte[] sha1Bytes = new byte[20];
            inputStream.read(sha1Bytes);
            String sha1 = rawBytesToHex(sha1Bytes);

            // Skip the entry if it already exists in the map
            if (treeEntries.containsKey(name.toString())) {
                continue; // Skip duplicate entries
            }

            if (mode.toString().equals("040000")) {
                // Directory entry (tree)
                treeEntries.put(name.toString() + "/", sha1);

                // Recursive call to process nested trees
                Map<String, String> subTreeEntries = readTree(sha1);
                for (Map.Entry<String, String> entry : subTreeEntries.entrySet()) {
                    // Add entries with unique names
                    String fullPath = entry.getKey();
                    if (!treeEntries.containsKey(fullPath)) {
                        treeEntries.put(fullPath, entry.getValue());
                    }
                }
            } else if (mode.toString().equals("100644")) {
                treeEntries.put(name.toString(), sha1);
            }
        }
        return treeEntries;
    }
```java
  public Map<String, List<String>> compareTrees(String currentTreeSha, String parentTreeSha) throws IOException {
        Map<String, String> currentTree = readTree(currentTreeSha);
        Map<String, String> parentTree = parentTreeSha != null ? readTree(parentTreeSha) : new HashMap<>();


        List<String> addedFiles = new ArrayList<>();
        List<String> deletedFiles = new ArrayList<>();
        List<String> modifiedFiles = new ArrayList<>();
        List<String> modifiedFilesShas = new ArrayList<>();

        // Detect added files
        for (String filePath : currentTree.keySet()) {
            if (!filePath.contains("/")){
                if (!parentTree.containsKey(filePath)) {
                    addedFiles.add(filePath);
                } else {
                    // Check if the file has been modified (compare SHA1)
                    String currentSha1 = currentTree.get(filePath);
                    String parentSha1 = parentTree.get(filePath);
                    if (!currentSha1.equals(parentSha1)) {
                        modifiedFiles.add(filePath);
                        modifiedFilesShas.add(currentSha1);
                        modifiedFilesShas.add(parentSha1);
                    }
                }
        }
        }

        // Detect deleted files
        for (String filePath : parentTree.keySet()) {
            if (!currentTree.containsKey(filePath)) {
                deletedFiles.add(filePath);
            }
        }

        // Prepare the changes map
        Map<String, List<String>> changes = new HashMap<>();
        changes.put("added", addedFiles);
        changes.put("deleted", deletedFiles);
        changes.put("modified", modifiedFiles);
        changes.put("modifiedShas",modifiedFilesShas);

        return changes;
    }

```



## Commits

The `CommitManager` class is responsible for managing and creating commits within gitty. It facilitates creating, reading, and traversing commits, and ensures the repository's state is properly updated during commit operations. 

## Key Responsibilities and Processes

### 1. **Writing a Commit**
   - **Preparing the Commit:**
     - The `writeCommit` method creates a tree object from the current working index using the `TreeManager`.
     - It compares the new tree with the previous commit's tree (if available) to identify added, modified, or deleted files.
     - Metadata such as the author's name, email, and timestamp are recorded.

   - **Commit Content:**
     - The commit includes:
       - The SHA of the tree object representing the current file state.
       - A reference to the parent commit (if it exists).
       - The commit message and metadata about the author and committer.

   - **Storing the Commit:**
     - The commit is serialized and saved as a Git object within the `.gitty/objects` directory.
     - The branch's reference file (e.g., `refs/heads/main`) is updated to point to the new commit SHA.

   - **Output:**
     - If no changes are detected in the working directory (e.g., no modified, added, or deleted files), a message indicates the working tree is clean.
     - Otherwise, the new commit details, including added, deleted, or modified files, are displayed.

```java
public String writeCommit(String commitMessage, List<IndexEntry> entries, String authorName, String authorEmail, String parentSha)
            throws IOException, NoSuchAlgorithmException {
    TreeManager treeManager = new TreeManager();
    // Create the tree object
    String treeSha = treeManager.createTreeObject(entries);

    Commit commit = readCommit(parentSha);
    Map<String, List<String>> changes = treeManager.compareTrees(treeSha, commit.getTreeSha());

    File headFile = new File(".gitty/HEAD");
    if (!headFile.exists()) {
        throw new IOException("No .gitty repository found. Are you inside a repository?");
    }

    String headContent = Files.readString(headFile.toPath()).trim();
    if (!headContent.startsWith("ref: ")) {
        throw new IOException("HEAD does not point to a valid branch reference.");
    }

    String branchPath = headContent.substring(5); 
    File branchFile = new File(".gitty/" + branchPath);

    if (!branchFile.exists()) {
        branchFile.getParentFile().mkdirs();
        branchFile.createNewFile();
    }
    String branchName = branchPath.replace("refs/heads/", "");

    if(changes.get("added").isEmpty() && changes.get("deleted").isEmpty() && changes.get("modified").isEmpty()){
        System.out.println("On branch:"+branchName);
        System.out.println("nothing to commit, working tree clean");
        return "";
    }

    long timestamp = System.currentTimeMillis() / 1000L; // Unix timestamp
    String timezoneOffset = "+0000"; // Use UTC for simplicity

    StringBuilder commitContent = new StringBuilder();
    commitContent.append("tree ").append(treeSha).append("\n");
    if (parentSha != null && !parentSha.isEmpty()) {
        commitContent.append("parent ").append(parentSha).append("\n");
    }
    commitContent.append("author ").append(authorName).append(" <").append(authorEmail).append("> ")
            .append(timestamp).append(" ").append(timezoneOffset).append("\n");
    commitContent.append("committer ").append(authorName).append(" <").append(authorEmail).append("> ")
            .append(timestamp).append(" ").append(timezoneOffset).append("\n\n");
    commitContent.append(commitMessage).append("\n");

    byte[] commitData = commitContent.toString().getBytes(StandardCharsets.UTF_8);
    String commitSha = GitObject.createObject(commitData, "commit", true); // Save commit to .git/objects

    Files.writeString(branchFile.toPath(), commitSha);

    System.out.println("[" + branchName + (parentSha == null ? " (root-commit)" : "") + " " + commitSha.substring(0, 7) + "] " + commitMessage);
    System.out.println("Added files: " + changes.get("added"));
    System.out.println("Deleted files: " + changes.get("deleted"));
    System.out.println("Modifies files: " + changes.get("modified"));
    return commitSha;
}
```

### 2. **Reading a Commit**
   - The `readCommit` method retrieves a commit object from the `.gitty/objects` directory.
   - The commit data is decompressed, parsed, and returned as a `Commit` object. This includes the commit tree SHA, parent SHAs, author, committer, and message.
```java
public Commit readCommit(String commitSha) throws IOException {
    String objectPath = ".gitty/objects/" + commitSha.substring(0, 2) + "/" + commitSha.substring(2);
    File commitFile = new File(objectPath);

    if (!commitFile.exists()) {
        throw new FileNotFoundException("Commit object not found: " + commitSha);
    }

    byte[] compressedData = Files.readAllBytes(commitFile.toPath());
    ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
    InflaterInputStream inflater = new InflaterInputStream(bais);
    ByteArrayOutputStream decompressedData = new ByteArrayOutputStream();

    int bytesRead;
    byte[] buffer = new byte[1024];
    while ((bytesRead = inflater.read(buffer)) != -1) {
        decompressedData.write(buffer, 0, bytesRead);
    }
    inflater.close();

    byte[] rawContent = decompressedData.toByteArray();
    String commitContent = new String(rawContent, StandardCharsets.UTF_8);

    int headerEnd = commitContent.indexOf('\0');
    if (headerEnd == -1) {
        throw new IOException("Invalid commit object format.");
    }
    String contentWithoutHeader = commitContent.substring(headerEnd + 1);

    String[] lines = contentWithoutHeader.split("\n");
    String treeSha = null;
    List<String> parentShas = new ArrayList<>();
    String author = null;
    String committer = null;
    StringBuilder messageBuilder = new StringBuilder();
    boolean isMessage = false;

    for (String line : lines) {
        if (line.startsWith("tree ")) {
            treeSha = line.substring(5).trim();
        } else if (line.startsWith("parent ")) {
            parentShas.add(line.substring(7).trim());
        } else if (line.startsWith("author ")) {
            author = line.substring(7).trim();
        } else if (line.startsWith("committer ")) {
            committer = line.substring(10).trim();
        } else if (line.isEmpty()) {
            isMessage = true;
        } else if (isMessage) {
            messageBuilder.append(line).append("\n");
        }
    }

    String message = messageBuilder.toString().trim();
    return new Commit(treeSha, parentShas, author, committer, message);
}

```

### 3. **Retrieving the Latest Commit**
   - The `getLatestCommitSha` method reads the current branch reference from `.gitty/HEAD` and retrieves the latest commit SHA from the branch file.

```java
public String getLatestCommitSha() throws IOException {
    File headFile = new File(".gitty/HEAD");
    if (!headFile.exists()) {
        throw new IOException("No .gitty repository found. Are you inside a repository?");
    }

    String headContent = Files.readString(headFile.toPath()).trim();
    if (!headContent.startsWith("ref: ")) {
        throw new IOException("HEAD does not point to a valid branch reference.");
    }

    String branchPath = headContent.substring(5);
    File branchFile = new File(".gitty/" + branchPath);

    if (!branchFile.exists()) {
        return null;
    }

    String latestCommitSha = Files.readString(branchFile.toPath()).trim();
    return latestCommitSha.isEmpty() ? null : latestCommitSha;
}

```

### 4. **Viewing Commit History**
   - The `viewCommitHistory` method iterates through the commit chain starting from the latest commit.
   - For each commit, it prints details such as the SHA, author, date, and commit message.
   - The chain is traversed via parent SHAs, stopping when no further parent is available.

This class implements foundational commit management.
```java

    public void viewCommitHistory() throws IOException {
        
        String headRef = Files.readString(new File(".gitty/HEAD").toPath()).trim();
        String currentCommitSha;

        if (headRef.startsWith("ref: ")) {
            
            String branchRef = headRef.substring(5);
            currentCommitSha = Files.readString(new File(".gitty/" + branchRef).toPath()).trim();
        } else {
           
            currentCommitSha = headRef;
        }

       
        System.out.println("Commit history:");
        while (currentCommitSha != null && !currentCommitSha.isEmpty()) {
            Commit commit = readCommit(currentCommitSha);

          
            System.out.println("Commit: " + currentCommitSha);
            System.out.println("Author: " + commit.getAuthor());
            System.out.println("Date: " + formatUnixTimestamp(commit.getCommitter())); 
            System.out.println("\n    " + commit.getMessage());
            System.out.println();

            
            if (commit.getParentShas().isEmpty()) {
                break; 
            }
            currentCommitSha = commit.getParentShas().get(0); 
        }
    }

    private String formatUnixTimestamp(String committerLine) {
        // Extract the timestamp from the committer line (e.g., "committer Name <email> 1699119735 +0000")
        String[] parts = committerLine.split(" ");
        if (parts.length >= 3) {
            long timestamp = Long.parseLong(parts[parts.length - 2]);
            return new java.util.Date(timestamp * 1000L).toString();
        }
        return "Unknown date";
    }

```


### Branch
## BranchManager Class Documentation

### createBranch Method
Creates a new branch in the repository, pointing to the same commit as the current HEAD. If the branch already exists, it prints a message and does nothing.

```java
public void createBranch(String branchName) throws IOException {
    // Get the current HEAD reference
    File headFile = new File(".gitty/HEAD");
    if (!headFile.exists()) {
        throw new IOException("No .gitty directory found. Are you inside a repository?");
    }

    // Read the current HEAD content
    String currentHead = Files.readString(headFile.toPath()).trim();
    System.out.println("current head 1" + currentHead);

    // If HEAD points to a branch, resolve the commit hash
    if (currentHead.startsWith("ref: ")) {
        String branchPath = currentHead.substring(5); // Remove "ref: " prefix
        File branchFile = new File(".gitty/" + branchPath);
        if (!branchFile.exists()) {
            throw new IOException("Current branch reference does not exist: " + branchPath);
        }
        currentHead = Files.readString(branchFile.toPath()).trim(); // Read the commit hash
    }

    // Create the new branch file in .git/refs/heads
    File newBranchFile = new File(".gitty/refs/heads/" + branchName);
    if (newBranchFile.exists()) {
        System.out.println("Branch " + branchName + " already exists.");
        return;
    }
    // Write the commit hash to the new branch file
    newBranchFile.getParentFile().mkdirs(); // Ensure parent directory exists
    System.out.println("current head" + currentHead);
    System.out.println("current head" + newBranchFile.toPath());
    Files.writeString(newBranchFile.toPath(), currentHead); // Write the commit hash
    System.out.println("Branch " + branchName + " created, pointing to commit " + currentHead + ".");
}
```

### switchBranch Method
Switches to an existing branch by updating the HEAD file to point to the specified branch. If the branch doesn't exist, it throws an error.
```java
public void switchBranch(String branchName) throws IOException {
    // Step 1: Verify if the branch exists
    File branchFile = new File(".gitty/refs/heads/" + branchName);
    if (!branchFile.exists()) {
        throw new IOException("Branch " + branchName + " does not exist.");
    }

    // Step 2: Read the commit hash the branch points to
    String branchCommitHash = Files.readString(branchFile.toPath()).trim();

    // Step 3: Update the HEAD file to point to the new branch
    File headFile = new File(".gitty/HEAD");
    if (!headFile.exists()) {
        throw new IOException("No .gitty directory found. Are you inside a repository?");
    }

    String newHeadContent = "ref: refs/heads/" + branchName;
    Files.writeString(headFile.toPath(), newHeadContent);

    System.out.println("Switched to branch '" + branchName + "'.");
    System.out.println("Branch now points to commit " + branchCommitHash + ".");
}
```

## Merge branch
## BranchMerger Class Documentation

### mergeBranch Method
Merges the specified branch into the current branch. It performs conflict detection and creates a new merge commit if no conflicts are found.

```java
public void mergeBranch(String branchName) throws IOException, NoSuchAlgorithmException {
    // Step 1: Ensure the branch exists
    File branchFile = new File(".gitty/refs/heads/" + branchName);
    if (!branchFile.exists()) {
        throw new IOException("Branch " + branchName + " does not exist.");
    }

    // Step 2: Get the current branch's HEAD SHA
    File headFile = new File(".gitty/HEAD");
    if (!headFile.exists()) {
        throw new IOException("No .gitty directory found. Are you inside a repository?");
    }

    String currentBranchRef = Files.readString(headFile.toPath()).trim();
    if (!currentBranchRef.startsWith("ref: ")) {
        throw new IOException("HEAD is not pointing to a branch.");
    }

    String currentBranch = currentBranchRef.substring(5); // Remove "ref: "
    File currentBranchFile = new File(".gitty/" + currentBranch);
    if (!currentBranchFile.exists()) {
        throw new IOException("Current branch reference does not exist.");
    }
    String currentHeadSha = Files.readString(currentBranchFile.toPath()).trim();

    // Step 3: Get the target branch's HEAD SHA
    String targetHeadSha = Files.readString(branchFile.toPath()).trim();

    // Step 4: Detect conflicting changes (basic conflict detection)
    List<String> conflicts = detectConflicts(currentHeadSha, targetHeadSha);
    if (!conflicts.isEmpty()) {
        System.out.println("Merge aborted: Conflicting changes detected in the following files:");
        for (String conflict : conflicts) {
            System.out.println(" - " + conflict);
        }
        return;
    }

    // Step 5: Create a new merge commit
    List<String> parentShas = new ArrayList<>();
    parentShas.add(currentHeadSha); // Current branch's HEAD
    parentShas.add(targetHeadSha);  // Target branch's HEAD

    // Dummy commit message for simplicity
    String mergeMessage = "Merge branch '" + branchName + "' into " + currentBranch;
    String authorName = "Default Author";
    String authorEmail = "default@example.com";

    String newCommitSha = new CommitManager().writeCommit(mergeMessage, Collections.emptyList(), authorName, authorEmail, String.join(" ", parentShas));

    // Step 6: Update the current branch to point to the new commit
    Files.writeString(currentBranchFile.toPath(), newCommitSha);
    String currentBranchName =  currentBranch.substring(11);
    System.out.println("Merged branch '" + branchName + "' into '" + currentBranchName + "'. New commit: " + newCommitSha);
}
```

### detectConflicts Method
Detects conflicts between the current branch and the target branch by comparing file content hashes in both branches' commit trees.

```java
private List<String> detectConflicts(String currentHeadSha, String targetHeadSha) throws IOException {
    CommitManager commitManager = new CommitManager();

    // Load the commits
    Commit currentCommit = commitManager.readCommit(currentHeadSha);
    Commit targetCommit = commitManager.readCommit(targetHeadSha);

    // Load the trees
    TreeManager treeManager = new TreeManager();
    Map<String, String> currentTree = treeManager.readTree(currentCommit.getTreeSha());
    Map<String, String> targetTree = treeManager.readTree(targetCommit.getTreeSha());

    // Detect conflicting files
    List<String> conflicts = new ArrayList<>();
    for (String filePath : currentTree.keySet()) {
        if (targetTree.containsKey(filePath)) {
            String currentFileSha = currentTree.get(filePath);
            String targetFileSha = targetTree.get(filePath);
            if (!currentFileSha.equals(targetFileSha)) {
                conflicts.add(filePath); // File exists in both trees with different content
            }
        }
    }

    return conflicts;
}
```

### clone repository

Clones the repository to a specified target path by copying files from the latest commit's tree. It identifies paths and files, then copies the relevant files to the target location, maintaining the directory structure.

```java
public void cloneRepo(String targetPath) throws IOException {

    CommitManager commitManager = new CommitManager();
    String commitSha = commitManager.getLatestCommitSha();

    // Read commit to get tree
    Commit commit = commitManager.readCommit(commitSha);
    String tree = commit.getTreeSha();

    TreeManager treeManager = new TreeManager();
    Map<String, String> treeEntries = treeManager.readTree(tree);
    Set<String> paths = new HashSet<>();
    Set<String> files = new HashSet<>();

    // Separate paths and files
    for (String filePath : treeEntries.keySet()) {
        if (filePath.endsWith("/")) {
            paths.add(filePath);
        } else {
            files.add(filePath);
        }
    }

    // Construct full paths for files
    Set<String> fullPaths = new HashSet<>();
    for (String path : paths) {
        File directory = new File(path);
        if (directory.exists() && directory.isDirectory()) {
            for (String file : files) {
                // Check if the file exists in the directory
                File potentialFile = new File(directory, file);
                if (potentialFile.exists()) {
                    // Construct the full path
                    fullPaths.add(potentialFile.getPath());
                }
            }
        }
    }

    // Clone files to target path
    for (String fullPath : fullPaths) {
        File sourceFile = new File(fullPath);
        // Determine the relative path from the original structure
        String relativePath = sourceFile.getParentFile().getPath().replace(File.separator, "/");
        String targetFilePath = targetPath + "/" + relativePath + "/" + sourceFile.getName();

        // Create directories in the target path
        File targetFile = new File(targetFilePath);
        targetFile.getParentFile().mkdirs();

        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    System.out.println("Files cloned successfully to: " + targetPath);
}
```

### Diffs

This method compares the latest commits of two branches, finds changes (added, deleted, modified files), and displays the differences.

```java
public void diffBranches(String otherBranch) throws IOException {
    // Get current branch
    String currentBranchName = getCurrentBranch();
    String currentBranchCommitSha = getBranchLatestCommitSha(currentBranchName);
    String otherBranchCommitSha = getBranchLatestCommitSha(otherBranch);

    // Get commits
    CommitManager commitManager = new CommitManager();
    Commit currentBranchCommit = commitManager.readCommit(currentBranchCommitSha);
    Commit otherBranchCommit = commitManager.readCommit(otherBranchCommitSha);

    // Compare trees
    TreeManager treeManager = new TreeManager();
    Map<String, List<String>> changes = treeManager.compareTrees(currentBranchCommit.getTreeSha(), otherBranchCommit.getTreeSha());

    if(changes.get("added").isEmpty() && changes.get("deleted").isEmpty() && changes.get("modified").isEmpty()){
        System.out.println("No difference between branches " + currentBranchName + " and " + otherBranch);
    }

    for (int i = 0; i < changes.get("modifiedShas").toArray().length; i += 2) {
        // Ensure we don't go out of bounds
        if (i + 1 < changes.get("modifiedShas").toArray().length) {
            compareBlobs((String) changes.get("modifiedShas").toArray()[i], (String) changes.get("modifiedShas").toArray()[i + 1]);
        }
    }

    System.out.println("Added files: " + changes.get("added"));
    System.out.println("Deleted files: " + changes.get("deleted"));
    System.out.println("Modified files: " + changes.get("modified"));
}
```

# Testing
# Limitations


Command list th
