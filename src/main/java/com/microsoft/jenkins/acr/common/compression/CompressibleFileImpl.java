/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.jenkins.acr.common.compression;

import com.microsoft.jenkins.acr.util.Constants;
import com.microsoft.jenkins.acr.util.Util;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

/**
 * Create a compressed file.
 */
public class CompressibleFileImpl extends TarArchiveOutputStream
        implements Compression.CompressedFile,
        Compression.CompressibleFile,
        Compression.CompressibleWithFile,
        Compression.CompressibleWithIgnore {
    private final List<String> fileList;
    private IgnoreRule[] ignores;
    private int workspaceLength;

    protected CompressibleFileImpl(String filename) throws IOException {
        super(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(filename))));
        this.fileList = new ArrayList<>();
        this.ignores = new IgnoreRule[0];
        this.workspaceLength = 0;
        this.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
    }

    @Override
    public Compression.CompressedFile compress() throws IOException {
        this.close();
        return this;
    }

    @Override
    public String[] fileList() {
        return fileList.toArray(new String[fileList.size()]);
    }

    @Override
    public Compression.CompressibleFile withFile(String filename) throws IOException {
        addFile(new File(filename), Optional.empty());
        return this;
    }

    @Override
    public Compression.CompressibleFile withDirectory(String directory) throws IOException {
        File dir = new File(directory);
        if (!dir.exists()) {
            return this;
        }

        workspaceLength = dir.getAbsolutePath().length() + 1;
        addChildren(dir, Optional.empty());
        return this;
    }

    @Override
    public Compression.CompressibleWithFile withIgnoreList(String[] ignoreList) {
        if (ignoreList == null || ignoreList.length == 0) {
            return this;
        }
        this.ignores = new IgnoreRule[ignoreList.length];
        for (int i = 0; i < this.ignores.length; i++) {
            this.ignores[i] = new IgnoreRule(ignoreList[i]);
        }
        return this;
    }

    /**
     * Add a file or directory into the compress list and record it.
     * If the file is in the ignore list, skip it.
     * @param file File need to be added
     * @throws IOException
     * @return true if file or any children were added.
     */
    private boolean addFile(File file, Optional<Boolean> parentIgnored) throws IOException {
        String relativePath = file.getAbsolutePath().substring(workspaceLength);
        if (!file.exists() || isCommonIgnore(file.getName())) {
            return false;
        }
        Optional<Boolean> ignore = isIgnoreFile(relativePath, parentIgnored);
        if (file.isFile() && !ignore.orElse(false)) {
            this.fileList.add(file.getAbsolutePath());
            this.putArchiveEntry(new TarArchiveEntry(file, relativePath));
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            IOUtils.copy(bis, this);
            this.closeArchiveEntry();
            bis.close();
            return true;
        } else if (file.isDirectory() && addChildren(file, ignore)) {
            this.fileList.add(file.getAbsolutePath());
            this.putArchiveEntry(new TarArchiveEntry(file, relativePath));
            this.closeArchiveEntry();
            return true;
        }
        return false;
    }

    /**
     * @return true if any children were added.
     */
    private boolean addChildren(File file, Optional<Boolean> parentIgnored) throws IOException {
        boolean added = false;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (addFile(child, parentIgnored)) {
                    added = true;
                }
            }
        }
        return added;
    }

    /**
     * Check whether the file is ignored.
     * 1. Start with a specific pattern?
     * 2. End with a specific extension?
     * @param path file path
     * @return boolean - empty optional if there is no explicit match for ignore/allow; otherwise true/false.
     */
    private Optional<Boolean> isIgnoreFile(String path, Optional<Boolean> parentIgnored) {
        path = Util.normalizeFilename(path);
        // iterate rules in reverse as last entries should be prioritized
        for (int i = this.ignores.length - 1; i >= 0; i--) {
            IgnoreRule rule = this.ignores[i];
            if (path.matches(rule.getPattern())) {
                return Optional.of(rule.isIgnore());
            }
        }
        return parentIgnored;
    }

    /**
     * Check the name is in the common ignore list.
     * @param name filename
     * @return boolean
     */
    private boolean isCommonIgnore(String name) {
        return Constants.COMMON_IGNORE.indexOf(name) >= 0;
    }

    public static Compression.CompressibleWithIgnore compressToFile(String filename) throws IOException {
        return new CompressibleFileImpl(filename);
    }
}
