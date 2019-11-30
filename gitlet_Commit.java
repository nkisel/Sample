package gitlet;

import java.io.File;
import java.util.*;
import java.text.SimpleDateFormat;
import java.io.Serializable;

import static gitlet.Utils.*;

public class Commit implements Serializable {

    /** Default commit constructor. */
    public Commit() {
        _message = "initial commit";
        updateTime(0);
        _files = new HashMap<>();
        _names = new HashMap<>();
        _hashes = new HashSet<>();

        _id = sha1(new String[] {String.valueOf(_time), _message, _files.toString()});
    }

    /** Construct a commit with the current time and commit message MSG,
     * inheriting unchanged files from PARENT. Files specified in UNTRACK
     * and STAGED are added and removed from the default state accordingly. */
    public Commit(Commit parent, String msg, List<String> untrack, List<String> staged) {
        this();

        _p = parent.id();
        _parent = parent.update();
        _files.putAll(parent.files());
        _names.putAll(parent.names());

        for (String fileID : untrack) {
            Blob remove = _files.get(fileID);
            _names.remove(remove.name());
            _files.remove(fileID);
            _hashes.remove(fileID);
        }

        for (String fileID : staged) {
            File fileLocation = new File("./.gitlet/staging/" + fileID);
            Blob stage = readObject(fileLocation, Blob.class);
            String fileName = stage.name();

            Blob obsolete = _names.get(fileName);
            if (obsolete != null) {
                _files.remove(obsolete.id());
                _hashes.remove(obsolete.id());
            }

            _names.put(fileName, stage);
            _hashes.add(fileID);
            _files.put(fileID, stage);
        }

        _message = msg;
        updateTime(-1);
        _id = sha1(new String[] {String.valueOf(_time),
                _message, _files.toString()});
    }

    /** Construct a commit with two parents. */
    public Commit(Commit first, Commit second, String msg,
                  List<String> untrack, List<String> staged) {
        this(first, msg, untrack, staged);
        _m = second.id();
    }

    /** Set the commit time to TIME. */
    private void updateTime(long time) {
        Date now = new Date();
        if (time != -1) {
            now.setTime(time);
        }
        _time = now.getTime();
        _datetime = fmt.format(now);
    }

    private void addFile(File newFile) {
        String name = newFile.getName();

    }

    /** Given a file's common name, return the corresponding tracked Blob. */
    public Blob getFile(String file) {
        return names().get(file);
    }

    /** Return all files tracked by this commit in Blob form.
     *  Must have been fully initialized or updated: inaccurate
     *  for newly-serialized files. */
    public List<Blob> getFiles() {
        ArrayList<Blob> allFiles = new ArrayList<>();
        for (String file : hashes()) {
            allFiles.add(_files.get(file));
        }
        return allFiles;
    }

    /** Given a Blob INPUT, compare it with the
     * stored Blob with the same name. */
    public boolean fileEquals(Blob input) {
        Blob compare = names().getOrDefault(input.name(), null);
        if (compare == null) {
            return false;
        } else {
            return compare.equals(input);
        }
    }

    /** Fill _names and _files from _hashes by converting each hash found
     *  in the file directory to a Blob object and extracting the name
     *  of the object. Used with transient _names and _files to avoid
     *  serialization of all Blobs during hashing at runtime. */
    public Commit update() {
        _names = new HashMap<>();
        _files = new HashMap<>();
        for (String hash : hashes()) {
            Blob file = readObject(new File(_filesPath + "/" + hash),
                    Blob.class);
            _names.put(file.name(), file);
            _files.put(file.id(), file);
        }
        return this;
    }


    public String id() {
        return _id;
    }

    public String time() {
        return _datetime;
    }

    public String message() {
        return _message;
    }

    /** Return the parent of this commit, searching through .gitlet/commits
     * if necessary. Null for the original commit. */
    public Commit parent() {
        if (_parent == null) {
            if (_p == null) {
                return null;
            }
            return readObject(new File(_commitPath + "/" + _p), Commit.class);
        }
        return _parent;
    }

    /** Return the secondary parent of this commit if it is the result
     *  of a merge. */
    public Commit mergeParent() {
        if (!isMerge()) {
            return null;
        } else {
            return readObject(new File(_commitPath + "/" + _m), Commit.class);
        }
    }

    /** Return the parents of this commit, searching through .gitlet/commits
     *  where necessary, and taking into account the secondary parent. */
    public Commit[] parents() {
        Commit[] parents = new Commit[2];
        if (_p != null) {
            parents[0] = readObject(join(_commitPath, "/", _p), Commit.class);
        }
        if (_m != null) {
            parents[1] = readObject(join(_commitPath, "/", _m), Commit.class);
        }
        return parents;
    }

    /** Return the number of parents this commit has. */
    public int parentCount() {
        if (_p == null) {
            return 0;
        } else if (_m == null) {
            return 1;
        } else {
            return 2;
        }
    }

    /** Return whether this commit is the result of a merge. */
    public boolean isMerge() {
        return (_m != null);
    }

    public HashSet<String> hashes() {
        return _hashes;
    }

    public HashMap<String, Blob> files() {
        return _files;
    }

    public HashMap<String, Blob> names() {
        return _names;
    }

    @Override
    public boolean equals(Object obj) {
        return _id.equals(((Commit) obj).id());
    }

    /** Path to all stored commits. */
    private final File _commitPath = new File("./.gitlet/commits/");

    /** Path to all stored files. */
    private final File _filesPath = new File("./.gitlet/files/");

    /** Commit time in ms since Unix Epoch. */
    private long _time;

    /** Commit date and time. */
    private String _datetime;

    /** The (almost) unique SHA-1 hash value for this commit. */
    private String _id;

    /** The commit message. */
    private String _message;

    /** This commit's parent. Null for the original commit (git init). */
    private transient Commit _parent;

    /** This commit's parent hash value. Null for the original commit. */
    private String _p;

    /** This commit's secondary parent. Null for all non-merged commits. */
    private String _m;

    /** The date format to be displayed by this commit in logs. */
    private transient SimpleDateFormat fmt = new SimpleDateFormat("E MMM dd k:mm:ss yyy Z");

    /** The set of hash codes for all files associated with this commit. */
    private HashSet<String> _hashes;

    /** The mapping between this commit's file hashes and files themselves. */
    private transient HashMap<String, Blob> _files;

    /** The mapping between file names and the files themselves. */
    private transient HashMap<String, Blob> _names;



}
