/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.torrent;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.torrent.torrent.TorrentUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UnusedTorrentFilesRemoverImpl implements UnusedTorrentFilesRemover {

  private final static Logger LOG = Logger.getInstance(UnusedTorrentFilesRemoverImpl.class.getName());

  private final FileRemover myFileRemover;
  private final FileWalker myFileWalker;

  public UnusedTorrentFilesRemoverImpl(FileRemover fileRemover, FileWalker fileWalker) {
    myFileRemover = fileRemover;
    myFileWalker = fileWalker;
  }

  @Override
  public void removeUnusedTorrents(List<BuildArtifact> artifacts, Path torrentsDir) {
    Collection<File> torrentsForRemoving = new ArrayList<>();
    Set<String> expectedTorrentPathsForArtifacts = artifacts
            .stream()
            .map(it -> it.getRelativePath() + TorrentUtil.TORRENT_FILE_SUFFIX)
            .collect(Collectors.toSet());
    try {
      myFileWalker.walkFileTree(torrentsDir, new SimpleFileVisitor<Path>() {
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
          Path relativePath = torrentsDir.relativize(path);
          if (expectedTorrentPathsForArtifacts.contains(relativePath.toString())) {
            return FileVisitResult.CONTINUE;
          }
          torrentsForRemoving.add(path.toFile());
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      LOG.warnAndDebugDetails("failed walk torrent files tree for removing useless torrents", e);
    }
    torrentsForRemoving.forEach(myFileRemover::remove);
  }
}
