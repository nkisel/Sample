package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Utils.readContents;
import static gitlet.Utils.sha1;

public class Blob implements Serializable, Comparable {

    /** Default blob constructor. */
    Blob(File path) {
        _file = path;
        _name = path.getName();
        _content = readContents(path);
        _id = sha1(_name, _content);
    }



    public String id() {
        return _id;
    }

    public String name() {
        return _name;
    }

    public byte[] content() {
        return _content;
    }

    @Override
    public int compareTo(Object o) {
        return name().compareTo(((Blob) o).name());
    }

    @Override
    public boolean equals(Object obj) {
        return id().equals(((Blob) obj).id());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /** The (almost) unique SHA-1 hash value for this blob. */
    String _id;

    /** Filename. */
    private String _name;

    /** File contents. */
    private byte[] _content;

    /** The file referred to by this blob. */
    private transient File _file;

}
