package stroom.query.common.v2;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class Key implements Iterable<KeyPart> {
    private final List<KeyPart> keyParts;

    public Key(final List<KeyPart> keyParts) {
        this.keyParts = keyParts;
    }

    KeyPart getLast() {
        if (keyParts.size() > 0) {
            return keyParts.get(keyParts.size() - 1);
        }
        return null;
    }

    Key getParent() {
        if (keyParts.size() > 0) {
            return new Key(keyParts.subList(0, keyParts.size() - 1));
        }
        return null;
    }

    int getDepth() {
        return keyParts.size() - 1;
    }

    int size() {
        return keyParts.size();
    }

    boolean isGrouped() {
        final KeyPart last = getLast();
        return last == null || last.isGrouped();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Key key = (Key) o;
        return keyParts.equals(key.keyParts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyParts);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyParts.size(); i++) {
            final KeyPart keyPart = keyParts.get(i);
            if (i > 0) {
                sb.append("/");
            }
            keyPart.append(sb);
        }
        return sb.toString();
    }

    @Override
    @Nonnull
    public Iterator<KeyPart> iterator() {
        return keyParts.iterator();
    }
}