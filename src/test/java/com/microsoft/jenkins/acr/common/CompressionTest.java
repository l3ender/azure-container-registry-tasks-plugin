/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.acr.common;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.microsoft.jenkins.acr.common.compression.CompressibleFileImpl;
import com.microsoft.jenkins.acr.common.compression.Compression;
import com.microsoft.jenkins.acr.util.Utils;

public class CompressionTest {
    private static String workspace = "compression_test";
    private static int contentLength = 10;

    /**
     * Create a workspace for each test.
     */
    @Before
    public void setUpWorkSpaces() {
        new File(workspace).mkdir();
    }

    /**
     * Delete the workspace directory after each test.
     */
    @After
    public void tearDown() {
        File dir = new File(workspace);
        Utils.deleteDir(dir);
    }

    @Test
    public void compressionWithLongFilenameTest() throws IOException {
        File source = prepareSource(getFilename(StringUtils.repeat("a", 100).concat(".txt")));
        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(null)
                .withFile(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 1);
        Assert.assertEquals(file.fileList()[0], source.getAbsolutePath());
    }

    @Test
    public void compressionWithoutIgnoreTest() throws IOException {
        File source = prepareSource(getFilename("a.txt"));
        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(null)
                .withFile(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 1);
        Assert.assertEquals(file.fileList()[0], source.getAbsolutePath());
    }

    @Test
    public void compressionWithNonExistFile() throws IOException {
        File source = new File(getFilename("a.txt"));
        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(null)
                .withFile(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 0);
    }

    @Test
    public void compressionWithDirectory() throws IOException {
        File source = prepareFiles("source", new String[]{
                "dir/",
                "dir\\a.txt",
                "dir/b.txt",
                ".git/",
                "directory/",
                "directory/a.txt",
                "directory/b.txt",
                "directory/.git"
        });

        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(null)
                .withDirectory(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 6);
    }

    @Test
    public void compressionIgnoringGitDirectories() throws IOException {
        File source = prepareFiles("source", new String[]{
                "dir/",
                "dir/a.txt",
                "dir/b.txt",
                ".git/",
                "directory/",
                "directory/a.txt",
                "directory/b.txt",
                "directory/.git"
        });

        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(null)
                .withDirectory(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 6);
        List<String> files = getFilesWithoutSourcePath(source.getAbsolutePath(), file.fileList());
        Assert.assertThat(files, Matchers.containsInAnyOrder(
                "dir",
                "dir/a.txt",
                "dir/b.txt",
                "directory",
                "directory/a.txt",
                "directory/b.txt"
        ));
    }

    @Test
    public void compressionWithDirectoryIgnoreDir() throws IOException {
        File source = prepareFiles("source", new String[]{
                "dir/",
                "dir/a.txt",
                "dir/b.txt",
                ".git/",
                "directory/",
                "directory/a.txt",
                "directory/b.txt",
                "directory/.git"
        });

        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(new String[]{"dir"})
                .withDirectory(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 3);
        List<String> files = getFilesWithoutSourcePath(source.getAbsolutePath(), file.fileList());
        Assert.assertThat(files, Matchers.containsInAnyOrder(
                "directory",
                "directory/a.txt",
                "directory/b.txt"
        ));
    }

    @Test
    public void compressionWithDirectoryIgnoreDirContents() throws IOException {
        File source = prepareFiles("source", new String[]{
                "dir/",
                "dir/a.txt",
                "dir/b.txt",
                ".git/",
                "directory/",
                "directory/a.txt",
                "directory/b.txt",
                "directory/.git"
        });

        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(new String[]{"dir/*"})
                .withDirectory(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 3);
        List<String> files = getFilesWithoutSourcePath(source.getAbsolutePath(), file.fileList());
        Assert.assertThat(files, Matchers.containsInAnyOrder(
                "directory",
                "directory/a.txt",
                "directory/b.txt"
        ));
    }

    @Test
    public void compressionWithIgnoreFile() throws IOException {
        File source = prepareFiles("source", new String[]{
                "dir/",
                "dir/a.txt",
                "dir/b.txt",
                ".git/",
                "directory/",
                "directory/a.txt",
                "directory/b.txt",
                "directory/.git"
        });

        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(new String[]{"dir/a.txt"})
                .withDirectory(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 5);
        List<String> files = getFilesWithoutSourcePath(source.getAbsolutePath(), file.fileList());
        Assert.assertThat(files, Matchers.containsInAnyOrder(
                "dir",
                "dir/b.txt",
                "directory",
                "directory/a.txt",
                "directory/b.txt"
        ));
    }

    @Test
    public void compressionWithDirectoryIgnoreAndFileExcluded() throws IOException {
        File source = prepareFiles("source", new String[]{
                "dir/",
                "dir/a.txt",
                "dir/b.txt",
                ".git/",
                "directory/",
                "directory/a.txt",
                "directory/b.txt",
                "directory/.git"
        });

        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(new String[]{"dir/**", "!dir/a.txt"})
                .withDirectory(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 5);
        List<String> files = getFilesWithoutSourcePath(source.getAbsolutePath(), file.fileList());
        Assert.assertThat(files, Matchers.containsInAnyOrder(
                "dir",
                "dir/a.txt",
                "directory",
                "directory/a.txt",
                "directory/b.txt"
        ));
    }

