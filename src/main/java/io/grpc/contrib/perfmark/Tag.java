package io.grpc.contrib.perfmark;

/**
 * Tag is a dynamic, runtime created identifier (such as an RPC id).
 */
public final class Tag {
    private static final long NO_TAG_ID = 0;
    private static final String NO_TAG_NAME = null;

    static final Tag NO_TAG = new Tag();

    final long tagId;
    final String tagName;

    Tag() {
        this.tagId = NO_TAG_ID;
        this.tagName = NO_TAG_NAME;
    }

    Tag(long tagId) {
        if (tagId == NO_TAG_ID) {
            throw new IllegalArgumentException("bad tag id");
        }
        this.tagId = tagId;
        this.tagName = NO_TAG_NAME;
    }

    Tag(String tagName) {
        this.tagId = NO_TAG_ID;
        if (tagName == NO_TAG_NAME) {
            throw new IllegalArgumentException("bad tag name");
        }
        this.tagName = tagName;
    }

    Tag(long tagId, String tagName) {
        if (tagId == NO_TAG_ID) {
            throw new IllegalArgumentException("bad tag id");
        }
        this.tagId = tagId;
        if (tagName == NO_TAG_NAME) {
            throw new IllegalArgumentException("bad tag name");
        }
        this.tagName = tagName;
    }
}
