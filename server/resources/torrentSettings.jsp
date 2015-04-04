<%@ include file="/include.jsp" %>
<jsp:useBean id="torrentConfigurator" type="jetbrains.buildServer.torrent.TorrentConfigurator" scope="request"/>
<jsp:useBean id="announcedTorrentsNum" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="connectedClientsNum" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="seededTorrentsNum" type="java.lang.Integer" scope="request"/>
<form method="post" action="<c:url value='/admin/torrentSettings.html'/>">
  <table class="runnerFormTable">
<tr>
  <th>Torrent tracker announce URL:</th>
  <td><c:out value="${torrentConfigurator.announceUrl}"/></td>
</tr>
    <c:if test="${torrentConfigurator.trackerEnabled}">
      <tr>
        <th>Torrent tracker information:</th>
        <td>
          <ul style="margin-top:0; padding-left: 1em;">
            <li>announced torrents: <strong>${announcedTorrentsNum}</strong></li>
            <li>connected/downloading clients: <strong>${connectedClientsNum}</strong></li>
            <li>number of currently seeded torrents: <strong>${seededTorrentsNum}
              (of ${torrentConfigurator.maxNumberOfSeededTorrents})</strong></li>
          </ul>
        </td>
      </tr>
    </c:if>
  <tr>
    <th>Settings:</th>
    <td>
      <div>
        <forms:checkbox name="transportEnabled" checked="${torrentConfigurator.transportEnabled}"/>
        <label for="transportEnabled">Use BitTorrent for artifact dependencies</label>
      </div>
      <div>
        <forms:checkbox name="downloadEnabled" checked="${torrentConfigurator.downloadEnabled}"/>
        <label for="downloadEnabled">Allow users to download artifacts via a BitTorrent client</label>
      </div>
    </td>
  </tr>
  <tr>
    <td colspan="2"><forms:submit label="Save" name="save"/></td>
  </tr>
  </table>
</form>