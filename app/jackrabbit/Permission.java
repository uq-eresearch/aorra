package jackrabbit;

public enum Permission {
    NONE(false, false), RO(true, false), RW(true, true);

    private boolean read;
    private boolean write;

    private Permission(boolean read, boolean write) {
        this.read = read;
        this.write = write;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isWrite() {
        return write;
    }

}
