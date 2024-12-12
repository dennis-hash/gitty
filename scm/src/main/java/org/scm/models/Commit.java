package org.scm.models;

import java.util.List;

public class Commit {
    private String treeSha;
    private List<String> parentShas;
    private String author;
    private String committer;
    private String message;

    public Commit(String treeSha, List<String> parentShas, String author, String committer, String message) {
        this.treeSha = treeSha;
        this.parentShas = parentShas;
        this.author = author;
        this.committer = committer;
        this.message = message;
    }

    public String getTreeSha() { return treeSha; }
    public List<String> getParentShas() { return parentShas; }
    public String getAuthor() { return author; }
    public String getCommitter() { return committer; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "Tree: " + treeSha + "\n" +
                "Parents: " + String.join(", ", parentShas) + "\n" +
                "Author: " + author + "\n" +
                "Committer: " + committer + "\n" +
                "Message:\n" + message;
    }
}

