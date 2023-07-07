package com.intellij.openapi.vfs;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@SuppressWarnings("deprecation")
final public class DokkaCompactVirtualFileSetFactory implements VirtualFileSetFactory {
    @Override
    public @NotNull VirtualFileSet createCompactVirtualFileSet() {
        return new DokkaCompactVirtualFileSet();
    }

    @Override
    public @NotNull VirtualFileSet createCompactVirtualFileSet(@NotNull Collection<? extends VirtualFile> files) {
        return new DokkaCompactVirtualFileSet(files);
    }
}