    @Test
    public void compressionWithIgnoreAll() throws IOException {
        File source = prepareFiles("source", new String[]{
                "dir/",
                "dir/a.txt",
                "dir/b.txt",
                "dir/nesteddir/",
                "dir/nesteddir/a.txt",
                "dir/nesteddir/b.txt",
                ".git/",
                "a.txt",
                "b.txt",
                "directory/",
                "directory/a.txt",
                "directory/b.txt",
                "directory/.git"
        });

        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(new String[]{ "*" })
                .withDirectory(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 0);
    }

    @Test
    public void compressionWithIgnoreAllExludingDirectory() throws IOException {
        File source = prepareFiles("source", new String[]{
                "dir/",
                "dir/a.txt",
                "dir/b.txt",
                ".git/",
                "a.txt",
                "b.txt",
                "directory/",
                "directory/a.txt",
                "directory/b.txt",
                "directory/.git"
        });

        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(new String[]{ "*", "!dir/**" })
                .withDirectory(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 3);
        List<String> files = getFilesWithoutSourcePath(source.getAbsolutePath(), file.fileList());
        Assert.assertThat(files, Matchers.containsInAnyOrder(
                "dir",
                "dir/a.txt",
                "dir/b.txt"
        ));
    }

    @Test
    public void compressionWithIgnoreAllExludingDirectoryWithNested() throws IOException {
        File source = prepareFiles("source", new String[]{
                "dir/",
                "dir/a.txt",
                "dir/b.txt",
                "dir/nesteddir/",
                "dir/nesteddir/a.txt",
                "dir/nesteddir/b.txt",
                ".git/",
                "a.txt",
                "b.txt",
                "directory/",
                "directory/a.txt",
                "directory/b.txt",
                "directory/.git"
        });

        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(new String[]{ "*", "!dir/" })
                .withDirectory(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 6);
        List<String> files = getFilesWithoutSourcePath(source.getAbsolutePath(), file.fileList());
        Assert.assertThat(files, Matchers.containsInAnyOrder(
                "dir",
                "dir/a.txt",
                "dir/b.txt",
                "dir/nesteddir",
                "dir/nesteddir/a.txt",
                "dir/nesteddir/b.txt"
        ));
    }

    @Test
    public void compressionWithIgnoreAllExludingFile() throws IOException {
        File source = prepareFiles("source", new String[]{
                "dir/",
                "dir/a.txt",
                "dir/b.txt",
                ".git/",
                "a.txt",
                "b.txt",
                "directory/",
                "directory/a.txt",
                "directory/b.txt",
                "directory/.git"
        });

        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(new String[]{ "*", "!dir/a.txt" })
                .withDirectory(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 2);
        List<String> files = getFilesWithoutSourcePath(source.getAbsolutePath(), file.fileList());
        Assert.assertThat(files, Matchers.containsInAnyOrder(
                "dir",
                "dir/a.txt"
        ));
    }

    @Test
    public void compressionWithIgnoreAllExludingDirectoryAndFile() throws IOException {
        File source = prepareFiles("source", new String[]{
                "dir/",
                "dir/a.txt",
                "dir/b.txt",
                ".git/",
                "a.txt",
                "b.txt",
                "directory/",
                "directory/a.txt",
                "directory/b.txt",
                "directory/.git"
        });

        String tarball = getFilename("a.tar.gz");
        Compression.CompressedFile file = CompressibleFileImpl.compressToFile(tarball)
                .withIgnoreList(new String[]{ "*", "!dir/**", "!a.txt" })
                .withDirectory(source.getAbsolutePath())
                .compress();
        Assert.assertTrue(new File(tarball).exists());
        Assert.assertEquals(file.fileList().length, 4);
        List<String> files = getFilesWithoutSourcePath(source.getAbsolutePath(), file.fileList());
        Assert.assertThat(files, Matchers.containsInAnyOrder(
                "dir",
                "dir/a.txt",
                "dir/b.txt",
                "a.txt"
        ));
    }

    private List<String> getFilesWithoutSourcePath(String absolutePath, String[] files) {
        return Arrays.asList(files).stream().map(file -> file.replace(absolutePath + "/", "")).collect(Collectors.toList());
    }

    private File prepareSource(String filename, String content) throws IOException {
        return Utils.writeFile(filename, content, false);
    }

    private String getFilename(String name) {
        return workspace + "/" + name;
    }

    private File prepareSource(String filename, int length) throws IOException {
        return prepareSource(filename, Utils.randomString(length));
    }

    private File prepareSource(String filename) throws IOException {
        return prepareSource(filename, contentLength);
    }


    private File prepareFiles(String name, String[] files) throws IOException {
        File file = new File(getFilename(name));
        file.mkdir();
        for (String s : files) {
            String filename = name + "/" + s;
            if (s.endsWith("/")) {
                new File(getFilename(filename)).mkdir();
            } else {
                prepareSource(getFilename(filename));
            }
        }
        return file;
    }
}
