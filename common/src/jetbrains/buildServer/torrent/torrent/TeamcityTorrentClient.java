package jetbrains.buildServer.torrent.torrent;

import com.intellij.openapi.diagnostic.Logger;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.client.peer.SharingPeer;
import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.common.TorrentHash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static jetbrains.buildServer.torrent.torrent.TorrentUtil.isConnectionManagerInitialized;

public class TeamcityTorrentClient {
  private final static Logger LOG = Logger.getInstance(TeamcityTorrentClient.class.getName());

  @NotNull
  private final Client myClient;

  public TeamcityTorrentClient(ExecutorService es) {
    myClient = new Client(es);
  }

  public void start(@NotNull InetAddress[] inetAddresses, @Nullable final URI defaultTrackerURI, final int announceInterval) throws IOException {
    myClient.start(inetAddresses, announceInterval, defaultTrackerURI);
  }

  public void stop() {
    myClient.stop();
  }

  public boolean seedTorrent(@NotNull File torrentFile, @NotNull File srcFile) throws IOException, NoSuchAlgorithmException {
    Torrent torrent = loadTorrent(torrentFile);
    return seedTorrent(torrent, srcFile);
  }

  public Set<SharingPeer> getPeers() {
    return myClient.getPeers();
  }

  public boolean seedTorrent(@NotNull Torrent torrent, @NotNull File srcFile) {
    try {
      final SharedTorrent st = new SharedTorrent(torrent, srcFile.getParentFile(), false, true);
      myClient.addTorrent(st);
      return true;
    } catch (Exception e) {
      LOG.warn("Failed to seed file: " + srcFile.getName(), e);
      return false;
    }
  }

  public void stopSeeding(@NotNull File torrentFile) {
    try {
      Torrent t = loadTorrent(torrentFile);
      myClient.removeTorrent(t);
    } catch (IOException e) {
      LOG.warn(e.toString());
    } catch (NoSuchAlgorithmException e) {
      LOG.warn(e.toString());
    }
  }
  public void stopSeeding(@NotNull TorrentHash torrentHash) {
    myClient.removeTorrent(torrentHash);
  }

  public void stopSeedingByPath(File file){
    final SharedTorrent torrentByName = myClient.getTorrentByFilePath(file);
    if (torrentByName != null) {
      LOG.info("Stopped seeding torrent by file: " + file.getAbsolutePath());
      myClient.removeTorrent(torrentByName);
    }
 }

  public boolean isSeedingByPath(File file){
    final SharedTorrent torrentByName = myClient.getTorrentByFilePath(file);
    return torrentByName != null;
  }

  private Torrent loadTorrent(File torrentFile) throws IOException, NoSuchAlgorithmException {
    return Torrent.load(torrentFile);
  }

  public boolean isSeeding(@NotNull File torrentFile) {
    try {
      return isSeeding(loadTorrent(torrentFile));
    } catch (IOException e) {
    } catch (NoSuchAlgorithmException e) {
    }
    return false;
  }

  public boolean isSeeding(@NotNull TorrentHash torrent) {
    return findSeedingTorrentFolder(torrent) != null;
  }

  public File findSeedingTorrentFolder(@NotNull TorrentHash torrent){
    for (SharedTorrent st : myClient.getTorrents()) {
      if (st.getHexInfoHash().equals(torrent.getHexInfoHash())){
        return st.getParentFile();
      }
    }
    return null;
  }

  public void setAnnounceInterval(final int announceInterval){
    myClient.setAnnounceInterval(announceInterval);
  }

  public void setSocketTimeout(final int socketTimeoutSec) {
    if (!isConnectionManagerInitialized(myClient)) return;
    myClient.setSocketConnectionTimeout(socketTimeoutSec, TimeUnit.SECONDS);
  }

  public void setCleanupTimeout(final int cleanupTimeoutSec) {
    if (!isConnectionManagerInitialized(myClient)) return;
    myClient.setCleanupTimeout(cleanupTimeoutSec, TimeUnit.SECONDS);
  }

  public void setMaxIncomingConnectionsCount(int maxIncomingConnectionsCount) {
    if (!isConnectionManagerInitialized(myClient)) return;
    myClient.setMaxInConnectionsCount(maxIncomingConnectionsCount);
  }

  public void setMaxOutgoingConnectionsCount(int maxOutgoingConnectionsCount) {
    if (!isConnectionManagerInitialized(myClient)) return;
    myClient.setMaxOutConnectionsCount(maxOutgoingConnectionsCount);
  }

  public int getNumberOfSeededTorrents() {
    return myClient.getTorrents().size();
  }

  public Thread downloadAndShareOrFailAsync(@NotNull final Torrent torrent,
                                            @NotNull final File destFile,
                                            @NotNull final File destDir,
                                            final long downloadTimeoutSec,
                                            final int minSeedersCount,
                                            final AtomicBoolean isInterrupted,
                                            final AtomicReference<Exception> occuredException) throws IOException, NoSuchAlgorithmException, InterruptedException {
    final Thread thread = new Thread(new Runnable() {
      public void run() {
        try {
          downloadAndShareOrFail(torrent, destFile, destDir, downloadTimeoutSec, minSeedersCount,isInterrupted);
        } catch (IOException e) {
          occuredException.set(e);
        } catch (NoSuchAlgorithmException e) {
          occuredException.set(e);
        } catch (InterruptedException e) {
          occuredException.set(e);
        }
      }
    });
    thread.start();
    return thread;
  }

  public void downloadAndShareOrFail(@NotNull final Torrent torrent,
                                     @NotNull final File destFile,
                                     @NotNull final File destDir,
                                     final long downloadTimeoutSec,
                                     final int minSeedersCount,
                                     final AtomicBoolean isInterrupted) throws IOException, NoSuchAlgorithmException, InterruptedException {
    boolean torrentContainsFile = false;
    for (String filePath : torrent.getFilenames()) {
      final String destFileAbsolutePath = destFile.getAbsolutePath();
      final String destFileCleaned = destFileAbsolutePath.replaceAll("\\\\", "/");
      final String filePathCleaned = filePath.replaceAll("\\\\", "/");
      if (destFileCleaned.endsWith(filePathCleaned)){
        torrentContainsFile = true;
        break;
      }
    }
    if (!torrentContainsFile){
      throw new IOException("File not found in torrent");
    }

    destDir.mkdirs();
    if (myClient.containsTorrentWithHash(torrent.getHexInfoHash())){
      LOG.info("Already seeding torrent with hash " + torrent.getHexInfoHash() + ". Stop seeding and try download again");
      stopSeeding(torrent);
    }
    SharedTorrent downTorrent = new SharedTorrent(torrent, destDir, false);
    LOG.info(String.format("Will attempt to download uninterruptibly %s into %s. Timeout:%d",
            destFile.getAbsolutePath(), destDir.getAbsolutePath(), downloadTimeoutSec));
    myClient.downloadUninterruptibly(downTorrent, downloadTimeoutSec, minSeedersCount, isInterrupted);
  }

  public Collection<SharedTorrent> getSharedTorrents(){
    return myClient.getTorrents();
  }
}
