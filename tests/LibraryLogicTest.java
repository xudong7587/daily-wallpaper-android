package cn.sunny.wallpaper;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class LibraryLogicTest {
    static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static void reject(File dir, File input) throws Exception {
        int before = Objects.requireNonNull(dir.list()).length;
        try { ContentStore.put(dir, input); throw new AssertionError("Invalid input accepted"); }
        catch (IOException expected) { }
        require(Objects.requireNonNull(dir.list()).length == before, "Failed import left a partial file");
    }
    public static void main(String[] args) throws Exception {
        File root = new File(args[0]); require(root.mkdirs(), "Use a new test directory");
        File store = new File(root, "library"); File source = new File(root, "input.bin");
        byte[] original = {1, 7, 20, 66, 31}; Files.write(source.toPath(), original);
        ContentStore.Result first = ContentStore.put(store, source);
        require(first.added && first.file.exists(), "First import failed");
        require(Arrays.equals(original, Files.readAllBytes(first.file.toPath())), "Original bytes changed");
        long modified = first.file.lastModified();
        ContentStore.Result second = ContentStore.put(store, source);
        require(!second.added && first.file.equals(second.file), "Duplicate was not deduplicated");
        require(second.file.lastModified() == modified, "Duplicate modified original timestamp");
        Files.write(source.toPath(), new byte[]{3, 2, 1});
        require(ContentStore.put(store, source).added, "Different content was lost");
        Files.write(source.toPath(), new byte[0]); reject(store, source);
        try (RandomAccessFile large = new RandomAccessFile(source, "rw")) { large.setLength(ContentStore.MAX_BYTES + 1); }
        reject(store, source);
        Files.write(source.toPath(), original); Thread.currentThread().interrupt(); reject(store, source); Thread.interrupted();
        List<String> ids = Arrays.asList("a", "b", "c"); Set<String> seen = new HashSet<>(); Random random = new Random(42);
        for (int i = 0; i < 1000; i++) { String value = Selection.choose(ids, "a", random); require(!value.equals("a"), "Consecutive repeat"); seen.add(value); }
        require(seen.equals(new HashSet<>(Arrays.asList("b", "c"))), "Random selection excluded an eligible image");
        require(Selection.choose(Collections.singletonList("a"), "a", random).equals("a"), "Single-image library failed");
        require(ids.equals(Arrays.asList("a", "b", "c")), "Selection mutated source list");
        try { Selection.choose(Collections.emptyList(), "", random); throw new AssertionError("Empty library accepted"); } catch (IllegalArgumentException expected) { }
        require(ids.contains(Selection.choose(ids, "deleted-image", random)), "Deleted previous image broke selection");
        System.out.println("PASS: original bytes, content deduplication, timestamp stability, changed content, empty/oversize/cancelled import cleanup, random selection, no-repeat, single/empty library, deleted previous entry");
    }
}
